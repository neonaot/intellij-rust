/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions

import com.intellij.codeInsight.CodeInsightUtilCore
import com.intellij.codeInsight.template.impl.MacroCallNode
import com.intellij.codeInsight.template.macro.CompleteMacro
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import org.rust.ide.inspections.fixes.insertGenericArgumentsIfNeeded
import org.rust.ide.refactoring.implementMembers.generateMissingTraitMembers
import org.rust.ide.utils.template.newTemplateBuilder
import org.rust.lang.core.psi.*
import org.rust.lang.core.psi.ext.*
import org.rust.openapiext.createSmartPointer

class AddImplTraitIntention : RsElementBaseIntentionAction<AddImplTraitIntention.Context>() {
    override fun getText() = "Implement trait"
    override fun getFamilyName() = text

    class Context(val type: RsStructOrEnumItemElement, val name: String)

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        val struct = element.ancestorStrict<RsStructOrEnumItemElement>() ?: return null
        val name = struct.name ?: return null
        return Context(struct, name)
    }

    override fun invoke(project: Project, editor: Editor, ctx: Context) {
        val impl = RsPsiFactory(project).createTraitImplItem(
            ctx.name,
            "T",
            ctx.type.typeParameterList,
            ctx.type.whereClause
        )

        val inserted = ctx.type.parent.addAfter(impl, ctx.type) as RsImplItem
        val traitName = inserted.traitRef?.path ?: return

        val implPtr = inserted.createSmartPointer()
        val traitNamePtr = traitName.createSmartPointer()
        val tpl = editor.newTemplateBuilder(inserted) ?: return
        tpl.replaceElement(traitNamePtr.element ?: return, MacroCallNode(CompleteMacro()))
        tpl.withFinishResultListener {
            val implCurrent = implPtr.element
            if (implCurrent != null) {
                runWriteAction {
                    afterTraitNameEntered(implCurrent, editor)
                }
            }
        }
        tpl.withDisabledDaemonHighlighting()
        tpl.runInline()
    }

    private fun afterTraitNameEntered(impl: RsImplItem, editor: Editor) {
        val traitRef = impl.traitRef ?: return
        val trait = traitRef.resolveToBoundTrait() ?: return

        val insertedGenericArgumentsPtr = if (trait.element.requiredGenericParameters.isNotEmpty()) {
            insertGenericArgumentsIfNeeded(traitRef.path)?.map { it.createSmartPointer() }
        } else {
            null
        }

        generateMissingTraitMembers(impl)

        showGenericArgumentsTemplate(
            editor,
            CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(impl) ?: return,
            insertedGenericArgumentsPtr
        )
    }

    private fun showGenericArgumentsTemplate(
        editor: Editor,
        impl: RsImplItem,
        insertedGenericArgumentsPtr: List<SmartPsiElementPointer<RsElement>>?
    ) {
        val insertedGenericArguments = insertedGenericArgumentsPtr?.mapNotNull { it.element }?.filterIsInstance<RsBaseType>()
        if (insertedGenericArguments != null && insertedGenericArguments.isNotEmpty()) {
            val members = impl.members ?: return
            val baseTypes = members.descendantsOfType<RsPath>()
                .filter { (it.parent is RsBaseType || it.parent is RsPathExpr) && !it.hasColonColon && it.path == null && it.typeQual == null }
                .groupBy { it.referenceName }
            val typeToUsage = insertedGenericArguments.associateWith { ty ->
                ty.path?.referenceName?.let { baseTypes[it] } ?: emptyList()
            }
            val tmp = editor.newTemplateBuilder(impl) ?: return
            for ((type, usages) in typeToUsage) {
                tmp.introduceVariable(type).apply {
                    for (usage in usages) {
                        replaceElementWithVariable(usage)
                    }
                }
            }
            tmp.withExpressionsHighlighting()
            tmp.withDisabledDaemonHighlighting()
            tmp.runInline()
        }
    }
}
