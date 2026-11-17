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
package it.neckar.open.app.clikt

import assertk.*
import assertk.assertions.*
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.ConsoleAppender
import com.github.ajalt.clikt.command.main
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.options.required
import it.neckar.commons.logback.ConsoleTarget
import it.neckar.commons.logback.LogbackConfigurer
import it.neckar.open.app.clikt.model.GitlabAccessToken
import it.neckar.open.app.clikt.model.ProjectPath
import it.neckar.open.app.env.EnvFileLocator
import it.neckar.open.app.env.EnvFileName
import it.neckar.open.app.env.SystemFileSystemAccess
import it.neckar.open.crypt.Base64CertificatePem
import it.neckar.open.http.Url
import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.runtime.context.ServiceHost
import kotlinx.io.files.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.event.Level
import java.io.File

class BaseCommandTest {
  @Test
  fun testFindEnvFile() {
    val envFilePath = EnvFileLocator.findEnvFile(Path("."), EnvFileName.ForUnitTest, SystemFileSystemAccess)
      .requireNotNull { "no ${EnvFileName.ForUnitTest} found above the working directory" }

    assertThat(File(envFilePath.toString())).exists()
  }

  @Test
  suspend fun `using command line args`() {
    class TestCommand : BaseCommand<ServiceHost.Default>("test", ServiceHost.Default) {
      init {
        configureContext {
          exitProcess = { throw ExitCalledException() }
        }
      }

      val server by gitlabServerUrlOption()
      val project by gitlabProjectOption().required()
      val token by gitlabAccessTokenOption().required()

      var capturedServer: Url.Absolute? = null
      var capturedProject: ProjectPath? = null
      var capturedToken: GitlabAccessToken? = null

      override suspend fun execute() {
        capturedServer = server
        capturedProject = project
        capturedToken = token
      }
    }

    val cmd = TestCommand()
    // Set all explicitly, so no defaults are used
    cmd.main(arrayOf("--gitlab-server-url", "https://git.example.com", "--gitlab-project", "id/123", "--gitlab-access-token", "tok"))

    assertThat(cmd.capturedServer?.value).isEqualTo("https://git.example.com")
    assertThat(cmd.capturedProject?.value).isEqualTo("id/123")
    assertThat(cmd.capturedToken?.value).isEqualTo("tok")
  }

  @Test
  suspend fun `the access token file option reads the token without its line break`() {
    class TestCommand : BaseCommand<ServiceHost.Default>("test", ServiceHost.Default) {
      init {
        configureContext {
          exitProcess = { throw ExitCalledException() }
        }
      }

      val token by gitlabAccessTokenFileOption().required()

      var capturedToken: GitlabAccessToken? = null

      override suspend fun execute() {
        capturedToken = token
      }
    }

    val tokenFile: File = File.createTempFile("access-token", null).apply {
      deleteOnExit()
      writeText("tok\n")
    }
    val cmd = TestCommand()
    cmd.main(arrayOf("--gitlab-access-token-file", tokenFile.absolutePath))
    assertThat(cmd.capturedToken?.value).isEqualTo("tok")
  }

  @Test
  suspend fun `a secret file wins over the value of the same secret from the env file`() {
    class TestCommand : BaseCommand<ServiceHost.Default>("test", ServiceHost.Default) {
      init {
        configureContext {
          exitProcess = { throw ExitCalledException() }
        }
      }

      val token by secretFromFileOrValue(value = gitlabAccessTokenOption(), file = gitlabAccessTokenFileOption()).required()

      var capturedToken: GitlabAccessToken? = null

      override suspend fun execute() {
        capturedToken = token
      }
    }

    val tokenFile: File = File.createTempFile("access-token", null).apply {
      deleteOnExit()
      writeText("from-file")
    }
    val cmd = TestCommand()
    // The env file for unit tests sets GITLAB_ACCESS_TOKEN=abc123, as the .env of a local run does.
    cmd.main(arrayOf("--gitlab-access-token-file", tokenFile.absolutePath))
    assertThat(cmd.capturedToken?.value).isEqualTo("from-file")
  }

