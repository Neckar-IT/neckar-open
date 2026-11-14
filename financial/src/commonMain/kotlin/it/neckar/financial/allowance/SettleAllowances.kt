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
package it.neckar.financial.allowance

/**
 * Settles [ordered] against [allowances] within one scope; quantities of one key are added up first,
 * so the ordered quantities of every section passed in at once settle an [AllowanceScope.AcrossSections]
 * allowance once. Keys keep the order of first appearance, a key only [allowances] name follows with
 * `ordered = 0`. Throws for allowances of more than one scope and for a key two allowances name.
 */
fun <K> settleAllowances(ordered: List<OrderedQuantity<K>>, allowances: List<QuantityAllowance<K>>): List<SettledQuantity<K>> {
  val scopes: Set<AllowanceScope> = allowances.mapTo(mutableSetOf()) { it.scope }
  require(scopes.size <= 1) { "Allowances of one settling share a scope, were <$scopes>" }
  val repeatedKey = allowances.groupingBy { it.key }.eachCount().entries.firstOrNull { it.value > 1 }?.key
  require(repeatedKey == null) { "Two allowances name the key <$repeatedKey>" }

  val orderedByKey: Map<K, Int> = ordered.groupingBy { it.key }.fold(0) { total, entry -> total + entry.quantity }
  val keys: List<K> = (ordered.map { it.key } + allowances.map { it.key }).distinct()
  return keys.map { key ->
    SettledQuantity(
      key = key,
      ordered = orderedByKey[key] ?: 0,
      included = allowances.firstOrNull { it.key == key }?.includedQuantity ?: 0,
    )
  }
}
