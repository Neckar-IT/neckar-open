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

import it.neckar.open.container.CompressionLevel
import it.neckar.open.container.ContainerFormat
import it.neckar.open.container.ContainerType
import it.neckar.open.container.ContainerTypeName
import it.neckar.open.container.ContainerWriter
import it.neckar.open.container.EntryName
import it.neckar.open.container.VersionedEntryName
import it.neckar.open.container.parseContainerVersion
import it.neckar.open.version.Version

/** One call of `nit-container`, parsed from its arguments. */
internal sealed interface Invocation {
  /** `write`: a container of [containerType] on stdout, with one entry per element of [entries] in their order. */
  data class Write(
    val containerType: ContainerType,
    val entries: List<CommandEntry>,
  ) : Invocation

  /** `list`: the header and every data entry of the container on stdin. */
  data object ListEntries : Invocation

  /** `extract`: the content of the first version of [name] whose major [majors] lists; an empty [majors] takes every major. */
  data class Extract(
    val name: EntryName,
    val majors: Set<Int>,
  ) : Invocation

  /** `help`, `--help` or `-h`. */
  data object Help : Invocation
}

/** An entry whose content [command] writes to its stdout. */
internal data class CommandEntry(
  val versionedName: VersionedEntryName,
  val compressionLevel: CompressionLevel,
  /** A shell command line, run by `/bin/bash -o pipefail -c`. */
  val command: String,
)

/** The arguments are no valid call; `nit-container` prints the message and [Usage]. */
internal class UsageException(message: String) : Exception(message)

/** Parses the arguments of `nit-container`; throws [UsageException] for an invalid call, also for no argument at all. */
internal fun parseInvocation(args: List<String>): Invocation {
  val command = args.firstOrNull() ?: throw UsageException("No command given")
  val options = args.drop(1)
  return when (command) {
    "write" -> parseWrite(options)
    "list" -> parseList(options)
    "extract" -> parseExtract(options)
    "help", "--help", "-h" -> parseHelp(options)
    else -> throw UsageException("Unknown command <$command>")
  }
}

/** `--entry` and `--precompressed-entry` share one list, so the entries keep the order of the arguments. */
private fun parseWrite(args: List<String>): Invocation.Write {
  var typeName: ContainerTypeName? = null
  var typeVersion: Version? = null
  val entries = mutableListOf<CommandEntry>()

  val remaining = ArrayDeque(args)
  while (remaining.isNotEmpty()) {
    val option = remaining.removeFirst()
    when (option) {
      "--type" -> {
        if (typeName != null) throw UsageException("Option <--type> is given twice")
        typeName = usageCheck { ContainerTypeName(remaining.nextValue(option)) }
      }

      "--type-version" -> {
        if (typeVersion != null) throw UsageException("Option <--type-version> is given twice")
        typeVersion = usageCheck { parseContainerVersion(remaining.nextValue(option)) }
      }

      "--entry" -> entries += parseCommandEntry(remaining.nextValue(option), CompressionLevel.Default)
      "--precompressed-entry" -> entries += parseCommandEntry(remaining.nextValue(option), CompressionLevel.Precompressed)
      // An argument after an entry may be part of an unquoted command, so only a plain option name is quoted.
      else -> throw UsageException(if (OptionNamePattern.matches(option)) "Unknown option <$option> of write" else "Unexpected argument of write; quote each <name>/<version>=<command> as one argument")
    }
  }

  if (typeName == null) throw UsageException("write needs --type <type>")
  if (typeVersion == null) throw UsageException("write needs --type-version <major>.<minor>.<patch>")
  if (entries.isEmpty()) throw UsageException("write needs at least one --entry or --precompressed-entry")
  // Rejects a wrong order before the first command runs, so no dump runs for a container that cannot be written.
  usageCheck { ContainerWriter.verifyEntryNames(entries.map { it.versionedName }) }
  return Invocation.Write(ContainerType(typeName, typeVersion), entries)
}

private fun parseHelp(args: List<String>): Invocation.Help {
  if (args.isNotEmpty()) throw UsageException("help takes no argument, found <${args.first()}>")
  return Invocation.Help
}

private fun parseList(args: List<String>): Invocation.ListEntries {
  if (args.isNotEmpty()) throw UsageException("list takes no argument, found <${args.first()}>")
  return Invocation.ListEntries
}

