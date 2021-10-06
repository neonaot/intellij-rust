/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import org.rust.ide.colors.RsColor
import org.rust.ide.injected.doctestInfo
import org.rust.lang.core.psi.ext.ancestorStrict
import org.rust.lang.core.psi.ext.descendantOfTypeStrict
import org.rust.lang.core.psi.ext.elementType
import org.rust.lang.doc.psi.*
import org.rust.openapiext.isUnitTestMode

class RsDocHighlightingAnnotator : AnnotatorBase() {
    override fun annotateInternal(element: PsiElement, holder: AnnotationHolder) {
        if (holder.isBatchMode) return
        val color = when {
            element.elementType == RsDocElementTypes.DOC_DATA -> when (val parent = element.parent) {
                is RsDocCodeFence -> when {
                    // Don't highlight code fences with language injections - otherwise our annotations
                    // interfere with injected highlighting
                    parent.isDoctestInjected -> null
                    else -> RsColor.DOC_CODE
                }
                is RsDocCodeFenceStartEnd, is RsDocCodeFenceLang -> RsColor.DOC_CODE
                is RsDocCodeSpan -> if (element.ancestorStrict<RsDocLink>() == null) {
                    RsColor.DOC_CODE
                } else {
                    null
                }
                is RsDocCodeBlock -> RsColor.DOC_CODE
                else -> null
            }
            element is RsDocEmphasis -> RsColor.DOC_EMPHASIS
            element is RsDocStrong -> RsColor.DOC_STRONG
            element is RsDocAtxHeading -> RsColor.DOC_HEADING
            element is RsDocLink && element.descendantOfTypeStrict<RsDocGap>() == null -> RsColor.DOC_LINK
            else -> null
        } ?: return

        val severity = if (isUnitTestMode) color.testSeverity else HighlightSeverity.INFORMATION

        holder.newSilentAnnotation(severity).textAttributes(color.textAttributesKey).create()
    }

    private val RsDocCodeFence.isDoctestInjected: Boolean
        get() = doctestInfo() != null
}
