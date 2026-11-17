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

import com.intellij.psi.PsiElement
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtProperty

/**
 * Checks where `@OpenApiEntityView` sits and what it names.
 *
 * No other code reads the annotation, so this rule is what keeps it present and true, for every
 * class in the repository.
 *
 * It reports unless
 *
 * - every interface named `<Entity>DataView` carries the annotation,
 * - the annotation marks an `interface`, the REST super-interface of the views of one entity,
 * - the interface is named after the entity it names plus `DataView`, which is what ties the
 *   annotation argument to the view,
 * - the interface declares `companion object { val entityType: EntityType = EntityType.of(<Entity>::class) }`
 *   for the entity the annotation names, as an initializer or an expression getter.
 *
 * Purely syntactic: the annotation is matched by its short name, the entity by the class literal of
 * its argument. A same-named annotation from another package is therefore checked too, and an
 * aliased import reads as a missing annotation; aliasing it is not supported.
 */
class OpenApiEntityViewPlacement(config: Config) : Rule(
  config,
  description = "Every `<Entity>DataView` interface carries `@OpenApiEntityView(<Entity>::class)` and declares `companion object { val entityType: EntityType = EntityType.of(<Entity>::class) }`.",
) {

  override fun visitClassOrObject(classOrObject: KtClassOrObject) {
    super.visitClassOrObject(classOrObject)

    val declaredName: String = classOrObject.name ?: return
    val annotation: KtAnnotationEntry? = classOrObject.entityViewAnnotation()
    if (annotation == null) {
      checkUnannotated(classOrObject, declaredName)
      return
    }

    //The argument is a KClass, which Kotlin accepts only as a class literal
    val entityName: String = annotation.valueArguments
      .firstNotNullOfOrNull { (it.getArgumentExpression() as? KtClassLiteralExpression)?.simpleName() }
      ?: return

    val view: KtClass = checkPlacement(classOrObject, annotation, declaredName, entityName) ?: return
    checkCompanion(view, declaredName, entityName)
  }

  private fun checkUnannotated(classOrObject: KtClassOrObject, declaredName: String) {
    //An annotation on the companion is reported where it sits, as misplaced
    val annotatedCompanion: Boolean = classOrObject.companionObjects.any { it.entityViewAnnotation() != null }
    if (classOrObject is KtClass && classOrObject.isInterface() && declaredName.endsWith(ViewNameSuffix) && annotatedCompanion.not()) {
      report(classOrObject, "The view [$declaredName] must carry `@OpenApiEntityView(${declaredName.removeSuffix(ViewNameSuffix)}::class)`.")
    }
  }

  /**
   * Returns the view when the annotation sits on an interface named after its entity, so the companion is checked
   * against an annotation that holds.
   */
  private fun checkPlacement(classOrObject: KtClassOrObject, annotation: KtAnnotationEntry, declaredName: String, entityName: String): KtClass? {
    val expectedName = "$entityName$ViewNameSuffix"
    val view: KtClass? = (classOrObject as? KtClass)?.takeIf { it.isInterface() }
    if (view == null) {
      val fix: String = if (declaredName == expectedName) "make [$declaredName] an interface" else "move it to the `$expectedName` interface"
      report(annotation, "`@OpenApiEntityView` marks the REST super-interface of the views of one entity, and [$declaredName] is no interface - $fix.")
      return null
    }

    if (declaredName != expectedName) {
      report(annotation, "The view [$declaredName] names the entity [$entityName], so its own name is [$expectedName] - rename the interface, or name the entity it projects.")
      return null
    }

    return view
  }

  private fun checkCompanion(view: KtClass, declaredName: String, entityName: String) {
    val expectedValue = "EntityType.of($entityName::class)"
    val property: KtProperty? = view.companionObjects
      .flatMap { it.declarations }
      .filterIsInstance<KtProperty>()
      .firstOrNull { it.name == EntityTypePropertyName }
    if (property == null) {
      report(view.companionObjects.firstOrNull() ?: view, "The view [$declaredName] must declare `companion object { val entityType: EntityType = $expectedValue }`.")
      return
    }

    val declaredType: String? = property.typeReference?.text
    if (property.isVar || (declaredType != null && declaredType.isEntityTypeName().not())) {
      val declaration: String = listOfNotNull("${property.valOrVarKeyword.text} entityType", declaredType).joinToString(": ")
      report(property, "The companion of the view [$declaredName] declares [$declaration] - it is `val entityType: EntityType`.")
      return
    }

    val value: KtExpression? = property.initializer ?: property.getter?.bodyExpression ?: property.delegateExpression
    if (value == null) {
      report(property, "The companion of the view [$declaredName] declares `entityType` without a value - the value is `$expectedValue`.")
      return
    }

    if (value.namesEntityTypeOf(entityName).not()) {
      report(property, "The companion of the view [$declaredName] declares `entityType` as [${value.text}] - the annotation names [$entityName], so the value is `$expectedValue`.")
    }
  }

  /** True for the expression `EntityType.of(<entityName>::class)`, the class literal plain or qualified; a block body is no such expression. */
  private fun KtExpression.namesEntityTypeOf(entityName: String): Boolean {
    val qualified = this as? KtDotQualifiedExpression ?: return false
    if (qualified.receiverExpression.text.isEntityTypeName().not()) {
      return false
    }

    val call = qualified.selectorExpression as? KtCallExpression ?: return false
    if (call.calleeExpression?.text != EntityTypeFactoryName) {
      return false
    }

    val classLiteral = call.valueArguments.singleOrNull()?.getArgumentExpression() as? KtClassLiteralExpression ?: return false
    return classLiteral.simpleName() == entityName
  }

  private fun KtClassOrObject.entityViewAnnotation(): KtAnnotationEntry? {
    return annotationEntries.firstOrNull { it.shortName?.asString() == AnnotationShortName }
  }

  private fun String.isEntityTypeName(): Boolean {
    return substringAfterLast('.') == EntityTypeClassName
  }

  private fun KtClassLiteralExpression.simpleName(): String? {
    return receiverExpression?.text?.substringAfterLast('.')
  }

  private fun report(element: PsiElement, message: String) {
    report(Finding(Entity.from(element), message))
  }

  private companion object {
    const val EntityTypeClassName: String = "EntityType"
    const val EntityTypeFactoryName: String = "of"
    const val AnnotationShortName: String = "OpenApiEntityView"
    const val ViewNameSuffix: String = "DataView"
    const val EntityTypePropertyName: String = "entityType"
  }
}
