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

import kotlin.js.JsExport
import kotlin.math.exp

/**
 * Gompertz sigmoid function for curve fitting
 * taken from https://de.mathworks.com/matlabcentral/answers/1754060-sigmoid-function-shaping-and-fitting-by-curve-fitting-toolbox
 */
object GompertzSigmoid {

  // Gompertz function
  fun gompertzFunction(x: Double, a: Double, b: Double, c: Double, d: Double): Double {
    return a * exp(-exp(-b - c * (x - d)))
  }

  // Compute gradients for Gompertz curve fitting
  fun computeGompertzGradients(parameters: DoubleArray, x: DoubleArray, y: DoubleArray): DoubleArray {
    val a = parameters[0]
    val b = parameters[1]
    val c = parameters[2]
    val d = parameters[3]

    val predicted = x.map { gompertzFunction(it, a, b, c, d) }
    val errors = predicted.toList().zip(y.toList()).map { (predicted, actual) -> predicted - actual }

    // f(x, a, b, c, d) = a * exp(-exp(-b - c*(x-d)))
    // Let z = -b - c*(x-d), u = exp(z), v = exp(-u). Then f = a * v and dz has the
    // following partials: dz/db = -1, dz/dc = -(x-d), dz/dd = c.
    //   df/da = v
    //   df/db = a *  u * v
    //   df/dc = a * (x-d) * u * v
    //   df/dd = -a * c * u * v
    // The earlier formulas multiplied gompertzFunction (= a*v) and a phantom
    // (1 - u) factor into every gradient — neither belongs in the real derivative.
    val gradientA = errors.toList().zip(x.toList()).map { (error, xValue) ->
      val u = exp(-b - c * (xValue - d))
      val v = exp(-u)
      error * v
    }
    val gradientB = errors.toList().zip(x.toList()).map { (error, xValue) ->
      val u = exp(-b - c * (xValue - d))
      val v = exp(-u)
      error * a * u * v
    }
    val gradientC = errors.toList().zip(x.toList()).map { (error, xValue) ->
      val u = exp(-b - c * (xValue - d))
      val v = exp(-u)
      error * a * (xValue - d) * u * v
    }
    val gradientD = errors.toList().zip(x.toList()).map { (error, xValue) ->
      val u = exp(-b - c * (xValue - d))
      val v = exp(-u)
      error * (-a) * c * u * v
    }

    return doubleArrayOf(gradientA.sum(), gradientB.sum(), gradientC.sum(), gradientD.sum())
  }

  // Fit Gompertz curve using built-in functions
  fun fitGompertz(x: DoubleArray, y: DoubleArray, a: Double, b: Double, c: Double, d: Double, learningRate: Double, maxIterations: Int): DoubleArray {
    val initialGuess = doubleArrayOf(a, b, c, d)

    var parameters = initialGuess.copyOf()

    repeat(maxIterations) {
      val gradients = computeGompertzGradients(parameters, x, y)
      parameters = parameters.zip(gradients).map { (param, gradient) ->
        param - learningRate * gradient
      }.toDoubleArray()
    }

    return parameters
  }

  /**
   * a*exp(-b*exp(-c*x))
   * taken from https://en.wikipedia.org/wiki/Gompertz_function
   * @param x the x value
   * @param a the asymptote
   * @param b the displacement on the x-axis
   * @param c the growth rate (y scaling)
   */
  fun wikiGompertz(x: Double, a: Double, b: Double, c: Double): Double {
    return a * exp(-b * exp(-c * x))
  }

  fun computeWikiGompertzGradients(parameters: DoubleArray, x: DoubleArray, y: DoubleArray): DoubleArray {
    val a = parameters[0]
    val b = parameters[1]
    val c = parameters[2]

    val predicted = x.map { wikiGompertz(it, a, b, c) }
    val errors = predicted.toList().zip(y.toList()).map { (predicted, actual) -> predicted - actual }

    // f(x, a, b, c) = a * exp(-b * exp(-c*x))
    // Let u = exp(-c*x), v = exp(-b*u). Then f = a * v.
    //   df/da = v
    //   df/db = -a * u * v
    //   df/dc = a * b * x * u * v
    // The earlier formulas multiplied wikiGompertz(x, a, b, c) (= a*v) into every
    // gradient and added a phantom (1 - v) factor — neither belongs in the correct
    // derivative.
    val gradientA = errors.toList().zip(x.toList()).map { (error, xValue) ->
      val u = exp(-c * xValue)
      val v = exp(-b * u)
      error * v
    }
    val gradientB = errors.toList().zip(x.toList()).map { (error, xValue) ->
      val u = exp(-c * xValue)
      val v = exp(-b * u)
      error * (-a) * u * v
    }
    val gradientC = errors.toList().zip(x.toList()).map { (error, xValue) ->
      val u = exp(-c * xValue)
      val v = exp(-b * u)
      error * a * b * xValue * u * v
    }

    return doubleArrayOf(gradientA.sum(), gradientB.sum(), gradientC.sum())
  }

  fun fitWikiGompertz(x: DoubleArray, y: DoubleArray, a: Double, b: Double, c: Double, learningRate: Double, maxIterations: Int): DoubleArray {
    val initialGuess = doubleArrayOf(a, b, c)

    var parameters = initialGuess.copyOf()

    repeat(maxIterations) {
      val gradients = computeWikiGompertzGradients(parameters, x, y)
      parameters = parameters.zip(gradients).map { (param, gradient) ->
        param - learningRate * gradient
      }.toDoubleArray()
    }

    return parameters
  }

}
