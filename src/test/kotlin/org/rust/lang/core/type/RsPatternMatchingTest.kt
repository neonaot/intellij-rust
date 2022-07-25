/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.type

import org.rust.CheckTestmarkHit
import org.rust.ProjectDescriptor
import org.rust.WithStdlibRustProjectDescriptor
import org.rust.lang.core.types.infer.PatternMatchingTestMarks

class RsPatternMatchingTest : RsTypificationTestBase() {
    fun `test if let pattern`() = testExpr("""
        enum E { L(i32), R(bool) }
        fn main() {
            let _ = if let E::L(x) = E::R(false) { x } else { x };
                                                 //^ i32
        }
    """)

    fun `test if let with or pattern 1`() = testExpr("""
        enum E { L(i32), M(i32), R(i32) }
        fn foo(e: E) {
            if let E::L(x) | E::M(x) | E::R(x) = e {
                x;
              //^ i32
            }
        }
    """)

    fun `test if let with or pattern 2`() = testExpr("""
        enum E { L(i32), M(i32), R(i32) }
        enum Option { Ok(E), None }
        fn foo(e: Option<E>) {
            if let Option::Ok(E::L(x) | E::M(x) | E::R(x)) = e {
                x;
              //^ i32
            }
        }
    """)

    fun `test while let pattern`() = testExpr("""
        enum E { L(i32), R(bool) }
        fn main() {
            let e = E::L(92);
            while let E::R(x) = e {
                x
            } //^ bool
        }
    """)

    fun `test while let with or pattern 1`() = testExpr("""
        enum E { L(i32), M(i32), R(i32) }
        fn foo(e: E) {
            while let E::L(x) | E::M(x) | E::R(x)  = e {
                x;
              //^ i32
            }
        }
    """)

    fun `test while let with or pattern 2`() = testExpr("""
        enum E { L(i32), M(i32), R(i32) }
        enum Option { Ok, None }
        fn foo(e: Option) {
            while let Option::Ok(E::L(x) | E::M(x) | E::R(x)) = e {
                x;
              //^ i32
            }
        }
    """)

    fun `test let type ascription`() = testExpr("""
        struct S;
        struct T;

        fn main() {
            let (x, _): (S, T) = unimplemented!();
            x;
          //^ S
        }
    """)

    fun `test let init expr`() = testExpr("""
        struct S;
        struct T;

        fn main() {
            let (_, x) = (S, T);
            x;
          //^ T
        }
    """)

    fun `test let remaining front single in tuple pat`() = testExpr("""
        struct S;
        struct T;
        struct U;

        fn main() {
            let (.., t, u) = (S, T, U);
            u;
          //^ U
        }
    """)

    fun `test let remaining front multiple in tuple pat`() = testExpr("""
        struct S;
        struct T;
        struct U;

        fn main() {
            let (.., u) = (S, T, U);
            u;
          //^ U
        }
    """)

    fun `test let remaining back single in tuple pat`() = testExpr("""
        struct S;
        struct T;
        struct U;

        fn main() {
            let (s, t, ..) = (S, T, U);
            t;
          //^ T
        }
    """)

    fun `test let remaining middle multiple in tuple pat`() = testExpr("""
        struct S;
        struct T;
        struct U;
        struct V;

        fn main() {
            let (s, .., v) = (S, T, U, V);
            v;
          //^ V
        }
    """)

    fun `test let multiple rest pat in tuple pat`() = testExpr("""
        struct S;
        struct T;
        struct U;
        struct V;
        struct W;

        fn main() {
            // Note, multiple `..` patterns are not allowed in correct Rust code
            let (s, .., u, .., w) = (S, T, U, V, W);
            (s, u, w);
          //^ (S, <unknown>, W)
        }
    """)

    fun `test let remaining front single in tuple struct pat`() = testExpr("""
        struct S;
        struct T;
        struct U;
        struct X(S, T, U);

        fn main() {
            let X(.., t, u) = X(S, T, U);
            u;
          //^ U
        }
    """)

    fun `test let remaining front multiple in tuple struct pat`() = testExpr("""
        struct S;
        struct T;
        struct U;
        struct X(S, T, U);

        fn main() {
            let X(.., u) = X(S, T, U);
            u;
          //^ U
        }
    """)

    fun `test let remaining back single in tuple struct pat`() = testExpr("""
        struct S;
        struct T;
        struct U;
        struct X(S, T, U);

        fn main() {
            let X(s, t, ..) = X(S, T, U);
            t;
          //^ T
        }
    """)

