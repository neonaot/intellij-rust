/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.target

import com.intellij.execution.target.getRuntimeType
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.titledRow
import com.intellij.util.execution.ParametersListUtil
import com.intellij.util.text.SemVer
import kotlin.reflect.KMutableProperty0

class RsLanguageRuntimeConfigurable(val config: RsLanguageRuntimeConfiguration) :
    BoundConfigurable(config.displayName, config.getRuntimeType().helpTopic) {

    override fun createPanel(): DialogPanel = panel {
        titledRow()

        row("Rustc executable:") {
            textField(config::rustcPath)
        }
        row("Rustc version:") {
            textField(config::rustcVersion.toSemverProperty()).enabled(false)
        }

        row("Cargo executable:") {
            textField(config::cargoPath)
        }
        row("Cargo version:") {
            textField(config::cargoVersion.toSemverProperty()).enabled(false)
        }

        row("Additional build parameters:") {
            textField(config::localBuildArgs.toParametersProperty())
                .comment("Additional arguments to pass to <b>cargo build</b> command " +
                    "in case of <b>Build on target</b> option is disabled")
        }
    }
}

private fun KMutableProperty0<SemVer?>.toSemverProperty(): PropertyBinding<String> =
    PropertyBinding({ get()?.parsedVersion.orEmpty() }, { set(SemVer.parseFromText(it)) })

private fun KMutableProperty0<List<String>>.toParametersProperty(): PropertyBinding<String> =
    PropertyBinding({ get().joinToString(" ") }, { set(ParametersListUtil.parse(it)) })
