/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix

class RefExprPostfixTemplateTest : RsPostfixTemplateTest(RefExprPostfixTemplate(RsPostfixTemplateProvider())) {
    fun `test expression`() = doTest("""
        fn main() {
            let v = vec![1, 2, 3];
            foo(v.ref/*caret*/);
        }

        fn foo(v: &Vec<i32>) {}
    """, """
        fn main() {
            let v = vec![1, 2, 3];
            foo(&v/*caret*/);
        }

        fn foo(v: &Vec<i32>) {}
    """)
}
