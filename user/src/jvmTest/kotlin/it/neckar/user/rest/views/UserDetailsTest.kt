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
package it.neckar.user.rest.views

import it.neckar.contact.EmailAddress
import it.neckar.open.authorization.CoreCapability
import it.neckar.open.authorization.RoleLabel
import it.neckar.open.authorization.UserCapabilities
import it.neckar.open.kotlin.serializers.JsonInclusionStrategy
import it.neckar.open.serialization.roundTrip
import it.neckar.open.tenancy.Tenant
import it.neckar.user.User
import it.neckar.user.UserLoginName
import it.neckar.uuid.parseUuid
import org.junit.jupiter.api.Test

class UserDetailsTest {
  /**
   * What the services serialize with — a field whose value equals a default would be missing from
   * their JSON, so the test that pins the wire format has to skip defaults the same way.
   */
  private val ServiceInclusionStrategy: JsonInclusionStrategy = JsonInclusionStrategy.SkipDefaultsIncludeNulls

  @Test
  fun testJson() {
    roundTrip(
      objectToSerialize = user().toDetails(
        tenantId = Tenant.Id(parseUuid("b2c3d4e5-f6a7-8901-bcde-f12345678901")),
        capabilities = UserCapabilities(setOf(CoreCapability.ReadData.key)),
        roleLabel = RoleLabel("owner"),
      ),
      inclusionStrategy = ServiceInclusionStrategy,
    ) {
      //language=JSON
      """
        {
          "id": "550e8400-e29b-41d4-a716-446655440000",
          "loginName": "test",
          "firstName": "first",
          "lastName": "last",
          "email": "test@example.com",
          "avatarUrl": null,
          "environment": [],
          "tenantId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
          "capabilities": [
            "core:read-data"
          ],
          "roleLabel": "owner"
        }
      """.trimIndent()
    }
  }

  /**
   * A product without tenants, capabilities or named roles sends the empty answer — as fields that
   * are there and empty, never as fields the frontend has to miss.
   */
  @Test
  fun `writes every field a product leaves empty`() {
    roundTrip(user().toDetails(), inclusionStrategy = ServiceInclusionStrategy) {
      //language=JSON
      """
        {
          "id": "550e8400-e29b-41d4-a716-446655440000",
          "loginName": "test",
          "firstName": "first",
          "lastName": "last",
          "email": "test@example.com",
          "avatarUrl": null,
          "environment": [],
          "tenantId": null,
          "capabilities": [],
          "roleLabel": null
        }
      """.trimIndent()
    }
  }

  private fun user(): User = User(
    id = User.Id(parseUuid("550e8400-e29b-41d4-a716-446655440000")),
    loginName = UserLoginName("test"),
    firstName = "first",
    lastName = "last",
    email = EmailAddress("test@example.com"),
    avatarUrl = null,
    environment = emptySet(),
  )
}
