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

/**
 * Sigmoid function for curve fitting
 */
object Sigmoid {

  /**
   * Sigmoid function
   *
   * This function returns the output of a sigmoid curve based on input parameters.
   * The sigmoid function is defined as:
   *   f(x) = l / (1 + exp(-a * (x - b)))
   * where:
   *   - x is the input value
   *   - a controls the slope (steepness) of the sigmoid curve
   *   - b controls the horizontal shift (center) of the curve
   *   - l is the curve's maximum value (upper asymptote)
   *
   * @param x The input value (independent variable)
   * @param a Parameter that affects the steepness of the curve
   * @param b Parameter that shifts the curve along the x-axis
   * @param l The maximum value (upper asymptote) of the curve
   * @return The output of the sigmoid function for the given input x
   */
  fun sigmoidFunction(x: Double, a: Double, b: Double, l: Double): Double {
    return l / (1 + exp(-a * (x - b)))  // Sigmoid equation
  }

  /**
   * Compute gradients for gradient descent optimization
   *
   * This function computes the partial derivatives (gradients) of the sigmoid function
   * with respect to the parameters a and b. These gradients are used in gradient descent
   * to update the parameters to fit the data.
   *
   * @param parameters An array containing the current values of a and b
   * @param x A list of x values (input data)
   * @param y A list of y values (target/observed data)
   * @param l The upper asymptote (maximum value) of the sigmoid function
   * @return A DoubleArray containing the computed gradients for a and b
   */
  fun computeGradients(parameters: DoubleArray, x: List<Double>, y: List<Double>, l: Double): DoubleArray {
    val a = parameters[0]  // Current value of parameter 'a' (steepness)
    val b = parameters[1]  // Current value of parameter 'b' (center/shift)

    // Predict the output using the current parameters for all x values
    val predicted = x.map { sigmoidFunction(it, a, b, l) }

    // Calculate the error between predicted and actual (y) values
    val errors = predicted.zip(y).map { (predicted, actual) -> predicted - actual }

    // f(x, a, b, l) = l / (1 + exp(-a*(x-b))) = l * σ(a*(x-b)),  σ(z)=1/(1+exp(-z))
    // Let s = sigmoidFunction(x, a, b, l) = l * σ(z); then σ(z) = s/l and
    // σ'(z) = σ*(1-σ) = (s/l) * (1 - s/l).
    //
    // df/da = l * σ'(z) * (x - b) = s * (l - s) / l * (x - b)
    // df/db = l * σ'(z) * (-a)    = -a * s * (l - s) / l
    val gradientA = errors.zip(x).map { (error, xValue) ->
      val sigmoid = sigmoidFunction(xValue, a, b, l)
      error * sigmoid * (l - sigmoid) / l * (xValue - b)
    }

    val gradientB = errors.zip(x).map { (error, xValue) ->
      val sigmoid = sigmoidFunction(xValue, a, b, l)
      error * (-a) * sigmoid * (l - sigmoid) / l
    }

    // Return the sum of gradients for 'a' and 'b' across all data points
    return doubleArrayOf(gradientA.sum(), gradientB.sum())
  }

  /**
   * Fit sigmoid curve using gradient descent
   *
   * This function fits a sigmoid curve to the given x and y data using gradient descent
   * to optimize the parameters a and b. The parameters are updated iteratively using the
   * gradients until a specified number of iterations is reached.
   *
   * @param x A list of x values (input data)
   * @param y A list of y values (target/observed data)
   * @param learningRate The learning rate for gradient descent (step size)
   * @param maxIterations The maximum number of iterations for the gradient descent loop
   * @param l The upper asymptote (maximum value) of the sigmoid function
   * @return The optimized parameters a and b as a DoubleArray
   */
  fun fitSigmoid(x: List<Double>, y: List<Double>, learningRate: Double, maxIterations: Int, l: Double): DoubleArray {
    // Initial guess for parameters 'a' (steepness) and 'b' (shift/center)
    val initialGuess = doubleArrayOf(1.0, 1.0)

    // Initialize parameters with the initial guess
    var parameters = initialGuess.copyOf()

    // Perform gradient descent for the specified number of iterations
    repeat(maxIterations) {
      // Compute the gradients for the current parameters
      val gradients = computeGradients(parameters, x, y, l)

      // Update each parameter using the learning rate and the corresponding gradient
      parameters = parameters.zip(gradients).map { (param, gradient) ->
        param - learningRate * gradient  // Update rule: param = param - learningRate * gradient
      }.toDoubleArray()
    }

    // Return the optimized parameters after gradient descent
    return parameters
  }
}
