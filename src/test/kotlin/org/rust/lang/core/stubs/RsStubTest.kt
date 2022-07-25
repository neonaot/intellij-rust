/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs

import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.stubs.StubTreeLoader
import org.intellij.lang.annotations.Language
import org.rust.RsTestBase
import org.rust.fileTreeFromText

class RsStubTest : RsTestBase() {

    fun `test literal is not stubbed inside statement`() = doTest("""
        fn foo() { 0; }
    """, """
        RsFileStub
          FUNCTION:RsFunctionStub
            VALUE_PARAMETER_LIST:RsPlaceholderStub
    """)

    fun `test expression is not stubbed inside statement`() = doTest("""
        fn foo() { 2 + 2; }
    """, """
        RsFileStub
          FUNCTION:RsFunctionStub
            VALUE_PARAMETER_LIST:RsPlaceholderStub
    """)

    fun `test literal is not stubbed inside function tail expr`() = doTest("""
        fn foo() -> i32 { 0 }
    """, """
        RsFileStub
          FUNCTION:RsFunctionStub
            VALUE_PARAMETER_LIST:RsPlaceholderStub
            RET_TYPE:RsPlaceholderStub
              BASE_TYPE:RsBaseTypeStub
                PATH:RsPathStub
    """)

    fun `test expression is not stubbed inside function tail expr`() = doTest("""
        fn foo() -> i32 { 2 + 2 }
    """, """
        RsFileStub
          FUNCTION:RsFunctionStub
            VALUE_PARAMETER_LIST:RsPlaceholderStub
            RET_TYPE:RsPlaceholderStub
              BASE_TYPE:RsBaseTypeStub
                PATH:RsPathStub
    """)

    fun `test lifetime is stubbed inside function signature`() = doTest("""
        fn foo<'a>(x: &'a str) -> i32 { 32 }
    """, """
        RsFileStub
          FUNCTION:RsFunctionStub
            TYPE_PARAMETER_LIST:RsPlaceholderStub
              LIFETIME_PARAMETER:RsLifetimeParameterStub
            VALUE_PARAMETER_LIST:RsPlaceholderStub
              VALUE_PARAMETER:RsValueParameterStub
                REF_LIKE_TYPE:RsRefLikeTypeStub
                  LIFETIME:RsLifetimeStub
                  BASE_TYPE:RsBaseTypeStub
                    PATH:RsPathStub
            RET_TYPE:RsPlaceholderStub
              BASE_TYPE:RsBaseTypeStub
                PATH:RsPathStub
    """)

    fun `test literal is not stubbed inside closure tail expr`() = doTest("""
        fn foo() {
            || -> i32 { 0 };
        }
    """, """
        RsFileStub
          FUNCTION:RsFunctionStub
            VALUE_PARAMETER_LIST:RsPlaceholderStub
    """)

    fun `test expression is not stubbed inside closure tail expr`() = doTest("""
        fn foo() {
            || -> i32 { 2 + 2 };
        }
    """, """
        RsFileStub
          FUNCTION:RsFunctionStub
            VALUE_PARAMETER_LIST:RsPlaceholderStub
    """)

    fun `test literal is stubbed inside const body`() = doTest("""
        const C: i32 = 0;
    """, """
        RsFileStub
          CONSTANT:RsConstantStub
            BASE_TYPE:RsBaseTypeStub
              PATH:RsPathStub
            LIT_EXPR:RsLitExprStub
    """)

    fun `test expression is stubbed inside const body`() = doTest("""
        const C: i32 = 2 + 2;
    """, """
        RsFileStub
          CONSTANT:RsConstantStub
            BASE_TYPE:RsBaseTypeStub
              PATH:RsPathStub
            BINARY_EXPR:RsPlaceholderStub
              LIT_EXPR:RsLitExprStub
              BINARY_OP:RsBinaryOpStub
              LIT_EXPR:RsLitExprStub
    """)

    fun `test statements are stubbed inside const body`() = doTest("""
        const C: i32 = {
            let a = 1;
            ;
            let b = a + 1;
            b
        };
    """, """
        RsFileStub
          CONSTANT:RsConstantStub
            BASE_TYPE:RsBaseTypeStub
              PATH:RsPathStub
            BLOCK_EXPR:RsBlockExprStub
              BLOCK:RsPlaceholderStub
                LET_DECL:RsLetDeclStub
                  LIT_EXPR:RsLitExprStub
                LET_DECL:RsLetDeclStub
                  BINARY_EXPR:RsPlaceholderStub
                    PATH_EXPR:RsPlaceholderStub
                      PATH:RsPathStub
                    BINARY_OP:RsBinaryOpStub
                    LIT_EXPR:RsLitExprStub
                EXPR_STMT:RsExprStmtStub
                  PATH_EXPR:RsPlaceholderStub
                    PATH:RsPathStub
    """)

