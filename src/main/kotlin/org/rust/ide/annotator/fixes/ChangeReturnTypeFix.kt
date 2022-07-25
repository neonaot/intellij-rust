/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator.fixes

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.rust.cargo.project.workspace.PackageOrigin
import org.rust.ide.presentation.render
import org.rust.ide.presentation.renderInsertionSafe
import org.rust.ide.utils.import.RsImportHelper.getTypeReferencesInfoFromTys
import org.rust.ide.utils.import.RsImportHelper.importTypeReferencesFromTy
import org.rust.lang.core.psi.RsFunction
import org.rust.lang.core.psi.RsLambdaExpr
import org.rust.lang.core.psi.RsPsiFactory
import org.rust.lang.core.psi.RsRetExpr
import org.rust.lang.core.psi.ext.*
import org.rust.lang.core.types.ty.Ty
import org.rust.lang.core.types.ty.TyUnit
import org.rust.lang.core.types.ty.TyUnknown
import org.rust.lang.core.types.type

class ChangeReturnTypeFix(
    element: RsElement,
    private val actualTy: Ty
) : LocalQuickFixAndIntentionActionOnPsiElement(element) {
    private val _text: String = run {
        val callable = findCallableOwner(startElement)

        val (item, name) = when (callable) {
            is RsFunction -> {
                val item = if (callable.owner.isImplOrTrait) " of method" else " of function"
                val name = callable.name?.let { " '$it'" } ?: ""
                item to name
            }
            is RsLambdaExpr -> " of closure" to ""
            else -> "" to ""
        }

        val useQualifiedName = if (callable != null) {
            getTypeReferencesInfoFromTys(callable, actualTy).toQualify
        } else {
            emptySet()
        }

        val rendered = actualTy.render(
            context = element,
            useQualifiedName = useQualifiedName
        )
        "Change return type$item$name to '$rendered'"
    }

    override fun getText(): String = _text
    override fun getFamilyName(): String = "Change return type"

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        val owner = findCallableOwner(startElement) ?: return
        val oldRetType = owner.retType

        if (actualTy is TyUnit) {
            oldRetType?.delete()
            return
        }

        val oldTy = oldRetType?.typeReference?.type ?: TyUnknown
        val (_, useQualifiedName) = getTypeReferencesInfoFromTys(owner, actualTy, oldTy)
        val text = actualTy.renderInsertionSafe(
            context = startElement as? RsElement,
            useQualifiedName = useQualifiedName,
            includeLifetimeArguments = true
        )
        val retType = RsPsiFactory(project).createRetType(text)

        if (oldRetType != null) {
            oldRetType.replace(retType)
        } else {
            owner.addAfter(retType, owner.valueParameterList)
        }

        importTypeReferencesFromTy(owner, actualTy)
    }

    companion object {
        private fun findCallableOwner(element: PsiElement): RsFunctionOrLambda? = element.contextStrict()

        fun createIfCompatible(element: RsElement, actualTy: Ty): ChangeReturnTypeFix? {
            if (element.containingCrate?.origin != PackageOrigin.WORKSPACE) return null

            val owner = findCallableOwner(element)
            val isOverriddenFn = owner is RsFunction && owner.superItem != null
            if (isOverriddenFn) return null // TODO: Support overridden items

            if (owner is RsLambdaExpr && owner.retType == null) {
                return null
            }

            val retExpr = when (owner) {
                is RsFunction -> owner.block?.expandedTailExpr
                is RsLambdaExpr -> owner.expr
                else -> return null
            }

            val isRetExpr = element.parent is RsRetExpr || retExpr === element
            if (!isRetExpr) return null

            return ChangeReturnTypeFix(element, actualTy)
        }
    }
}
