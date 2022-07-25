/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa.liveness

import org.rust.lang.core.dfa.*
import org.rust.lang.core.psi.*
import org.rust.lang.core.psi.ext.*
import org.rust.lang.core.resolve.ImplLookup
import org.rust.lang.core.types.controlFlowGraph
import org.rust.lang.core.types.declaration
import org.rust.lang.core.types.infer.RsInferenceResult
import org.rust.lang.core.types.inference

enum class DeclarationKind { Parameter, Variable }

data class DeadDeclaration(val binding: RsPatBinding, val kind: DeclarationKind)

data class Liveness(
    val deadDeclarations: List<DeadDeclaration>,

    /**
     * For each local binding, [lastUsages] provides the locations after which the binding
     * is certainly not accessed during any execution flow of the program.
     * See [org.rust.lang.core.dfa.RsLivenessTest] for examples
     */
    val lastUsages: Map<RsPatBinding, List<RsElement>>
)

class LivenessContext private constructor(
    val inference: RsInferenceResult,
    val body: RsBlock,
    val cfg: ControlFlowGraph,
    val implLookup: ImplLookup = ImplLookup.relativeTo(body),
    private val deadDeclarations: MutableList<DeadDeclaration> = mutableListOf(),
    private val lastUsages: MutableMap<RsPatBinding, MutableList<RsElement>> = mutableMapOf()
) {
    fun registerDeadDeclaration(binding: RsPatBinding, kind: DeclarationKind) {
        deadDeclarations.add(DeadDeclaration(binding, kind))
    }

    fun registerLastUsage(binding: RsPatBinding, usageElement: RsElement) {
        lastUsages.getOrPut(binding, ::mutableListOf).add(usageElement)
    }

    fun check(): Liveness {
        val gatherLivenessContext = GatherLivenessContext(this)
        val livenessData = gatherLivenessContext.gather()
        val flowedLiveness = FlowedLivenessData.buildFor(this, livenessData, cfg)
        flowedLiveness.collectDeadDeclarations()
        flowedLiveness.collectLastUsages()
        return Liveness(deadDeclarations, lastUsages)
    }

    companion object {
        fun buildFor(owner: RsInferenceContextOwner): LivenessContext? {
            val body = owner.body as? RsBlock ?: return null
            val cfg = owner.controlFlowGraph ?: return null
            return LivenessContext(owner.inference, body, cfg)
        }
    }
}

object LiveDataFlowOperator : DataFlowOperator {
    override fun join(succ: Int, pred: Int): Int = succ or pred     // liveness from both preds are in scope
    override val initialValue: Boolean get() = false                // dead by default
}

typealias LivenessDataFlow = DataFlowContext<LiveDataFlowOperator>

class FlowedLivenessData(
    private val ctx: LivenessContext,
    private val livenessData: LivenessData,
    private val dfcxLivePaths: LivenessDataFlow
) {
    fun collectDeadDeclarations() {
        val basePaths = livenessData.paths.filterIsInstance<UsagePath.Base>()
        for (usagePath in basePaths) {
            val usageDeclaration = usagePath.declaration
            if (usagePath.isDeadOnEntry(usageDeclaration)) {
                ctx.registerDeadDeclaration(usageDeclaration, usagePath.declarationKind)
            }
        }
    }

    fun collectLastUsages() {
        for (usage in livenessData.usages) {
            val usagePath = usage.path
            val usageElement = usage.element
            if (usagePath.isDeadOnEntry(usageElement)) {
                ctx.registerLastUsage(usagePath.declaration, usageElement)
            }
        }
    }

    private fun UsagePath.isDeadOnEntry(element: RsElement): Boolean {
        var isDead = true
        return dfcxLivePaths.eachBitOnEntry(element) { index ->
            val path = livenessData.paths[index]
            // declaration/assignment of `a`, use of `a`
            if (this == path) {
                isDead = false
            } else {
                // declaration/assignment of `a`, use of `a.b.c`
                val isEachExtensionDead = livenessData.eachBasePath(path) { it != this }
                if (!isEachExtensionDead) isDead = false
            }
            isDead
        }
    }

    companion object {
        fun buildFor(ctx: LivenessContext, livenessData: LivenessData, cfg: ControlFlowGraph): FlowedLivenessData {
            val dfcxLivePaths = DataFlowContext(cfg, LiveDataFlowOperator, livenessData.paths.size, FlowDirection.Backward)

            livenessData.addGenKills(dfcxLivePaths)
            dfcxLivePaths.propagate()

            return FlowedLivenessData(ctx, livenessData, dfcxLivePaths)
        }
    }
}

