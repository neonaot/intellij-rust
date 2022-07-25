/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.typecheck

import org.rust.ExpandMacros
import org.rust.MockRustcVersion
import org.rust.ProjectDescriptor
import org.rust.WithStdlibRustProjectDescriptor
import org.rust.ide.inspections.RsInspectionsTestBase
import org.rust.ide.inspections.RsTypeCheckInspection
import org.rust.lang.core.macros.MacroExpansionScope

class RsTypeCheckInspectionTest : RsInspectionsTestBase(RsTypeCheckInspection::class) {
    fun `test type mismatch E0308 primitive`() = checkByText("""
        fn main () {
            let _: u8 = <error>1u16</error>;
        }
    """)

    fun `test typecheck in constant`() = checkByText("""
        const A: u8 = <error>1u16</error>;
    """)

    fun `test typecheck in array size`() = checkByText("""
        const A: [u8; <error>1u8</error>] = [0];
    """)

    fun `test typecheck in const argument`() = checkByText("""
        #![feature(const_generics)]
        #![feature(const_generics_defaults)]
        struct S<const N: usize = <error>1u8</error>>;
        trait T<const N: usize = <error>1u8</error>> {
            fn foo<const N: usize>(&self) -> S<N>;
        }
        impl T<<error>1u8</error>> for S<<error>1u8</error>> {
            fn foo<const N: usize>(self) -> S<<error>1u8</error>> { self }
        }
        fn bar(x: S<<error>1u8</error>>) -> S<<error>1u8</error>> {
            let s: S<<error>1u8</error>> = S::<<error>1u8</error>>;
            s.foo::<<error>1u8</error>>()
        }
    """)

    fun `test typecheck in enum variant discriminant`() = checkByText("""
        enum Foo { BAR = <error>1u8</error> }
    """)

    fun `test type mismatch E0308 coerce ptr mutability`() = checkByText("""
        fn fn_const(p: *const u8) { }
        fn fn_mut(p: *mut u8) { }

        fn main () {
            let mut ptr_const: *const u8;
            let mut ptr_mut: *mut u8;

            fn_const(ptr_const);
            fn_const(ptr_mut);
            fn_mut(<error>ptr_const</error>);
            fn_mut(ptr_mut);

            ptr_const = ptr_mut;
            ptr_mut = <error>ptr_const</error>;
        }
    """)

    fun `test type mismatch E0308 coerce reference to ptr`() = checkByText("""
        fn fn_const(p: *const u8) { }
        fn fn_mut(p: *mut u8) { }

        fn main () {
            let const_u8 = &1u8;
            let mut_u8 = &mut 1u8;
            fn_const(const_u8);
            fn_const(mut_u8);
            fn_mut(<error>const_u8</error>);
            fn_mut(mut_u8);
        }
    """)

    fun `test type mismatch E0308 struct`() = checkByText("""
        struct X; struct Y;
        fn main () {
            let _: X = <error>Y</error>;
        }
    """)

    fun `test type mismatch E0308 tuple`() = checkByText("""
        fn main () {
            let _: (u8, ) = (<error>1u16</error>, );
            let _: (u8, ) = <error>(1, 1)</error>;
        }
    """)

    // TODO error should be more local
    fun `test type mismatch E0308 array`() = checkByText("""
        fn main () {
            let _: [u8; 1] = <error>[1u16]</error>;
        }
    """)

    fun `test type mismatch E0308 array size`() = checkByText("""
        fn main () {
            let _: [u8; 1] = <error>[1, 2]</error>;
        }
    """)

    fun `test type mismatch E0308 struct field`() = checkByText("""
        struct S { f: u8 }
        fn main () {
            S { f: <error>1u16</error> };
        }
    """)

    fun `test type mismatch E0308 struct shorthand`() = checkByText("""
        struct S { f: u8 }
        fn main () {
            let f = "42";
            S { <error>f</error> };
        }
    """)

    fun `test type mismatch E0308 function parameter`() = checkByText("""
        fn foo(_: u8) {}
        fn main () {
            foo(<error>1u16</error>)
        }
    """)

    fun `test type mismatch same type name`() = checkByText("""
        mod a {
            pub struct Foo;
        }
        struct Foo;
        fn foo() -> Foo {
            <error descr="mismatched types [E0308]expected `Foo`, found `test_package::a::Foo`">a::Foo</error>
        }
    """)

