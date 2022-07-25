/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions

import org.rust.ProjectDescriptor
import org.rust.WithStdlibRustProjectDescriptor
import org.rust.ide.intentions.createFromUsage.CreateFunctionIntention

class CreateFunctionIntentionTest : RsIntentionTestBase(CreateFunctionIntention::class) {
    fun `test function availability range`() = checkAvailableInSelectionOnly("""
        fn main() {
            <selection>foo</selection>(bar::baz);
        }
    """)

    fun `test method availability range`() = checkAvailableInSelectionOnly("""
        struct S;

        fn foo(s: S) {
            s.<selection>foo</selection>();
        }
    """)

    fun `test unavailable on resolved function`() = doUnavailableTest("""
        fn foo() {}

        fn main() {
            /*caret*/foo();
        }
    """)

    fun `test unavailable on arguments`() = doUnavailableTest("""
        fn main() {
            foo(1/*caret*/);
        }
    """)

    fun `test unavailable on path argument`() = doUnavailableTest("""
        fn main() {
            foo(bar::baz/*caret*/);
        }
    """)

    @ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
    fun `test create function unavailable on std`() = doUnavailableTest("""
        fn main() {
            std::foo/*caret*/();
        }
    """)

    @ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
    fun `test create method unavailable on std`() = doUnavailableTest("""
        fn main() {
            let v: Vec<u32> = Vec::new();
            v.foo/*caret*/();
        }
    """)

    fun `test unavailable on trait associated function`() = doUnavailableTest("""
        trait Trait {}

        fn foo() {
            Trait::baz/*caret*/();
        }
    """)

    fun `test create function`() = doAvailableTest("""
        fn main() {
            /*caret*/foo();
        }
    """, """
        fn main() {
            foo();
        }

        fn foo() {
            todo!()/*caret*/
        }
    """)

    fun `test create function in an existing module`() = doAvailableTest("""
        mod foo {}

        fn main() {
            foo::bar/*caret*/();
        }
    """, """
        mod foo {
            pub(crate) fn bar() {
                todo!()/*caret*/
            }
        }

        fn main() {
            foo::bar();
        }
    """)

    fun `test create function in an existing file`() = doAvailableTestWithFileTreeComplete("""
        //- main.rs
            mod foo;

            fn main() {
                foo::bar/*caret*/();
            }
        //- foo.rs
            fn test() {}
    """, """
        //- main.rs
            mod foo;

            fn main() {
                foo::bar();
            }
        //- foo.rs
            fn test() {}

            pub(crate) fn bar() {
                todo!()
            }
    """)

    fun `test create function in an existing file in other crate`() = doAvailableTestWithFileTreeComplete("""
    //- main.rs
        fn main() {
            test_package::foo/*caret*/();
        }
    //- lib.rs
    """, """
    //- main.rs
        fn main() {
            test_package::foo();
        }
    //- lib.rs
        pub fn foo() {
            todo!()
        }
    """)

    fun `test unresolved function call in a missing module`() = doUnavailableTest("""
        fn main() {
            foo::bar/*caret*/();
        }
    """)

    fun `test unresolved function call in a nested function`() = doAvailableTest("""
        fn main() {
            fn foo() {
                /*caret*/bar();
            }
        }
    """, """
        fn main() {
            fn foo() {
                bar();
            }
            fn bar() {
                todo!()/*caret*/
            }
        }
    """)

    fun `test unresolved function call inside a module`() = doAvailableTest("""
        mod foo {
            fn main() {
                /*caret*/bar();
            }
        }
    """, """
        mod foo {
            fn main() {
                bar();
            }

            fn bar() {
                todo!()/*caret*/
            }
        }
    """)

    fun `test simple parameters`() = doAvailableTest("""
        fn main() {
            let a = 5;
            foo/*caret*/(1, "hello", &a);
        }
    """, """
        fn main() {
            let a = 5;
            foo(1, "hello", &a);
        }

        fn foo(p0: i32, p1: &str, p2: &i32) {
            todo!()
        }
    """)