class GatherLivenessContext(
    private val ctx: LivenessContext,
    private val livenessData: LivenessData = LivenessData()
) : Delegate {

    override fun consume(element: RsElement, cmt: Cmt, mode: ConsumeMode) {
        livenessData.addUsage(element, cmt)
    }

    override fun matchedPat(pat: RsPat, cmt: Cmt, mode: MatchMode) {}

    override fun consumePat(pat: RsPat, cmt: Cmt, mode: ConsumeMode) {
        pat.descendantsOfType<RsPatBinding>().forEach { binding ->
            livenessData.addDeclaration(binding)
        }
        livenessData.addUsage(pat, cmt)
    }

    override fun declarationWithoutInit(binding: RsPatBinding) {
        livenessData.addDeclaration(binding)
    }

    override fun mutate(assignmentElement: RsElement, assigneeCmt: Cmt, mode: MutateMode) {
        if (mode == MutateMode.WriteAndRead) {
            livenessData.addUsage(assignmentElement, assigneeCmt)
        }
    }

    override fun useElement(element: RsElement, cmt: Cmt) {
        livenessData.addUsage(element, cmt)
    }

    fun gather(): LivenessData {
        val gatherVisitor = ExprUseWalker(this, MemoryCategorizationContext(ctx.implLookup, ctx.inference))
        gatherVisitor.consumeBody(ctx.body)
        return livenessData
    }
}

sealed class UsagePath {
    abstract val declaration: RsPatBinding

    data class Base(override val declaration: RsPatBinding) : UsagePath() {
        override fun toString(): String = declaration.text
    }

    data class Extend(val parent: UsagePath) : UsagePath() {
        override val declaration: RsPatBinding = parent.declaration
        override fun toString(): String = "Extend($parent)"
    }

    private val base: Base
        get() = when (this) {
            is Base -> this
            is Extend -> parent.base
        }

    val declarationKind: DeclarationKind
        get() = if (base.declaration.ancestorOrSelf<RsValueParameter>() != null) {
            DeclarationKind.Parameter
        } else {
            DeclarationKind.Variable
        }

    companion object {
        fun computeFor(cmt: Cmt): UsagePath? {
            return when (val category = cmt.category) {
                is Categorization.Rvalue -> {
                    val declaration = (cmt.element as? RsExpr)?.declaration as? RsPatBinding ?: return null
                    Base(declaration)
                }

                is Categorization.Local -> {
                    val declaration = category.declaration as? RsPatBinding ?: return null
                    Base(declaration)
                }

                is Categorization.Deref -> {
                    val baseCmt = category.unwrapDerefs()
                    computeFor(baseCmt)
                }

                is Categorization.Interior -> {
                    val baseCmt = category.cmt
                    val parent = computeFor(baseCmt) ?: return null
                    Extend(parent)
                }

                else -> null
            }
        }
    }
}

data class Usage(val path: UsagePath, val element: RsElement) {
    override fun toString(): String = "Usage($path)"
}

data class Declaration(val path: UsagePath.Base, val element: RsElement = path.declaration) {
    override fun toString(): String = "Declaration($path)"
}

class LivenessData(
    val usages: MutableSet<Usage> = linkedSetOf(),
    val declarations: MutableSet<Declaration> = linkedSetOf(),
    val paths: MutableList<UsagePath> = mutableListOf(),
    private val pathToIndex: MutableMap<UsagePath, Int> = mutableMapOf()
) {
    private fun addUsagePath(usagePath: UsagePath) {
        if (!pathToIndex.containsKey(usagePath)) {
            val index = paths.size
            paths.add(usagePath)
            pathToIndex[usagePath] = index
        }
    }

    fun eachBasePath(usagePath: UsagePath, predicate: (UsagePath) -> Boolean): Boolean {
        var path = usagePath
        while (true) {
            if (!predicate(path)) return false
            when (path) {
                is UsagePath.Base -> return true
                is UsagePath.Extend -> path = path.parent
            }
        }
    }

    fun addGenKills(dfcxLiveness: LivenessDataFlow) {
        for (usage in usages) {
            val bit = pathToIndex[usage.path] ?: error("No such usage path in pathToIndex")
            dfcxLiveness.addGen(usage.element, bit)
        }
        for (declaration in declarations) {
            val bit = pathToIndex[declaration.path] ?: error("No such declaration path in pathToIndex")
            dfcxLiveness.addKill(KillFrom.ScopeEnd, declaration.element, bit)
        }
    }

    fun addUsage(element: RsElement, cmt: Cmt) {
        val usagePath = UsagePath.computeFor(cmt) ?: return
        if (!pathToIndex.containsKey(usagePath)) return
        usages.add(Usage(usagePath, element))
    }

    fun addDeclaration(element: RsPatBinding) {
        val usagePath = UsagePath.Base(element)
        addUsagePath(usagePath)
        declarations.add(Declaration(usagePath))
    }
}