    fun `test let remaining middle multiple in tuple struct pat`() = testExpr("""
        struct S;
        struct T;
        struct U;
        struct V;
        struct X(S, T, U, V);

        fn main() {
            let X(s, .., v) = X(S, T, U, V);
            v;
          //^ V
        }
    """)

    fun `test let multiple rest pat in tuple struct pat`() = testExpr("""
        struct S;
        struct T;
        struct U;
        struct V;
        struct W;
        struct X(S, T, U, V, W);

        fn main() {
            // Note, multiple `..` patterns are not allowed in correct Rust code
            let X(s, .., u, .., w) = X(S, T, U, V, W);
            (s, u, w);
          //^ (S, <unknown>, W)
        }
    """)

    fun `test let array rest pat`() = testExpr("""
        struct S;
        fn main() {
            let [x, xs @ ..] = [S, S, S];
            (x, xs);
          //^ (S, [S; 2])
        }
    """)

    fun `test let slice rest pat`() = testExpr("""
         struct S;
        fn main() {
            let slice: &[S] = &[S, S, S];
            let [x, xs @ ..] = slice;
            (x, xs);
          //^ (&S, &[S])
        }
    """)

    fun `test let array ref rest pat`() = testExpr("""
        struct S;
        fn main() {
            let [ref x, ref xs @ ..] = [S, S, S];
            (x, xs);
          //^ (&S, &[S; 2])
        }
    """)

    fun `test let slice ref rest pat`() = testExpr("""
        struct S;
        fn main() {
            let slice: &[S] = &[S, S, S];
            let [ref x, ref xs @ ..] = slice;
            (x, xs);
          //^ (&S, &[S])
        }
    """)

    fun `test let array rest pat without binding`() = testExpr("""
        struct S;
        fn main() {
            let [x, ..] = [S, S, S];
            x;
          //^ S
        }
    """)

    fun `test let array rest pat in front`() = testExpr("""
        struct S;
        fn main() {
            let [xs @ .., x] = [S, S, S];
            (xs, x);
          //^ ([S; 2], S)
        }
    """)

    fun `test let array rest pat in middle`() = testExpr("""
        struct S;
        fn main() {
            let [x1, xs @ .., x2] = [S, S, S];
            (x1, xs, x2);
          //^ (S, [S; 1], S)
        }
    """)

    @CheckTestmarkHit(PatternMatchingTestMarks.NegativeRestSize::class)
    fun `test let array too few elements for rest pat`() = testExpr("""
        struct S;
        fn main() {
            let [x1, x2, x3, x4, xs @ ..] = [S, S, S];
            (x1, x2, x3, x4, xs);
          //^ (S, S, S, S, [S; <unknown>])
        }
    """)

    @CheckTestmarkHit(PatternMatchingTestMarks.MultipleRestPats::class)
    fun `test let array multiple rest pats`() = testExpr("""
        struct S;
        fn main() {
            let [x1, xs1 @ .., x2, xs2 @ .., x3] = [S, S, S, S, S];
            (x1, xs1, x2, xs2, x3);
          //^ (S, [S; <unknown>], S, [S; <unknown>], S)
        }
    """)

    fun `test let or pattern 1`() = testExpr("""
        enum E { L(i32), M(i32), R(i32) }
        fn foo(e: E) {
            let E::L(x) | E::M(x) | E::R(x) = e;
            x;
          //^ i32
        }
    """)

    fun `test let or pattern 2`() = testExpr("""
        enum E { L(i32), M(i32), R(i32) }
        enum Option { Ok(E) }
        fn foo(e: Option) {
            let Option::Ok(E::L(x) | E::M(x) | E::R(x)) = e;
            x;
          //^ i32
        }
    """)

    fun `test struct pattern`() = testExpr("""
        struct S;
        struct T {
            s: S
        }

        fn main() {
            let T { s: x } = T { s: S };
            x;
          //^ S
        }
    """)

    fun `test tuple struct pattern 1`() = testExpr("""
        struct S;
        struct T(S);

        fn main() {
            let T(x) = T(S);
            x;
          //^ S
        }
    """)

    fun `test tuple struct pattern 2`() = testExpr("""
        struct S1;
        struct S2;
        struct T(S1, S2);

        fn main() {
            let T { 1: x, 0: y } = T { 0: S1, 1: S2 };
            (x, y);
          //^ (S2, S1)
        }
    """)

    fun `test nested struct ref pattern 1`() = testExpr("""
        struct S;
        struct T {
            s: S
        }

        fn main() {
            let T { s: ref x } = T { s: S };
            x;
          //^ &S
        }
    """)

