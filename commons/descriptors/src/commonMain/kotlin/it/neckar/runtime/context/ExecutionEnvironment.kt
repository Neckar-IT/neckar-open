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
package it.neckar.runtime.context

/**
 * Technical execution environment
 */
enum class ExecutionEnvironment {
  /**
   * Local development run — typically started from an IDE or directly on the developer's machine.
   * May include hot reload, local mocks, and relaxed security.
   */
  LocalDev,

  /**
   * Executable JAR running directly on a server or VM,
   * without containerization or orchestration.
   */
  Standalone,

  /**
   * Running inside a Docker container without orchestration (e.g., `docker run` locally or on a server).
   */
  Docker,

  /**
   * Running in a Continuous Integration environment (build/test pipeline).
   * Typically short-lived, with ephemeral resources and mocked external services.
   */
  CI,
  ;

  /**
   * Returns `true`, if this is a production-like environment ([Standalone], [Docker])
   */
  fun isProduction(): Boolean {
    return this == Standalone || this == Docker
  }

  fun inCi(): Boolean {
    return this == CI
  }

  /**
   * Returns true if this is [LocalDev]
   */
  fun isLocalDev(): Boolean {
    return this == LocalDev
  }
}
