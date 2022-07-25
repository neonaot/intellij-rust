/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions

import org.rust.ProjectDescriptor
import org.rust.WithStdlibRustProjectDescriptor

@ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
class MatchToIfLetIntentionTest : RsIntentionTestBase(MatchToIfLetIntention::class) {
    fun `test availability range`() = checkAvailableInSelectionOnly("""
        enum MyOption { Some(x) }

        fn main() {
            let color = MyOption::Some(52);

            <selection>match</selection> color {
                MyOption::Some(42) => {
                    let a = x + 1;
                }
                _ => {}
            }
        }
    """)

    fun `test unavailable all void arms`() = doUnavailableTest("""
        enum MyOption {
            Nothing,
            Some(x),
        }

        fn main() {
            let a = MyOption::Some(52);

            /*caret*/match a {
                MyOption::Some(x) => {}
                Nothing => {}
            }
        }
    """)

    fun `test unavailable all not void arms`() = doUnavailableTest("""
        enum MyOption {
            Nothing,
            Some(x),
        }

        fn main() {
            let a = MyOption::Some(52);

            match a {
                MyOption::Some(x) => {42}
                Nothing => {43}/*caret*/
            }
        }
    """)

    fun `test unavailable pattern`() = doAvailableTest("""
        enum OptionColor {
            NoColor,
            Color(i32, i32, i32),
        }

        fn main() {
            let color = OptionColor::Color(255, 255, 255);

            /*caret*/match color {
                OptionColor::Color(_, _, _) => {}
                _ => {print!("No color")}
            };
        }
    """, """
        enum OptionColor {
            NoColor,
            Color(i32, i32, i32),
        }

        fn main() {
            let color = OptionColor::Color(255, 255, 255);

            if let _ = color { print!("No color") };
        }
    """)

    fun `test simple 1`() = doAvailableTest("""
        enum MyOption {
            Some(x)
        }

        fn main() {
            let color = MyOption::Some(52);

            /*caret*/match color {
                MyOption::Some(42) => {
                    let a = x + 1;
                    let b = x + 2;
                    let c = a + b;
                }
                _ => {}
            }
        }
    """, """
        enum MyOption {
            Some(x)
        }

        fn main() {
            let color = MyOption::Some(52);

            if let MyOption::Some(42) = color {
                let a = x + 1;
                let b = x + 2;
                let c = a + b;
            }
        }
    """)

    fun `test simple 2`() = doAvailableTest("""
        enum OptionColor {
            NoColor,
            Color(i32, i32, i32),
        }

        fn main() {
            let color = OptionColor::Color(255, 255, 255);

            /*caret*/match color {
                OptionColor::Color(255, 255, 255) => print!("White"),
                OptionColor::Color(_,   _,   _  ) => {}
                OptionColor::NoColor => {}
            };
        }
    """, """
        enum OptionColor {
            NoColor,
            Color(i32, i32, i32),
        }

        fn main() {
            let color = OptionColor::Color(255, 255, 255);

            if let OptionColor::Color(255, 255, 255) = color {
                print!("White")
            };
        }
    """)

    fun `test simple with range`() = doAvailableTest("""
        fn main() {
            let e = 4;
            /*caret*/match e {
                1..=5 => println!("got {}", e),
                _ => {}
            };
        }
    """, """
        fn main() {
            let e = 4;
            if let 1..=5 = e {
                println!("got {}", e)
            };
        }
    """)

    fun `test simple with const`() = doAvailableTest("""
        fn main() {
            let e = 4;
            /*caret*/match e {
                4 => println!("got {}", e),
                _ => {}
            };
        }
    """, """
        fn main() {
            let e = 4;
            if let 4 = e {
                println!("got {}", e)
            };
        }
    """)

    fun `test simple with struct`() = doAvailableTest("""
        struct Point {
            x: bool,
            y: bool,
        }

        fn main() {
            let point = Point { x: false, y: true };
            /*caret*/match point {
                Point { x: true, .. } => println!("42"),
                _ => {}
            }
        }
    """, """
        struct Point {
            x: bool,
            y: bool,
        }

        fn main() {
            let point = Point { x: false, y: true };
            if let Point { x: true, .. } = point {
                println!("42")
            }
        }
    """)

