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
 * Business/lifecycle environment.
 *
 * Describes the functional environment in the software delivery lifecycle.
 */
enum class DeploymentStage {
  /**
   * Active development environment — unstable, feature work in progress,
   * often using test data and relaxed validation.
   */
  Development,

  /**
   * Pre-production environment mirroring Production setup and configuration.
   * Used for final integration, performance, and acceptance testing.
   */
  Staging,

  /**
   * Demonstration environment — contains demo data and possibly hard-coded credentials.
   * No registration possible; may auto-login a demo user.
   */
  Demo,

  /**
   * Live, customer-facing production environment — stable, secured,
   * with real customer data and strict policies.
   */
  Production,

  /**
   * Quality assurance / dedicated testing environment.
   * Used for automated/manual testing with controlled data sets.
   */
  QA,

  /**
   * Isolated environment for experiments, proof-of-concepts, or user sandboxes.
   * No guarantee of data persistence or stability.
   */
  Sandbox;

  fun isDev(): Boolean {
    return this == Development
  }

  fun isDevelopmentOrDemo(): Boolean {
    return this == Development || this == Demo
  }

  fun isDemo(): Boolean {
    return this == Demo
  }
}