    fun `test type mismatch same type name behind alias`() = checkByText("""
        mod a {
            pub struct Foo {}
            pub type Bar = Foo;
            impl Bar {
                pub fn new() -> Bar { Foo {} }
            }
        }
        struct Bar;
        fn foo() -> Bar {
            <error descr="mismatched types [E0308]expected `Bar`, found `test_package::a::Bar`">a::Bar::new()</error>
        }
    """)

    fun `test type mismatch E0308 unconstrained integer`() = checkByText("""
        struct S;
        fn main () {
            let mut a = 0;
            a = <error>S</error>;
        }
    """)

    // issue #1753
    fun `test no type mismatch E0308 for multiple impls of the same trait`() = checkByText("""
        pub trait From<T> { fn from(_: T) -> Self; }
        struct A; struct B; struct C;
        impl From<B> for A { fn from(_: B) -> A { unimplemented!() } }
        impl From<C> for A { fn from(_: C) -> A { unimplemented!() } }
        fn main() {
            A::from(C);
        }
    """)

    // issue #1790
    fun `test not type mismatch E0308 with unknown size array`() = checkByText("""
        struct Foo {
            v: [usize; COUNT]
        }

        fn main() {
            let x = Foo { v: [10, 20] };
        }
    """)

    // issue 1753
    fun `test no type mismatch E0308 on struct argument reference coercion`() = checkByText("""
        #[lang = "deref"]
        trait Deref { type Target; }

        struct Wrapper<T>(T);
        struct RefWrapper<'a, T : 'a>(&'a T);

        impl<T> Deref for Wrapper<T> { type Target = T; }

        fn foo(w: &Wrapper<u32>) {
            let _: RefWrapper<u32> = RefWrapper(w);
        }
    """)

    // https://github.com/intellij-rust/intellij-rust/issues/2670
    // https://github.com/intellij-rust/intellij-rust/issues/3791
    fun `test no type mismatch E0308 when matching with string literal`() = checkByText("""
        fn main() {
            match "" {
                "" => {}
                _ => {}
            }
            match &("", "") {
                ("", "") => {},
                _ => {}
            }
        }
    """)

    fun `test no type mismatch E0308 when matching with string constant`() = checkByText("""
        mod a {
            pub const A: &str = "";
        }
        fn main() {
            match "" {
                a::A => {}
                _ => {}
            }
        }
    """)

    // https://github.com/intellij-rust/intellij-rust/issues/2482
    @ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
    fun `test issue 2482`() = checkByText("""
        fn main() {
            let string: String = "string".to_owned();
        }
    """)

    // https://github.com/intellij-rust/intellij-rust/issues/2460
    @ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
    fun `test issue 2460`() = checkByText("""
        fn f64compare(x: &f64, y: &f64) -> ::std::cmp::Ordering {
            x.partial_cmp(y).unwrap()
        }
    """)

    /** Issue [2713](https://github.com/intellij-rust/intellij-rust/issues/2713) */
    @ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
    @ExpandMacros(MacroExpansionScope.ALL, "std")
    fun `test issue 2713`() = checkByText("""
        fn main() { u64::from(0u8); }
    """)

    fun `test assoc fn of type param with multiple bounds of the same trait`() = checkByText("""
        trait From<T> { fn from(_: T) -> Self; }
        fn foo<T: From<u8> + From<i8>>(_: T) {
            T::from(0u8);
            T::from(0i8);
        }
    """)

    /** Issue [3125](https://github.com/intellij-rust/intellij-rust/issues/3125) */
    @ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
    fun `test issue 3125`() = checkByText("""
        struct S;

        trait Trait {
            type Item;
            fn foo() -> Self::Item;
        }

        impl Trait for S {
            type Item = &'static str;
            fn foo() -> Self::Item { <error>0</error> }
        }
    """)

    fun `test type mismatch E0308 yield expr`() = checkByText("""
        fn main() {
            || {
                yield 0;
                yield <error>"string"</error>;
            };
        }
    """)

    fun `test ! unification`() = checkByText("""
        fn unify<T>(_: T, _: T) {}

        fn main() {
            unify(0, panic!());
            unify(panic!(), 0);
        }
    """)

