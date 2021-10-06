/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rustPerformanceTests

import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInspection.ex.InspectionToolRegistrar
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import org.rust.ide.annotator.AnnotatorBase
import org.rust.ide.annotator.RsErrorAnnotator
import org.rust.ide.experiments.RsExperiments
import org.rust.ide.inspections.RsLocalInspectionTool
import org.rust.ide.inspections.RsUnresolvedReferenceInspection
import org.rust.ide.inspections.lints.RsUnusedImportInspection
import org.rust.lang.RsFileType
import org.rust.lang.core.macros.MacroExpansionScope
import org.rust.lang.core.macros.macroExpansionManager
import org.rust.lang.core.psi.RsFile
import org.rust.openapiext.runWithEnabledFeatures
import org.rust.openapiext.toPsiFile

open class RsRealProjectAnalysisTest : RsRealProjectTestBase() {

    /** Don't run it on Rustc! It's a kind of stress-test */
    fun `test analyze rustc`() = doTest(RUSTC)

    fun `test analyze Cargo`() = doTest(CARGO)
    fun `test analyze mysql_async`() = doTest(MYSQL_ASYNC)
    fun `test analyze tokio`() = doTest(TOKIO)
    fun `test analyze amethyst`() = doTest(AMETHYST)
    fun `test analyze clap`() = doTest(CLAP)
    fun `test analyze diesel`() = doTest(DIESEL)
    fun `test analyze rust_analyzer`() = doTest(RUST_ANALYZER)
    fun `test analyze xi_editor`() = doTest(XI_EDITOR)
    fun `test analyze juniper`() = doTest(JUNIPER)

    private val earlyTestRootDisposable = Disposer.newDisposable()

    protected fun doTest(info: RealProjectInfo, failOnFirstFileWithErrors: Boolean = false) {
        val errorConsumer = if (failOnFirstFileWithErrors) FAIL_FAST else COLLECT_ALL_EXCEPTIONS
        runWithEnabledFeatures(RsExperiments.EVALUATE_BUILD_SCRIPTS, RsExperiments.PROC_MACROS) {
            doTest(info, errorConsumer)
        }
    }

    protected fun doTest(info: RealProjectInfo, consumer: AnnotationConsumer) {
        Disposer.register(
            earlyTestRootDisposable,
            project.macroExpansionManager.setUnitTestExpansionModeAndDirectory(MacroExpansionScope.ALL, name)
        )
        AnnotatorBase.enableAnnotator(RsErrorAnnotator::class.java, testRootDisposable)
        setUpInspections()

        println("Opening the project `${info.name}`")
        val base = openRealProject(info) ?: return

        println("Collecting files to analyze")

        val baseDirToCheck = if (info.name == STDLIB) rustupFixture.stdlib!! else base

        val filesToCheck = baseDirToCheck.findDescendants {
            it.fileType == RsFileType && run {
                val file = it.toPsiFile(project)
                file is RsFile && file.crateRoot != null && file.cargoWorkspace != null
            }
        }
        for (file in filesToCheck) {
            val path = if (VfsUtil.isAncestor(base, file, true)) {
                file.path.substring(base.path.length + 1)
            } else {
                file.path
            }
            println("Analyzing $path")
            myFixture.openFileInEditor(file)
            val infos = myFixture.doHighlighting(HighlightSeverity.ERROR)
            val text = myFixture.editor.document.text
            for (highlightInfo in infos) {
                val position = myFixture.editor.offsetToLogicalPosition(highlightInfo.startOffset)
                val annotation = Annotation(
                    path,
                    // Use 1-based indexing to be compatible with editor UI and `Go to Line/Column` action
                    position.line + 1,
                    position.column + 1,
                    text.substring(highlightInfo.startOffset, highlightInfo.endOffset),
                    highlightInfo.description,
                    highlightInfo.inspectionToolId
                )
                consumer.consumeAnnotation(annotation)
            }
        }
        consumer.finish()
    }

    private fun setUpInspections() {
        val inspections = InspectionToolRegistrar.getInstance().createTools()
            .map { it.tool }
            .filterIsInstance<RsLocalInspectionTool>()

        for (inspection in inspections) {
            setUpInspection(inspection)
        }

        myFixture.enableInspections(*inspections.toTypedArray())

        // Make some inspections with "WARNING" level to be also included in report
        val profile = InspectionProjectProfileManager.getInstance(project).currentProfile
        for (inspection in forciblyEnabledInspections) {
            profile.getTools(inspection, project).level = HighlightDisplayLevel.ERROR
        }
    }

    protected open fun setUpInspection(inspection: RsLocalInspectionTool) {
        when (inspection) {
            is RsUnresolvedReferenceInspection -> inspection.ignoreWithoutQuickFix = false
        }
    }

    override fun tearDown() {
        Disposer.dispose(earlyTestRootDisposable)
        super.tearDown()
    }

    companion object {

        private const val STDLIB = "stdlib"

        val FAIL_FAST = object : AnnotationConsumer {
            override fun consumeAnnotation(annotation: Annotation) {
                error(annotation.toString())
            }
            override fun finish() {}
        }

        val COLLECT_ALL_EXCEPTIONS = object : AnnotationConsumer {

            val annotations = mutableListOf<Annotation>()

            override fun consumeAnnotation(annotation: Annotation) {
                annotations += annotation
            }

            override fun finish() {
                if (annotations.isNotEmpty()) {
                    error("Error annotations found:\n\n" + annotations.joinToString("\n\n"))
                }
            }
        }
    }

    interface AnnotationConsumer {
        fun consumeAnnotation(annotation: Annotation)
        fun finish()
    }

    data class Annotation(
        val filePath: String,
        val line: Int,
        val column: Int,
        val highlightedText: String,
        val error: String?,
        val inspectionToolId: String?
    ) {
        override fun toString(): String {
            val suffix = if (inspectionToolId != null) " by $inspectionToolId" else ""
            return "$filePath:$line:$column '$highlightedText' ($error)$suffix"
        }
    }

    /** Inspections with "WARNING" level should be added to this list to be checked */
    private val forciblyEnabledInspections: List<String> = listOf(RsUnusedImportInspection.SHORT_NAME)
}
