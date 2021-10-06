/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.intellij.lang.annotations.Language
import org.rust.ExpandMacros
import org.rust.TestProject
import org.rust.fileTreeFromText
import org.rust.lang.core.macros.MacroExpansionManagerImpl.Testmarks
import org.rust.lang.core.psi.RsFile
import org.rust.lang.core.psi.RsMacroCall
import org.rust.lang.core.psi.ext.childrenOfType
import org.rust.lang.core.psi.ext.expansion
import org.rust.lang.core.psi.ext.stubChildrenOfType
import org.rust.lang.core.psi.ext.stubDescendantsOfTypeOrSelf
import org.rust.openapiext.Testmark
import org.rust.openapiext.TestmarkPred
import org.rust.openapiext.not

@ExpandMacros
class RsMacroExpansionCachingTest : RsMacroExpansionTestBase() {
    private fun type(text: String = "a"): () -> Unit = {
        myFixture.type(text)
        PsiDocumentManager.getInstance(project).commitAllDocuments()
    }

    private fun replaceInFile(path: String, find: String, replace: String): (TestProject) -> Unit = { p ->
        val file = p.file(path)
        runWriteAction {
            VfsUtil.saveText(file, VfsUtil.loadText(file).replace(find, replace))
        }
    }

    private fun List<RsMacroCall>.collectStamps(): Map<String, Long> =
        associate {
            val expansion = it.expansion ?: error("failed to expand macro ${it.path.referenceName.orEmpty()}!")
            val first = expansion.elements.firstOrNull()
                ?: error("Macro expanded to empty ${it.path.referenceName.orEmpty()}!")
            check(first.isValid)
            it.path.referenceName.orEmpty() to expansion.file.virtualFile.timeStamp
        }

    private fun PsiFile.collectMacros(): List<RsMacroCall> =
        stubChildrenOfType<RsMacroCall>().flatMap { macroCall ->
            val calls = macroCall.expansion?.elements
                ?.flatMap<RsExpandedElement, RsMacroCall> { it.stubDescendantsOfTypeOrSelf() }
            listOf(macroCall) + calls.orEmpty()
        }

    private fun checkReExpandedInner(
        testmark: TestmarkPred?,
        action: () -> Unit,
        @Language("Rust") code: String,
        vararg names: String
    ) {
        InlineFile(code).withCaret()
        val oldStamps = myFixture.file.collectMacros().collectStamps()
        testmark?.checkHit { action() } ?: action()
        val collectMacros = myFixture.file.collectMacros()
        val changed = collectMacros.collectStamps().entries
            .filter { oldStamps[it.key] != it.value }
            .map { it.key }
        check(changed == names.toList()) {
            "Expected to re-expand ${names.asList()}, re-expanded $changed instead"
        }
    }

    private fun checkReExpanded(action: () -> Unit, @Language("Rust") code: String, vararg names: String) =
        checkReExpandedInner(null, action, code, *names)

    private fun TestmarkPred.checkReExpanded(action: () -> Unit, @Language("Rust") code: String, vararg names: String) =
        checkReExpandedInner(this, action, code, *names)

    private fun checkReExpandedTree(
        action: (p: TestProject) -> Unit,
        @Language("Rust") code: String,
        names: List<String>
    ) {
        val p = fileTreeFromText(code).create()
        val file = p.psiFile("main.rs") as RsFile
        checkAstNotLoaded()
        val oldStamps = file.collectMacros().collectStamps()
        action(p)
        assertNotNull(file.stub)
        val changed = file.collectMacros().collectStamps().entries
            .filter { oldStamps[it.key] != it.value }
            .map { it.key }
        check(changed == names) {
            "Expected to re-expand $names, re-expanded $changed instead"
        }
    }

    private fun checkReExpandedTree(
        action: (p: TestProject) -> Unit,
        @Language("Rust") code: String,
        names: List<String>,
        mark: Testmark
    ) = mark.checkHit { checkReExpandedTree(action, code, names) }

    private fun checkExpansionAfterAction(
        action: () -> Unit,
        @Language("Rust") code: String,
        @Language("Rust") vararg expectedExpansions: String
    ) {
        InlineFile(code)

        action()

        checkAllMacroExpansionsInFile(myFixture.file, expectedExpansions.map { Pair<String, Testmark?>(it, null) }.toTypedArray())
    }

    fun `test edit call`() = Testmarks.refsRecover.not().checkReExpanded(type(), """
        macro_rules! foo { ($ i:ident) => { mod $ i {} } }
        macro_rules! bar { ($ i:ident) => { mod $ i {} } }
        foo!(a/*caret*/);
        bar!(a);
    """, "foo")

