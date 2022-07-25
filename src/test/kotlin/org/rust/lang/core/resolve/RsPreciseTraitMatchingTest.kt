/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve

import org.rust.CheckTestmarkHit
import org.rust.lang.core.types.infer.TypeInferenceMarks

class RsPreciseTraitMatchingTest : RsResolveTestBase() {
    fun `test method in specialized trait impl for struct`() = checkByCode("""
        trait Tr { fn some_fn(&self); }
        struct S<T> { value: T }
        impl Tr for S<u8> {
            fn some_fn(&self) { }
        }
        impl Tr for S<u16> {
            fn some_fn(&self) { }
             //X
        }
        fn main() {
            let v = S {value: 5u16};
            v.some_fn();
            //^
        }
    """)

    fun `test method in specialized trait impl for struct 2`() = checkByCode("""
        trait Tr { fn some_fn(&self); }
        struct S<T1, T2> { value1: T1, value2: T2 }
        impl Tr for S<u8, u8> {
            fn some_fn(&self) { }
        }
        impl Tr for S<u16, u8> {
            fn some_fn(&self) { }
             //X
        }
        impl Tr for S<u8, u16> {
            fn some_fn(&self) { }
        }
        impl Tr for S<u16, u16> {
            fn some_fn(&self) { }
        }
        fn main() {
            let v = S {value1: 5u16, value2: 5u8};
            v.some_fn();
            //^
        }
    """)

    fun `test method in specialized trait impl for tuple struct`() = checkByCode("""
        trait Tr { fn some_fn(&self); }
        struct S<T> (T);
        impl Tr for S<u8> {
            fn some_fn(&self) { }
        }
        impl Tr for S<u16> {
            fn some_fn(&self) { }
             //X
        }
        fn main() {
            let v = S (5u16);
            v.some_fn();
            //^
        }
    """)

    fun `test method in specialized trait impl for enum`() = checkByCode("""
        trait Tr { fn some_fn(&self); }
        enum S<T> { Var1{value: T}, Var2 }
        impl Tr for S<u8> {
            fn some_fn(&self) { }
        }
        impl Tr for S<u16> {
            fn some_fn(&self) { }
             //X
        }
        fn main() {
            let v = S::Var1 {value: 5u16};
            v.some_fn();
            //^
        }
    """)

    fun `test method in specialized trait impl for tuple enum`() = checkByCode("""
        trait Tr { fn some_fn(&self); }
        enum S<T> { Var1(T), Var2  }
        impl Tr for S<u8> {
            fn some_fn(&self) { }
        }
        impl Tr for S<u16> {
            fn some_fn(&self) { }
             //X
        }
        fn main() {
            let v = S::Var1 (5u16);
            v.some_fn();
            //^
        }
    """)

    fun `test method in specialized impl for struct`() = checkByCode("""
        struct S<T> { value: T }
        impl S<u8> {
            fn some_fn(&self) { }
        }
        impl S<u16> {
            fn some_fn(&self) { }
             //X
        }
        fn main(v: S<u16>) {
            v.some_fn();
            //^
        }
    """)

    @CheckTestmarkHit(TypeInferenceMarks.MethodPickCheckBounds::class)
    fun `test trait bound satisfied for struct`() = checkByCode("""
        trait Tr1 { fn some_fn(&self) {} }
        trait Tr2 { fn some_fn(&self) {} }
                     //X
        trait Bound1 {}
        trait Bound2 {}
        struct S<T> { value: T }
        impl<T: Bound1> Tr1 for S<T> {}
        impl<T: Bound2> Tr2 for S<T> {}
        struct S0;
        impl Bound2 for S0 {}
        fn main(v: S<S0>) {
            v.some_fn();
            //^
        }
    """)

    @CheckTestmarkHit(TypeInferenceMarks.MethodPickCheckBounds::class)
    fun `test trait bound satisfied for dyn trait`() = checkByCode("""
        #[lang = "sized"]
        trait Sized {}
        trait Tr1 { fn some_fn(&self) {} }
        trait Tr2 { fn some_fn(&self) {} }
                     //X
        trait Bound1 {}
        trait Bound2 {}
        trait ChildOfBound2 : Bound2 {}
        struct S<T: ?Sized> { value: T }
        impl<T: Bound1 + ?Sized> Tr1 for S<T> { }
        impl<T: Bound2 + ?Sized> Tr2 for S<T> { }
        fn f(v: &S<dyn ChildOfBound2>) {
            v.some_fn();
            //^
        }
    """)

