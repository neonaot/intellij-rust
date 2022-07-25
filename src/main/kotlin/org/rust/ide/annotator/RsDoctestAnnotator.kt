/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.injected.InjectionBackgroundSuppressor
import org.rust.ide.injected.RsDoctestLanguageInjector
import org.rust.ide.injected.doctestInfo
import org.rust.lang.core.psi.ext.startOffset
import org.rust.lang.doc.psi.RsDocCodeFence

/**
 * Adds missing background for injections from [RsDoctestLanguageInjector].
 * Background is disabled by [InjectionBackgroundSuppressor] marker implemented for [RsDocCodeFence].
 *
 * We have to do it this way because we want to highlight fully range inside ```backticks```
 * but a real injections is shifted by 1 character and empty lines are skipped.
 */
class RsDoctestAnnotator : AnnotatorBase() {
    override fun annotateInternal(element: PsiElement, holder: AnnotationHolder) {
        if (holder.isBatchMode) return
        if (element !is RsDocCodeFence) return
        val doctest = element.doctestInfo() ?: return

        val startOffset = element.startOffset
        doctest.rangesForBackgroundHighlighting.forEach {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(it.shiftRight(startOffset))
                .textAttributes(EditorColors.INJECTED_LANGUAGE_FRAGMENT)
                .create()
        }
    }
}