    fun `test generic parameters`() = doAvailableTest("""
        trait Trait1 {}
        trait Trait2 {}

        fn foo<T, X, R: Trait1>(t1: T, t2: T, r: R) where T: Trait2 {
            bar/*caret*/(r, t1, t2);
        }
    """, """
        trait Trait1 {}
        trait Trait2 {}

        fn foo<T, X, R: Trait1>(t1: T, t2: T, r: R) where T: Trait2 {
            bar(r, t1, t2);
        }

        fn bar<T, R: Trait1>(p0: R, p1: T, p2: T) where T: Trait2 {
            todo!()
        }
    """)

    fun `test complex generic constraints inside impl`() = doAvailableTest("""
        struct S<T>(T);
        trait Trait {}
        trait Trait2 {}

        impl<'a, 'b, T: 'a> S<T> where for<'c> T: Trait + Fn(&'c i32) {
            fn foo<R>(t: T, r: &R) where T: Trait2 + Trait, R: Trait + for<'d> Fn(&'d i32) {
                bar/*caret*/(t, r);
            }
        }
    """, """
        struct S<T>(T);
        trait Trait {}
        trait Trait2 {}

        impl<'a, 'b, T: 'a> S<T> where for<'c> T: Trait + Fn(&'c i32) {
            fn foo<R>(t: T, r: &R) where T: Trait2 + Trait, R: Trait + for<'d> Fn(&'d i32) {
                bar(t, r);
            }
        }

        fn bar<'a, R, T: 'a>(p0: T, p1: &R) where R: Trait + for<'d> Fn(&'d i32), T: Trait + Trait2, for<'c> T: Fn(&'c i32) + Trait {
            todo!()
        }
    """)

    fun `test nested function generic parameters`() = doAvailableTest("""
        fn foo<T>() where T: Foo {
            fn bar<T>(t: T) where T: Bar {
                baz/*caret*/(t);
            }
        }
    """, """
        fn foo<T>() where T: Foo {
            fn bar<T>(t: T) where T: Bar {
                baz(t);
            }
            fn baz<T>(p0: T) where T: Bar {
                todo!()
            }
        }
    """)

    fun `test guess return type let decl`() = doAvailableTest("""
        fn foo() {
            let x: u32 = bar/*caret*/();
        }
    """, """
        fn foo() {
            let x: u32 = bar();
        }

        fn bar() -> u32 {
            todo!()
        }
    """)

    fun `test guess return unknown type`() = doAvailableTest("""
        fn foo() {
            let x: S = bar/*caret*/();
        }
    """, """
        fn foo() {
            let x: S = bar();
        }

        fn bar() -> _/*caret*/ {
            todo!()
        }
    """)

    fun `test guess return type empty let decl`() = doAvailableTest("""
        fn foo() {
            let x = bar/*caret*/();
        }
    """, """
        fn foo() {
            let x = bar();
        }

        fn bar() -> _/*caret*/ {
            todo!()
        }
    """)

    fun `test guess return type assignment`() = doAvailableTest("""
        fn foo() {
            let mut x: u32 = 0;
            x = bar/*caret*/();
        }
    """, """
        fn foo() {
            let mut x: u32 = 0;
            x = bar();
        }

        fn bar() -> u32 {
            todo!()
        }
    """)

    fun `test guess return type function call`() = doAvailableTest("""
        fn bar(x: u32) {}
        fn foo() {
            bar(baz/*caret*/());
        }
    """, """
        fn bar(x: u32) {}
        fn foo() {
            bar(baz());
        }

        fn baz() -> u32 {
            todo!()
        }
    """)

    fun `test guess return type method call`() = doAvailableTest("""
        struct S;
        impl S {
            fn bar(&self, x: u32) {}
        }
        fn foo(s: S) {
            s.bar(baz/*caret*/());
        }
    """, """
        struct S;
        impl S {
            fn bar(&self, x: u32) {}
        }
        fn foo(s: S) {
            s.bar(baz());
        }

        fn baz() -> u32 {
            todo!()
        }
    """)

