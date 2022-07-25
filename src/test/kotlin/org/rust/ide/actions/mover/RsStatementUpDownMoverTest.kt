/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.mover

import org.rust.CheckTestmarkHit

class RsStatementUpDownMoverTest : RsStatementUpDownMoverTestBase() {
    fun `test straightline down`() = moveDown("""
        fn main() {
            1;
            2/*caret*/;
            3;
        }
    """, """
        fn main() {
            1;
            3;
            2/*caret*/;
        }
    """)

    fun `test straightline up`() = moveUp("""
        fn main() {
            1;
            2;
            3/*caret*/;
        }
    """, """
        fn main() {
            1;
            3/*caret*/;
            2;
        }
    """)

    @CheckTestmarkHit(UpDownMoverTestMarks.MoveOutOfBody::class)
    fun `test straightline down out of body`() = moveDown("""
        fn main() {
            1;
            2;
            3/*caret*/;
        }
    """)

    @CheckTestmarkHit(UpDownMoverTestMarks.MoveOutOfBody::class)
    fun `test straightline up out of body`() = moveUp("""
        fn main() {
            1/*caret*/;
            2;
            3;
        }
    """)

    fun `test up into if block`() = moveUp("""
        fn main() {
            if something {
            }
            1/*caret*/;
        }
    """, """
        fn main() {
            if something {
                1/*caret*/;
            }
        }
    """)

    fun `test down from if block`() = moveDown("""
        fn main() {
            if something {
               1/*caret*/;
            }
        }
    """, """
        fn main() {
            if something {
            }
            1/*caret*/;
        }
    """)

    fun `test up into else block`() = moveUp("""
        fn main() {
            if something {
            } else {
            }
            1/*caret*/;
        }
    """, """
        fn main() {
            if something {
            } else {
                1/*caret*/;
            }
        }
    """)

    fun `test down from else block`() = moveDown("""
        fn main() {
            if something {
            } else {
                1/*caret*/;
            }
        }
    """, """
        fn main() {
            if something {
            } else {
            }
            1/*caret*/;
        }
    """)

    fun `test down into if block`() = moveDown("""
        fn main() {
            1/*caret*/;
            if something {
            } else {
                2;
            }
        }
    """, """
        fn main() {
            if something {
                1/*caret*/;
            } else {
                2;
            }
        }
    """)

    fun `test up from if block`() = moveUp("""
        fn main() {
            if something {
                1/*caret*/;
            } else {
            }
        }
    """, """
        fn main() {
            1/*caret*/;
            if something {
            } else {
            }
        }
    """)

    fun `test down into else block from if block`() = moveDown("""
        fn main() {
            if something {
                1/*caret*/;
            } else {
            }
        }
    """, """
        fn main() {
            if something {
            } else {
                1/*caret*/;
            }
        }
    """)

    fun `test up into if block from else block`() = moveDown("""
        fn main() {
            if something {
                1/*caret*/;
            } else {
            }
        }
    """, """
        fn main() {
            if something {
            } else {
                1/*caret*/;
            }
        }
    """)

    fun `test expr down`() = moveDown("""
        fn main() {
            if something {
                1;
                2/*caret*/
            }
            3;
        }
    """, """
        fn main() {
            if something {
                1;
            }
            2/*caret*/
            3;
        }
    """)

    fun `test expr down 2`() = moveDown("""
        fn main() {
            for x in xs {
                1;
                if something/*caret*/ {}
            }
            2;
            3;
        }
    """, """
        fn main() {
            for x in xs {
                1;
            }
            if something/*caret*/ {}
            2;
            3;
        }
    """)

    fun `test expr up 2`() = moveUp("""
        fn main() {
            1;
            for x in xs {
                2;
                if something/*caret*/ {}
            }
            3;
        }
    """, """
        fn main() {
            1;
            for x in xs {
                if something/*caret*/ {}
                2;
            }
            3;
        }
    """)


    fun `test expr up`() = moveUp("""
        fn main() {
            1;
            2/*caret*/
        }
    """, """
        fn main() {
            2/*caret*/
            1;
        }
    """)

    fun `test expr down nested blocks`() = moveDown("""
        fn main() {
            if something1 {
                if something2 {
                    1;
                    42/*caret*/
                }
                2;
            }
            3;
        }
    """, """
        fn main() {
            if something1 {
                if something2 {
                    1;
                }
                42/*caret*/
                2;
            }
            3;
        }
    """)

    fun `test expr up nested blocks`() = moveUp("""
        fn main() {
            if something1 {
                1;
                if something2 {
                    2;
                    match x/*caret*/ {}
                }
            }
        }
    """, """
        fn main() {
            if something1 {
                1;
                if something2 {
                    match x/*caret*/ {}
                    2;
                }
            }
        }
    """)

    fun `test down into for loop`() = moveDown("""
        fn main() {
            1/*caret*/;
            for x in xs {
                2;
            }
        }
    """, """
        fn main() {
            for x in xs {
                1/*caret*/;
                2;
            }
        }
    """)

    fun `test down into while loop`() = moveDown("""
        fn main() {
            1/*caret*/;
            while something {
                2;
            }
        }
    """, """
        fn main() {
            while something {
                1/*caret*/;
                2;
            }
        }
    """)

    fun `test down into loop`() = moveDown("""
        fn main() {
            1/*caret*/;
            loop {
                2;
            }
        }
    """, """
        fn main() {
            loop {
                1/*caret*/;
                2;
            }
        }
    """)

    fun `test inside closure`() = moveDown("""
        fn main() {
            let f = |x| {
                1/*caret*/
                2;
            };
        }
    """, """
        fn main() {
            let f = |x| {
                2;
                1/*caret*/
            };
        }
    """)

