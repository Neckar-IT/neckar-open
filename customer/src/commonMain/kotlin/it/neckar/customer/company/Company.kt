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
package it.neckar.customer.company

import it.neckar.customer.Address
import it.neckar.lifeCycle.HasArchiveState
import it.neckar.lifeCycle.ArchiveState
import it.neckar.open.http.Url
import it.neckar.uuid.NativeUuid
import it.neckar.uuid.ValueClassUuid
import it.neckar.uuid.randomForEnvironment
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

/**
 * Represents a company
 */
@Serializable
data class Company(
  /**
   * The company ID
   */
  val id: Id,

  /**
   * The namespace of the company
   */
  @Deprecated("????", level = DeprecationLevel.WARNING)
  val namespace: String,

  /**
   * The name of the company
   */
  val name: String,

  /**
   * The (main) address of the company
   */
  val address: Address.Confirmed? = null,

  val contactInformation: ContactInformation? = null,
  val bankAccountInformation: BankAccountInformation? = null,
  val legalInformation: CompanyLegalInformation? = null,

  /**
   * URL of the company logo.
   * Defaults to [logoPlaceholder] if no logo has been uploaded.
   */
  val logoUrl: Url = logoPlaceholder,

  override val archiveState: ArchiveState = ArchiveState.Active,
) : HasArchiveState {

  @JvmInline
  @Serializable
  value class Id(override val uuid: NativeUuid) : ValueClassUuid {
    override fun toString(): String {
      return uuid.toString()
    }

    companion object {
      fun random(): Id = Id(Uuid.randomForEnvironment())
    }
  }

  fun oneLiner(delimiter: String = " * "): String {
    if (address == null) {
      return name
    }
    return "$name$delimiter${address.streetWithHouseNumber}$delimiter${address.cityWithZipCode}"
  }

  companion object {
    /**
     * Placeholder logo URL used when no logo has been uploaded.
     */
    val logoPlaceholder: Url = Url.relative("img/logo/logo-not-available.png")

    fun empty(id: Id = Id.random(), namespace: String, name: String): Company {
      return Company(
        id = id,
        namespace = namespace,
        name = name,
        address = null,
        contactInformation = null,
        bankAccountInformation = null,
        legalInformation = null,
      )
    }
  }
}
