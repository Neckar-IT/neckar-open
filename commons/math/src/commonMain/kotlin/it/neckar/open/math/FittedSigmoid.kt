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

import kotlin.math.abs
import kotlin.math.exp
import kotlin.random.Random

/**
 * A utility object for generating synthetic data and fitting sigmoidal models.
 */
object FittedSigmoid {

  /**
   * Generates synthetic data for sigmoidal fitting.
   *
   * The sigmoidal fitting process involves modeling a relationship between an independent variable (often denoted as x)
   * and a dependent variable (often denoted as y or p) using a sigmoidal curve. Sigmoidal curves are commonly used
   * in various fields such as biology, economics, and machine learning to represent growth, saturation, or probability
   * distribution functions.
   *
   * In the context of this function, the independent variable 'x' represents a set of input values or predictors. These
   * values typically represent the range over which the sigmoidal curve will be fitted. For example, if modeling the
   * relationship between time and a biological response, 'x' could represent different time points at which the response
   * is measured.
   *
   * The dependent variable 'p' (often referred to as 'probability' in the context of logistic regression) represents
   * the corresponding output values or responses associated with the input values 'x'. In the context of logistic
   * regression or sigmoidal fitting, 'p' typically represents the probability of an event occurring given the input value
   * 'x'. For example, if modeling the probability of a disease occurrence based on a person's age, 'x' could represent
   * the age of individuals, and 'p' could represent the probability of the disease occurrence.
   *
   * This function generates synthetic data for sigmoidal fitting by simulating a set of 'x' values and corresponding 'p'
   * values. The 'x' values are generated within a specified range, and the 'p' values are computed using a logistic
   * function with added random noise to simulate variability in the data. The amount of noise, controlled by the 'variance'
   * parameter, determines the spread of the 'p' values around the sigmoidal curve.
   *
   * @param variance The variance of the synthetic data. Default is 0.1.
   * @return A pair of lists representing the generated synthetic data. The first list contains the independent variables (x),
   *         which represent the input values. The second list contains the dependent variables (p), which represent the output
   *         values or probabilities associated with the input values.
   */
  fun generateSyntheticData(amount:Int,variance: Double = 0.1): Pair<List<Double>, List<Double>> {
    val rng = Random(0) // For reproducibility
    var x = List(amount) { it.toDouble() / 10 - 5 }
    val p = x.map { 1 / (1 + exp(-it)) + variance * rng.nextDouble() }
    x = List(amount) { it.toDouble() / 10 }
    return Pair(x, p)
  }

  /**
   * Computes the logistic function value.
   * @param x The input value.
   * @param a The parameter for the horizontal asymptote.
   * @param b The growth rate parameter.
   * @param c The x-coordinate of the inflection point.
   * @return The value of the logistic function at the given input value.
   */
  fun logistic(x: Double, a: Double, b: Double, c: Double): Double {
    return a + (1 - a) / (1 + exp(-b * (x - c)))
  }

  /**
   * Fits a logistic model to the provided data.
   * @param x The list of independent variables (x).
   * @param p The list of dependent variables (p).
   * @return An array containing the fitted parameters [a, b, c] for the logistic model.
   */
  fun fitLogisticModel(x: List<Double>, p: List<Double>): DoubleArray {
    fun minimize(initialParams: DoubleArray, objective: (DoubleArray) -> Double): DoubleArray {
      val learningRate = 0.005 // Reduced for better optimization on small data
      val maxIterations = 5000
      val tolerance = 1e-5 // Looser tolerance for small data

      val params = initialParams.copyOf()
      var prevError = Double.POSITIVE_INFINITY

      for (iteration in 0 until maxIterations) {
        val currentError = objective(params)
        if (abs(currentError - prevError) < tolerance) {
          break // Convergence achieved
        }
        prevError = currentError

        // Central difference for gradient estimation
        val gradients = DoubleArray(params.size) { index ->
          val perturbedParamsUp = params.copyOf()
          perturbedParamsUp[index] += 1e-6
          val perturbedParamsDown = params.copyOf()
          perturbedParamsDown[index] -= 1e-6

          val errorUp = objective(perturbedParamsUp)
          val errorDown = objective(perturbedParamsDown)
          (errorUp - errorDown) / (2 * 1e-6)
        }

        // Update parameters
        for (i in params.indices) {
          params[i] -= learningRate * gradients[i]
        }
      }

      return params
    }

    // Regularized objective function
    fun objective(params: DoubleArray): Double {
      val (a, b, c) = params
      val error = x.zip(p).sumOf { (xi, pi) ->
        val pred = logistic(xi, a, b, c)
        (pi - pred) * (pi - pred)
      }
      val regularization = 0.01 * (a * a + b * b + c * c) // Regularization term
      return error + regularization
    }

    require(x.isNotEmpty()) { "Cannot fit logistic model to empty x data" }
    val xRange = x.max() - x.min()
    require(xRange > 0.0) { "Cannot fit logistic model to constant x data (range = 0); slope is undefined" }

    // Improved initial guess
    val aInit = p.minOrNull() ?: 0.0 // Lower bound of ydata
    val bInit = 1.0 / xRange // Approximate slope
    val cInit = x.average() // Midpoint of xdata
    val initialGuess = doubleArrayOf(aInit, bInit, cInit)

    return minimize(initialGuess, ::objective)
  }

  /**
   * Calculates the R-squared (coefficient of determination) value for the fitted model.
   *
   * @param actual The list of actual values (p).
   * @param predicted The list of predicted values obtained from the logistic model.
   * @return The R-squared value.
   */
  fun calculateRSquared(actual: List<Double>, predicted: List<Double>): Double {
    val meanActual = actual.average()
    val ssTotal = actual.sumOf { (it - meanActual) * (it - meanActual) }
    val ssResidual = actual.zip(predicted).sumOf { (act, pred) -> (act - pred) * (act - pred) }

    // ssTotal == 0 means actual is constant (zero variance). The classical formula
    // 1 - ssResidual/ssTotal is undefined; pick a sensible fallback instead of
    // propagating NaN/Infinity: 1.0 if predicted matched perfectly, 0.0 otherwise.
    if (ssTotal == 0.0) {
      return if (ssResidual == 0.0) 1.0 else 0.0
    }

    return 1 - (ssResidual / ssTotal)
  }
}
