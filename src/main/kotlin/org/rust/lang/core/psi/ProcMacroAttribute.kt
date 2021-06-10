/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi

import org.rust.lang.core.crate.Crate
import org.rust.lang.core.macros.proc.ProcMacroApplicationService
import org.rust.lang.core.psi.ext.*
import org.rust.lang.core.resolve2.resolveToProcMacroWithoutPsiUnchecked
import org.rust.lang.core.stubs.RsAttributeOwnerStub
import org.rust.lang.core.stubs.common.RsAttrProcMacroOwnerPsiOrStub
import org.rust.lang.core.stubs.common.RsMetaItemPsiOrStub

sealed class ProcMacroAttribute<out T : RsMetaItemPsiOrStub> {
    abstract val attr: T?

    object None: ProcMacroAttribute<Nothing>() {
        override val attr: Nothing? get() = null
        override fun toString(): String = "None"
    }
    object Derive: ProcMacroAttribute<Nothing>() {
        override val attr: Nothing? get() = null
        override fun toString(): String = "Derive"
    }
    data class Attr<T : RsMetaItemPsiOrStub>(
        override val attr: T,
        val index: Int
    ): ProcMacroAttribute<T>()

    companion object {
        /**
         *
         * Can't be after derive:
         *
         * ```
         * #[derive(Foo)]
         * #[foo] // NOT an attribute macro
         * struct S
         * ```
         *
         * Legacy derive helpers aren't supported
         * https://github.com/rust-lang/rust/issues/79202
         */
        fun <T : RsMetaItemPsiOrStub> getProcMacroAttributeRaw(
            owner: RsAttrProcMacroOwnerPsiOrStub<T>,
            stub: RsAttributeOwnerStub? = if (owner is RsDocAndAttributeOwner) owner.attributeStub else owner as RsAttributeOwnerStub,
            explicitCrate: Crate? = null,
            explicitCustomAttributes: CustomAttributes? = null,
        ): ProcMacroAttribute<T> {
            if (!ProcMacroApplicationService.isEnabled()) return None
            if (stub != null) {
                if (!stub.mayHaveCustomAttrs) {
                    return if (stub.mayHaveCustomDerive) Derive else None
                }
            }

            val crate = explicitCrate ?: owner.containingCrate ?: return None
            val customAttributes = explicitCustomAttributes ?: CustomAttributes.fromCrate(crate)

            owner.getQueryAttributes(crate, stub, fromOuterAttrsOnly = true).metaItems.forEachIndexed { index, meta ->
                if (meta.name == "derive") return Derive
                if (RsProcMacroPsiUtil.canBeProcMacroAttributeCallWithoutContextCheck(meta, customAttributes)) {
                    return Attr(meta, index)
                }
            }
            return None
        }

        fun getProcMacroAttribute(
            owner: RsAttrProcMacroOwner,
            stub: RsAttributeOwnerStub? = owner.attributeStub,
            explicitCrate: Crate? = null,
        ): ProcMacroAttribute<RsMetaItem> {
            return when (val attr = getProcMacroAttributeRaw(owner, stub, explicitCrate)) {
                Derive -> Derive
                None -> None
                is Attr -> {
                    val props = attr.attr.resolveToProcMacroWithoutPsiUnchecked(checkIsMacroAttr = false)?.props
                    if (props != null && props.treatAsBuiltinAttr && RsProcMacroPsiUtil.canFallbackItem(owner)) {
                        None
                    } else {
                        attr
                    }
                }
            }
        }
    }
}
