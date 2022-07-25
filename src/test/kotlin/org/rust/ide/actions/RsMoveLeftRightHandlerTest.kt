/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions

import com.intellij.openapi.actionSystem.IdeActions
import org.intellij.lang.annotations.Language
import org.rust.RsTestBase

class RsMoveLeftRightHandlerTest : RsTestBase() {

    fun `test function arguments`() = doRightLeftTest("""
        fn main() {
            foo("12/*caret*/3", 123);
        }
    """, """
        fn main() {
            foo(123, "12/*caret*/3");
        }
    """)

    fun `test function parameters`() = doRightLeftTest("""
        fn foo(str: /*caret*/String, int: i32) {}
    """, """
        fn foo(int: i32, str: /*caret*/String) {}
    """)

    fun `test don't move self parameter`() = doMoveRightTest("""
        impl S {
            fn foo(&/*caret*/self, str: String) {}
        }
    """, """
        impl S {
            fn foo(&/*caret*/self, str: String) {}
        }
    """)

    fun `test type parameters`() = doRightLeftTest("""
        fn foo<T1/*caret*/, T2>(p1: T1, p2: T2) {}
    """, """
        fn foo<T2, T1/*caret*/>(p1: T1, p2: T2) {}
    """)

    fun `test lifetime parameters`() = doRightLeftTest("""
        fn foo<'a/*caret*/, 'b>(p1: &'a str, p2: &'b str) {}
    """, """
        fn foo<'b, 'a/*caret*/>(p1: &'a str, p2: &'b str) {}
    """)

    fun `test const parameters`() = doRightLeftTest("""
        fn foo<const C1/*caret*/: i32, const C2: usize>() {}
    """, """
        fn foo<const C2: usize, const C1/*caret*/: i32>() {}
    """)

    fun `test generic parameters 1`() = doMoveRightTest("""
        fn foo<'a/*caret*/, T, const C: usize>(p1: &'a str, p2: T, p3: [T; C]) {}
    """, """
        fn foo<T, 'a/*caret*/, const C: usize>(p1: &'a str, p2: T, p3: [T; C]) {}
    """)

    fun `test generic parameters 2`() = doMoveRightTest("""
        fn foo<T, 'a/*caret*/, const C: usize>(p1: &'a str, p2: T, p3: [T; C]) {}
    """, """
        fn foo<T, const C: usize, 'a/*caret*/>(p1: &'a str, p2: T, p3: [T; C]) {}
    """)

    fun `test generic parameters 3`() = doMoveRightTest("""
        fn foo<'a, T/*caret*/, const C: usize>(p1: &'a str, p2: T, p3: [T; C]) {}
    """, """
        fn foo<'a, const C: usize, T/*caret*/>(p1: &'a str, p2: T, p3: [T; C]) {}
    """)

    fun `test type param bounds`() = doRightLeftTest("""
        fn foo<T: Ord/*caret*/ + Hash>(p: T) {}
    """, """
        fn foo<T: Hash + Ord/*caret*/>(p: T) {}
    """)

    fun `test lifetime param bounds`() = doRightLeftTest("""
        fn foo<'a, 'b, 'c>(i: &'a i32) where 'a: 'c/*caret*/ + 'b {}
    """, """
        fn foo<'a, 'b, 'c>(i: &'a i32) where 'a: 'b + 'c/*caret*/ {}
    """)

    fun `test array expr 1 `() = doRightLeftTest("""
        fn main() {
            let a = [1, 2/*caret*/, 3];
        }
    """, """
        fn main() {
            let a = [1, 3, 2/*caret*/];
        }
    """)

    fun `test array expr 2`() = doMoveRightTest("""
        fn main() {
            let a = [0/*caret*/; 2];
        }
    """, """
        fn main() {
            let a = [0/*caret*/; 2];
        }
    """)

    fun `test vec macro 1 `() = doRightLeftTest("""
        fn main() {
            let a = vec![1, 2/*caret*/, 3];
        }
    """, """
        fn main() {
            let a = vec![1, 3, 2/*caret*/];
        }
    """)

    fun `test vec macro 2`() = doMoveRightTest("""
        fn main() {
            let a = vec![0/*caret*/; 2];
        }
    """, """
        fn main() {
            let a = vec![0/*caret*/; 2];
        }
    """)

    fun `test tuple expr`() = doRightLeftTest("""
        fn main() {
            let a = (1, "foo"/*caret*/, 3);
        }
    """, """
        fn main() {
            let a = (1, 3, "foo"/*caret*/);
        }
    """)

    fun `test tuple type`() = doRightLeftTest("""
        fn foo() -> (i32/*caret*/, String) { !unimplemented() }
    """, """
        fn foo() -> (String, i32/*caret*/) { !unimplemented() }
    """)

    fun `test struct tuple fields`() = doRightLeftTest("""
        struct Foo(i32/*caret*/, String);
    """, """
        struct Foo(String, i32/*caret*/);
    """)

    fun `test attributes 1`() = doRightLeftTest("""
        #[derive(Copy/*caret*/, Clone)]
        struct Foo;
    """, """
        #[derive(Clone, Copy/*caret*/)]
        struct Foo;
    """)

    fun `test attributes 2`() = doRightLeftTest("""
        #[deprecated(note = /*caret*/"...", since = "0.10.0")]
        struct Foo;
    """, """
        #[deprecated(since = "0.10.0", note = /*caret*/"...")]
        struct Foo;
    """)

    fun `test use item`() = doRightLeftTest("""
        use std::collections::{HashMap/*caret*/, BinaryHeap};
    """, """
        use std::collections::{BinaryHeap, HashMap/*caret*/};
    """)

    fun `test format macros`() {
        for (macros in listOf("println", "info")) {
            doRightLeftTest("""
                fn main() {
                    !$macros("{} {}", 123/*caret*/, "foo");
                }
            """, """
                fn main() {
                    !$macros("{} {}", "foo", 123/*caret*/);
                }
            """)
        }
    }

    fun `test don't move target in log macros`() = doMoveRightTest("""
        fn main() {
            warn!(target: /*caret*/"foo", "warn log");
        }
    """, """
        fn main() {
            warn!(target: /*caret*/"foo", "warn log");
        }
    """)

    fun `test trait type 1`() = doRightLeftTest("""
        fn foo(a: &(Read/*caret*/ + Sync)) {}
    """, """
        fn foo(a: &(Sync + Read/*caret*/)) {}
    """)

    fun `test trait type 2`() = doRightLeftTest("""
        fn foo() -> impl Read/*caret*/ + Sync {}
    """, """
        fn foo() -> impl Sync + Read/*caret*/ {}
    """)

    fun `test where clause predicates`() = doRightLeftTest("""
        trait Trait1 {}
        trait Trait2 {}
        fn foo<A, B>(a: A, b: B)
            where /*caret*/A: Trait1,
                  B: Trait2
        {}
    """, """
        trait Trait1 {}
        trait Trait2 {}
        fn foo<A, B>(a: A, b: B)
            where B: Trait2,
                  A: Trait1
        {}
    """)

    private fun doRightLeftTest(@Language("Rust") before: String, @Language("Rust") after: String) {
        doMoveRightTest(before, after)
        doMoveLeftTest(after, before)
    }

    private fun doMoveLeftTest(@Language("Rust") before: String, @Language("Rust") after: String) =
        checkEditorAction(before, after, IdeActions.MOVE_ELEMENT_LEFT)

    private fun doMoveRightTest(@Language("Rust") before: String, @Language("Rust") after: String) =
        checkEditorAction(before, after, IdeActions.MOVE_ELEMENT_RIGHT)
}