    fun `test down into closure`() = moveDown("""
        fn main() {
            1/*caret*/;
            |x| {
                2;
            }
        }
    """, """
        fn main() {
            |x| {
                1/*caret*/;
                2;
            }
        }
    """)

    fun `test up from closure`() = moveUp("""
        fn main() {
            |x| {
                1/*caret*/;
                2;
            }
        }
    """, """
        fn main() {
            1/*caret*/;
            |x| {
                2;
            }
        }
    """)

    fun `test multiple dot expr`() = moveUp("""
        fn main() {
            if something {
                1;
            }
            some_function/*caret*/()
                .abc()
                .xyz()
        }
    """, """
        fn main() {
            if something {
                1;
                some_function/*caret*/()
                    .abc()
                    .xyz()
            }
        }
    """)

    fun `test up empty line`() = moveUp("""
        fn main() {
            1;

            2/*caret*/;
        }
    """, """
        fn main() {
            1;
            2/*caret*/;

        }
    """)

    fun `test down empty line`() = moveDown("""
        fn main() {
            1/*caret*/;

            2;
        }
    """, """
        fn main() {

            1/*caret*/;
            2;
        }
    """)

    fun `test down empty line block`() = moveDown("""
        fn main() {
            1/*caret*/;

            if cond {
            }
        }
    """, """
        fn main() {

            1/*caret*/;
            if cond {
            }
        }
    """)

    fun `test up empty line block`() = moveUp("""
        fn main() {
            if cond {
            }

            1/*caret*/;
        }
    """, """
        fn main() {
            if cond {
            }
            1/*caret*/;

        }
    """)

    fun `test up empty line at start`() = moveUp("""
        fn main() {

            1/*caret*/;
        }
    """, """
        fn main() {
            1/*caret*/;

        }
    """)

    fun `test up empty line at end`() = moveDown("""
        fn main() {
            1/*caret*/;

        }
    """, """
        fn main() {

            1/*caret*/;
        }
    """)


    // https://github.com/intellij-rust/intellij-rust/issues/3548
    fun `test statement ends with comment`() = moveDown("""
        fn main() {
            1;
            {
                2;
                /*caret*/3; // 123
                4;
            }
            5;
        }
    """, """
        fn main() {
            1;
            {
                2;
                4;
                /*caret*/3; // 123
            }
            5;
        }
    """)

    fun `test move up statement in match arm body`() = moveUp("""
        fn main() {
             match x {
                 foo => {
                     1;
                 }
                 bar => {
                     2;
                     /*caret*/3;
                     4;
                 }
                 baz => {
                     5;
                 }
             };
        }
    """, """
        fn main() {
             match x {
                 foo => {
                     1;
                 }
                 bar => {
                     /*caret*/3;
                     2;
                     4;
                 }
                 baz => {
                     5;
                 }
             };
        }
    """)

    fun `test move down statement in match arm body`() = moveDown("""
        fn main() {
             match x {
                 foo => {
                     1;
                 }
                 bar => {
                     2;
                     3;/*caret*/
                     4;
                 }
                 baz => {
                     5;
                 }
             };
        }
    """, """
        fn main() {
             match x {
                 foo => {
                     1;
                 }
                 bar => {
                     2;
                     4;
                     3;/*caret*/
                 }
                 baz => {
                     5;
                 }
             };
        }
    """)

    @CheckTestmarkHit(UpDownMoverTestMarks.MoveOutOfBody::class)
    fun `test move up first statement in match arm body`() = moveUp("""
        fn main() {
             match x {
                 foo => {
                     1;
                 }
                 bar => {
                     2;/*caret*/
                     3;
                     4;
                 }
                 baz => {
                     5;
                 }
             };
        }
    """)

    @CheckTestmarkHit(UpDownMoverTestMarks.MoveOutOfBody::class)
    fun `test move down last statement in match arm body`() = moveDown("""
        fn main() {
             match x {
                 foo => {
                     1;
                 }
                 bar => {
                     2;
                     3;
                     4;/*caret*/
                 }
                 baz => {
                     5;
                 }
             };
        }
    """)

    // https://github.com/intellij-rust/intellij-rust/issues/7376
    fun `test move down in block inside argument list`() = moveDown("""
        fn foo(a: u32) {}

        fn main() {
            foo({
                println!("1");/*caret*/
                println!("2");
                1
            });
        }
    """, """
        fn foo(a: u32) {}

        fn main() {
            foo({
                println!("2");
                println!("1");/*caret*/
                1
            });
        }
    """)

    @CheckTestmarkHit(UpDownMoverTestMarks.MoveOutOfBody::class)
    fun `test move up first statement of block expr inside argument list`() = moveUp("""
        fn foo(a: u32) {}

        fn main() {
            foo({
                println!("1");/*caret*/
                println!("2");
                1
            });
        }
    """)

    @CheckTestmarkHit(UpDownMoverTestMarks.MoveOutOfBody::class)
    fun `test move up first statement of lambda inside argument list`() = moveUp("""
        fn foo<F: Fn() -> ()>(f: F) -> u32 { 0 }

        struct S {
            a: u32,
            b: u32
        }

        fn main() {
            let s = S {
                a: 0,
                b: foo(|| {
                    println!("1");/*caret*/
                    println!("2");
                    1
                })
            };
        }
    """)

    @CheckTestmarkHit(UpDownMoverTestMarks.MoveOutOfBody::class)
    fun `test move up first statement of lambda ref inside argument list`() = moveUp("""
        fn foo<F: Fn() -> ()>(f: &mut F) -> u32 { 0 }

        struct S {
            a: u32,
            b: u32
        }

        fn main() {
            let s = S {
                a: 0,
                b: foo(&mut || {
                    println!("1");/*caret*/
                    println!("2");
                    1
                })
            };
        }
    """)
}
