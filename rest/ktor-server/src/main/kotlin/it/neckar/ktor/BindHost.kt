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
package it.neckar.ktor

import it.neckar.projects.common.LocalDev
import it.neckar.runtime.context.ExecutionEnvironment
import it.neckar.runtime.context.RuntimeContext

/**
 * The addresses the connectors of a Ktor service listen on. Every case includes an address `localhost` reaches, so a client on the
 * machine and the event loop watchdog connect to `127.0.0.1` in each of them.
 */
enum class BindHost(val addresses: List<String>) {
  /** The listener of a unit test. */
  Loopback(listOf(LoopbackAddress)),

  /** The listener of a local run: the containers of the local stack reach [LocalDev.Address], no other machine reaches either address. */
  LocalRun(listOf(LoopbackAddress, LocalDev.Address)),

  /** The listener of a deployed service: the container network decides who reaches it. */
  AllInterfaces(listOf("0.0.0.0")),
  ;

  companion object {
    /** The addresses for a process in [executionEnvironment]; a unit test listens on [Loopback] in every environment. */
    fun of(executionEnvironment: ExecutionEnvironment, inUnitTest: Boolean): BindHost {
      if (inUnitTest) return Loopback

      return when (executionEnvironment) {
        ExecutionEnvironment.LocalDev -> LocalRun
        ExecutionEnvironment.Standalone, ExecutionEnvironment.Docker, ExecutionEnvironment.CI -> AllInterfaces
      }
    }

    /** The addresses for this process, from the current [RuntimeContext]. */
    fun current(): BindHost = of(RuntimeContext.executionEnvironment, RuntimeContext.inUnitTest)
  }
}

private const val LoopbackAddress: String = "127.0.0.1"