    fun `test simple with struct 2`() = doAvailableTest("""
        struct Point {
            x: bool,
            y: bool,
        }

        fn main() {
            let point = Point { x: false, y: true };
            /*caret*/match point {
                Point { x: true, y: f } => println!("42"),
                _ => {}
            }
        }
    """, """
        struct Point {
            x: bool,
            y: bool,
        }

        fn main() {
            let point = Point { x: false, y: true };
            if let Point { x: true, y: f } = point {
                println!("42")
            }
        }
    """)

    fun `test irrefutable pattern with struct`() = doAvailableTest("""
        struct Point {
            x: bool,
            y: bool,
        }

        fn main() {
            let point = Point { x: false, y: true };
            /*caret*/match point {
                Point { x, .. } => println!("42"),
                _ => {}
            }
        }
    """, """
        struct Point {
            x: bool,
            y: bool,
        }

        fn main() {
            let point = Point { x: false, y: true };
            if let Point { x, .. } = point {
                println!("42")
            }
        }
    """)

    fun `test irrefutable pattern with ident`() = doAvailableTest("""
        fn main() {
            let e = Some(32);
            /*caret*/match e {
                a => println!("got {}", a.unwrap()),
                _ => ()
            }
        }
    """, """
        fn main() {
            let e = Some(32);
            if let a = e {
                println!("got {}", a.unwrap())
            }
        }
    """)

    fun `test irrefutable pattern with tup`() = doAvailableTest("""
        fn main() {
            let e = Some(32);
            /*caret*/match e {
                (a) => println!("got {:?}", a),
                _ => ()
            }
        }
    """, """
        fn main() {
            let e = Some(32);
            if let (a) = e {
                println!("got {:?}", a)
            }
        }
    """)

    fun `test available pattern with tup`() = doAvailableTest("""
        fn main() {
            let e = Some(32);
            /*caret*/match e {
                (Some(42)) => println!("got {:?}", a),
                _ => ()
            }
        }
    """, """
        fn main() {
            let e = Some(32);
            if let (Some(42)) = e {
                println!("got {:?}", a)
            }
        }
    """)

    fun `test available pattern with tup 2`() = doAvailableTest("""
        fn main() {
            let e = (42, 50);
            /*caret*/match e {
                (a, 50) => println!("got {:?}", a),
                _ => ()
            }
        }
    """, """
        fn main() {
            let e = (42, 50);
            if let (a, 50) = e {
                println!("got {:?}", a)
            }
        }
    """)

    fun `test unavailable pattern with guard`() = doUnavailableTest("""
        fn main() {
            let e = 42;
            /*caret*/match e {
                a if a < 6 => println!("got {}", a),
                _ => ()
            }
        }
    """)

    fun `test unavailable pattern with attr`() = doUnavailableTest("""
        fn main() {
            let e = Some(42);
            /*caret*/match e {
                #[cold] Some(a) => println!("got {}", a),
                _ => ()
            }
        }
    """)

    fun `test available with slice`() = doAvailableTest("""
        fn main() {
            let x = [1, 2];
            /*caret*/match x {
                [f] => println!("42"),
                _ => {}
            }
        }
    """, """
        fn main() {
            let x = [1, 2];
            if let [f] = x {
                println!("42")
            }
        }
    """)

    fun `test irrefutable pattern with slice 1`() = doAvailableTest("""
        fn main() {
            let x = [1, 2];
            /*caret*/match x {
                [..] => println!("42"),
                _ => {}
            }
        }
    """, """
        fn main() {
            let x = [1, 2];
            if let [..] = x {
                println!("42")
            }
        }
    """)

    fun `test irrefutable pattern with slice 2`() = doAvailableTest("""
        fn main() {
            let x = [1, 2];
            /*caret*/match x {
                [f @ ..] => println!("42"),
                _ => {}
            }
        }
    """, """
        fn main() {
            let x = [1, 2];
            if let [f @ ..] = x {
                println!("42")
            }
        }
    """)

