/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.coverage

import com.intellij.coverage.CoverageDataManager
import com.intellij.coverage.CoverageRunnerData
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.rt.coverage.data.ClassData
import com.intellij.rt.coverage.data.LineData
import com.intellij.rt.coverage.data.ProjectData
import org.rust.FileTreeBuilder
import org.rust.lang.core.psi.isRustFile
import org.rust.openapiext.toPsiFile
import org.rustSlowTests.cargo.runconfig.RunConfigurationTestBase

class RsCoverageTest : RunConfigurationTestBase() {
    private val coverageData: ProjectData?
        get() = CoverageDataManager.getInstance(project).currentSuitesBundle?.coverageData

    override fun shouldRunTest(): Boolean {
        if (!super.shouldRunTest()) return false
        return System.getenv("CI") == null
    }

    fun `test main`() = doTest {
        toml("Cargo.toml", """
            [package]
            name = "hello"
            version = "0.1.0"
            authors = []
        """)

        dir("src") {
            rust("main.rs", """
                fn main() {                     // Hits: 1
                    println!("Hello, world!");  // Hits: 1
                }                               // Hits: 1
            """)
        }
    }

    fun `test function call`() = doTest {
        toml("Cargo.toml", """
            [package]
            name = "hello"
            version = "0.1.0"
            authors = []
        """)

        dir("src") {
            rust("main.rs", """
                fn foo() {                      // Hits: 1
                    println!("Hello, world!");  // Hits: 1
                }                               // Hits: 1

                fn main() {                     // Hits: 1
                    foo();                      // Hits: 1
                }                               // Hits: 1
            """)
        }
    }

    fun `test function call from lib`() = doTest {
        toml("Cargo.toml", """
            [package]
            name = "hello"
            version = "0.1.0"
            authors = []
            edition = "2018"
        """)

        dir("src") {
            rust("lib.rs", """
                pub fn foo() {                  // Hits: 1
                    println!("Hello, world!");  // Hits: 1
                }                               // Hits: 1
            """)

            rust("main.rs", """
                use hello::foo;

                fn main() {                     // Hits: 1
                    foo();                      // Hits: 1
                }                               // Hits: 1
            """)
        }
    }

    fun `test for`() = doTest {
        toml("Cargo.toml", """
            [package]
            name = "hello"
            version = "0.1.0"
            authors = []
        """)

        dir("src") {
            rust("main.rs", """
                fn main() {                         // Hits: 1
                    for i in 1..5 {                 // Hits: 5
                        println!("Hello, world!");  // Hits: 4
                    }
                }                                   // Hits: 1
            """)
        }
    }

    fun `test for function call`() = doTest {
        toml("Cargo.toml", """
            [package]
            name = "hello"
            version = "0.1.0"
            authors = []
        """)

        dir("src") {
            rust("main.rs", """
                fn foo() {                      // Hits: 4
                    println!("Hello, world!");  // Hits: 4
                }                               // Hits: 4

                fn main() {                     // Hits: 1
                    for _ in 1..5 {             // Hits: 5
                        foo();                  // Hits: 4
                    }
                }                               // Hits: 1
            """)
        }
    }

    fun `test tests`() {
        if (SystemInfo.isWindows) return // https://github.com/mozilla/grcov/issues/462

        doTest(runTests = true) {
            toml("Cargo.toml", """
                [package]
                name = "hello"
                version = "0.1.0"
                authors = []
            """)

            dir("src") {
                rust("lib.rs", """
                    fn foo() {                      // Hits: 2
                        println!("Hello, world!");  // Hits: 2
                    }                               // Hits: 2

                    #[test]
                    fn test1() {                    // Hits: 2
                        foo();                      // Hits: 1
                    }                               // Hits: 2

                    #[test]
                    fn test2() {                    // Hits: 2
                        foo();                      // Hits: 1
                    }                               // Hits: 2
                """)
            }
        }
    }

    fun `test proc macro tests`() {
        if (SystemInfo.isWindows) return // https://github.com/mozilla/grcov/issues/462

        doTest(runTests = true) {
            toml("Cargo.toml", """
                [package]
                name = "hello"
                version = "0.1.0"
                authors = []

                [lib]
                proc-macro = true
            """)

            dir("src") {
                rust("lib.rs", """
                    fn foo() {                      // Hits: 2
                        println!("Hello, world!");  // Hits: 2
                    }                               // Hits: 2

                    #[test]
                    fn test1() {                    // Hits: 2
                        foo();                      // Hits: 1
                    }                               // Hits: 2

                    #[test]
                    fn test2() {                    // Hits: 2
                        foo();                      // Hits: 1
                    }                               // Hits: 2
                """)
            }
        }
    }

    fun `test panic`() = doTest {
        toml("Cargo.toml", """
            [package]
            name = "hello"
            version = "0.1.0"
            authors = []
        """)

        dir("src") {
            rust("main.rs", """
                fn main() {          // Hits: 1
                    let x = 1;       // Hits: 1
                    let y = 2;       // Hits: 1
                    panic!("fail");  // Hits: 1
                    let z = 3;
                }
            """)
        }
    }

    private fun doTest(runTests: Boolean = false, builder: FileTreeBuilder.() -> Unit) {
        buildProject(builder)

        val expected = hashMapOf<String, Set<Hits>>()
        VfsUtil.iterateChildrenRecursively(cargoProjectDirectory, null) { fileOrDir ->
            if (!fileOrDir.isRustFile) return@iterateChildrenRecursively true
            val psiFile = fileOrDir.toPsiFile(project) ?: return@iterateChildrenRecursively true
            expected[fileOrDir.path] = hitsFrom(psiFile.text)
            true
        }

        val configuration = createConfiguration(if (runTests) "test" else "run")

        executeWithCoverage(configuration)
        runWithInvocationEventsDispatching("Failed to fetch coverage data", retries = 10000) { coverageData != null }
        val projectData = coverageData!!

        val actual = hashMapOf<String, Set<Hits>>()
        for (path in expected.keys) {
            val data = projectData.getClassData(path)!!
            actual[path] = hitsFrom(data)
        }

        assertEquals(expected, actual)
    }

    private fun executeWithCoverage(configuration: RunConfiguration): RunContentDescriptor {
        val result = execute(configuration) { runnerSettings(CoverageRunnerData()) }
        return result.descriptor ?: error("Failed to get `RunContentDescriptor` object")
    }

    private fun hitsFrom(text: String): Set<Hits> =
        text.split('\n')
            .withIndex()
            .filter { it.value.contains(MARKER) }
            .map {
                val lineNumber = it.index + 1
                val startIndex = it.value.indexOf(MARKER) + MARKER.length
                val rawHits = it.value.substring(startIndex).trim()
                val hits = Integer.parseInt(rawHits)
                Hits(lineNumber, hits)
            }.toSortedSet()

    private fun hitsFrom(data: ClassData): Set<Hits> =
        data.lines
            .filterIsInstance<LineData>()
            .map { Hits(it.lineNumber, it.hits) }
            .toSortedSet()

    private data class Hits(val lineNumber: Int, val count: Int) : Comparable<Hits> {
        override fun compareTo(other: Hits): Int {
            val result = lineNumber.compareTo(other.lineNumber)
            if (result != 0) return result
            return count.compareTo(other.count)
        }
    }

    private companion object {
        private const val MARKER: String = "// Hits:"
    }
}
