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
package it.neckar.open.javafx.properties

import javafx.beans.binding.DoubleExpression
import javafx.beans.binding.IntegerExpression
import javafx.beans.binding.LongExpression
import javafx.beans.property.Property
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableDoubleValue
import javafx.beans.value.WritableIntegerValue
import javafx.beans.value.WritableLongValue
import kotlin.reflect.KProperty

/**
 * Extension methods to allow simplified JavaFX properties
 */
operator fun <T> ObservableValue<T>.getValue(thisRef: Any, property: KProperty<*>): T = value

operator fun <T> Property<T>.setValue(thisRef: Any, property: KProperty<*>, value: T?): Unit = setValue(value)


operator fun DoubleExpression.getValue(thisRef: Any, property: KProperty<*>): Double = value

operator fun WritableDoubleValue.setValue(thisRef: Any, property: KProperty<*>, value: Double?): Unit = setValue(value)


operator fun LongExpression.getValue(thisRef: Any, property: KProperty<*>): Long = value

operator fun WritableLongValue.setValue(thisRef: Any, property: KProperty<*>, value: Long?): Unit = setValue(value)


operator fun IntegerExpression.getValue(thisRef: Any, property: KProperty<*>): Int = value

operator fun WritableIntegerValue.setValue(thisRef: Any, property: KProperty<*>, value: Int?): Unit = setValue(value)
