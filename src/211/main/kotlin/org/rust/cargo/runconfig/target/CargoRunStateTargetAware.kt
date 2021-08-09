/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.target

import com.intellij.execution.runners.ExecutionEnvironment
import org.rust.cargo.runconfig.CargoRunStateBase
import org.rust.cargo.runconfig.command.CargoCommandConfiguration

abstract class CargoRunStateTargetAware(
    environment: ExecutionEnvironment,
    runConfiguration: CargoCommandConfiguration,
    config: CargoCommandConfiguration.CleanConfiguration.Ok
) : CargoRunStateBase(environment, runConfiguration, config)
