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
package it.neckar.open.javafx.threed

import javafx.collections.ObservableFloatArray
import javafx.scene.Group
import javafx.scene.paint.Color
import javafx.scene.shape.TriangleMesh
import org.fxyz3d.geometry.Point3D
import org.fxyz3d.shapes.composites.PolyLine3D
import java.util.function.Consumer

/**
 * Draws a line for each edge of a [TriangleMesh].
 */
class MeshLines(
  mesh: TriangleMesh,
  val lineColor: Color?,
  private val lineType: PolyLine3D.LineType,
  private val lineWidth: Float,
  private val offset: Float,
  private val divisions: Int,
) : Group() {

  constructor(
    mesh: TriangleMesh,
    divisions: Int,
  ) : this(mesh, Color.DARKGRAY, divisions) {
  }

  constructor(
    mesh: TriangleMesh,
    lineColor: Color,
    divisions: Int,
  ) : this(mesh, lineColor, PolyLine3D.LineType.TRIANGLE, divisions) {
  }

  constructor(
    mesh: TriangleMesh,
    lineColor: Color,
    lineType: PolyLine3D.LineType,
    divisions: Int,
  ) : this(mesh, lineColor, lineType, 1.0f, 0.0f, divisions) {
  }

  init {
    children.addAll(createPolyLines(mesh))
    mesh.points.addListener { observableArray: ObservableFloatArray?, sizeChanged: Boolean, from: Int, to: Int ->
      children.clear()
      children.addAll(createPolyLines(mesh))
    }
  }

  private fun createPolyLines(mesh: TriangleMesh): List<PolyLine3D> {
    val meshPoints = mesh.points
    val coordinates = meshPoints.toArray(FloatArray(0))
    val pointsCount = coordinates.size / 3

    //The amount of slices
    val slicesCount = pointsCount / divisions
    val polyLines: MutableList<PolyLine3D> = ArrayList()
    run {
      //Create the "slices" for each division
      for (sliceIndex in 0 until slicesCount) {
        val baseIndex = sliceIndex * 3 * divisions
        val points: MutableList<Point3D> = ArrayList()
        for (divisionIndex in 0 until divisions) {
          val x = coordinates[baseIndex + divisionIndex * 3]
          val y = coordinates[baseIndex + divisionIndex * 3 + 1]
          val z = coordinates[baseIndex + divisionIndex * 3 + 2]
          points.add(Point3D(x, y, z))
        }

        //close the slice
        points.add(points[0])

        // apply offset
        points.forEach(Consumer { point3D: Point3D -> this.applyOffset(point3D) })
        polyLines.add(PolyLine3D(points, lineWidth, this.lineColor, lineType))
      }
    }

    //Create the lines along the x coordinates
    run {
      for (divisionIndex in 0 until divisions) {
        val points: MutableList<Point3D> = ArrayList()
        for (sliceIndex in 0 until slicesCount) {
          val baseIndex = (divisionIndex + sliceIndex * divisions) * 3
          val x = coordinates[baseIndex]
          val y = coordinates[baseIndex + 1]
          val z = coordinates[baseIndex + 2]
          points.add(Point3D(x, y, z))
        }

        // apply offset
        points.forEach(Consumer { point3D: Point3D -> this.applyOffset(point3D) })
        polyLines.add(PolyLine3D(points, lineWidth, this.lineColor, lineType))
      }
    }
    return polyLines
  }

  private fun applyOffset(point3D: Point3D) {
    point3D.y = applyOffset(point3D.y)
    point3D.z = applyOffset(point3D.z)
  }

  private fun applyOffset(value: Float): Float {
    // TODO
    // This does not work for folds. A positive value alone
    // is no indicator whether a positive offset or a negative
    // offset must be used.
    if (offset.toDouble() == 0.0) {
      return value
    }
    return if (value < 0) {
      value - offset
    } else value + offset
  }
}
