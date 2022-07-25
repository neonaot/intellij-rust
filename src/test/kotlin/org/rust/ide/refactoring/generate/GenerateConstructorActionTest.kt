/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.generate

class GenerateConstructorActionTest : RsGenerateBaseTest() {
    override val generateId: String = "Rust.GenerateConstructor"

    fun `test generic struct`() = doTest("""
        struct S<T> {
            n: i32,/*caret*/
            m: T
        }
    """, listOf(
        MemberSelection("n: i32", true),
        MemberSelection("m: T", true)
    ), """
        struct S<T> {
            n: i32,
            m: T
        }

        impl<T> S<T> {
            pub fn new(n: i32, m: T) -> Self {
                Self { n, m }
            }/*caret*/
        }
    """)

    fun `test empty type declaration`() = doTest("""
        struct S {
            n: i32,/*caret*/
            m:
        }
    """, listOf(
        MemberSelection("n: i32", true),
        MemberSelection("m: ()", true)
    ), """
        struct S {
            n: i32,
            m:
        }

        impl S {
            pub fn new(n: i32, m: ()) -> Self {
                Self { n, m }
            }/*caret*/
        }
    """)

    fun `test empty struct`() = doTest("""
        struct S {/*caret*/}
    """, emptyList(), """
        struct S {}

        impl S {
            pub fn new() -> Self {
                Self {}
            }
        }
    """)

    fun `test tuple struct`() = doTest("""
        struct Color(i32, i32, i32)/*caret*/;
    """, listOf(
        MemberSelection("field0: i32", true),
        MemberSelection("field1: i32", true),
        MemberSelection("field2: i32", true)
    ), """
        struct Color(i32, i32, i32);

        impl Color {
            pub fn new(field0: i32, field1: i32, field2: i32) -> Self {
                Self(field0, field1, field2)
            }/*caret*/
        }
    """)

    fun `test select none fields`() = doTest("""
        struct S {
            n: i32,/*caret*/
            m: i64,
        }
    """, listOf(
        MemberSelection("n: i32", false),
        MemberSelection("m: i64", false)
    ), """
        struct S {
            n: i32,
            m: i64,
        }

        impl S {
            pub fn new() -> Self {
                Self { n: (), m: () }
            }/*caret*/
        }
    """)

    fun `test select all fields`() = doTest("""
        struct S {
            n: i32,/*caret*/
            m: i64,
        }
    """, listOf(
        MemberSelection("n: i32", true),
        MemberSelection("m: i64", true)
    ), """
        struct S {
            n: i32,
            m: i64,
        }

        impl S {
            pub fn new(n: i32, m: i64) -> Self {
                Self { n, m }
            }/*caret*/
        }
    """)

    fun `test select some fields`() = doTest("""
        struct S {
            n: i32,/*caret*/
            m: i64,
        }
    """, listOf(
        MemberSelection("n: i32", true),
        MemberSelection("m: i64", false)
    ), """
        struct S {
            n: i32,
            m: i64,
        }

        impl S {
            pub fn new(n: i32) -> Self {
                Self { n, m: () }
            }/*caret*/
        }
    """)

    fun `test generate all fields on impl`() = doTest("""
        struct S {
            n: i32,
            m: i64,
        }

        impl S {
            /*caret*/
        }
    """, listOf(
        MemberSelection("n: i32", true),
        MemberSelection("m: i64", true)
    ), """
        struct S {
            n: i32,
            m: i64,
        }

        impl S {
            pub fn new(n: i32, m: i64) -> Self {
                Self { n, m }
            }/*caret*/
        }
    """)

    fun `test not available when new method exists`() = doUnavailableTest("""
        struct S {
            n: i32,
            m: i64,
        }

        impl S {
            pub fn new() {}
            /*caret*/
        }
    """)

    fun `test not available on trait impl`() = doUnavailableTest("""
        trait T { fn foo() }

        struct S {
            n: i32,
            m: i64,
        }

        impl T for S {
            /*caret*/
        }
    """)

    fun `test take type parameters from impl block`() = doTest("""
        struct S<T>(T);

        impl S<i32> {
            /*caret*/
        }
    """, listOf(MemberSelection("field0: i32", true)), """
        struct S<T>(T);

        impl S<i32> {
            pub fn new(field0: i32) -> Self {
                Self(field0)
            }/*caret*/
        }
    """)

    fun `test take lifetimes from impl block`() = doTest("""
        struct S<'a, T>(&'a T);

        impl <'a> S<'a, i32> {
            /*caret*/
        }
    """, listOf(MemberSelection("field0: &'a i32", true)), """
        struct S<'a, T>(&'a T);

        impl <'a> S<'a, i32> {
            pub fn new(field0: &'a i32) -> Self {
                Self(field0)
            }/*caret*/
        }
    """)

    fun `test type alias`() = doTest("""
        type Coordinates = (u32, u32);

        struct System/*caret*/ {
            point: Coordinates,
        }
    """, listOf(MemberSelection("point: Coordinates", true)), """
        type Coordinates = (u32, u32);

        struct System {
            point: Coordinates,
        }

        impl System {
            pub fn new(point: Coordinates) -> Self {
                Self { point }
            }/*caret*/
        }
    """)

    fun `test qualified path named struct`() = doTest("""
        mod foo {
            pub struct S;
        }

        struct System/*caret*/ {
            s: foo::S
        }
    """, listOf(MemberSelection("s: foo::S", true)), """
        mod foo {
            pub struct S;
        }

        struct System {
            s: foo::S
        }

        impl System {
            pub fn new(s: foo::S) -> Self {
                Self { s }
            }/*caret*/
        }
    """)

    fun `test qualified path tuple struct`() = doTest("""
        mod foo {
            pub struct S;
        }

        struct System/*caret*/(foo::S);
    """, listOf(MemberSelection("field0: foo::S", true)), """
        mod foo {
            pub struct S;
        }

        struct System(foo::S);

        impl System {
            pub fn new(field0: foo::S) -> Self {
                Self(field0)
            }/*caret*/
        }
    """)

    fun `test reuse impl block`() = doTest("""
        struct System {
            s: u32/*caret*/
        }

        impl System {
            fn foo(&self) {}
        }
    """, listOf(MemberSelection("s: u32", true)), """
        struct System {
            s: u32
        }

        impl System {
            fn foo(&self) {}
            pub fn new(s: u32) -> Self {
                Self { s }
            }/*caret*/
        }
    """)
}
