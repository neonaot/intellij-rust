/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator

import kotlin.reflect.KClass

abstract class RsAnnotatorTestBase(private val annotatorClass: KClass<out AnnotatorBase>) : RsAnnotationTestBase() {

    override fun createAnnotationFixture(): RsAnnotationTestFixture<Unit> =
        RsAnnotationTestFixture(this, myFixture, annotatorClasses = listOf(annotatorClass))
}
