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
package it.neckar.otel

import io.opentelemetry.api.common.AttributeKey

/**
 * OTel resource attribute keys set by [NeckarRuntimeContextResourceProvider].
 *
 * `deployment.environment` and `service.namespace` are OTel-standard keys; the `neckar.*` keys are
 * our own. Kept as plain string literals (not the `opentelemetry-semconv` constants) on purpose:
 * this module must stay dependency-free so the JAR is self-contained for the OTel Java Agent's
 * isolated extension classloader.
 */
object NeckarResourceAttributes {
  val DeploymentEnvironment: AttributeKey<String> = AttributeKey.stringKey("deployment.environment")
  val ServiceNamespace: AttributeKey<String> = AttributeKey.stringKey("service.namespace")
  val ExecutionEnvironment: AttributeKey<String> = AttributeKey.stringKey("neckar.execution_environment")
  val ServiceHost: AttributeKey<String> = AttributeKey.stringKey("neckar.service_host")
  val ServiceVersion: AttributeKey<String> = AttributeKey.stringKey("service.version")
}
