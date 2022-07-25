/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractTrait

import com.intellij.lang.Language
import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.refactoring.actions.ExtractSuperActionBase
import org.rust.lang.RsLanguage

class RsExtractTraitAction : ExtractSuperActionBase() {

    init {
        setInjectedContext(true)
    }

    override fun isAvailableForLanguage(language: Language): Boolean = language == RsLanguage

    override fun getRefactoringHandler(provider: RefactoringSupportProvider): RsExtractTraitHandler =
        RsExtractTraitHandler()
}
