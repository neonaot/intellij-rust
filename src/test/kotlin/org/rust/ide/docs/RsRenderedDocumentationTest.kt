/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.docs

import org.intellij.lang.annotations.Language
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwner
import org.rust.lang.doc.docElements
import org.rust.lang.doc.psi.RsDocComment

class RsRenderedDocumentationTest : RsDocumentationProviderTest() {

    fun `test outer comment`() = doTest("""
        /// Adds one to the number given.
        ///
        /// # Examples
        ///
        /// Some text
        ///
        fn add_one(x: i32) -> i32 {
            //^
            x + 1
        }
    """, """
        <p>Adds one to the number given.</p><h2>Examples</h2><p>Some text</p>
    """)

    fun `test inner comment`() = doTest("""
        fn add_one(x: i32) -> i32 {
            //^
            //! Inner comment
            x + 1
        }
    """, """
        <p>Inner comment</p>
    """)

    fun `test several comments`() = doTest("""
        /// Outer comment
        fn add_one(x: i32) -> i32 {
            //^
            //! Inner comment
            x + 1
        }
    """, """
        <p>Outer comment</p>
        <p>Inner comment</p>
    """)

    fun `test code highlighting`() = doTest("""
        /// A cheap, reference-to-reference conversion.
        ///
        /// `AsRef` is very similar to, but different than, `Borrow`. See
        /// [the book][book] for more.
        ///
        /// [book]: ../../book/borrow-and-asref.html
        ///
        /// **Note: this trait must not fail**. If the conversion can fail, use a dedicated method which
        /// returns an `Option<T>` or a `Result<T, E>`.
        ///
        /// # Examples
        ///
        /// Both `String` and `&str` implement `AsRef<str>`:
        ///
        /// ```
        /// fn is_hello<T: AsRef<str>>(s: T) {
        ///    assert_eq!("hello", s.as_ref());
        /// }
        ///
        /// let s = "hello";
        /// is_hello(s);
        ///
        /// let s = "hello".to_string();
        /// is_hello(s);
        /// ```
        ///
        /// # Generic Impls
        ///
        /// - `AsRef` auto-dereference if the inner type is a reference or a mutable
        /// reference (eg: `foo.as_ref()` will work the same if `foo` has type `&mut Foo` or `&&mut Foo`)
        ///
        #[stable(feature = "rust1", since = "1.0.0")]
        pub trait AsRef<T: ?Sized> {
                  //^
            /// Performs the conversion.
            #[stable(feature = "rust1", since = "1.0.0")]
            fn as_ref(&self) -> &T;
        }
    """, """
        <p>A cheap, reference-to-reference conversion.</p><p><code>AsRef</code> is very similar to, but different than, <code>Borrow</code>. See
        <a href="psi_element://../book/borrow-and-asref.html">the book</a> for more.</p><p><strong>Note: this trait must not fail</strong>. If the conversion can fail, use a dedicated method which
        returns an <code>Option&lt;T&gt;</code> or a <code>Result&lt;T, E&gt;</code>.</p><h2>Examples</h2><p>Both <code>String</code> and <code>&amp;str</code> implement <code>AsRef&lt;str&gt;</code>:</p><pre style="..."><span style="...">fn </span><span style="...">is_hello&lt;T: AsRef&lt;str&gt;&gt;(s: T) {</span>
           <span style="...">assert_eq!(</span><span style="...">&quot;hello&quot;</span><span style="...">, s.as_ref());</span>
        <span style="...">}</span>

        <span style="...">let </span><span style="...">s = </span><span style="...">&quot;hello&quot;</span><span style="...">;</span>
        <span style="...">is_hello(s);</span>

        <span style="...">let </span><span style="...">s = </span><span style="...">&quot;hello&quot;</span><span style="...">.to_string();</span>
        <span style="...">is_hello(s);</span>
        </pre>
        <h2>Generic Impls</h2><ul><li><code>AsRef</code> auto-dereference if the inner type is a reference or a mutable
        reference (eg: <code>foo.as_ref()</code> will work the same if <code>foo</code> has type <code>&amp;mut Foo</code> or <code>&amp;&amp;mut Foo</code>)</li></ul>
    """)

    fun `test rust identifier as inline link`() = doTest("""
        /// [link](Result)
        fn main() {}
           //^
    """,
        """<p><a href="psi_element://Result">link</a></p>"""
    )

    fun `test rust path as inline link`() = doTest("""
        /// [link](caret::io::Result)
        fn main() {}
           //^
    """,
        """<p><a href="psi_element://caret::io::Result">link</a></p>"""
    )

    fun `test rust identifier as reference link`() = doTest("""
        /// [my link][ref]
        ///
        /// [ref]: MyType
        fn main() {}
           //^
    """,
        """<p><a href="psi_element://MyType">my link</a></p>"""
    )

    fun `test rust path as reference link 2`() = doTest("""
        /// [my link][ref]
        ///
        /// [ref]: Long::Path::For::MyType
        fn main() {}
           //^
    """,
        """<p><a href="psi_element://Long::Path::For::MyType">my link</a></p>"""
    )

    fun `test implied shortcut reference link`() = doTest("""
        fn foo() {}
        /// [foo]
        fn main() {}
           //^
    """,
        """<p><a href="psi_element://foo">foo</a></p>"""
    )

    fun `test implied shortcut reference link with backticks`() = doTest("""
        /// [`Iterator`]
        fn main() {}
           //^
    """,
        """<p><a href="psi_element://Iterator"><code>Iterator</code></a></p>"""
    )

    fun `test implied shortcut reference link, ignore non-valid rust identifier or path`() {
        val badVariants = listOf("some.web.site", "foo bar w spaces", "some/path")
        for (s in badVariants) {
            doTest("""
                    /// [${s}]
                    fn main() {}
                       //^
                """, """<p>[${s}]</p>"""
            )
        }
    }

    fun `test implied shortcut reference link with reference, reference has higher priority`() = doTest("""
        fn foo() {}
        /// [foo]
        ///
        /// [foo]: Path::For::Smth
        fn main() {}
           //^
    """,
        """<p><a href="psi_element://Path::For::Smth">foo</a></p>"""
    )

    private fun doTest(@Language("Rust") code: String, @Language("Html") expected: String?) {
        doTest(code, expected) { originalItem, _ ->
            (originalItem as? RsDocAndAttributeOwner)
                ?.docElements()
                ?.filterIsInstance<RsDocComment>()
                ?.mapNotNull { generateRenderedDoc(it) }
                ?.joinToString("\n")
                ?.hideSpecificStyles()
        }
    }
}
