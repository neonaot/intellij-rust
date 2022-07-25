/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDocumentManager
import org.intellij.lang.annotations.Language
import org.rust.ExpandMacros
import org.rust.RsTestBase
import org.rust.fileTreeFromText
import org.rust.lang.core.psi.RsRustStructureModificationTrackerTest.TestAction.INC
import org.rust.lang.core.psi.RsRustStructureModificationTrackerTest.TestAction.NOT_INC
import org.rust.lang.core.psi.ext.childOfType
import org.rust.lang.core.resolve2.updateDefMapForAllCrates
import org.rust.openapiext.runWriteCommandAction

class RsRustStructureModificationTrackerTest : RsTestBase() {
    private enum class TestAction(val function: (Long, Long) -> Boolean, val comment: String) {
        INC({ a, b -> a > b }, "expected to be incremented, but it remained the same"),
        NOT_INC({ a, b -> a == b }, "expected to remain the same, but it was incremented");

        fun check(modTracker: ModificationTracker, oldValue: Long, modTrackerName: String) {
            check(function(modTracker.modificationCount, oldValue)) {
                "$modTrackerName $comment"
            }
        }
    }

    private fun checkModCount(op: TestAction, depsOp: TestAction = NOT_INC, action: () -> Unit) {
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        updateDefMapForAllCrates(project, EmptyProgressIndicator(), multithread = false)
        val modTracker = project.rustStructureModificationTracker
        val modTrackerInDeps = project.rustPsiManager.rustStructureModificationTrackerInDependencies
        val oldCount = modTracker.modificationCount
        val oldCountInDeps = modTrackerInDeps.modificationCount
        action()
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        op.check(modTracker, oldCount, "rustStructureModificationTracker")
        depsOp.check(modTrackerInDeps, oldCountInDeps, "rustStructureModificationTrackerInDependencies")
    }

    private fun checkModCount(op: TestAction, @Language("Rust") code: String, text: String) {
        InlineFile(code).withCaret()
        checkModCount(op) { myFixture.type(text) }
    }

    private fun doTest(op: TestAction, @Language("Rust") code: String, text: String = "a") {
        checkModCount(op, code, text)
        checkModCount(op, """
            fn wrapped() {
                $code
            }
        """, text)
    }

    fun `test comment`() = doTest(NOT_INC, """
        // /*caret*/
    """)

    //

    fun `test fn`() = doTest(INC, """
        /*caret*/
    """, "fn foo() {}")

    fun `test fn vis`() = doTest(INC, """
        /*caret*/fn foo() {}
    """, "pub ")

    fun `test fn name`() = doTest(INC, """
        fn foo/*caret*/() {}
    """)

    fun `test fn params`() = doTest(INC, """
        fn foo(/*caret*/) {}
    """)

    //TODO really should not inc
    fun `test fn param name`() = doTest(INC, """
        fn foo(a/*caret*/: i32) {}
    """)

    fun `test fn param type`() = doTest(INC, """
        fn foo(a: i32/*caret*/) {}
    """)

    fun `test fn return type 1`() = doTest(INC, """
        fn foo()/*caret*/ {}
    """, "-> u8")

    fun `test fn return type 2`() = doTest(INC, """
        fn foo() -> u/*caret*/ {}
    """, "-> 8")

    fun `test fn body`() = doTest(NOT_INC, """
        fn foo() { /*caret*/ }
    """)

    //

    fun `test struct vis`() = doTest(INC, """
        /*caret*/struct Foo;
    """, "pub ")

    fun `test struct name`() = doTest(INC, """
        struct Foo/*caret*/;
    """)

    fun `test struct body`() = doTest(INC, """
        struct Foo { /*caret*/ }
    """)

    //

    fun `test const vis`() = doTest(INC, """
        /*caret*/const FOO: u8 = 0;
    """, "pub ")

    fun `test const name`() = doTest(INC, """
        const FOO/*caret*/: u8 = 0;
    """)

    fun `test const body`() = doTest(INC, """
        const FOO: u8 = 0/*caret*/;
    """, "1")

    //

    fun `test impl type`() = doTest(INC, """
        impl Foo/*caret*/ {}
    """)