    fun `test literal is stubbed inside array type`() = doTest("""
        type T = [u8; 1];
    """, """
        RsFileStub
          TYPE_ALIAS:RsTypeAliasStub
            ARRAY_TYPE:RsArrayTypeStub
              BASE_TYPE:RsBaseTypeStub
                PATH:RsPathStub
              LIT_EXPR:RsLitExprStub
    """)

    fun `test expression is stubbed inside array type`() = doTest("""
        type T = [u8; 2 + 2];
    """, """
        RsFileStub
          TYPE_ALIAS:RsTypeAliasStub
            ARRAY_TYPE:RsArrayTypeStub
              BASE_TYPE:RsBaseTypeStub
                PATH:RsPathStub
              BINARY_EXPR:RsPlaceholderStub
                LIT_EXPR:RsLitExprStub
                BINARY_OP:RsBinaryOpStub
                LIT_EXPR:RsLitExprStub
    """)

    fun `test function block is stubbed if contains item`() = doTest("""
        fn foo() {
            struct S;
        }
    """, """
        RsFileStub
          FUNCTION:RsFunctionStub
            VALUE_PARAMETER_LIST:RsPlaceholderStub
            BLOCK:RsPlaceholderStub
              STRUCT_ITEM:RsStructItemStub
    """)

    fun `test function block is stubbed if contains union`() = doTest("""
        fn foo() {
            union Foo {}
        }
    """, """
        RsFileStub
          FUNCTION:RsFunctionStub
            VALUE_PARAMETER_LIST:RsPlaceholderStub
            BLOCK:RsPlaceholderStub
              STRUCT_ITEM:RsStructItemStub
                BLOCK_FIELDS:RsPlaceholderStub
    """)

    fun `test function block is stubbed if contains inner attrs`() = doTest("""
        fn foo() {
            #![foo]
        }
    """, """
        RsFileStub
          FUNCTION:RsFunctionStub
            VALUE_PARAMETER_LIST:RsPlaceholderStub
            BLOCK:RsPlaceholderStub
              INNER_ATTR:RsInnerAttrStub
                META_ITEM:RsMetaItemStub
                  PATH:RsPathStub
    """)

    fun `test nested block is stubbed if contains items`() = doTest("""
        fn foo() {
            if true {
                struct S;
            } else {
                foobar();
            }
        }
    """, """
        RsFileStub
          FUNCTION:RsFunctionStub
            VALUE_PARAMETER_LIST:RsPlaceholderStub
            BLOCK:RsPlaceholderStub
              BLOCK:RsPlaceholderStub
                STRUCT_ITEM:RsStructItemStub
    """)

    fun `test intermediate block is not stubbed even if nested block contains items`() = doTest("""
        fn foo() {
            let a = {
                if true {
                    struct S;
                } else {
                    foobar();
                }
            };
        }
    """, """
        RsFileStub
          FUNCTION:RsFunctionStub
            VALUE_PARAMETER_LIST:RsPlaceholderStub
            BLOCK:RsPlaceholderStub
              BLOCK:RsPlaceholderStub
                STRUCT_ITEM:RsStructItemStub
    """)

    fun `test statements with attributes are not stubbed if has no nested items`() = doTest("""
        fn foo() {
            #[cfg(unix)]
            let a = 1;
            #[cfg(unix)]
            let b = { 2 };
            #[cfg(unix)]
            {
                let c = 3;
            };
            #[cfg(unix)]
            { 4 }
        }
    """, """
        RsFileStub
          FUNCTION:RsFunctionStub
            VALUE_PARAMETER_LIST:RsPlaceholderStub
    """)

    fun `test intermediate statement is stubbed if has attributes and nested block contains items 1`() = doTest("""
        fn foo() {
            #[cfg(unix)]
            let a = {
                if true {
                    struct S;
                } else {
                    foobar();
                }
            };
        }
    """, """
        RsFileStub
          FUNCTION:RsFunctionStub
            VALUE_PARAMETER_LIST:RsPlaceholderStub
            BLOCK:RsPlaceholderStub
              LET_DECL:RsLetDeclStub
                OUTER_ATTR:RsPlaceholderStub
                  META_ITEM:RsMetaItemStub
                    PATH:RsPathStub
                    META_ITEM_ARGS:RsMetaItemArgsStub
                      META_ITEM:RsMetaItemStub
                        PATH:RsPathStub
                BLOCK:RsPlaceholderStub
                  STRUCT_ITEM:RsStructItemStub
    """)

    fun `test intermediate statement is stubbed if has attributes and nested block contains items 2`() = doTest("""
        fn foo() {
            #[cfg(unix)]
            {
                if true {
                    struct S;
                } else {
                    foobar();
                }
            }
        }
    """, """
        RsFileStub
          FUNCTION:RsFunctionStub
            VALUE_PARAMETER_LIST:RsPlaceholderStub
            BLOCK:RsPlaceholderStub
              EXPR_STMT:RsExprStmtStub
                OUTER_ATTR:RsPlaceholderStub
                  META_ITEM:RsMetaItemStub
                    PATH:RsPathStub
                    META_ITEM_ARGS:RsMetaItemArgsStub
                      META_ITEM:RsMetaItemStub
                        PATH:RsPathStub
                BLOCK:RsPlaceholderStub
                  STRUCT_ITEM:RsStructItemStub
    """)

