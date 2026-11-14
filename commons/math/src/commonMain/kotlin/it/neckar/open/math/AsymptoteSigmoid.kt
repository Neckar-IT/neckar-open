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
package it.neckar.open.math

import kotlin.math.exp

object AsymptoteSigmoid {
  fun value(x: Double, low: Double, high: Double): Double {
    return low + (high - low) / (1 + exp(-x))
  }

  fun computeGradients(parameters: DoubleArray, x: DoubleArray, y: DoubleArray): DoubleArray {
    val low = parameters[0]
    val high = parameters[1]

    val predicted = x.map { value(it, low, high) }
    val errors = predicted.toList().zip(y.toList()).map { (predicted, actual) -> predicted - actual }

    // f(x, low, high) = low + (high - low) / (1 + exp(-x)) = low * (1 - σ(x)) + high * σ(x)
    // df/dlow  = 1 - σ(x)
    // df/dhigh = σ(x)
    // The earlier version evaluated σ(low) and σ(high) on the parameters themselves,
    // which has nothing to do with the actual gradient.
    val sigmoid = x.map { 1.0 / (1.0 + exp(-it)) }
    val gradientLow = errors.zip(sigmoid).map { (error, s) -> error * (1.0 - s) }
    val gradientHigh = errors.zip(sigmoid).map { (error, s) -> error * s }

    return doubleArrayOf(gradientLow.sum(), gradientHigh.sum())
  }

  fun fit(x: DoubleArray, y: DoubleArray, learningRate: Double, maxIterations: Int): DoubleArray {
    val initialGuess = doubleArrayOf(1.0, 1.0) // Initial guess for parameters (low, high)

    var parameters = initialGuess.copyOf()

    repeat(maxIterations) {
      val gradients = computeGradients(parameters, x, y)
      parameters = parameters.zip(gradients).map { (param, gradient) ->
        param - learningRate * gradient
      }.toDoubleArray()
    }

    return parameters
  }
}
