/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rustSlowTests

import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.util.PsiTreeUtil
import org.rust.RsTestBase
import org.rust.WithStdlibRustProjectDescriptor
import org.rust.lang.RsFileType
import kotlin.system.measureTimeMillis

class RsCompilerSourcesPerfTest : RsTestBase() {
    override fun getProjectDescriptor() = WithStdlibRustProjectDescriptor


    // Use this function to check that some code does not blow up
    // on some strange real-world PSI.
//    fun `test anything`() = forAllPsiElements { element ->
//        if (element is RsNamedElement) {
//            check(element.name != null)
//        }
//    }

    fun `test parsing standard library sources`() {
        val sources = rustSrcDir()
        parseRustFiles(
            sources,
            ignored = setOf("test", "doc", "etc", "grammar"),
            expectedNumberOfFiles = 500,
            checkForErrors = true
        )
    }

    private data class FileStats(
        val path: String,
        val time: Long,
        val fileLength: Int
    )

    private fun parseRustFiles(directory: VirtualFile,
                               ignored: Collection<String>,
                               expectedNumberOfFiles: Int,
                               checkForErrors: Boolean) {
        val processed = mutableListOf<FileStats>()
        val errors = mutableListOf<String>()
        val totalTime = measureTimeMillis {
            VfsUtilCore.visitChildrenRecursively(directory, object : VirtualFileVisitor<Void>() {
                override fun visitFileEx(file: VirtualFile): Result {
                    if (file.isDirectory && file.name in ignored) return SKIP_CHILDREN
                    if (file.fileType != RsFileType) return CONTINUE
                    val fileContent = file.loadText()

                    val time = measureTimeMillis {
                        val psi = PsiFileFactory.getInstance(project).createFileFromText(file.name, file.fileType, fileContent)
                        val psiString = DebugUtil.psiToString(psi, /* skipWhitespace = */ true)

                        if (checkForErrors) {
                            if ("PsiErrorElement" in psiString) {
                                errors += file.path
                            }
                        }
                    }

                    val relPath = FileUtil.getRelativePath(directory.path, file.path, '/')!!
                    processed += FileStats(relPath, time, fileContent.length)
                    return CONTINUE
                }
            })
        }
        if (checkForErrors) {
            check(errors.isEmpty()) {
                "Failed to parse:\n${errors.joinToString("\n")}"
            }
        }
        check(processed.size > expectedNumberOfFiles)

        reportTeamCityMetric("$name totalTime", totalTime)

        println("\n$name " +
            "\nTotal: ${totalTime}ms" +
            "\nFileTree: ${processed.size}")
        val slowest = processed.sortedByDescending { it.time }.take(5)
        println("\nSlowest files")
        for ((path, time, fileLength) in slowest) {
            println("${"%3d".format(time)}ms ${"%3d".format(fileLength / 1024)}kb: $path")
        }
        println()
    }

    @Suppress("unused")
    private fun forAllPsiElements(f: (PsiElement) -> Unit) {
        VfsUtilCore.visitChildrenRecursively(rustSrcDir(), object : VirtualFileVisitor<Void>() {
            override fun visitFileEx(file: VirtualFile): Result {
                if (file.fileType != RsFileType) return CONTINUE
                val fileContent = file.loadText()

                val psi = PsiFileFactory.getInstance(project).createFileFromText(file.name, file.fileType, fileContent)

                PsiTreeUtil.processElements(psi) {
                    f(it)
                    true
                }
                return CONTINUE
            }
        })
    }

    private fun rustSrcDir(): VirtualFile = projectDescriptor.stdlib!!

    private fun VirtualFile.loadText(): CharSequence = LoadTextUtil.loadText(this)
}