    fun `test guess return type struct literal`() = doAvailableTest("""
        struct S {
            a: u32
        }
        fn foo() {
            S {
                a: baz/*caret*/()
            };
        }
    """, """
        struct S {
            a: u32
        }
        fn foo() {
            S {
                a: baz()
            };
        }

        fn baz() -> u32 {
            todo!()
        }
    """)

    fun `test guess return type self parameter`() = doAvailableTest("""
        struct S;
        impl S {
            fn bar(&self) {}
        }
        fn foo() {
            S::bar(baz/*caret*/());
        }
    """, """
        struct S;
        impl S {
            fn bar(&self) {}
        }
        fn foo() {
            S::bar(baz());
        }

        fn baz() -> &S {
            todo!()
        }
    """)

    fun `test guess return type generic parameter`() = doAvailableTest("""
        fn foo<T>() {
            let x: T = bar/*caret*/();
        }
    """, """
        fn foo<T>() {
            let x: T = bar();
        }

        fn bar<T>() -> T {
            todo!()
        }
    """)

    fun `test navigate to created function`() = doAvailableTest("""
        fn foo() {
            bar/*caret*/();
        }
    """, """
        fn foo() {
            bar();
        }

        fn bar() {
            todo!()/*caret*/
        }
    """)

    fun `test create method create impl`() = doAvailableTest("""
        trait Trait {}
        struct S<T>(T) where T: Trait;

        fn foo(s: S<u32>) {
            s.foo/*caret*/(1, 2);
        }
    """, """
        trait Trait {}
        struct S<T>(T) where T: Trait;

        impl<T> S<T> where T: Trait {
            pub(crate) fn foo(&self, p0: i32, p1: i32) {
                todo!()
            }
        }

        fn foo(s: S<u32>) {
            s.foo(1, 2);
        }
    """)

    fun `test create method no arguments`() = doAvailableTest("""
        struct S;

        fn foo(s: S) {
            s.foo/*caret*/();
        }
    """, """
        struct S;

        impl S {
            pub(crate) fn foo(&self) {
                todo!()
            }
        }

        fn foo(s: S) {
            s.foo();
        }
    """)

    fun `test create generic method`() = doAvailableTest("""
        struct S;

        fn foo<R>(s: S, r: R) {
            s.foo/*caret*/(r);
        }
    """, """
        struct S;

        impl S {
            pub(crate) fn foo<R>(&self, p0: R) {
                todo!()
            }
        }

        fn foo<R>(s: S, r: R) {
            s.foo(r);
        }
    """)

    fun `test create method inside impl`() = doAvailableTest("""
        struct S;
        impl S {
            fn foo(&self) {
                self.bar/*caret*/(0);
            }
        }
    """, """
        struct S;
        impl S {
            fn foo(&self) {
                self.bar(0);
            }
            fn bar(&self, p0: i32) {
                todo!()
            }
        }
    """)

    fun `test create method inside generic impl`() = doAvailableTest("""
        struct S<T>(T);
        impl<T> S<T> {
            fn foo(&self, t: T) {
                self.bar/*caret*/(t);
            }
        }
    """, """
        struct S<T>(T);
        impl<T> S<T> {
            fn foo(&self, t: T) {
                self.bar(t);
            }
            fn bar(&self, p0: T) {
                todo!()
            }
        }
    """)

    fun `test create method inside generic impl with where`() = doAvailableTest("""
        trait Trait {}
        struct S<T>(T);
        impl<T> S<T> where T: Trait {
            fn foo(&self, t: T) {
                self.bar/*caret*/(t);
            }
        }
    """, """
        trait Trait {}
        struct S<T>(T);
        impl<T> S<T> where T: Trait {
            fn foo(&self, t: T) {
                self.bar(t);
            }
            fn bar(&self, p0: T) {
                todo!()
            }
        }
    """)

    fun `test unavailable inside method arguments`() = doUnavailableTest("""
        struct S;
        fn foo(s: S) {
            s.bar(1, /*caret*/2);
        }
    """)

    fun `test available inside method name`() = doAvailableTest("""
        struct S;
        fn foo(s: S) {
            s.b/*caret*/ar(1, 2);
        }
    """, """
        struct S;

        impl S {
            pub(crate) fn bar(&self, p0: i32, p1: i32) {
                todo!()
            }
        }

        fn foo(s: S) {
            s.bar(1, 2);
        }
    """)

