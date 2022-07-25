/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.spellchecker

import com.intellij.spellchecker.inspections.SpellCheckingInspection
import org.intellij.lang.annotations.Language
import org.rust.ide.inspections.RsInspectionsTestBase

class RsSpellCheckerTest : RsInspectionsTestBase(SpellCheckingInspection::class) {

    fun `test comments`() = doTest("""// Hello, <TYPO descr="Typo: In word 'Wodrl'">Wodrl</TYPO>!""")

    fun `test string literals`() = doTest("""
        fn main() {
            let s = "Hello, <TYPO descr="Typo: In word 'Wodlr'">Wodlr</TYPO>!";
            let invalid_escape = "aadsds\z";
        }
    """)

    fun `test comments suppressed`() = doTest("// Hello, Wodrl!", processComments = false)

    fun `test string literals suppressed`() = doTest("""
        fn main() {
            let s = "Hello, Wodlr!";
        }
    """, processLiterals = false)

    fun `test string literals with escapes`() = doTest("""
        fn main() {
            let s = "Hello, <TYPO>W\u{6F}dlr</TYPO>!";
            let s = "Hello, <TYPO>W\x6Fdlr</TYPO>!";
        }
    """)

    fun `test raw identifiers`() = doTest("""
        fn r#<TYPO>wodrl</TYPO>() {}
    """)

    fun `test lifetimes`() = doTest("""
        const FOO: & 'static str = "123";
        fn foo<'<TYPO>wodrl</TYPO>>(x: &'<TYPO>wodrl</TYPO> str) {}
    """)

    fun `test do not highlight word from rust bundled dictionary`() = doTest("""
        pub struct Bar;
        impl<T> Deref for Bar {
            type Target = ();
            fn deref(&self) -> &Self::Target { unimplemented!() }
        }
    """)

    // https://youtrack.jetbrains.com/issue/CPP-28113
    fun `test do not highlight word from rust bundled dictionary 2`() = doTest("""
        fn foo(addr: *const usize) {}
    """)

    private fun doTest(@Language("Rust") text: String, processComments: Boolean = true, processLiterals: Boolean = true) {
        (inspection as SpellCheckingInspection).processLiterals = processLiterals
        (inspection as SpellCheckingInspection).processComments = processComments
        checkByText(text, checkWarn = false, checkWeakWarn = true, checkInfo = false)
    }
}
