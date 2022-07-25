/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty

import org.rust.lang.core.psi.RsTypeAlias
import org.rust.lang.core.types.BoundElement
import org.rust.lang.core.types.consts.Const
import org.rust.lang.core.types.consts.asLong
import org.rust.lang.core.types.infer.TypeFolder
import org.rust.lang.core.types.infer.TypeVisitor

data class TyArray(
    val base: Ty,
    val const: Const,
    override val aliasedBy: BoundElement<RsTypeAlias>? = null
) : Ty(base.flags or const.flags) {
    val size: Long? get() = const.asLong()

    override fun superFoldWith(folder: TypeFolder): Ty =
        TyArray(base.foldWith(folder), const.foldWith(folder), aliasedBy?.foldWith(folder))

    override fun superVisitWith(visitor: TypeVisitor): Boolean =
        base.visitWith(visitor) || const.visitWith(visitor)

    override fun withAlias(aliasedBy: BoundElement<RsTypeAlias>): Ty = copy(aliasedBy = aliasedBy)

    override fun isEquivalentToInner(other: Ty): Boolean {
        if (this === other) return true
        if (other !is TyArray) return false

        if (!base.isEquivalentTo(other.base)) return false
        if (const != other.const) return false

        return true
    }
}
