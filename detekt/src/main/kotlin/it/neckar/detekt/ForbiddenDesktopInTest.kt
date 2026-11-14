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
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtTypeParameterListOwner
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.KtValueArgumentName
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents

/**
 * Flags every reference to `java.awt.Desktop`; `internal/buildplatform/config/detekt/detekt.yml` applies it to test sources only.
 *
 * PSI-based, so it fires without type resolution. Reported are the imports of `java.awt.Desktop` and its members (also
 * with an alias), `java.awt.Desktop` fully qualified as expression or type, and the simple name `Desktop` behind
 * `import java.awt.*` unless a `Desktop` declared in the file takes precedence.
 */
class ForbiddenDesktopInTest(config: Config) : Rule(
  config,
  description = "A test runs unattended: `java.awt.Desktop` opens the written file in the browser or PDF viewer of whoever runs the build.",
) {

  override fun visitImportDirective(importDirective: KtImportDirective) {
    super.visitImportDirective(importDirective)

    val imported = importDirective.importedFqName?.asString() ?: return
    if (imported == DesktopFqName || imported.startsWith("$DesktopFqName.")) {
      reportDesktop(importDirective)
    }
  }

  override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
    super.visitDotQualifiedExpression(expression)

    if (expression.getParentOfType<KtImportDirective>(strict = true) != null) return
    if (expression.qualifiedName() == DesktopFqName) {
      reportDesktop(expression)
    }
  }

  override fun visitUserType(type: KtUserType) {
    super.visitUserType(type)

    if (type.qualifier != null && type.qualifiedName() == DesktopFqName) {
      reportDesktop(type)
    }
  }

  override fun visitReferenceExpression(expression: KtReferenceExpression) {
    super.visitReferenceExpression(expression)

    val name = expression as? KtNameReferenceExpression ?: return
    if (name.getReferencedName() != DesktopName) return
    if (name.getParentOfType<KtImportDirective>(strict = true) != null) return
    if (name.parent is KtValueArgumentName) return
    //A qualifier makes it another `Desktop`, or the fully qualified one visitDotQualifiedExpression and visitUserType report
    val parent = name.parent
    if (parent is KtQualifiedExpression && parent.selectorExpression == name) return
    if (parent is KtCallableReferenceExpression && parent.callableReference == name) return
    if ((parent as? KtUserType)?.qualifier != null) return
    if (name.containingKtFile.importsDesktopByStar() && name.seesOwnDesktop().not()) {
      reportDesktop(name)
    }
  }

  private fun reportDesktop(element: KtElement) {
    report(
      Finding(
        Entity.from(element),
        "`java.awt.Desktop` opens a window on the machine that runs the test. Write the file below `build/` and leave it closed.",
      ),
    )
  }

  /**
   * `java.awt.Desktop` for the expression `java.awt.Desktop`, or `null` when a segment is no plain name.
   */
  private fun KtExpression.qualifiedName(): String? = when (this) {
    is KtNameReferenceExpression -> getReferencedName()
    is KtDotQualifiedExpression -> {
      val receiver = receiverExpression.qualifiedName()
      val selector = (selectorExpression as? KtNameReferenceExpression)?.getReferencedName()
      if (receiver == null || selector == null) null else "$receiver.$selector"
    }
    else -> null
  }

  /**
   * `java.awt.Desktop` for the type `java.awt.Desktop`, or `null` when a segment has no name
   */
  private fun KtUserType.qualifiedName(): String? {
    val name = referencedName ?: return null
    val typeQualifier = qualifier ?: return name
    val qualifierName = typeQualifier.qualifiedName() ?: return null
    return "$qualifierName.$name"
  }

  /**
   * Whether a `Desktop` declared in the file takes precedence over the star import: at the top level, in an enclosing
   * class, companion or block, or as a type parameter
   */
  private fun KtNameReferenceExpression.seesOwnDesktop(): Boolean {
    val enclosingDeclarations = parents.flatMap { parent ->
      when (parent) {
        is KtClassOrObject -> parent.declarations.asSequence() +
          parent.primaryConstructorParameters.filter { it.hasValOrVar() } +
          parent.companionObjects.flatMap { it.declarations }
        is KtBlockExpression -> parent.statements.asSequence()
        else -> emptySequence()
      }
    }
    val typeParameters = parents.filterIsInstance<KtTypeParameterListOwner>().flatMap { it.typeParameters }
    return (containingKtFile.declarations.asSequence() + enclosingDeclarations + typeParameters).any { declaration ->
      (declaration as? KtNamedDeclaration)?.name == DesktopName
    }
  }

  /**
   * Whether `Desktop` resolves to `java.awt.Desktop` through `import java.awt.*`: no explicit import binds the name.
   */
  private fun KtFile.importsDesktopByStar(): Boolean {
    val starImport = importDirectives.any { it.isAllUnder && it.importedFqName?.asString() == awtPackage }
    val simpleNameTaken = importDirectives.any { it.importedName?.asString() == DesktopName }
    return starImport && simpleNameTaken.not()
  }

  companion object {
    private const val DesktopName: String = "Desktop"
    private const val awtPackage: String = "java.awt"
    private const val DesktopFqName: String = "$awtPackage.$DesktopName"
  }
}