    fun `test impl for`() = doTest(INC, """
        impl Foo/*caret*/ {}
    """, " for")

    fun `test impl for trait`() = doTest(INC, """
        impl Foo for/*caret*/ {}
    """, " Bar")

    fun `test impl body`() = doTest(INC, """
        impl Foo { /*caret*/ }
    """, "fn foo() {}")

    fun `test impl fn`() = doTest(INC, """
        impl Foo { fn foo(/*caret*/) {} }
    """, "&self")

    fun `test impl fn body`() = doTest(NOT_INC, """
        impl Foo { fn foo() { /*caret*/ } }
    """)

    //

    fun `test macro`() = doTest(INC, """
        macro_rules! foo { () => { /*caret*/ } }
    """)

    fun `test macro2`() = doTest(INC, """
        macro foo { () => { /*caret*/ } }
    """)

    fun `test macro call (old engine)`() = checkModCount(INC, """
        foo! { /*caret*/ }
    """, "a")

    @ExpandMacros
    fun `test macro call (new engine)`() = checkModCount(NOT_INC, """
        foo! { /*caret*/ }
    """, "a")

    @ExpandMacros
    fun `test macro expanded call`() = checkModCount(INC, """
        macro_rules! foo {
            ($ i:ident) => { fn $ i() {} };
        }
        foo! { a/*caret*/ }
    """, "a")

    fun `test macro call inside a function (old engine)`() = checkModCount(NOT_INC, """
        fn wrapped() { foo! { /*caret*/ } }
    """, "a")

    @ExpandMacros
    fun `test macro call a function (new engine)`() = checkModCount(NOT_INC, """
        fn wrapped() { foo! { /*caret*/ } }
    """, "a")

    //

    fun `test vfs file change`() {
        val p = fileTreeFromText("""
        //- main.rs
            mod foo;
              //^
        //- foo.rs
            // fn bar() {}
        """).createAndOpenFileWithCaretMarker()
        val file = p.psiFile("foo.rs").virtualFile!!
        checkModCount(INC) {
            runWriteAction {
                VfsUtil.saveText(file, VfsUtil.loadText(file).replace("//", ""))
            }
        }
    }

    fun `test vfs file removal`() {
        val p = fileTreeFromText("""
        //- main.rs
            mod foo;
              //^
        //- foo.rs
            fn bar() {}
        """).createAndOpenFileWithCaretMarker()
        val file = p.psiFile("foo.rs").virtualFile!!
        checkModCount(INC, depsOp = INC) {
            runWriteAction {
                file.delete(null)
            }
        }
    }

    fun `test vfs directory removal`() {
        val p = fileTreeFromText("""
        //- main.rs
            mod foo;
              //^
        //- foo/mod.rs
            fn bar() {}
        """).createAndOpenFileWithCaretMarker()
        val file = p.psiFile("foo").virtualFile!!
        checkModCount(INC, depsOp = INC) {
            runWriteAction {
                file.delete(null)
            }
        }
    }

    fun `test vfs file rename`() {
        val p = fileTreeFromText("""
        //- main.rs
            mod foo;
              //^
            mod bar;
        //- foo.rs
            fn bar() {}
        """).createAndOpenFileWithCaretMarker()
        val file = p.psiFile("foo.rs").virtualFile!!
        checkModCount(INC) {
            runWriteAction {
                file.rename(null, "bar.rs")
            }
        }
    }

    //

    fun `test replace function with comment`() = doTest(INC, """
        /*caret*/fn foo() {}
    """, "//")

    fun `test replace expr with block with item`() = doTest(INC, """
        fn foo() { 2/*caret*/; }
    """, "\b{ fn bar() {} }")

    fun `test replace expr with block with macro definition`() = doTest(INC, """
        fn foo() { 2/*caret*/; }
    """, "\b{ macro_rules! foo { () => {} } }")

    fun `test replace expr with block with call`() = doTest(NOT_INC, """
        fn foo() { 2/*caret*/; }
    """, "\b{ foo!() }")

    fun `test delete use item via PSI`() {
        InlineFile("""
            use foo::bar;
        """)

        checkModCount(INC) {
            project.runWriteCommandAction {
                myFixture.file.childOfType<RsUseItem>()!!.delete()
            }
        }
    }
}
