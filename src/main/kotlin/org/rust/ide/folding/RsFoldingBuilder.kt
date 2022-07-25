/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.folding

import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.folding.CodeFoldingSettings
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.CustomFoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import org.rust.ide.injected.RsDoctestLanguageInjector.Companion.INJECTED_MAIN_NAME
import org.rust.lang.RsLanguage
import org.rust.lang.core.parser.RustParserDefinition.Companion.BLOCK_COMMENT
import org.rust.lang.core.parser.RustParserDefinition.Companion.INNER_BLOCK_DOC_COMMENT
import org.rust.lang.core.parser.RustParserDefinition.Companion.INNER_EOL_DOC_COMMENT
import org.rust.lang.core.parser.RustParserDefinition.Companion.OUTER_BLOCK_DOC_COMMENT
import org.rust.lang.core.parser.RustParserDefinition.Companion.OUTER_EOL_DOC_COMMENT
import org.rust.lang.core.psi.*
import org.rust.lang.core.psi.RsElementTypes.*
import org.rust.lang.core.psi.ext.*
import org.rust.openapiext.document
import java.lang.Integer.max

class RsFoldingBuilder : CustomFoldingBuilder(), DumbAware {
    override fun getLanguagePlaceholderText(node: ASTNode, range: TextRange): String =
        when {
            node.elementType == LBRACE -> " { "
            node.elementType == RBRACE -> " }"
            node.elementType == USE_ITEM -> "/* uses */"
            node.psi is RsModDeclItem -> "/* mods */"
            node.psi is RsExternCrateItem -> "/* crates */"
            node.psi is RsWhereClause -> "/* where */"
            node.psi is PsiComment -> "/* ... */"
            node.psi is RsValueParameterList -> "(...)"
            else -> "{...}"
        }

    override fun buildLanguageFoldRegions(descriptors: MutableList<FoldingDescriptor>, root: PsiElement, document: Document, quick: Boolean) {
        if (root !is RsFile) return

        val usingRanges: MutableList<TextRange> = ArrayList()
        val modsRanges: MutableList<TextRange> = ArrayList()
        val cratesRanges: MutableList<TextRange> = ArrayList()

        val rightMargin = CodeStyle.getSettings(root).getRightMargin(RsLanguage)
        val visitor = FoldingVisitor(descriptors, usingRanges, modsRanges, cratesRanges, rightMargin)
        PsiTreeUtil.processElements(root) { it.accept(visitor); true }
    }