  @Test
  suspend fun `using env file`() {
    class TestCommand : BaseCommand<ServiceHost.Default>("test", ServiceHost.Default) {
      init {
        configureContext {
          exitProcess = { throw ExitCalledException() }
        }
      }

      val server by gitlabServerUrlOption()
      val project by gitlabProjectOption().required()
      val token by gitlabAccessTokenOption().required()

      var capturedServer: Url.Absolute? = null
      var capturedProject: ProjectPath? = null
      var capturedToken: GitlabAccessToken? = null

      override suspend fun execute() {
        capturedServer = server
        capturedProject = project
        capturedToken = token
      }
    }

    val cmd = TestCommand()
    // Set all explicitly, so no defaults are used
    cmd.main(arrayOf())

    assertThat(cmd.capturedServer?.value).isEqualTo("https://default.server")
    assertThat(cmd.capturedProject?.value).isEqualTo("group/sub/project")
    assertThat(cmd.capturedToken?.value).isEqualTo("abc123")
  }

  @Test
  suspend fun `caCertBase64Option should accept Base64 certificate via command line`() {
    class TestCommand : BaseCommand<ServiceHost.Default>("test", ServiceHost.Default) {
      init {
        configureContext {
          exitProcess = { throw ExitCalledException() }
        }
      }

      val caCertBase64 by caCertBase64Option()

      var capturedCaCertBase64: Base64CertificatePem? = null

      override suspend fun execute() {
        capturedCaCertBase64 = caCertBase64
      }
    }

    val testBase64Cert = "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCnRlc3QK"
    val cmd = TestCommand()
    cmd.main(arrayOf("--ca-cert-base64", testBase64Cert))

    assertThat(cmd.capturedCaCertBase64?.value).isEqualTo(testBase64Cert)
  }

  @Test
  suspend fun `caCertOption should accept certificate path via command line`() {
    class TestCommand : BaseCommand<ServiceHost.Default>("test", ServiceHost.Default) {
      init {
        configureContext {
          exitProcess = { throw ExitCalledException() }
        }
      }

      val caCertPath by caCertOption()

      var capturedCaCertPath: String? = null

      override suspend fun execute() {
        capturedCaCertPath = caCertPath
      }
    }

    val cmd = TestCommand()
    cmd.main(arrayOf("--ca-cert", "/path/to/cert.pem"))

    assertThat(cmd.capturedCaCertPath).isEqualTo("/path/to/cert.pem")
  }

  @Test
  suspend fun `gitlabServerUrlOption should reject invalid URL format`() {
    class TestCommand : BaseCommand<ServiceHost.Default>("test", ServiceHost.Default) {
      init {
        configureContext {
          exitProcess = { statusCode ->
            // Clikt exits with code 1 on error
            if (statusCode == 1) {
              throw ExitCalledException()
            }
          }
        }
      }

      val server by gitlabServerUrlOption()

      override suspend fun execute() {
        // Should never reach here
      }
    }

    val cmd = TestCommand()

    // Clikt catches the conversion exception and exits with code 1
    assertThrows<ExitCalledException> {
      cmd.main(arrayOf("--gitlab-server-url", "invalid-url-without-scheme"))
    }
  }

  @Test
  fun `a command that carries its answer on stdout logs to stderr`() {
    class StdoutAnswerCommand : BaseCommand<ServiceHost.Default>("test", ServiceHost.Default, ConsoleTarget.Stderr) {
      override suspend fun execute() {}
    }

    try {
      val command = StdoutAnswerCommand()

      assertThat(consoleTargets(), "the log target ${command.commandName} configured").containsExactly("System.err")
    } finally {
      //The Logback configuration is global: leaving it on stderr would decide where every following test logs
      LogbackConfigurer.configureLoggingConsoleOnly(levelForRoot = Level.INFO, levelForNeckarIt = Level.INFO)
    }
  }

  @Test
  fun `a command without an answer on stdout logs to stdout`() {
    class PlainCommand : BaseCommand<ServiceHost.Default>("test", ServiceHost.Default) {
      override suspend fun execute() {}
    }

    val command = PlainCommand()

    assertThat(consoleTargets(), "the log target ${command.commandName} configured").containsExactly("System.out")
  }

  /** The streams the root logger's console appenders write to, as Logback names them. */
  private fun consoleTargets(): List<String> {
    val loggerContext = org.slf4j.LoggerFactory.getILoggerFactory() as LoggerContext
    val rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)

    return rootLogger.iteratorForAppenders().asSequence()
      .filterIsInstance<ConsoleAppender<*>>()
      .map { it.target }
      .toList()
  }

  private class ExitCalledException : RuntimeException("System.exit() called")
}
