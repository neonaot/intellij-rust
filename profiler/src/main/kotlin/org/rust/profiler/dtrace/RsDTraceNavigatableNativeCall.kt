/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.profiler.dtrace

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.profiler.api.BaseCallStackElement
import com.intellij.profiler.model.NativeCall
import com.intellij.psi.NavigatablePsiElement
import com.intellij.util.containers.Interner
import com.intellij.util.containers.stream
import org.rust.profiler.RsSymbolSearcher

@Suppress("UnstableApiUsage")
data class RsDTraceNavigatableNativeCall(private val nativeCall: NativeCall) : BaseCallStackElement() {
    override fun fullName(): String = nativeCall.fullName()

    override val isNavigatable: Boolean get() = true

    override fun calcNavigatables(project: Project): Array<out NavigatablePsiElement> {
        val searcher = RsSymbolSearcher(project)
        val symbols = searcher.find(nativeCall)
        val navigatables = runReadAction {
            symbols.stream()
                .filter { it.isValid }
                .toArray<NavigatablePsiElement> { length -> arrayOfNulls(length) }
        }
        if (navigatables.isEmpty()) {
            LOG.warn("Failed to navigate: ${nativeCall.methodWithClassOrFunction()}")
        }
        return navigatables
    }

    companion object {
        private val LOG = logger<RsDTraceNavigatableNativeCall>()

        fun read(string: String, interner: Interner<String>? = null): RsDTraceNavigatableNativeCall? {
             return NativeCall.read(string, interner)?.let(::RsDTraceNavigatableNativeCall)
        }

    }
}
