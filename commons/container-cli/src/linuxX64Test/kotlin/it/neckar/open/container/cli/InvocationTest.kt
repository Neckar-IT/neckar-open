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
package it.neckar.open.container.cli

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.doesNotContain
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.messageContains
import it.neckar.open.container.CompressionLevel
import it.neckar.open.container.ContainerType
import it.neckar.open.container.ContainerTypeName
import it.neckar.open.container.EntryName
import it.neckar.open.container.VersionedEntryName
import it.neckar.open.version.Version
import kotlin.test.Test

class InvocationTest {
  @Test
  fun keepsTheOrderOfBothEntryOptions() {
    val invocation = parseInvocation(
      listOf(
        "write", "--type", "it.neckar.backup.archive", "--type-version", "2.3.0",
        "--entry", "config/users/1.2.0=cat users.json",
        "--precompressed-entry", "mongodb/1.0.0=mongodump --archive --gzip --uri=mongodb://db",
        "--entry", "volume/2.1.0=tar -cf - -C /data .",
      )
    )

    assertThat(invocation).isEqualTo(
      Invocation.Write(
        ContainerType(ContainerTypeName("it.neckar.backup.archive"), Version.of(2, 3, 0)),
        listOf(
          CommandEntry(VersionedEntryName.parse("config/users/1.2.0"), CompressionLevel.Default, "cat users.json"),
          CommandEntry(VersionedEntryName.parse("mongodb/1.0.0"), CompressionLevel.Precompressed, "mongodump --archive --gzip --uri=mongodb://db"),
          CommandEntry(VersionedEntryName.parse("volume/2.1.0"), CompressionLevel.Default, "tar -cf - -C /data ."),
        ),
      )
    )
  }

  @Test
  fun rejectsAWriteWithoutTypeVersion() {
    assertFailure { parseInvocation(listOf("write", "--type", "it.neckar.backup.archive", "--entry", "volume/2.1.0=true")) }
      .isInstanceOf<UsageException>().hasMessage("write needs --type-version <major>.<minor>.<patch>")
  }

  @Test
  fun rejectsAnIncompleteTypeVersion() {
    assertFailure { parseInvocation(listOf("write", "--type", "it.neckar.backup.archive", "--type-version", "2.3", "--entry", "volume/2.1.0=true")) }
      .isInstanceOf<UsageException>().messageContains("<2.3>")
  }

  @Test
  fun rejectsAnEntryWithoutVersion() {
    assertFailure { parseInvocation(write("--entry", "volume=true")) }.isInstanceOf<UsageException>().hasMessage("An entry does not start with <name>/<version>=, such as mongodb/1.0.0=")
  }

  @Test
  fun rejectsAnUnknownOption() {
    assertFailure { parseInvocation(listOf("write", "--type", "it.neckar.backup.archive", "--compress")) }
      .isInstanceOf<UsageException>().hasMessage("Unknown option <--compress> of write")
  }

  @Test
  fun rejectsAnEntryWithoutSeparator() {
    assertFailure { parseInvocation(write("--entry", "mongodb/1.0.0 mysqldump -psecret")) }
      .isInstanceOf<UsageException>().hasMessage("An entry does not start with <name>/<version>=, such as mongodb/1.0.0=")
  }

  /** The text before the first = of a command without name can hold a password. */
  @Test
  fun quotesNoCommandOfAnEntryWithoutName() {
    assertFailure { parseInvocation(write("--entry", "mongodump --uri mongodb://user:secret@db/app --archive=x")) }
      .isInstanceOf<UsageException>().transform { it.message.orEmpty() }.doesNotContain("secret")
  }

  @Test
  fun quotesNoUnquotedCommandPart() {
    assertFailure { parseInvocation(write("--entry", "mongodb/1.0.0=mongodump", "--uri=mongodb://user:secret@db")) }
      .isInstanceOf<UsageException>().hasMessage("Unexpected argument of write; quote each <name>/<version>=<command> as one argument")
  }

  @Test
  fun rejectsAnEntryWithoutCommand() {
    assertFailure { parseInvocation(write("--entry", "volume/2.1.0= ")) }.isInstanceOf<UsageException>().hasMessage("Entry <volume/2.1.0> has no command")
  }

  @Test
  fun rejectsAscendingVersionsBeforeAnyCommandRuns() {
    assertFailure { parseInvocation(write("--entry", "mongodb/1.0.0=true", "--entry", "mongodb/2.0.0=true")) }
      .isInstanceOf<UsageException>().hasMessage("Entry <mongodb/2.0.0> follows <mongodb/1.0.0>; the versions of a name stand highest first")
  }

  @Test
  fun rejectsAnEntryGivenTwice() {
    assertFailure { parseInvocation(write("--entry", "mongodb/1.0.0=true", "--entry", "mongodb/1.0.0=true")) }
      .isInstanceOf<UsageException>().hasMessage("Entry <mongodb/1.0.0> is given twice")
  }

  @Test
  fun rejectsAWriteWithoutType() {
    assertFailure { parseInvocation(listOf("write", "--type-version", "2.3.0", "--entry", "volume/2.1.0=true")) }
      .isInstanceOf<UsageException>().hasMessage("write needs --type <type>")
  }

  @Test
  fun rejectsAnOptionWithoutValue() {
    assertFailure { parseInvocation(write("--entry")) }.isInstanceOf<UsageException>().hasMessage("Option <--entry> has no value")
  }

