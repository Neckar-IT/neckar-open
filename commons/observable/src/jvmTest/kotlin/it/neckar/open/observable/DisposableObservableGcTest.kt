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
package it.neckar.open.observable

import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import org.junit.jupiter.api.Test
import java.lang.ref.WeakReference

/**
 * Verifies that intermediate observables created via map/select/reduce can be garbage collected
 * after the caller disposes them.
 *
 * Without upstream-subscription tracking, the source observable would retain the intermediate via
 * its listener list (the consume lambda captures the intermediate), preventing collection.
 */
internal class DisposableObservableGcTest {
  @Test
  fun `intermediate from map leaks without dispose - source retains it via listener list`() {
    val source = ObservableObject(1)

    var intermediate: DisposableReadOnlyObservableObject<Int>? = source.map { it * 10 }
    val intermediateRef = WeakReference(intermediate)

    //Drop the caller reference but do NOT dispose - source still holds intermediate via listener list
    intermediate = null
    forceGc()
    assertThat(intermediateRef.get()).isNotNull()

    //Source update still fires on the retained intermediate - confirms the listener is alive
    source.value = 2
    assertThat(intermediateRef.get()).isNotNull()
  }

  @Test
  fun `intermediate from map can be garbage collected after dispose`() {
    val source = ObservableObject(1)

    var intermediate: DisposableReadOnlyObservableObject<Int>? = source.map { it * 10 }
    val intermediateRef = WeakReference(intermediate)

    intermediate!!.dispose()
    intermediate = null

    forceGc()
    assertThat(intermediateRef.get()).isNull()

    //Updating the source must not fire on disposed intermediate (would NPE if listener still registered and intermediate field accessed)
    source.value = 2
  }

  @Test
  fun `intermediate from map with two sources can be garbage collected after dispose`() {
    val sourceA = ObservableObject(1)
    val sourceB = ObservableObject(2)

    var intermediate: DisposableReadOnlyObservableObject<Int>? = sourceA.map(sourceB) { a, b -> a + b }
    val intermediateRef = WeakReference(intermediate)

    intermediate!!.dispose()
    intermediate = null

    forceGc()
    assertThat(intermediateRef.get()).isNull()

    sourceA.value = 10
    sourceB.value = 20
  }

  @Test
  fun `intermediate from reduce can be garbage collected after dispose`() {
    val sourceA = ObservableObject(1)
    val sourceB = ObservableObject(2)
    val sourceC = ObservableObject(3)

    var intermediate: DisposableReadOnlyObservableObject<Int>? = reduceObservables(sourceA, sourceB, sourceC) { it.sum() }
    val intermediateRef = WeakReference(intermediate)

    intermediate!!.dispose()
    intermediate = null

    forceGc()
    assertThat(intermediateRef.get()).isNull()
  }

  @Test
  fun `intermediate from select can be garbage collected after dispose`() {
    val inner1 = ObservableObject("a")
    val inner2 = ObservableObject("b")
    val outer = ObservableObject(inner1)

    var intermediate: DisposableReadOnlyObservableObject<String>? = outer.select { it }
    val intermediateRef = WeakReference(intermediate)

    //Switch nested to trigger the inner-subscription replacement path
    outer.value = inner2
    inner2.value = "b-updated"

    intermediate!!.dispose()
    intermediate = null

    forceGc()
    assertThat(intermediateRef.get()).isNull()
  }

  @Test
  fun `bound observable releases upstream subscription on dispose`() {
    val source = ObservableObject("source")

    var bound: ObservableObject<String>? = ObservableObject("initial")
    bound!!.bind(source)
    val boundRef = WeakReference(bound)

    bound.dispose()
    bound = null

    forceGc()
    assertThat(boundRef.get()).isNull()
  }

  @Test
  fun `bidirectionally bound observable releases upstream subscriptions on dispose`() {
    val other = ObservableObject("other")

    var owner: ObservableObject<String>? = ObservableObject("owner")
    owner!!.bindBidirectional(other)
    val ownerRef = WeakReference(owner)

    owner.dispose()
    owner = null

    forceGc()
    assertThat(ownerRef.get()).isNull()
  }

  private fun forceGc() {
    //Best-effort GC nudge - not deterministic, but reliable enough in HotSpot for weak-reference tests.
    repeat(5) {
      System.gc()
      Thread.sleep(10)
    }
  }
}
