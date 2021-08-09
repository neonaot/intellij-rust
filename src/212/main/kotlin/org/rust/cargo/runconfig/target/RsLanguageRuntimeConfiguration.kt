/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.target

import com.intellij.execution.target.LanguageRuntimeConfiguration
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.util.execution.ParametersListUtil
import com.intellij.util.text.SemVer

class RsLanguageRuntimeConfiguration : LanguageRuntimeConfiguration(RsLanguageRuntimeType.TYPE_ID),
                                       PersistentStateComponent<RsLanguageRuntimeConfiguration.MyState> {
    var rustcPath: String = ""
    var rustcVersion: SemVer? = null

    var cargoPath: String = ""
    var cargoVersion: SemVer? = null

    // TODO: suggest flags in [RsLanguageRuntimeIntrospector]
    var localBuildArgs: List<String> = listOf("--target=x86_64-unknown-linux-gnu")

    override fun getState() = MyState().also {
        it.rustcPath = this.rustcPath
        it.rustcVersion = this.rustcVersion?.parsedVersion ?: ""

        it.cargoPath = this.cargoPath
        it.cargoVersion = this.cargoVersion?.parsedVersion ?: ""

        it.localBuildArgs = this.localBuildArgs.joinToString(" ")
    }

    override fun loadState(state: MyState) {
        this.rustcPath = state.rustcPath ?: ""
        this.rustcVersion = SemVer.parseFromText(state.rustcVersion)

        this.cargoPath = state.cargoPath ?: ""
        this.cargoVersion = SemVer.parseFromText(state.cargoVersion)

        this.localBuildArgs = ParametersListUtil.parse(state.localBuildArgs ?: "")
    }

    class MyState : BaseState() {
        var rustcPath by string()
        var rustcVersion by string()

        var cargoPath by string()
        var cargoVersion by string()

        var localBuildArgs by string()
    }
}