    private class FoldingVisitor(
        private val descriptors: MutableList<FoldingDescriptor>,
        private val usesRanges: MutableList<TextRange>,
        private val modsRanges: MutableList<TextRange>,
        private val cratesRanges: MutableList<TextRange>,
        val rightMargin: Int
    ) : RsVisitor() {

        override fun visitMacroBody(o: RsMacroBody) = fold(o)

        override fun visitMacroExpansion(o: RsMacroExpansion) = fold(o)

        override fun visitMacro2(o: RsMacro2) {
            foldBetween(o, o.lparen, o.rparen)
            foldBetween(o, o.lbrace, o.rbrace)
        }

        override fun visitStructLiteralBody(o: RsStructLiteralBody) = fold(o)

        override fun visitEnumBody(o: RsEnumBody) = fold(o)

        override fun visitBlockFields(o: RsBlockFields) = fold(o)

        override fun visitBlock(o: RsBlock) {
            if (tryFoldBlockWhitespaces(o)) return
            val parentFn = o.parent as? RsFunction
            if (parentFn?.name != INJECTED_MAIN_NAME) fold(o)
        }

        override fun visitMatchBody(o: RsMatchBody) = fold(o)

        override fun visitUseGroup(o: RsUseGroup) = fold(o)

        override fun visitWhereClause(o: RsWhereClause) = fold(o)

        override fun visitMembers(o: RsMembers) = foldBetween(o, o.lbrace, o.rbrace)

        override fun visitModItem(o: RsModItem) = foldBetween(o, o.lbrace, o.rbrace)

        override fun visitMacroArgument(o: RsMacroArgument) {
            val macroCall = o.parent as? RsMacroCall
            if (macroCall?.bracesKind == MacroBraces.BRACES) {
                foldBetween(o, o.lbrace, o.rbrace)
            }
        }

        override fun visitValueParameterList(o: RsValueParameterList) {
            if (o.valueParameterList.isEmpty()) return
            foldBetween(o, o.firstChild, o.lastChild)
        }

        override fun visitComment(comment: PsiComment) {
            when (comment.tokenType) {
                BLOCK_COMMENT,
                INNER_BLOCK_DOC_COMMENT,
                OUTER_BLOCK_DOC_COMMENT,
                INNER_EOL_DOC_COMMENT,
                OUTER_EOL_DOC_COMMENT -> fold(comment)
            }
        }

        override fun visitStructItem(o: RsStructItem) {
            val blockFields = o.blockFields
            if (blockFields != null) {
                fold(blockFields)
            }
        }

        private fun fold(element: PsiElement) {
            descriptors += FoldingDescriptor(element.node, element.textRange)
        }

        private fun foldBetween(element: PsiElement, left: PsiElement?, right: PsiElement?) {
            if (left != null && right != null && right.textLength > 0) {
                val range = TextRange(left.textOffset, right.textOffset + 1)
                descriptors += FoldingDescriptor(element.node, range)
            }
        }

        private fun tryFoldBlockWhitespaces(block: RsBlock): Boolean {
            if (block.parent !is RsFunction) return false

            val doc = block.containingFile.document ?: return false
            val maxLength = rightMargin - block.getOffsetInLine(doc) - ONE_LINER_PLACEHOLDERS_EXTRA_LENGTH
            if (!block.isSingleLine(doc, maxLength)) return false

            val lbrace = block.lbrace
            val rbrace = block.rbrace ?: return false

            val blockElement = lbrace.getNextNonCommentSibling()
            if (blockElement == null || blockElement != rbrace.getPrevNonCommentSibling()) return false
            if (blockElement.textContains('\n')) return false
            if (!(doc.areOnAdjacentLines(lbrace, blockElement) && doc.areOnAdjacentLines(blockElement, rbrace))) return false

            val leadingSpace = lbrace.nextSibling as? PsiWhiteSpace ?: return false
            val trailingSpace = rbrace.prevSibling as? PsiWhiteSpace ?: return false

            val leftEl = block.prevSibling as? PsiWhiteSpace ?: lbrace
            val range1 = TextRange(leftEl.textOffset, leadingSpace.endOffset)
            val range2 = TextRange(trailingSpace.textOffset, rbrace.endOffset)
            val group = FoldingGroup.newGroup("one-liner")
            descriptors += FoldingDescriptor(lbrace.node, range1, group)
            descriptors += FoldingDescriptor(rbrace.node, range2, group)

            return true
        }

        override fun visitUseItem(o: RsUseItem) {
            foldRepeatingItems(o, usesRanges)
        }

        override fun visitModDeclItem(o: RsModDeclItem) {
            foldRepeatingItems(o, modsRanges)
        }

        override fun visitExternCrateItem(o: RsExternCrateItem) {
            foldRepeatingItems(o, cratesRanges)
        }

        private inline fun <reified T> foldRepeatingItems(startNode: T, ranges: MutableList<TextRange>) {
            if (isInRangesAlready(ranges, startNode as PsiElement)) return

            var lastNode: PsiElement? = null
            var tmpNode: PsiElement? = startNode

            while (tmpNode is T || tmpNode is PsiWhiteSpace) {
                tmpNode = tmpNode.getNextNonCommentSibling()
                if (tmpNode is T)
                    lastNode = tmpNode
            }

            if (lastNode == startNode) return

            if (lastNode != null) {
                val range = TextRange(startNode.startOffset, lastNode.endOffset)
                descriptors += FoldingDescriptor(startNode.node, range)
                ranges.add(range)
            }
        }

        private fun isInRangesAlready(ranges: MutableList<TextRange>, element: PsiElement?): Boolean {
            if (element == null) return false
            return ranges.any { x -> element.textOffset in x }
        }
    }

    override fun isCustomFoldingRoot(node: ASTNode) = node.elementType == BLOCK

    override fun isRegionCollapsedByDefault(node: ASTNode): Boolean =
        (RsCodeFoldingSettings.getInstance().collapsibleOneLineMethods && node.elementType in COLLAPSED_BY_DEFAULT)
            || CodeFoldingSettings.getInstance().isDefaultCollapsedNode(node)

    private companion object {
        val COLLAPSED_BY_DEFAULT = TokenSet.create(LBRACE, RBRACE)
        const val ONE_LINER_PLACEHOLDERS_EXTRA_LENGTH = 4
    }
}

private fun CodeFoldingSettings.isDefaultCollapsedNode(node: ASTNode) =
    (this.COLLAPSE_DOC_COMMENTS && node.elementType in RS_DOC_COMMENTS)
        || (this.COLLAPSE_IMPORTS && node.elementType == USE_ITEM)
        || (this.COLLAPSE_METHODS && node.elementType == BLOCK && node.psi.parent is RsFunction)

private fun Document.areOnAdjacentLines(first: PsiElement, second: PsiElement): Boolean =
    getLineNumber(first.endOffset) + 1 == getLineNumber(second.startOffset)

private fun RsBlock.isSingleLine(doc: Document, maxLength: Int): Boolean {
    // remove all leading and trailing spaces before counting lines
    val startContents = lbrace.rightSiblings.dropWhile { it is PsiWhiteSpace }.firstOrNull() ?: return false
    if (startContents.node.elementType == RBRACE) return false
    val endContents = rbrace?.leftSiblings?.dropWhile { it is PsiWhiteSpace }?.firstOrNull() ?: return false
    if (endContents.endOffset - startContents.textOffset > maxLength) return false

    return doc.getLineNumber(startContents.textOffset) == doc.getLineNumber(endContents.endOffset)
}

private fun PsiElement.getOffsetInLine(doc: Document): Int {
    val blockLine = doc.getLineNumber(startOffset)
    return leftLeaves
        .takeWhile { doc.getLineNumber(it.endOffset) == blockLine }
        .sumOf { el -> el.text.lastIndexOf('\n').let { el.text.length - max(it + 1, 0) } }
}