    fun `test nested struct ref pattern 2`() = testExpr("""
        struct S;
        struct T {
            s: S
        }

        fn main() {
            let T { ref s } = T { s: S };
            s;
          //^ &S
        }
    """)

    fun `test braced enum variant`() = testExpr("""
        enum E { S { foo: i32 }}

        fn main() {
            let x: E = unimplemented!();
            match x { E::S { foo } => foo };
        }                           //^ i32
    """)

    fun `test fn argument pattern`() = testExpr("""
        struct S;
        struct T;

        fn main((x, _): (S, T)) {
            x;
          //^ S
        }
    """)

    fun `test closure argument`() = testExpr("""
        fn main() {
            let _ = |x: ()| {
                x
              //^ ()
            };
        }
    """)

    fun `test ref pattern`() = testExpr("""
        struct Vec;

        fn bar(vr: &Vec) {
            let &v = vr;
            v;
          //^ Vec
        }
    """)

    fun `test ref pattern 2`() = testExpr("""
        struct Vec;

        fn bar(vr: Vec) {
            let ref v = vr;
            v;
          //^ &Vec
        }
    """)

    fun `test ref pattern 3`() = testExpr("""
        struct Vec;

        fn bar(vr: Vec) {
            let ref v = &vr;
            v;
          //^ &&Vec
        }
    """)

    fun `test mut ref pattern`() = testExpr("""
        struct Vec;

        fn bar(vr: &mut Vec) {
            let &v = vr;
            v;
          //^ Vec
        }
    """)

    fun `test mut ref pattern 2`() = testExpr("""
        struct Vec;

        fn bar(vr: Vec) {
            let ref mut v = vr;
            v;
          //^ &mut Vec
        }
    """)

    fun `test tuple out of bounds`() = testExpr("""
        fn main() {
            let (_, _, x) = (1, 2);
            x
          //^ <unknown>
        }
    """)

    fun `test literal pattern`() = testExpr("""
    fn main() {
        let x: (i32, String) = unimplemented!();
        match x { (x, "foo") => x };
    }                         //^ i32
    """)

    fun `test generic tuple struct pattern`() = testExpr("""
        struct S<T>(T);
        fn main() {
            let s = S(123);
            if let S(x) = s { x }
                            //^ i32
        }
    """)

    fun `test generic struct pattern`() = testExpr("""
        struct S<T> { s: T }
        fn main() {
            let s = S { s: 123 };
            match s { S { s: x } => x };
                                  //^ i32
        }
    """)

    fun `test generic enum tuple struct pattern`() = testExpr("""
        enum E<T1, T2> { L(T1), R { r: T2 } }
        fn foo(e: E<i32, bool>) {
            match e {
                E::L(x) => x,
                         //^ i32
                E::R { r: x } => x
            };
        }
    """)

    fun `test generic enum struct pattern`() = testExpr("""
        enum E<T1, T2> { L(T1), R { r: T2 } }
        fn foo(e: E<i32, bool>) {
            match e {
                E::L(x) => x,
                E::R { r: x } => x
                               //^ bool
            };
        }
    """)

    @ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
    fun `test box pattern (pat box)`() = stubOnlyTypeInfer("""
    //- main.rs
        #![feature(box_patterns)]
        fn main() {
            let a = Box::new(0);
            match a {
                box b => { b; }
            }            //^ i32
        }
    """)

    @ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
    fun `test box pattern (pat field)`() = stubOnlyTypeInfer("""
    //- main.rs
        #![feature(box_patterns)]
        struct S { x: Box<i32> }
        fn main() {
            let a = S { x: Box::new(0) };
            let S { box x } = a;
            x;
          //^ i32
        }
    """)

    // Match ergonomics are enabled if a reference value (on the right side)
    // is matched by a non reference pattern (on the left side)
    // See https://github.com/rust-lang/rfcs/blob/master/text/2005-match-ergonomics.md
    fun `test match ergonomics tuple 1`() = testExpr("""
        fn main() {
            let (a, b) = &(1, 2);
            (a, b);
        } //^ (&i32, &i32)
    """)

    fun `test match ergonomics tuple 2`() = testExpr("""
        fn main() {
            let (a, b) = &mut (1, 2);
            (a, b);
        } //^ (&mut i32, &mut i32)
    """)

    fun `test match ergonomics tuple 3`() = testExpr("""
        fn main() {
            let (a,) = &&(1,);
            a;
        } //^ &i32
    """)

    fun `test match ergonomics tuple 4`() = testExpr("""
        fn main() {
            let (a,) = &mut&(1,);
            a;
        } //^ &i32
    """)

