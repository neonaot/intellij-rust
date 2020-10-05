/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve

class RsMacroResolveTest : RsResolveTestBase() {
    fun `test resolve simple matching with multiple pattern definition`() = checkByCode("""
        macro_rules! test {
            ($ test:expr) => (
               //X
                $ test
                 //^
            )
            ($ test:expr) => (
                $ test
            )
        }
    """)

    fun `test resolve simple matching with multiple matching`() = checkByCode("""
        macro_rules! test {
            ($ test:expr, $ ty:ty) => (
               //X
                $ test
                 //^
            )
        }
    """)

    fun `test resolve simple matching in complex matching`() = checkByCode("""
        macro_rules! test {
            ($ ($ test:expr),+, $ ty:ty) => (
              //X
                $ ($ test),+
                   //^
            )
        }
    """)

    fun `test resolve macro same scope`() = checkByCode("""
        macro_rules! foo_bar { () => () }
        //X
        foo_bar!();
        //^
    """)

    fun `test resolve macro in function`() = checkByCode("""
        macro_rules! foo_bar { () => () }
        //X
        fn main() {
            foo_bar!();
            //^
        }
    """)

    fun `test resolve macro mod lower`() = checkByCode("""
        macro_rules! foo_bar { () => () }
        //X
        mod b {
            fn main() {
                foo_bar!();
                //^
            }
        }
    """)

    fun `test resolve macro mod`() = checkByCode("""
        #[macro_use]
        mod a {
            macro_rules! foo_bar { () => () }
            //X
        }
        mod b {
            fn main() {
                foo_bar!();
                //^
            }
        }
    """)

    // Macros are scoped by lexical order
    // see https://github.com/intellij-rust/intellij-rust/issues/1474
    fun `test resolve macro mod wrong order`() = checkByCode("""
        mod b {
            fn main() {
                foo_bar!();
                //^ unresolved
            }
        }
        #[macro_use]
        mod a {
            macro_rules! foo_bar { () => () }
        }
    """)

    fun `test resolve macro in lexical order 1`() = checkByCode("""
        macro_rules! foo { () => () }
        #[macro_use]
        mod a {
            macro_rules! foo { () => () }
            #[macro_use]
            mod b {
                macro_rules! foo { () => () }
                macro_rules! foo { () => () }
            }  //X
        }
        fn main() {
            foo!();
        } //^
    """)

    fun `test resolve macro in lexical order 2`() = checkByCode("""
       #[macro_use]
        pub mod a {
            macro_rules! foo { () => () }
            macro_rules! foo { () => () }
            //X
            pub mod b {
                pub fn foo() {
                    foo!();
                    //^
                }
            }

        }
    """)

    fun `test resolve macro in lexical order 3`() = checkByCode("""
        macro_rules! foo { () => () }
        #[macro_use]
        mod a {
            macro_rules! bar { () => () }
            #[macro_use]
            mod b {
                macro_rules! foo { () => () }
                //X
                macro_rules! bar { () => () }
            }
        }
        fn main() {
            foo!();
        } //^
    """)

    fun `test resolve macro missing macro_use`() = checkByCode("""
        // Missing #[macro_use] here
        mod a {
            macro_rules! foo_bar { () => () }
        }
        fn main() {
            foo_bar!();
            //^ unresolved
        }
    """)

    fun `test macro_export macro is visible in the same crate without macro_use`() = checkByCode("""
        // #[macro_use] is not needed here
        mod a {
            #[macro_export]
            macro_rules! foo_bar { () => () }
        }                //X
        fn main() {
            foo_bar!();
            //^
        }
    """)

    fun `test resolve macro missing macro_use mod`() = checkByCode("""
        // Missing #[macro_use] here
        mod a {
            macro_rules! foo_bar { () => () }
        }
        mod b {
            fn main() {
                foo_bar!();
                //^ unresolved
            }
        }
    """)

    fun `test raw identifier 1`() = checkByCode("""
        macro_rules! r#match { () => () }
                     //X
        r#match!();
           //^
    """)

    fun `test raw identifier 2`() = checkByCode("""
        macro_rules! foo { () => () }
                    //X
        r#foo!();
         //^
    """)

    fun `test macro call with crate prefix 1`() = checkByCode("""
        #[macro_export]
        macro_rules! foo { () => () }
                    //X
        crate::foo!();
              //^
    """)

    fun `test macro call with crate prefix 2`() = checkByCode("""
        macro_rules! foo { () => () }

        crate::foo!();
              //^ unresolved
    """)

    // More macro tests in [RsPackageLibraryResolveTest] and [RsStubOnlyResolveTest]
}
