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
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor

/**
 * Shared helpers for the `@Hot` hot-path rules ([HotFunctionMustNotBoxValueClass],
 * [HotFunctionMustOnlyCallHotFunctions]).
 *
 * The value-class check they share lives in [isValueClass] (in [LayerConventions]).
 * [isHotAnnotated] resolves symbols and must run inside an `analyze { … }` block;
 * [hasHotAnnotationEntry] and [isAcknowledgedHotAllocation] are cheap PSI checks that need no
 * analysis session.
 */

internal const val HotAnnotationFqn: String = "it.neckar.open.annotations.Hot"

private const val HotAnnotationShortName: String = "Hot"

/**
 * Cheap syntactic pre-filter: true when the function has an annotation entry named `Hot`, without
 * resolving it. Lets a rule skip opening a K2 analysis session for the vast majority of functions
 * that are not hot. The FQN is verified via [isHotAnnotated] afterwards (guards against a same-named
 * annotation from another package).
 */
internal fun KtNamedFunction.hasHotAnnotationEntry(): Boolean =
  annotationEntries.any { it.shortName?.asString() == HotAnnotationShortName }

/** True when the declaration carries `@Hot`. Must run inside an `analyze { … }` block. */
internal fun KaDeclarationSymbol.isHotAnnotated(): Boolean =
  annotations.any { it.classId?.asFqNameString() == HotAnnotationFqn }

/**
 * Annotations that mark code as explicitly **anti-hot**: calling (or reading) it from a `@Hot`
 * body is always a finding, before any `inline`/whitelist exemption. `@Slow` marks slow code,
 * `@Allocates` marks allocating code (including property accessors that allocate).
 */
internal val hotPathDenyAnnotationFqns: Set<String> = setOf(
  "it.neckar.open.annotations.Slow",
  "it.neckar.open.annotations.Allocates",
)

/**
 * Short name of the first anti-hot annotation ([hotPathDenyAnnotationFqns]) on this declaration,
 * or `null` when the declaration is not deny-marked. Must run inside an `analyze { … }` block.
 */
internal fun KaDeclarationSymbol.findHotPathDenyAnnotationShortName(): String? =
  annotations
    .firstNotNullOfOrNull { annotation -> annotation.classId?.asFqNameString()?.takeIf { it in hotPathDenyAnnotationFqns } }
    ?.substringAfterLast('.')

private const val HotAllocationShortName: String = "HotAllocation"

private const val HotBoundaryShortName: String = "HotBoundary"

/**
 * True when this expression sits inside a statement acknowledged with `@HotAllocation("reason")` —
 * a deliberate allocation. Suppresses the `@Slow`/`@Allocates` deny and boxing findings.
 */
internal fun KtExpression.isAcknowledgedHotAllocation(): Boolean = isAcknowledgedWith(HotAllocationShortName)

/**
 * True when this expression sits inside a statement acknowledged with `@HotBoundary("reason")` —
 * a polymorphic delegation point over a deliberately un-colored interface (e.g. `Layer.paint`).
 * Suppresses only the plain "not itself `@Hot`" coloring finding.
 */
internal fun KtExpression.isAcknowledgedHotBoundary(): Boolean = isAcknowledgedWith(HotBoundaryShortName)

/**
 * The acknowledgement sits on an annotated expression (`@Ack(...) call()`) or an annotated local
 * variable (`@Ack(...) val x = call()`). The walk stops at the enclosing function/accessor, so an
 * acknowledgement never leaks beyond its statement.
 *
 * Matched by short name on PSI (no resolution): the acknowledgement annotations have SOURCE
 * retention and only suppress a finding, so a same-named foreign annotation merely hides a
 * finding — the conservative direction.
 */
private fun KtExpression.isAcknowledgedWith(annotationShortName: String): Boolean {
  return generateSequence<PsiElement>(this) { it.parent }
    .takeWhile { it !is KtNamedFunction && it !is KtPropertyAccessor }
    .any { element ->
      when (element) {
        is KtAnnotatedExpression -> element.hasAnnotationEntry(annotationShortName)
        is KtProperty -> element.hasAnnotationEntry(annotationShortName)
        else -> false
      }
    }
}

private fun KtAnnotatedExpression.hasAnnotationEntry(shortName: String): Boolean =
  annotationEntries.any { it.shortName?.asString() == shortName }

private fun KtProperty.hasAnnotationEntry(shortName: String): Boolean =
  annotationEntries.any { it.shortName?.asString() == shortName }