    @CheckTestmarkHit(TypeInferenceMarks.MethodPickCheckBounds::class)
    fun `test trait bound satisfied for other bound`() = checkByCode("""
        trait Tr1 { fn some_fn(&self) {} }
        trait Tr2 { fn some_fn(&self) {} }
                     //X
        trait Bound1 {}
        trait Bound2 {}
        struct S<T> { value: T }
        impl<T: Bound1> Tr1 for S<T> { }
        impl<T: Bound2> Tr2 for S<T> { }

        struct S1<T> { value: T }
        impl<T: Bound2> S1<T> {
            fn f(&self, t: S<T>) {
                t.some_fn();
                //^
            }
        }
    """)

    @CheckTestmarkHit(TypeInferenceMarks.MethodPickCheckBounds::class)
    fun `test Sized trait bound satisfied`() = checkByCode("""
        #[lang = "sized"] trait Sized {}
        trait Tr1 { fn some_fn(&self) {} }
        trait Tr2 { fn some_fn(&self) {} }
                     //X
        trait Bound1 {}
        struct S<T: ?Sized> { value: T }
        impl<T> Tr1 for S<T> { }
        impl<T: ?Sized> Tr2 for S<T> { }
        trait Dyn {}
        fn f(v: &S<dyn Dyn>) {
            v.some_fn();
            //^
        }
    """)

    fun `test allow ambiguous trait bounds for postponed selection`() = checkByCode("""
        trait Into<A> { fn into(&self) -> A; }
        trait From<B> { fn from(_: B) -> Self; }
        impl<T, U> Into<U> for T where U: From<T>
        {
            fn into(self) -> U { U::from(self) }
        }    //X

        struct S1;
        struct S2;
        impl From<S1> for S2 { fn from(_: B) -> Self { unimplemented!() }
}
        fn main() {
            let a = (&S1).into();
                        //^
            let _: S2 = a;
        }
    """)

    @CheckTestmarkHit(TypeInferenceMarks.MethodPickTraitScope::class)
    fun `test method defined in out of scope trait 1`() = checkByCode("""
        struct S;

        mod a {
            use super::S;
            pub trait A { fn foo(&self){} }
                           //X
            impl A for S {}
        }

        mod b {
            use super::S;
            pub trait B { fn foo(&self){} }
            impl B for S {}
        }

        fn main() {
            use a::A;
            S.foo();
        }   //^
    """)

    @CheckTestmarkHit(TypeInferenceMarks.MethodPickTraitScope::class)
    fun `test method defined in out of scope trait 2`() = checkByCode("""
        struct S;

        mod a {
            use super::S;
            pub trait A { fn foo(&self){} }
            impl A for S {}
        }

        mod b {
            use super::S;
            pub trait B { fn foo(&self){} }
                           //X
            impl B for S {}
        }

        fn main() {
            use b::B;
            S.foo();
        }   //^
    """)

    @CheckTestmarkHit(TypeInferenceMarks.MethodPickTraitScope::class)
    fun `test method defined in out of scope trait (with aliased import)`() = checkByCode("""
        struct S;

        mod a {
            use super::S;
            pub trait A { fn foo(&self){} }
                           //X
            impl A for S {}
        }

        mod b {
            use super::S;
            pub trait B { fn foo(&self){} }
            impl B for S {}
        }

        fn main() {
            use a::A as _A;
            S.foo();
        }   //^
    """)

    @CheckTestmarkHit(TypeInferenceMarks.MethodPickTraitScope::class)
    fun `test method defined in out of scope trait (with underscore import)`() = checkByCode("""
        struct S;

        mod a {
            use super::S;
            pub trait A { fn foo(&self){} }
                           //X
            impl A for S {}
        }

        mod b {
            use super::S;
            pub trait B { fn foo(&self){} }
            impl B for S {}
        }

        fn main() {
            use a::A as _;
            S.foo();
        }   //^
    """)