/** An option name such as `--type`, which holds no command text. */
private val OptionNamePattern: Regex = Regex("--[a-z][a-z-]*")

/** A major as in a version: digits without leading zero. */
private val MajorPattern: Regex = Regex("0|[1-9][0-9]*")

private fun parseExtract(args: List<String>): Invocation.Extract {
  var name: EntryName? = null
  val majors = mutableSetOf<Int>()

  val remaining = ArrayDeque(args)
  while (remaining.isNotEmpty()) {
    val argument = remaining.removeFirst()
    when {
      argument == "--major" -> {
        val value = remaining.nextValue(argument)
        if (MajorPattern.matches(value).not()) throw UsageException("Major <$value> is no non-negative number without leading zero")
        majors += value.toIntOrNull() ?: throw UsageException("Major <$value> exceeds ${Int.MAX_VALUE}")
      }

      argument.startsWith("-") -> throw UsageException("Unknown option <$argument> of extract")
      name != null -> throw UsageException("extract takes one entry name, found <$name> and <$argument>")
      else -> {
        name = usageCheck { EntryName(argument) }
        if (name.isInMetaDirectory) throw UsageException("Entry <$name> lies in <${ContainerFormat.MetaDirectory}/>; extract reads data entries, list shows the header")
      }
    }
  }

  if (name == null) throw UsageException("extract needs the name of an entry, e.g. mongodb")
  return Invocation.Extract(name, majors)
}

/** Removes the value of [option] from the arguments. */
private fun ArrayDeque<String>.nextValue(option: String): String = removeFirstOrNull() ?: throw UsageException("Option <$option> has no value")

/**
 * `<name>/<version>=<command>`: the first `=` ends the name, so the command may contain `=` and the name cannot.
 * A value that misses its name is command text, which may hold credentials, so only a valid name reaches a message.
 */
private fun parseCommandEntry(value: String, compressionLevel: CompressionLevel): CommandEntry {
  val separator = value.indexOf('=')
  val versionedName = if (separator < 0) null else runCatching { VersionedEntryName.parse(value.substring(0, separator)) }.getOrNull()
  if (versionedName == null) throw UsageException("An entry does not start with <name>/<version>=, such as mongodb/1.0.0=")
  val command = value.substring(separator + 1)
  if (command.isBlank()) throw UsageException("Entry <$versionedName> has no command")
  return CommandEntry(versionedName, compressionLevel, command)
}

/** Turns the [IllegalArgumentException] a constructor or parser of the container library throws into a [UsageException]. */
private fun <T> usageCheck(parse: () -> T): T {
  return try {
    parse()
  } catch (e: IllegalArgumentException) {
    throw UsageException(e.message ?: "Invalid argument")
  }
}

internal const val Usage: String = """Usage:
  nit-container write --type <type> --type-version <major>.<minor>.<patch>
                      (--entry | --precompressed-entry) <name>/<version>=<command> ...
      Writes a container to stdout. Each entry holds the stdout of its <command>, run by
      /bin/bash -o pipefail -c, in the order of the options. --entry deflates with level 6;
      --precompressed-entry deflates with level 0 for content that is compressed already.
      Exits with 1 as soon as a <command> fails; the bytes on stdout are then no valid container.
      Every <command> stands in the process list, and so does every argument bash passes to a program.
      An exported environment variable inside single quotes, such as '--uri="${'$'}MONGODB_URI"', keeps
      a secret out of the arguments of nit-container and bash only; a password file of the program
      keeps it out of the process list. Bash prints a line of <command> it cannot parse to stderr.

  nit-container list
      Reads a container from stdin and prints its header as JSON, then one line
      <name>/<version> TAB <uncompressed bytes> per data entry. Exits with 1 for a corrupt entry;
      the lines of the entries before it are printed already.

  nit-container extract <name> [--major <major>]...
      Reads a container from stdin and writes the content of the first version of <name> to stdout,
      restricted to the given majors. The versions of a name stand highest first. Reads stdin to the
      end of the container and exits with 1 when the container has no such entry or any entry is
      corrupt; the bytes on stdout are then incomplete or unverified.

  nit-container help
      Prints this text to stdout; also --help and -h.

Exit codes: 0 success, 2 invalid call, 141 (SIGPIPE) when the reader of stdout stops reading and
SIGPIPE has its default action (a systemd service ignores it, then 1), 1 every other failure: a failed <command>, an invalid container, a missing entry or a failed
read or write of stdin or stdout.
"""