    fun `test match ergonomics tuple 5`() = testExpr("""
        fn main() {
            let (a,) = &&mut(1,);
            a;
        } //^ &i32
    """)

    fun `test match ergonomics tuple 6`() = testExpr("""
        fn main() {
            let (a,) = &mut&mut(1,);
            a;
        } //^ &mut i32
    """)

    // https://github.com/rust-lang/rust/issues/46688
    fun `test match ergonomics tuple 7`() = testExpr("""
        fn main() {
            let (_, &a) = &(1, &2);
            a;
        } //^ i32
    """)

    fun `test match ergonomics tuple ref`() = testExpr("""
        fn main() {
            let (ref a,) = &(1,);
            a;
        } //^ &i32
    """)

    fun `test match ergonomics enum`() = testExpr("""
        enum E { A(i32) }
        fn main() {
            let E::A(a) = &E::A(1);
            a;
        } //^ &i32
    """)

    fun `test match ergonomics enum ref`() = testExpr("""
        enum E { A(i32) }
        fn main() {
            let E::A(ref a) = &E::A(1);
            a;
        } //^ &i32
    """)

    fun `test match ergonomics enum mut`() = testExpr("""
        enum E { A(i32) }
        fn main() {
            let E::A(mut a) = &E::A(1);
            a;
        } //^ i32
    """)

    // This produces "error[E0596]: cannot borrow anonymous field of immutable binding as mutable"
    // but still infers the type &mut i32
    fun `test match ergonomics enum ref mut incorrect`() = testExpr("""
        enum E { A(i32) }
        fn main() {
            let E::A(ref mut a) = &E::A(1);
            a;
        } //^ &mut i32
    """)

    fun `test match ergonomics enum ref mut`() = testExpr("""
        enum E { A(i32) }
        fn main() {
            let E::A(ref mut a) = &mut E::A(1);
            a;
        } //^ &mut i32
    """)

    fun `test match ergonomics struct short`() = testExpr("""
        struct S { a: i32 }
        fn main() {
            let S { a } = &S{ a: 1 };
            a;
        } //^ &i32
    """)

    fun `test match ergonomics struct long`() = testExpr("""
        struct S { a: i32 }
        fn main() {
            let S { a: a } = &S{ a: 1 };
            a;
        } //^ &i32
    """)

    fun `test match ergonomics struct ref short`() = testExpr("""
        struct S { a: i32 }
        fn main() {
            let S { ref a } = &S{ a: 1 };
            a;
        } //^ &i32
    """)

    fun `test match ergonomics struct ref long`() = testExpr("""
        struct S { a: i32 }
        fn main() {
            let S { a: ref a } = &S{ a: 1 };
            a;
        } //^ &i32
    """)

    fun `test match ergonomics struct mut short`() = testExpr("""
        struct S { a: i32 }
        fn main() {
            let S { mut a } = &S { a: 1 };
            a;
        } //^ i32
    """)

    fun `test match ergonomics struct mut long`() = testExpr("""
        struct S { a: i32 }
        fn main() {
            let S { a: mut a } = &S { a: 1 };
            a;
        } //^ i32
    """)

    fun `test match ergonomics struct ref mut short`() = testExpr("""
        struct S { a: i32 }
        fn main() {
            let S { ref mut a } = &S { a: 1 };
            a;
        } //^ &mut i32
    """)

    fun `test match ergonomics struct ref mut long`() = testExpr("""
        struct S { a: i32 }
        fn main() {
            let S { a: ref mut a } = &S { a: 1 };
            a;
        } //^ &mut i32
    """)

    fun `test match ergonomics slice 1`() = testExpr("""
        fn main() {
            let [a, b] = &[1, 2];
            a;
        } //^ &i32
    """)

    fun `test match ergonomics slice 2`() = testExpr("""
        fn main() {
            let [ref a, b] = &[1, 2];
            a;
        } //^ &i32
    """)

    @ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
    fun `test match ergonomics box`() = stubOnlyTypeInfer("""
    //- main.rs
        #![feature(box_patterns)]
        fn main() {
            let a = &Box::new(0);
            match a {
                box b => { b; }
            }            //^ &i32
        }
    """)

    fun `test double ref`() = testExpr("""
        fn main() {
            let ref a = &0;
            a;
        } //^ &&i32
    """)

    @ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
    fun `test unknown`() = testExpr("""
        fn main() {
            let x = unknown;
            match x {
                Some(n) => {
                    n;
                  //^ <unknown>
                }
                _ => {}
            };
        }
    """)
}
