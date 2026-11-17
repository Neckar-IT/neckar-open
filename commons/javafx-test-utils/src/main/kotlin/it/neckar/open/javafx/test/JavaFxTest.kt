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
package it.neckar.open.javafx.test

import assertk.*
import assertk.assertions.*
import it.neckar.open.test.utils.DisableIfHeadless
import javafx.application.Platform
import org.junit.jupiter.api.extension.ExtendWith
import org.testfx.framework.junit5.ApplicationExtension

/**
 * Marks a test as a JavaFX test that is run using [ApplicationExtension]
 */
@DisableIfHeadless
@ExtendWith(ApplicationExtension::class)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class JavaFxTest


/**
 * Throws an exception if the current thread is not the Java FX UI thread
 */
fun assertFxThread() {
  assertThat(Platform.isFxApplicationThread(), "Current Thread: <${Thread.currentThread().name}>").isTrue()
}

/**
 * Throws an exception if the current thread is the Java FX UI thread
 */
fun assertNotFxThread() {
  assertThat(Platform.isFxApplicationThread(), "Current Thread: <${Thread.currentThread().name}>").isFalse()
}