    fun `test available after method name`() = doAvailableTest("""
        struct S;
        fn foo(s: S) {
            s.bar/*caret*/(1, 2);
        }
    """, """
        struct S;

        impl S {
            pub(crate) fn bar(&self, p0: i32, p1: i32) {
                todo!()
            }
        }

        fn foo(s: S) {
            s.bar(1, 2);
        }
    """)

    fun `test guess method return type`() = doAvailableTest("""
        struct S;
        fn foo(s: S) {
            let a: u32 = s.bar/*caret*/(1, 2);
        }
    """, """
        struct S;

        impl S {
            pub(crate) fn bar(&self, p0: i32, p1: i32) -> u32 {
                todo!()
            }
        }

        fn foo(s: S) {
            let a: u32 = s.bar(1, 2);
        }
    """)

    fun `test create method inside trait impl`() = doAvailableTest("""
        trait Trait {
            fn foo(&self);
        }
        struct S;
        impl Trait for S {
            fn foo(&self) {
                self.bar/*caret*/();
            }
        }
    """, """
        trait Trait {
            fn foo(&self);
        }
        struct S;

        impl S {
            pub(crate) fn bar(&self) {
                todo!()
            }
        }

        impl Trait for S {
            fn foo(&self) {
                self.bar();
            }
        }
    """)

    fun `test create method inside different impl`() = doAvailableTest("""
        struct S;
        struct T;
        impl T {
            fn foo(&self, s: S) {
                s.bar/*caret*/();
            }
        }
    """, """
        struct S;

        impl S {
            pub(crate) fn bar(&self) {
                todo!()
            }
        }

        struct T;
        impl T {
            fn foo(&self, s: S) {
                s.bar();
            }
        }
    """)

    fun `test create method for struct in other crate`() = doAvailableTestWithFileTreeComplete("""
    //- main.rs
        fn main(s: test_package::S) {
            s.foo/*caret*/();
        }
    //- lib.rs
        pub struct S;
    """, """
    //- main.rs
        fn main(s: test_package::S) {
            s.foo();
        }
    //- lib.rs
        pub struct S;

        impl S {
            pub fn foo(&self) {
                todo!()
            }
        }
    """)

    fun `test create associated function for struct`() = doAvailableTest("""
        struct S;
        fn foo() {
            S::bar/*caret*/(1, 2);
        }
    """, """
        struct S;

        impl S {
            fn bar(p0: i32, p1: i32) {
                todo!()
            }
        }

        fn foo() {
            S::bar(1, 2);
        }
    """)

    fun `test create associated function for enum`() = doAvailableTest("""
        enum S {
            V1
        }
        fn foo() {
            S::bar/*caret*/(1, 2);
        }
    """, """
        enum S {
            V1
        }

        impl S {
            fn bar(p0: i32, p1: i32) {
                todo!()
            }
        }

        fn foo() {
            S::bar(1, 2);
        }
    """)

    fun `test create associated function for generic struct`() = doAvailableTest("""
        struct S<T>(T);
        fn foo() {
            S::<u32>::bar/*caret*/(1, 2);
        }
    """, """
        struct S<T>(T);

        impl<T> S<T> {
            fn bar(p0: i32, p1: i32) {
                todo!()
            }
        }

        fn foo() {
            S::<u32>::bar(1, 2);
        }
    """)

    fun `test function call type to create async function`() = doAvailableTest("""
        async fn foo() {
            /*caret*/bar().await;
        }
    ""","""
        async fn foo() {
            bar().await;
        }

        async fn bar() {
            todo!()
        }
    """)

    fun `test function call type create async function in blocks`() = doAvailableTest("""
        fn foo() {
            async {
                /*caret*/bar().await
            };
        }
    """, """
        fn foo() {
            async {
                bar().await
            };
        }

        async fn bar() {
            todo!()
        }
    """)