    fun `test edit def 1`() = Testmarks.refsRecover.not().checkReExpanded(type(), """
        macro_rules! foo { ($ i:ident) => { mod $ i {/*caret*/} } }
        macro_rules! bar { ($ i:ident) => { mod $ i {} } }
        foo!(a);
        bar!(a);
    """, "foo")

    fun `test edit def 2`() = Testmarks.refsRecover.not().checkReExpanded(type(), """
        macro_rules! foo { ($ i:ident) => { mod $ i {/*caret*/} } }
        macro_rules! bar { ($ i:ident) => { mod $ i {} } }
        macro_rules! if_std { ($ i:item) => { $ i } }
        foo!(a);
        if_std! { if_std! { if_std! { foo!(a); } } }
        bar!(a);
    """, "foo")

    fun `test edit def 3`() = checkReExpanded(type(), """
        macro_rules! foo { ($ i:ident) => { mod $ i {} } }
        macro_rules! bar { () => { foo!(a/*caret*/); } }
        bar!();
    """, "bar", "foo")

    fun `test edit def 4`() = Testmarks.refsRecover.not().checkReExpanded(type(), """
        macro_rules! foo { () => { mod a/*caret*/ {} } }
        macro_rules! bar { () => { foo!(); } }
        bar!();
    """, "foo")

    fun `test stub call 1`() = checkReExpandedTree(replaceInFile("main.rs", "aaa", "aab"), """
    //- main.rs
        macro_rules! foo { ($ i:ident) => { mod $ i {} } }
        macro_rules! bar { ($ i:ident) => { mod $ i {} } }
        foo!(aaa);
        bar!(bbb);
    """, listOf("foo"), Testmarks.refsRecover)

    fun `test stub call 2`() = checkReExpandedTree(replaceInFile("main.rs", "//", ""), """
    //- main.rs
        macro_rules! foo { ($ i:ident) => { mod $ i {} } }
        macro_rules! bar { ($ i:ident) => { mod $ i {} } }
        foo!(aaa);
        //bar!(bbb);
    """, listOf("bar"), Testmarks.refsRecoverNotHit)

    fun `test add a call`() = checkExpansionAfterAction(type("\b\b\b"), """
        macro_rules! foo {
            () => { mod foo {} }
        }
        // /*caret*/foo!();
    """, """
        mod foo {}
    """)

    fun `test add a second call 1`() = checkExpansionAfterAction(type("foo!(bar);"), """
        macro_rules! foo {
            () => { mod foo {} }
            ($ i:ident) => { mod $ i {} }
        }
        foo!();
        /*caret*/
    """, """
        mod foo {}
    """, """
        mod bar {}
    """)

    fun `test add a second call 2`() = checkExpansionAfterAction(type("\b\b\b"), """
        macro_rules! foo {
            () => { mod foo {} }
            ($ i:ident) => { mod $ i {} }
        }
        foo!();
        // /*caret*/foo!(bar);
    """, """
        mod foo {}
    """, """
        mod bar {}
    """)

    fun `test remove a macro`() = checkExpansionAfterAction(type("//"), """
        macro_rules! foo {
            () => { mod foobar {} }
            ($ i:ident) => { mod $ i {} }
        }
        /*caret*/foo!();
        foo!(a);
    """, """
        mod a {}
    """)

    fun `test edit-save-reload document`() = checkExpansionAfterAction({
        Testmarks.refsRecover.checkNotHit {
            myFixture.type("\b\b\b")
            PsiDocumentManager.getInstance(project).commitAllDocuments()

            FileDocumentManager.getInstance().saveAllDocuments()
            FileDocumentManager.getInstance().reloadFromDisk(myFixture.getDocument(myFixture.file))
        }

        Testmarks.stubBasedLookup.checkHit {
            myFixture.file.childrenOfType<RsMacroCall>()
                .forEach { it.expansion }
        }
    }, """
        macro_rules! foo {
            () => { mod foo {} }
            ($ i:ident) => { mod $ i {} }
        }
        // /*caret*/fn foobar(){}fn foobar(){}fn foobar(){}

        foo!();
        foo!(bar);
    """, """
        mod foo {}
    """, """
        mod bar {}
    """)

    fun `test add 1st macro to expansion`() = checkExpansionAfterAction(type("\b\b\b\b\b\b\b\b\bfoo!{struct S;}"), """
        macro_rules! foo {
            ($($ i:item)*) => { $($ i)* };
        }

        foo! {
            struct S;/*caret*/
        }
    """, """
        struct S;
    """)

    fun `test add 2nd macro to expansion`() = checkExpansionAfterAction(type("foo!{struct S3;}"), """
        macro_rules! foo {
            ($($ i:item)*) => { $($ i)* };
        }

        foo! {
            struct S1;
            foo!{struct S2;}
            /*caret*/
        }
    """, """
        struct S1;
        struct S2;
        struct S3;
    """)
}