  @Test
  fun rejectsAnEntryInTheMetaDirectory() {
    assertFailure { parseInvocation(write("--entry", "meta/info/1.0.0=true")) }.isInstanceOf<UsageException>().messageContains("<meta/>")
  }

  @Test
  fun rejectsATypeGivenTwice() {
    assertFailure { parseInvocation(write("--type", "it.neckar.invoice.export", "--entry", "volume/2.1.0=true")) }
      .isInstanceOf<UsageException>().hasMessage("Option <--type> is given twice")
  }

  @Test
  fun parsesExtractWithMajors() {
    val invocation = parseInvocation(listOf("extract", "mongodb", "--major", "1", "--major", "2"))

    assertThat(invocation).isEqualTo(Invocation.Extract(EntryName("mongodb"), setOf(1, 2)))
  }

  @Test
  fun rejectsAMajorThatIsNoNumber() {
    assertFailure { parseInvocation(listOf("extract", "mongodb", "--major", "one")) }.isInstanceOf<UsageException>().hasMessage("Major <one> is no non-negative number without leading zero")
  }

  @Test
  fun rejectsAMetaEntryForExtract() {
    assertFailure { parseInvocation(listOf("extract", "meta/type")) }.isInstanceOf<UsageException>().messageContains("<meta/>")
  }

  @Test
  fun rejectsTwoEntryNamesForExtract() {
    assertFailure { parseInvocation(listOf("extract", "mongodb", "volume")) }
      .isInstanceOf<UsageException>().hasMessage("extract takes one entry name, found <mongodb> and <volume>")
  }

  @Test
  fun rejectsAnUnknownCommand() {
    assertFailure { parseInvocation(listOf("unpack")) }.isInstanceOf<UsageException>().hasMessage("Unknown command <unpack>")
  }

  @Test
  fun rejectsATypeVersionGivenTwice() {
    assertFailure { parseInvocation(write("--type-version", "2.4.0", "--entry", "volume/2.1.0=true")) }
      .isInstanceOf<UsageException>().hasMessage("Option <--type-version> is given twice")
  }

  @Test
  fun rejectsAMajorWithLeadingZero() {
    assertFailure { parseInvocation(listOf("extract", "mongodb", "--major", "01")) }.isInstanceOf<UsageException>().hasMessage("Major <01> is no non-negative number without leading zero")
  }

  @Test
  fun rejectsAMajorBeyondInt() {
    assertFailure { parseInvocation(listOf("extract", "mongodb", "--major", "99999999999")) }.isInstanceOf<UsageException>().hasMessage("Major <99999999999> exceeds 2147483647")
  }

  @Test
  fun rejectsANegativeMajor() {
    assertFailure { parseInvocation(listOf("extract", "mongodb", "--major", "-1")) }.isInstanceOf<UsageException>().hasMessage("Major <-1> is no non-negative number without leading zero")
  }

  @Test
  fun rejectsAnUnknownOptionOfExtract() {
    assertFailure { parseInvocation(listOf("extract", "mongodb", "--all")) }.isInstanceOf<UsageException>().hasMessage("Unknown option <--all> of extract")
  }

  @Test
  fun namesTheVersionThatIsNoNumber() {
    assertFailure { parseInvocation(listOf("write", "--type", "it.neckar.backup.archive", "--type-version", "1.0.x", "--entry", "volume/2.1.0=true")) }
      .isInstanceOf<UsageException>().hasMessage("Version <1.0.x> is no plain <major>.<minor>.<patch>")
  }

  @Test
  fun rejectsAMajorWithoutValue() {
    assertFailure { parseInvocation(listOf("extract", "mongodb", "--major")) }.isInstanceOf<UsageException>().hasMessage("Option <--major> has no value")
  }

  @Test
  fun rejectsExtractWithoutName() {
    assertFailure { parseInvocation(listOf("extract", "--major", "1")) }.isInstanceOf<UsageException>().hasMessage("extract needs the name of an entry, e.g. mongodb")
  }

  @Test
  fun rejectsAnArgumentForList() {
    assertFailure { parseInvocation(listOf("list", "mongodb")) }.isInstanceOf<UsageException>().hasMessage("list takes no argument, found <mongodb>")
  }

  @Test
  fun parsesHelp() {
    listOf("help", "--help", "-h").forEach { assertThat(parseInvocation(listOf(it))).isEqualTo(Invocation.Help) }
  }

  /** `help write --type x` would otherwise print the help with exit code 0 for a mangled write call. */
  @Test
  fun rejectsAnArgumentForHelp() {
    assertFailure { parseInvocation(listOf("help", "write")) }.isInstanceOf<UsageException>().hasMessage("help takes no argument, found <write>")
  }

  @Test
  fun rejectsAShortOptionOfExtract() {
    assertFailure { parseInvocation(listOf("extract", "-h")) }.isInstanceOf<UsageException>().hasMessage("Unknown option <-h> of extract")
  }

  @Test
  fun rejectsAWriteWithoutEntry() {
    assertFailure { parseInvocation(write()) }.isInstanceOf<UsageException>().hasMessage("write needs at least one --entry or --precompressed-entry")
  }

  /** An empty argument list in `nit-container $ARGS | age …` would otherwise encrypt the help text with exit code 0. */
  @Test
  fun rejectsNoArgument() {
    assertFailure { parseInvocation(emptyList()) }.isInstanceOf<UsageException>().hasMessage("No command given")
  }

  /** A valid `write` call followed by [arguments]. */
  private fun write(vararg arguments: String): List<String> {
    return listOf("write", "--type", "it.neckar.backup.archive", "--type-version", "2.3.0") + arguments
  }
}