    fun `test function call type create async function in nested blocks`() = doAvailableTest("""
        fn foo() {
            async {
                {
                    /*caret*/bar().await
                }
            };
        }
    """, """
        fn foo() {
            async {
                {
                    bar().await
                }
            };
        }

        async fn bar() {
            todo!()
        }
    """)

    fun `test function call type create async function in nested function`() = doAvailableTest("""
        fn main() {
            async fn foo() {
                /*caret*/bar().await;
            }
        }
    """, """
        fn main() {
            async fn foo() {
                bar().await;
            }
            async fn bar() {
                todo!()
            }
        }
    """)

    fun `test method call type to create async function`() = doAvailableTest("""
        struct S;

        impl S {
            async fn foo(&self) {
                self./*caret*/bar(1, 2).await;
            }
        }
    """, """
        struct S;

        impl S {
            async fn foo(&self) {
                self.bar(1, 2).await;
            }
            async fn bar(&self, p0: i32, p1: i32) {
                todo!()
            }
        }
    """)

    fun `test method call type create async function in blocks`() = doAvailableTest("""
        struct S;

        impl S {
            fn foo(&self) {
                async {
                    self./*caret*/bar(1, 2).await;
                };
            }
        }
    """, """
        struct S;

        impl S {
            fn foo(&self) {
                async {
                    self.bar(1, 2).await;
                };
            }
            async fn bar(&self, p0: i32, p1: i32) {
                todo!()
            }
        }
    """)

    fun `test method call type create async function in nested blocks`() = doAvailableTest("""
        struct S;

        impl S {
            fn foo(&self) {
                async {
                    {
                        self./*caret*/bar(1, 2).await;
                    }
                };
            }
        }
    """, """
        struct S;

        impl S {
            fn foo(&self) {
                async {
                    {
                        self.bar(1, 2).await;
                    }
                };
            }
            async fn bar(&self, p0: i32, p1: i32) {
                todo!()
            }
        }
    """)

    fun `test method call type create async function in nested function`() = doAvailableTest("""
        struct S;

        impl S {
            fn foo(&self) {
                async fn foo_a(s: &S) {
                    s./*caret*/bar(1, 2).await;
                }
            }
        }
    """, """
        struct S;

        impl S {
            async fn bar(&self, p0: i32, p1: i32) {
                todo!()
            }
        }

        impl S {
            fn foo(&self) {
                async fn foo_a(s: &S) {
                    s.bar(1, 2).await;
                }
            }
        }
    """)

    fun `test function call type create in the async function call`() = doAvailableTest("""
        async fn foo() {
            baz(/*caret*/bar()).await;
        }
        async fn baz(a: u32) {}
    """, """
        async fn foo() {
            baz(bar()).await;
        }

        fn bar() -> u32 {
            todo!()
        }

        async fn baz(a: u32) {}
    """)

    fun `test method call type create in the async function call`() = doAvailableTest("""
        struct S;

        async fn foo(s: S) {
            baz(s./*caret*/bar()).await;
        }
        async fn baz(a: u32) {}
    """, """
        struct S;

        impl S {
            pub(crate) fn bar(&self) -> u32 {
                todo!()
            }
        }

        async fn foo(s: S) {
            baz(s.bar()).await;
        }
        async fn baz(a: u32) {}
    """)

    fun `test aliased argument type`() = doAvailableTest("""
        type Alias = (u32, u32);

        fn foo(a: Alias) {
            bar/*caret*/(a);
        }
    """, """
        type Alias = (u32, u32);

        fn foo(a: Alias) {
            bar(a);
        }

        fn bar(p0: Alias) {
            todo!()
        }
    """)

    fun `test import argument and return types`() = doAvailableTest("""
        mod bar {
            pub struct S;
            pub struct T;
            pub fn get_s() -> S { S }
        }

        fn foo() -> bar::T {
            baz/*caret*/(bar::get_s())
        }
    """, """
        use crate::bar::{S, T};

        mod bar {
            pub struct S;
            pub struct T;
            pub fn get_s() -> S { S }
        }

        fn foo() -> bar::T {
            baz(bar::get_s())
        }

        fn baz(p0: S) -> T {
            todo!()
        }
    """)
}