    fun `test literal is not stubbed inside nested block tail expr`() = doTest("""
        fn foo() {
            if true {
                struct S;
                0
            } else {
                0
            };
        }
    """, """
        RsFileStub
          FUNCTION:RsFunctionStub
            VALUE_PARAMETER_LIST:RsPlaceholderStub
            BLOCK:RsPlaceholderStub
              BLOCK:RsPlaceholderStub
                STRUCT_ITEM:RsStructItemStub
    """)

    fun `test expression is not stubbed inside nested block tail expr`() = doTest("""
        fn foo() {
            if true {
                struct S;
                1 + 2
            } else if false {
                3 + 4
            } else {
                struct T;
                -5
            };
        }
    """, """
        RsFileStub
          FUNCTION:RsFunctionStub
            VALUE_PARAMETER_LIST:RsPlaceholderStub
            BLOCK:RsPlaceholderStub
              BLOCK:RsPlaceholderStub
                STRUCT_ITEM:RsStructItemStub
              BLOCK:RsPlaceholderStub
                STRUCT_ITEM:RsStructItemStub
    """)

    fun `test macro call is not stubbed inside a code block without items`() = doTest("""
        fn foo() {
            bar!();
        }
    """, """
        RsFileStub
          FUNCTION:RsFunctionStub
            VALUE_PARAMETER_LIST:RsPlaceholderStub
    """)

    fun `test macro call is stubbed inside a stubbed code block`() = doTest("""
        fn foo() {
            bar!();
            struct S;
        }
    """, """
        RsFileStub
          FUNCTION:RsFunctionStub
            VALUE_PARAMETER_LIST:RsPlaceholderStub
            BLOCK:RsPlaceholderStub
              MACRO_CALL:RsMacroCallStub
                PATH:RsPathStub
              STRUCT_ITEM:RsStructItemStub
    """)

    fun `test macro call is stubbed inside function local module`() = doTest("""
        fn foo() {
            mod bar {
                include!("a.rs");
            }
        }
    """, """
        RsFileStub
          FUNCTION:RsFunctionStub
            VALUE_PARAMETER_LIST:RsPlaceholderStub
            BLOCK:RsPlaceholderStub
              MOD_ITEM:RsModItemStub
                MACRO_CALL:RsMacroCallStub
                  PATH:RsPathStub
                  INCLUDE_MACRO_ARGUMENT:RsPlaceholderStub
                    LIT_EXPR:RsLitExprStub
    """)

    fun `test macro call is stubbed inside a file`() = doTest("""
        include!("foo.rs");
    """, """
        RsFileStub
          MACRO_CALL:RsMacroCallStub
            PATH:RsPathStub
            INCLUDE_MACRO_ARGUMENT:RsPlaceholderStub
              LIT_EXPR:RsLitExprStub
    """)

    fun `test incomplete paths`() = doTest("""
        use foo::;
        use foo::::bar;
    """, """
        RsFileStub
          USE_ITEM:RsUseItemStub
            USE_SPECK:RsUseSpeckStub
              PATH:RsPathStub
                PATH:RsPathStub
          USE_ITEM:RsUseItemStub
            USE_SPECK:RsUseSpeckStub
              PATH:RsPathStub
                PATH:RsPathStub
                  PATH:RsPathStub
    """)

    fun `test local macro definition is stubbed`() = doTest("""
        fn foo() {
            macro_rules! foo {}
        }
    """, """
        RsFileStub
          FUNCTION:RsFunctionStub
            VALUE_PARAMETER_LIST:RsPlaceholderStub
            BLOCK:RsPlaceholderStub
              MACRO:RsMacroStub
    """)

    fun `test local macro definition is stubbed in a nested block, but the block is not stubbed`() = doTest("""
        fn foo() {
            {
                macro_rules! foo {}
            };
        }
    """, """
        RsFileStub
          FUNCTION:RsFunctionStub
            VALUE_PARAMETER_LIST:RsPlaceholderStub
            BLOCK:RsPlaceholderStub
              MACRO:RsMacroStub
    """)

    private fun doTest(@Language("Rust") code: String, expectedStubText: String) {
        val fileName = "main.rs"
        fileTreeFromText("//- $fileName\n$code").create()
        val vFile = myFixture.findFileInTempDir(fileName)
        val stubTree = StubTreeLoader.getInstance().readFromVFile(project, vFile) ?: error("Stub tree is null")
        val stubText = DebugUtil.stubTreeToString(stubTree.root)
        assertEquals(expectedStubText.trimIndent() + "\n", stubText)
    }
}