    @ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
    fun `test question mark expression in closure with different error types`() = checkByText("""
        struct ErrorTo;
        struct ErrorFrom;
        impl From<ErrorFrom> for ErrorTo {
            fn from(_: ErrorFrom) -> Self { ErrorTo }
        }
        fn main() {
            let a: Result<(), ErrorTo> = (|| {
                Err(ErrorFrom)?;
                Ok(())
            })();
        }
    """)

    fun `test type mismatch E0308 partially unknown type`() = checkByText("""
        struct A<T>(T);
        struct B<T>(T);

        fn foo() -> A<i32> {
            return <error>B(Unknown)</error>
        }
    """)

    fun `test no type mismatch E0308 on reference coercion of partially unknown type`() = checkByText("""
        struct Bar;
        fn foo(a: &Bar) {}

        fn main() {
            foo(&Unknown);
        }
    """)

    fun `test async block expr expected`() = checkByText("""
        #[lang = "core::future::future::Future"]
        trait Future { type Output; }
        fn foo() -> impl Future<Output=i32> {
            async { <error>42.0</error> }
        }
    """)

    fun `test async block expr return`() = checkByText("""
        #[lang = "core::future::future::Future"]
        trait Future { type Output; }
        fn main() {
            async {
                return 1;
                return 2;
                return <error>3.0</error>;
                <error>"4"</error>
            };
        }
    """)

    fun `test wrong ref mutability with unknown generic type`() = checkErrors("""
        struct S<T>(T);
        impl<T> S<T> {
            fn new() -> Self { unreachable!() }
        }

        fn foo(t: &mut S<u64>) {}
        fn bar() {
            let mut s = S::new();
            foo(<error>&s</error>);
        }
    """)

    // Issue https://github.com/intellij-rust/intellij-rust/issues/7420
    fun `test correctly match const arguments with const parameters`() = checkErrors("""
        struct Foo<const N: u8, const S: bool>(u32);

        impl<const N: u8> From<u32> for Foo<N, true> {
                                            // ^^^^ the error should not appear here
            fn from(val: u32) -> Self { Foo(val) }
        }
    """)

    @MockRustcVersion("1.56.0-nightly")
    fun `test let else block`() = checkErrors("""
        #![feature(let_else)]

        fn main() {
            let _x = 0 else { <error descr="mismatched types [E0308]expected `!`, found `i32`">0</error> };
            let _y = 0 else { return <error descr="mismatched types [E0308]expected `()`, found `i32`">0</error> };
        }
    """)

    fun `test const arguments`() = checkErrors("""
        fn foo<T, const C: i32>() {}
        const C1: i32 = 1;
        const C2: bool = true;
        struct S;

        fn main() {
            foo::<i32, 1>;
            foo::<i32, <error descr="mismatched types [E0308]expected `i32`, found `bool`">true</error>>;
            foo::<S, C1>;
            foo::<S, <error descr="mismatched types [E0308]expected `i32`, found `bool`">C2</error>>;
        }
    """)

    fun `test const arguments in path exprs`() = checkErrors("""
        struct S<const N: usize>;

        impl <const N: usize> S<N> {
            fn foo() {}
            fn bar(&self) {}
        }

        fn main() {
            S::<"">::foo(); // TODO: issue https://github.com/intellij-rust/intellij-rust/issues/8150
            S::<<error descr="mismatched types [E0308]expected `usize`, found `&str`">""</error>>.bar();
        }
    """)

    fun `test no errors about unsizing`() = checkErrors("""
        #![feature(unsized_tuple_coercion)]

        #[lang = "sized"]  pub trait Sized {}
        #[lang = "unsize"] pub trait Unsize<T: ?Sized> {}
        #[lang = "coerce_unsized"] pub trait CoerceUnsized<T: ?Sized> {}
        impl<'a, T: ?Sized + Unsize<U>, U: ?Sized> CoerceUnsized<&'a U> for &'a T {}

        struct S<T: ?Sized> {
            head: i32,
            tail: T,
        }

        fn main() {
            let _: &S<[i32]> = &S { head: 0, tail: [1, 2] };
            let _: &(i32, [u8]) = &(1, [2, 3]);
            foo(&bar([1, 2, 3]));
        }
        fn foo(_: &[u8]) {}
        fn bar<T>(t: T) -> T { t }
    """)
}
