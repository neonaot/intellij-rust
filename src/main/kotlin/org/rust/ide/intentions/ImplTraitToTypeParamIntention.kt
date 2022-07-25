/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.rust.ide.utils.template.newTemplateBuilder
import org.rust.lang.core.psi.*
import org.rust.lang.core.psi.ext.ancestorStrict
import org.rust.lang.core.psi.ext.descendantsOfType
import org.rust.lang.core.psi.ext.getNextNonWhitespaceSibling
import org.rust.lang.core.psi.ext.getPrevNonWhitespaceSibling
import org.rust.openapiext.Testmark
import org.rust.openapiext.createSmartPointer

class ImplTraitToTypeParamIntention : RsElementBaseIntentionAction<ImplTraitToTypeParamIntention.Context>() {
    override fun getText(): String = "Convert `impl Trait` to type parameter"
    override fun getFamilyName(): String = text

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        val traitType = element.ancestorStrict<RsTraitType>() ?: return null
        if (traitType.impl == null) return null
        val fnParent = element.ancestorStrict<RsValueParameterList>()?.parent as? RsFunction ?: return null
        return Context(traitType, fnParent)
    }

    override fun invoke(project: Project, editor: Editor, ctx: Context) {
        val psiFactory = RsPsiFactory(project)
        val (argType, fnSignature) = ctx

        // can't convert outer `impl Trait` because inner one
        // will appear in type parameter constrains which is invalid
        if (argType.descendantsOfType<RsTraitType>().any { it.impl != null }) {
            OuterImplTestMark.hit()
            HintManager.getInstance().showErrorHint(
                editor,
                "Please convert innermost `impl Trait` first",
                HintManager.UNDER)
            return
        }

        var typeParameterList = fnSignature.typeParameterList
        if (typeParameterList == null) {
            typeParameterList = fnSignature.addAfter(psiFactory.createTypeParameterList(""), fnSignature.identifier)
                as RsTypeParameterList
        }

        val anchor = typeParameterList.constParameterList.firstOrNull() ?: typeParameterList.gt

        val typeParameterName = "T"
        var typeParameter = psiFactory
            .createTypeParameterList("$typeParameterName:${argType.polyboundList.joinToString("+") { it.text }}")
            .typeParameterList
            .first()

        typeParameter = typeParameterList.addBefore(
            typeParameter,
            anchor
        ) as RsTypeParameter

        val prev = typeParameter.getPrevNonWhitespaceSibling()
        val next = typeParameter.getNextNonWhitespaceSibling()
        if (prev != typeParameterList.lt && prev.elementType != RsElementTypes.COMMA) {
            typeParameterList.addBefore(psiFactory.createComma(), typeParameter)
        }
        if (next != typeParameterList.gt && next.elementType != RsElementTypes.COMMA) {
            typeParameterList.addAfter(psiFactory.createComma(), typeParameter)
        }

        val newArgType = argType.replace(psiFactory.createType(typeParameterName)).createSmartPointer()

        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)

        val tpl = editor.newTemplateBuilder(fnSignature) ?: return
        tpl.introduceVariable(typeParameter.identifier, typeParameterName).apply {
            replaceElementWithVariable(newArgType.element ?: return)
        }
        tpl.withExpressionsHighlighting()
        tpl.runInline()
    }

    data class Context(
        val argType: RsTraitType,
        val fnSignature: RsFunction
    )

    companion object {
        object OuterImplTestMark : Testmark()
    }
}
