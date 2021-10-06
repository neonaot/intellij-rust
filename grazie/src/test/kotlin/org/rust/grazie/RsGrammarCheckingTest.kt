/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

// BACKCOMPAT: 2021.1
@file:Suppress("DEPRECATION")

package org.rust.grazie

import com.intellij.grazie.GrazieConfig
import com.intellij.grazie.ide.inspection.grammar.GrazieInspection
import com.intellij.grazie.ide.language.LanguageGrammarChecking
import com.intellij.grazie.jlanguage.Lang
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.ThrowableRunnable
import org.intellij.lang.annotations.Language
import org.rust.ide.annotator.RsAnnotationTestFixture
import org.rust.ide.inspections.RsInspectionsTestBase
import org.rust.lang.RsLanguage

class RsGrammarCheckingTest : RsInspectionsTestBase(GrazieInspection::class) {

    override fun createAnnotationFixture(): RsAnnotationTestFixture<Unit> =
        RsAnnotationTestFixture(this, myFixture, inspectionClasses = listOf(inspectionClass), baseFileName = "lib.rs")

    override fun runTestRunnable(testRunnable: ThrowableRunnable<Throwable>) {
        // TODO: find out why `org.languagetool.JLanguageTool.getBuildDate(JLanguageTool.java:134)` fails
        //  during `GrazieInitializerManager` initialization
        if (ApplicationInfo.getInstance().build < BUILD_213) {
            super.runTestRunnable(testRunnable)
        }
    }

    override fun setUp() {
        super.setUp()
        // TODO: find out why `org.languagetool.JLanguageTool.getBuildDate(JLanguageTool.java:134)` fails
        //  during `GrazieInitializerManager` initialization
        if (ApplicationInfo.getInstance().build >= BUILD_213) return

        val strategy = LanguageGrammarChecking.getStrategies().first { it is RsGrammarCheckingStrategy }
        val currentState = GrazieConfig.get()
        if (strategy.getID() !in currentState.enabledGrammarStrategies || currentState.enabledLanguages != enabledLanguages) {
            updateSettings { state ->
                val checkingContext = state.checkingContext
                state.copy(
                    enabledGrammarStrategies = state.enabledGrammarStrategies + strategy.getID(),
                    enabledLanguages = enabledLanguages,
                    checkingContext = checkingContext.copy(enabledLanguages = checkingContext.enabledLanguages + RsLanguage.id)
                )
            }
        }
        Disposer.register(testRootDisposable) {
            updateSettings { currentState }
        }
    }

    fun `test check literals`() = doTest("""
        fn foo() {
            let literal = "It is <TYPO>friend</TYPO> of human";
            let raw_literal = r"It is <TYPO>friend</TYPO> of human";
            let binary_literal = b"It is <TYPO>friend</TYPO> of human";
        }
    """, checkInStringLiterals = true)

    fun `test check comments`() = doTest("""
        fn foo() {
            // It is <TYPO>friend</TYPO> of human
            /* It is <TYPO>friend</TYPO> of human */
            let literal = 123;
        }
    """, checkInComments = true)

    // https://github.com/intellij-rust/intellij-rust/issues/7024
    fun `test check single sentence in sequential comments 1`() = doTest("""
        fn main() {
            // Path to directory where someone <TYPO>write</TYPO>
            // and from where someone reads
            let path1 = "/foo/bar";
            /* Path to directory where someone <TYPO>write</TYPO> */
            /* and from where someone reads */
            let path2 = "/foo/bar";
        }
    """, checkInComments = true)

    // https://github.com/intellij-rust/intellij-rust/issues/7024
    fun `test check single sentence in sequential comments 2`() = doTest("""
        fn main() {
            // Path to directory where someone writes

            // <TYPE>and</TYPE> from where someone reads
            let path = "/foo/bar";
        }
    """, checkInComments = true)

    fun `test check doc comments`() = doTest("""
        /// It is <TYPO>friend</TYPO> of human
        mod foo {
            //! It is <TYPO>friend</TYPO> of human

            /** It is <TYPO>friend</TYPO> of human */
            fn bar() {}
        }
    """, checkInDocumentation = true)

    fun `test check injected Rust code in doc comments`() = doTest("""
        ///
        /// ```
        /// let literal = "It is <TYPO>friend</TYPO> of human";
        /// for i in 1..10 {}
        /// ```
        pub fn foo() {}
    """, checkInStringLiterals = true)

    fun `test no typos in injected Rust code in doc comments`() = doTest("""
        ///
        /// ```
        /// foo!(There is two apples);
        /// ```
        pub fn foo() {}
    """, checkInDocumentation = true)

    private fun doTest(
        @Language("Rust") text: String,
        checkInStringLiterals: Boolean = false,
        checkInComments: Boolean = false,
        checkInDocumentation: Boolean = false
    ) {
        updateSettings { state ->
            val newContext = state.checkingContext.copy(
                isCheckInStringLiteralsEnabled = checkInStringLiterals,
                isCheckInCommentsEnabled = checkInComments,
                isCheckInDocumentationEnabled = checkInDocumentation
            )
            state.copy(checkingContext = newContext)
        }
        checkByText(text)

        updateSettings { state ->
            val newContext = state.checkingContext.copy(
                isCheckInStringLiteralsEnabled = false,
                isCheckInCommentsEnabled = false,
                isCheckInDocumentationEnabled = false
            )
            state.copy(checkingContext = newContext)
        }

        checkByText(text.replace("<TYPO.*?>(.*?)</TYPO>".toRegex(), "$1"))
    }

    private fun updateSettings(change: (GrazieConfig.State) -> GrazieConfig.State) {
        GrazieConfig.update(change)
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    }

    companion object {
        private val enabledLanguages = setOf(Lang.AMERICAN_ENGLISH)
        private val BUILD_213: BuildNumber = BuildNumber.fromString("213")!!
    }
}
