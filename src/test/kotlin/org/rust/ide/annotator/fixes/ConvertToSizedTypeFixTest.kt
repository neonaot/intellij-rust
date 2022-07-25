/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator.fixes

import org.intellij.lang.annotations.Language
import org.rust.ide.annotator.RsAnnotatorTestBase
import org.rust.ide.annotator.RsErrorAnnotator

class ConvertToSizedTypeFixTest : RsAnnotatorTestBase(RsErrorAnnotator::class) {

    fun `test convert function arg to reference`() = checkFixByText("Convert to reference", """
        fn foo(slice: <error>[u8]/*caret*/</error>) {}
    """, """
        fn foo(slice: &[u8]) {}
    """)

    fun `test convert function return type to reference`() = checkFixByText("Convert to reference", """
        fn foo() -> <error>[u8]/*caret*/</error> { unimplemented!() }
    """, """
        fn foo() -> &[u8] { unimplemented!() }
    """)

    fun `test convert function arg to Box`() = checkFixByText("Convert to Box", """
        trait Foo {}
        fn foo(foo: <error>Foo/*caret*/</error>) {}
    """, """
        trait Foo {}
        fn foo(foo: Box<Foo>) {}
    """)

    fun `test convert function return type to Box`() = checkFixByText("Convert to Box", """
        trait Foo {}
        fn foo() -> <error>Foo/*caret*/</error> { unimplemented!() }
    """, """
        trait Foo {}
        fn foo() -> Box<Foo> { unimplemented!() }
    """)

    private fun checkFixByText(
        fixName: String,
        @Language("Rust") before: String,
        @Language("Rust") after: String,
    ) {
        super.checkFixByText(
            fixName,
            "#[lang = \"sized\"] trait Sized {}\n" + before.trimIndent(),
            "#[lang = \"sized\"] trait Sized {}\n" + after.trimIndent(),
            checkWarn = true,
            checkInfo = false,
            checkWeakWarn = false,
        )
    }
}
