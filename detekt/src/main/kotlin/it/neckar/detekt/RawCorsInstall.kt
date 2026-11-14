/*
 * Copyright (C) 2013-2026 Neckar IT GmbH, Mössingen, Germany
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library. Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules, regardless of
 * the license terms of these independent modules, and to copy and distribute
 * the resulting combined work under terms of your choice, provided that every
 * copy of the combined work is accompanied by a complete copy of the source
 * code of this library.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package it.neckar.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Flags `install(CORS)` outside the file declaring `installProductCors`, which derives the allowed hosts from the
 * `DomainDescriptor`; `internal/buildplatform/config/detekt/detekt.yml` applies it to main sources. PSI-based, so it fires without type resolution.
 */
class RawCorsInstall(config: Config) : Rule(
  config,
  description = "Every service configures CORS through installProductCors; a raw install(CORS) configures it by hand.",
) {

  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)
    if (expression.calleeExpression?.text != "install") return
    if (expression.valueArgumentList?.arguments?.singleOrNull()?.getArgumentExpression()?.text != "CORS") return
    if (expression.containingKtFile.declarations.any { it is KtNamedFunction && it.name == CanonicalHelperName }) return

    report(
      Finding(
        Entity.from(expression),
        "`install(CORS)` configures CORS by hand. Call `$CanonicalHelperName` of it.neckar.ktor, which derives the allowed hosts " +
          "from the DomainDescriptor; a service-specific header or host goes into its additionalConfig.",
      ),
    )
  }

  companion object {
    private const val CanonicalHelperName: String = "installProductCors"
  }
}