    @CheckTestmarkHit(TypeInferenceMarks.MethodPickTraitScope::class)
    fun `test method defined in out of scope trait (with underscore re-export)`() = checkByCode("""
        struct S;

        mod a {
            use super::S;
            pub trait A { fn foo(&self){} }
                           //X
            impl A for S {}
        }

        mod b {
            use super::S;
            pub trait B { fn foo(&self){} }
            impl B for S {}
        }

        mod c {
            pub use super::a::A as _;
        }

        fn main() {
            use c::*;
            S.foo();
        }   //^
    """)

    @CheckTestmarkHit(TypeInferenceMarks.MethodPickTraitScope::class)
    fun `test method defined in out of scope trait (inside impl)`() = checkByCode("""
        mod foo {
            pub trait Foo { fn foo(&self) {} }
            pub trait Bar { fn foo(&self) {} }
        }

        struct S2;
        impl foo::Foo for S2 { fn foo(&self) {} }
        impl foo::Bar for S2 {} //X

        struct S1(S2);
        impl foo::Foo for S1 {
            fn foo(&self) {
                self.0.foo()
            }        //^
        }
    """)

    @CheckTestmarkHit(TypeInferenceMarks.WinnowSpecialization::class)
    fun `test specialization simple`() = checkByCode("""
        trait Tr { fn foo(&self); }
        struct S;
        impl<T> Tr for T { fn foo(&self) {} }
        impl Tr for S { fn foo(&self) {} }
                         //X
        fn main() {
            S.foo();
        }   //^
    """)

    @CheckTestmarkHit(TypeInferenceMarks.MethodPickTraitsOutOfScope::class)
    fun `test pick correct method from out of scope traits`() = checkByCode("""
        struct Foo;
        struct Bar;

        #[lang="deref"]
        trait Deref {
            type Target;
        }

        impl Deref for Bar {
            type Target = Foo;
        }
        mod a {
            pub trait X {
                fn do_x(&self);
            }

            impl X for crate::Foo {
                fn do_x(&self) {}
            }

            impl X for crate::Bar {
                fn do_x(&self) {}
                   //X
            }
        }

        fn main() {
            Bar.do_x();
               //^
        }
    """)

    fun `test filter associated functions by trait visibility`() = checkByCode("""
        struct S;

        mod foo {
            pub trait Foo {
                fn new() {}
            }    //X
            impl Foo for super::S {}
        }
        mod bar {
            pub trait Bar {
                fn new() {}
            }
            impl Bar for super::S {}
        }
        use foo::Foo;

        fn main () {
            S::new();
        }    //^
    """)

    fun `test filter associated constants by trait visibility`() = checkByCode("""
        struct S;

        mod foo {
            pub trait Foo {
                const C: i32 = 0;
            }       //X
            impl Foo for super::S {}
        }
        mod bar {
            pub trait Bar {
                const C: i32 = 0;
            }
            impl Bar for super::S {}
        }
        use foo::Foo;

        fn main () {
            let a = S::C;
        }            //^
    """)

    fun `test filter associated functions by trait bounds`() = checkByCode("""
        struct S;
        trait Bound1 {}
        trait Bound2 {}
        impl Bound1 for S {}

        pub trait Foo {
            fn new() {}
        }    //X
        pub trait Bar {
            fn new() {}
        }
        impl<T: Bound1> Foo for T {}
        impl<T: Bound2> Bar for T {}

        fn main () {
            S::new();
        }    //^
    """)

    fun `test filter associated constants by trait bounds`() = checkByCode("""
        struct S;
        trait Bound1 {}
        trait Bound2 {}
        impl Bound1 for S {}

        pub trait Foo {
            const C: i32 = 0;
        }       //X
        pub trait Bar {
            const C: i32 = 0;
        }
        impl<T: Bound1> Foo for T {}
        impl<T: Bound2> Bar for T {}

        fn main () {
            let a = S::C;
        }            //^
    """)