    fun `test available with box`() = doAvailableTest("""
        fn main() {
            let x = box 42;
            /*caret*/match x {
                box 42 => println!("42"),
                _ => {}
            }
        }
    """, """
        fn main() {
            let x = box 42;
            if let box 42 = x {
                println!("42")
            }
        }
    """)

    fun `test irrefutable pattern with box`() = doAvailableTest("""
        fn main() {
            let x = box 42;
            /*caret*/match x {
                box a => println!("42"),
                _ => {}
            }
        }
    """, """
        fn main() {
            let x = box 42;
            if let box a = x {
                println!("42")
            }
        }
    """)

    fun `test available with ref`() = doAvailableTest("""
        fn main() {
            let x = &42;
            /*caret*/match x {
                &42 => println!("42"),
                _ => {}
            }
        }
    """, """
        fn main() {
            let x = &42;
            if let &42 = x {
                println!("42")
            }
        }
    """)

    fun `test irrefutable pattern with ref`() = doAvailableTest("""
        fn main() {
            let x = &42;
            /*caret*/match x {
                &a => println!("42"),
                _ => {}
            }
        }
    """, """
        fn main() {
            let x = &42;
            if let &a = x {
                println!("42")
            }
        }
    """)

    fun `test available with some`() = doAvailableTest("""
        fn main() {
            let x = Some(42);
            /*caret*/match x {
                Some(a) => println!("42"),
                _ => {}
            }
        }
    """, """
        fn main() {
            let x = Some(42);
            if let Some(a) = x {
                println!("42")
            }
        }
    """)

    fun `test available with none`() = doAvailableTest("""
        fn main() {
            let x = Some(42);
            /*caret*/match x {
                None => println!("42"),
                _ => {}
            }
        }
    """, """
        fn main() {
            let x = Some(42);
            if let None = x {
                println!("42")
            }
        }
    """)

    fun `test available with ok`() = doAvailableTest("""
        fn main() {
            let x: Result<i32, i32> = Ok(42);
            /*caret*/match x {
                Ok(a) => println!("42"),
                _ => {}
            }
        }
    """, """
        fn main() {
            let x: Result<i32, i32> = Ok(42);
            if let Ok(a) = x {
                println!("42")
            }
        }
    """)

    fun `test available with err`() = doAvailableTest("""
        fn main() {
            let x: Result<i32, i32> = Ok(42);
            /*caret*/match x {
                Err(e) => println!("42"),
                _ => {}
            }
        }
    """, """
        fn main() {
            let x: Result<i32, i32> = Ok(42);
            if let Err(e) = x {
                println!("42")
            }
        }
    """)

    fun `test available with unnecessary parentheses around pattern`() = doAvailableTest("""
        fn main() {
            let x: Result<i32, i32> = Ok(42);
            /*caret*/match x {
                (((Err(e)))) => println!("42"),
                _ => {}
            }
        }
    """, """
        fn main() {
            let x: Result<i32, i32> = Ok(42);
            if let (((Err(e)))) = x {
                println!("42")
            }
        }
    """)

    fun `test multiple if let pattern`() = doAvailableTest("""
        enum V { V1(i32), V2(i32), V3 }
        fn foo(v: V) {
            /*caret*/match v {
                V1(x) | V2(x) => {
                    println!("{}", x);
                }
                _ => {}
            }
        }
    """, """
        enum V { V1(i32), V2(i32), V3 }
        fn foo(v: V) {
            if let V1(x) | V2(x) = v {
                println!("{}", x);
            }
        }
    """)

    fun `test multiple if let pattern with a leading vertical bar`() = doAvailableTest("""
        enum V { V1(i32), V2(i32), V3 }
        fn foo(v: V) {
            /*caret*/match v {
                | V1(x) | V2(x) => {
                    println!("{}", x);
                }
                _ => {}
            }
        }
    """, """
        enum V { V1(i32), V2(i32), V3 }
        fn foo(v: V) {
            if let | V1(x) | V2(x) = v {
                println!("{}", x);
            }
        }
    """)
}
