/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.suggested

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import com.intellij.refactoring.suggested.*
import org.rust.lang.core.psi.RsFunction
import org.rust.lang.core.psi.RsPatBinding
import org.rust.lang.core.psi.RsPatIdent
import org.rust.lang.core.psi.RsValueParameterList
import org.rust.lang.core.psi.ext.RsNameIdentifierOwner

class RsSuggestedRefactoringSupport : SuggestedRefactoringSupport {
    override val availability: SuggestedRefactoringAvailability
        get() = RsSuggestedRefactoringAvailability(this)
    override val execution: SuggestedRefactoringExecution
        get() = RsSuggestedRefactoringExecution(this)
    override val stateChanges: SuggestedRefactoringStateChanges
        get() = RsSuggestedRefactoringStateChanges(this)
    override val ui: SuggestedRefactoringUI
        get() = RsSuggestedRefactoringUI()

    override fun importsRange(psiFile: PsiFile): TextRange? = null

    override fun isAnchor(psiElement: PsiElement): Boolean = when (psiElement) {
        // May return true for const pat binding since we can't distinguish them
        // without name resolution, which is forbidden here.
        // Refactoring for constants is suppressed by `RsSuggestedRefactoringAvailability`.
        is RsPatBinding -> psiElement.parent is RsPatIdent && psiElement.parentOfType<RsValueParameterList>() == null
        is RsNameIdentifierOwner -> true
        else -> false
    }

    override fun isIdentifierPart(c: Char): Boolean = Character.isUnicodeIdentifierStart(c)
    override fun isIdentifierStart(c: Char): Boolean = Character.isUnicodeIdentifierPart(c)

    override fun nameRange(anchor: PsiElement): TextRange? = (anchor as? RsNameIdentifierOwner)?.nameIdentifier?.textRange
    override fun signatureRange(anchor: PsiElement): TextRange? {
        if (anchor is RsFunction) {
            val start = anchor.identifier
            val end = anchor.valueParameterList?.lastChild ?: anchor.identifier
            return TextRange(start.startOffset, end.endOffset)
        }
        return (anchor as? RsNameIdentifierOwner)?.nameIdentifier?.textRange
    }
}