    fun `test bound for type parameter wins over blanket impl (type-related path)`() = checkByCode("""
        trait Foo { fn foo(&self); }
                     //X
        impl<T> Foo for T { fn foo(&self) {} }
        fn asd<T: Foo>(t: T) {
            T::foo(&t);
        }    //^
    """)

    fun `test bound for type parameter wins over blanket impl (UFCS path)`() = checkByCode("""
        trait Foo { fn foo(&self); }
                     //X
        impl<T> Foo for T { fn foo(&self) {} }
        fn asd<T: Foo>(t: T) {
            <T as Foo>::foo(&t);
        }             //^
    """)

    fun `test bound for type parameter wins over blanket impl (method call)`() = checkByCode("""
        trait Foo { fn foo(&self); }
                     //X
        impl<T> Foo for T { fn foo(&self) {} }
        fn asd<T: Foo>(t: T) {
            t.foo()
        }    //^
    """)

    fun `test trait in scope wins for trait bounds (type-related path) 1`() = checkByCode("""
        mod a {
            pub trait Foo { fn foo(&self) {} }
            pub trait Bar { fn foo(&self) {} }
        }                    //X
        struct S<T>(T);
        use a::Bar;
        impl<T> Bar for S<T> {}
        fn foo<T>(a: S<T>) where S<T>: a::Foo {
            <S<T>>::foo(&a);
        }         //^
    """)

    fun `test trait in scope wins for trait bounds (type-related path) 2`() = checkByCode("""
        mod a {
            pub trait Foo { fn foo(&self) {} }
            pub trait Bar { fn foo(&self) {} }
        }                    //X
        struct S<T>(T);
        use a::Bar;
        fn foo<T>(a: S<T>)
            where S<T>: a::Foo,
                  S<T>: a::Bar,
        {
            <S<T>>::foo(&a);
        }         //^
    """)

    fun `test trait in scope wins for trait bounds (method call) 1`() = checkByCode("""
        mod a {
            pub trait Foo { fn foo(&self) {} }
            pub trait Bar { fn foo(&self) {} }
        }                    //X
        struct S<T>(T);
        use a::Bar;
        impl<T> Bar for S<T> {}
        fn foo<T>(a: S<T>) where S<T>: a::Foo {
            a.foo();
        }   //^
    """)

    fun `test trait in scope wins for trait bounds (method call) 2`() = checkByCode("""
        mod a {
            pub trait Foo { fn foo(&self) {} }
            pub trait Bar { fn foo(&self) {} }
        }                    //X
        struct S<T>(T);
        use a::Bar;
        fn foo<T>(a: S<T>)
            where S<T>: a::Foo,
                  S<T>: a::Bar,
        {
            a.foo();
        }   //^
    """)

    fun `test cycle using glob-imports (and underscore trait re-export)`() = checkByCode("""
        mod a {
            pub use crate::b::*;
            pub use crate::c::T1 as _;
        }
        mod b {
            pub use crate::a::*;
        }
        mod c {
            pub trait T0 { fn foo(&self) {} }
            pub trait T1 { fn foo(&self) {} }
                            //X
            impl T0 for crate::S {}
            impl T1 for crate::S {}
        }
        struct S;
        fn main() {
            use b::*;
            S.foo();
        }      //^
    """)

    fun `test trait method and private inherent method`() = checkByCode("""
        use foo::{Foo, Trait};

        mod foo {
            pub struct Foo;
            impl Foo {
                // Private
                fn get(&self) { println!("struct"); }
            }

            pub trait Trait {
                fn get(&self);
            }
            impl Trait for Foo {
                fn get(&self) { println!("trait"); }
            }    //X
        }

        fn main() {
            let f = foo::Foo;
            f.get();
            //^
        }
    """)

    fun `test trait bound satisfied for struct with negative impl`() = checkByCode("""
        auto trait Sync {}
        trait Tr1 { fn some_fn(&self) {} }
        trait Tr2 { fn some_fn(&self) {} }
                     //X
        struct S<T> { value: T }
        impl<T: Sync> Tr1 for S<T> {}
        impl<T> Tr2 for S<T> {}
        struct S0;
        impl !Sync for S0 {}
        fn main1(v: S<S0>) {
            v.some_fn();
            //^
        }
    """)
}
