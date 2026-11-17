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
@file:Suppress("RETURN_VALUE_NOT_USED")

package it.neckar.result

import assertk.*
import assertk.assertions.*
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.getOr
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.toResultOr
import org.junit.jupiter.api.Test

/**
 * Demonstrates the kotlin-result API (https://github.com/michaelbull/kotlin-result).
 *
 * Result<V, E> is a monad for modelling success (Ok) or failure (Err) operations
 * without relying on exceptions for control flow.
 */
class KotlinResultTest {

  sealed interface AppError {
    data class NotFound(val id: Int) : AppError
    data class ValidationFailed(val message: String) : AppError
  }

  @Test
  fun `create Ok and Err results`() {
    val success: Result<Int, String> = Ok(42)
    val failure: Result<Int, String> = Err("something went wrong")

    assertThat(success.get()).isEqualTo(42)
    assertThat(failure.getError()).isEqualTo("something went wrong")
  }

  @Test
  fun `unwrap values with get and getError`() {
    val success: Result<Int, String> = Ok(42)
    val failure: Result<Int, String> = Err("boom")

    assertThat(success.get()).isEqualTo(42)
    assertThat(success.getError()).isNull()

    assertThat(failure.get()).isNull()
    assertThat(failure.getError()).isEqualTo("boom")
  }

  @Test
  fun `map transforms the success value`() {
    val result: Result<Int, String> = Ok(10)

    val mapped: Result<String, String> = result.map { "value=$it" }

    assertThat(mapped.get()).isEqualTo("value=10")
  }

  @Test
  fun `map does nothing on Err`() {
    val result: Result<Int, String> = Err("error")

    val mapped: Result<String, String> = result.map { "value=$it" }

    assertThat(mapped.getError()).isEqualTo("error")
  }

  @Test
  fun `mapError transforms the error value`() {
    val result: Result<Int, String> = Err("not found")

    val mapped: Result<Int, Int> = result.mapError { it.length }

    assertThat(mapped.getError()).isEqualTo(9)
  }

  @Test
  fun `flatMap aka andThen chains operations`() {
    fun parseInt(s: String): Result<Int, String> =
      s.toIntOrNull()?.let { Ok(it) } ?: Err("'$s' is not a number")

    fun ensurePositive(n: Int): Result<Int, String> =
      if (n > 0) Ok(n) else Err("$n is not positive")

    val result = parseInt("42").andThen { ensurePositive(it) }
    assertThat(result.get()).isEqualTo(42)

    val notANumber = parseInt("abc").andThen { ensurePositive(it) }
    assertThat(notANumber.getError()).isEqualTo("'abc' is not a number")

    val negative = parseInt("-5").andThen { ensurePositive(it) }
    assertThat(negative.getError()).isEqualTo("-5 is not positive")
  }

  @Test
  fun `getOr provides a default value on error`() {
    val success: Result<Int, String> = Ok(42)
    val failure: Result<Int, String> = Err("error")

    assertThat(success.getOr(0)).isEqualTo(42)
    assertThat(failure.getOr(0)).isEqualTo(0)
  }

  @Test
  fun `getOrElse computes a default value from the error`() {
    val failure: Result<Int, String> = Err("error")

    val value = failure.getOrElse { error -> error.length }

    assertThat(value).isEqualTo(5)
  }

  @Test
  fun `fold handles both cases`() {
    fun describe(result: Result<Int, String>): String =
      result.fold(
        success = { "got $it" },
        failure = { "failed: $it" },
      )

    assertThat(describe(Ok(42))).isEqualTo("got 42")
    assertThat(describe(Err("oops"))).isEqualTo("failed: oops")
  }

  @Test
  fun `onSuccess and onFailure for side effects`() {
    val log = mutableListOf<String>()

    val ok: Result<Int, String> = Ok(42)
    ok.onSuccess { log.add("success: $it") }
      .onFailure { log.add("failure: $it") }

    val err: Result<Int, String> = Err("boom")
    err.onSuccess { log.add("success: $it") }
      .onFailure { log.add("failure: $it") }

    assertThat(log).containsExactly("success: 42", "failure: boom")
  }

  @Test
  fun `toResultOr converts nullable to Result`() {
    val present: Int? = 42
    val absent: Int? = null

    assertThat(present.toResultOr { "was null" }.get()).isEqualTo(42)
    assertThat(absent.toResultOr { "was null" }.getError()).isEqualTo("was null")
  }

  @Test
  fun `runCatching converts exceptions to Err`() {
    val success = runCatching { 42 }
    val failure = runCatching { error("boom") }

    assertThat(success.get()).isEqualTo(42)
    assertThat(failure.getError()).isNotNull().isInstanceOf<IllegalStateException>()
  }

  @Test
  fun `binding block for ergonomic chaining`() {
    fun fetchUser(id: Int): Result<String, String> =
      if (id == 1) Ok("Alice") else Err("user not found")

    fun fetchEmail(name: String): Result<String, String> =
      if (name == "Alice") Ok("alice@example.com") else Err("email not found")

    val result: Result<String, String> = binding {
      val user = fetchUser(1).bind()
      val email = fetchEmail(user).bind()
      "User: $user, Email: $email"
    }

    assertThat(result.get()).isEqualTo("User: Alice, Email: alice@example.com")

    val failedResult: Result<String, String> = binding {
      val user = fetchUser(99).bind()
      val email = fetchEmail(user).bind()
      "User: $user, Email: $email"
    }

    assertThat(failedResult.getError()).isEqualTo("user not found")
  }

  @Test
  fun `typed errors instead of exceptions`() {
    fun findItem(id: Int): Result<String, AppError> = when (id) {
      1 -> Ok("Item-1")
      else -> Err(AppError.NotFound(id))
    }

    fun validateName(name: String): Result<String, AppError> = when {
      name.isBlank() -> Err(AppError.ValidationFailed("name must not be blank"))
      else -> Ok(name)
    }

    val result = findItem(1).andThen { validateName(it) }
    assertThat(result.get()).isEqualTo("Item-1")

    val notFound = findItem(99)
    val error = notFound.getError()
    assertThat(error).isNotNull()
    assertThat((error as AppError.NotFound).id).isEqualTo(99)
  }
}
