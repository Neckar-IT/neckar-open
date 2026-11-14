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

import it.neckar.open.javafx.threed.MeshGenerator.flatten
import it.neckar.open.javafx.threed.MeshGenerator.generateMeshFrom3DPoints
import it.neckar.open.javafx.threed.MeshGenerator.generatePoints
import it.neckar.open.javafx.threed.TransformUtil.bindAll
import it.neckar.open.javafx.threed.TransformUtil.installInverseRotate
import com.google.common.collect.ImmutableList
import javafx.application.Application
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Point3D
import javafx.scene.AmbientLight
import javafx.scene.Group
import javafx.scene.LightBase
import javafx.scene.Node
import javafx.scene.PerspectiveCamera
import javafx.scene.PointLight
import javafx.scene.Scene
import javafx.scene.SceneAntialiasing
import javafx.scene.SubScene
import javafx.scene.control.Button
import javafx.scene.control.ButtonBase
import javafx.scene.control.Label
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.Box
import javafx.scene.shape.CullFace
import javafx.scene.shape.MeshView
import javafx.scene.shape.Shape3D
import javafx.scene.shape.TriangleMesh
import javafx.scene.transform.Rotate
import javafx.scene.transform.Translate
import javafx.stage.Stage
import org.fxyz3d.shapes.composites.PolyLine3D
import org.fxyz3d.shapes.primitives.FrustumMesh
import java.util.Arrays
import javax.annotation.Nonnull

/**
 *
 **/
class SoundClubDemo : Application() {
  override fun start(primaryStage: Stage) {
    val resetViewButton = Button("Reset view")
    val subScene = createSceneWithParallelCamera(resetViewButton)
    subScene.fill = Color.WHITE
    val vBox = VBox(0.0)
    vBox.background = Background(BackgroundFill(Color.ORANGE, CornerRadii.EMPTY, Insets.EMPTY))
    vBox.children.add(subScene)
    vBox.children.add(resetViewButton)
    val scene = Scene(vBox)
    primaryStage.title = SoundClubDemo::class.java.simpleName
    primaryStage.scene = scene
    primaryStage.show()
  }

  companion object {
    private const val GridCellWidth = 40
    private const val GridCellDepth = 40
    private const val GridCellCountX = 10
    private const val GridCellCountZ = 20
    private val ORANGE = Color.rgb(220, 120, 40)
    private val BLUE = Color.rgb(8, 80, 121, 0.35)
    private val SOUND_CLUB_LINE_COLOR = Color.rgb(8, 80, 121)
    private const val GridYOffset = 100.0f
    private const val SoundClubDivisions = 24
    private const val FontHeight = 8

    // FIXME real values from sensor
    private val SOUND_CLUB_X_COORDINATES = doubleArrayOf(
      300.0,
      350.0,
      400.0,
      450.0,
      500.0,
      550.0,
      600.0,
      650.0,
      700.0,
      750.0,
      800.0,
      850.0,
      900.0,
      950.0,
      1000.0,
      1050.0,
      1100.0,
      1150.0,
      1200.0,
      1250.0,
      1300.0,
      1350.0,
      1400.0,
      1450.0,
      1500.0,
      1550.0,
      1600.0,
      1650.0,
      1700.0,
      1750.0,
      1800.0,
      1850.0,
      1900.0,
      1950.0,
      2000.0,
      2050.0,
      2100.0,
      2150.0,
      2200.0,
      2250.0,
      2300.0,
      2350.0,
      2400.0,
      2450.0,
      2500.0,
      2550.0,
      2600.0,
      2650.0,
      2700.0,
      2750.0,
      2800.0,
      2850.0,
      2900.0,
      2950.0,
      3000.0,
      3050.0,
      3100.0,
      3150.0,
      3200.0,
      3250.0,
      3300.0,
      3350.0,
      3400.0,
      3450.0,
      3500.0,
      3550.0,
      3600.0,
      3650.0,
      3700.0,
      3750.0,
      3800.0,
      3850.0,
      3900.0,
      3950.0,
      4000.0,
      4000.0
    )
    private val SOUND_CLUB_Y_COORDINATES = doubleArrayOf(
      25.71999740600586,
      11.055261611938477,
      5.3968000411987305,
      5.48799991607666,
      5.455999851226807,
      5.446400165557861,
      5.408319473266602,
      5.276480197906494,
      5.193919658660889,
      5.157759666442871,
      5.12384033203125,
      4.964159965515137,
      4.799999713897705,
      4.751999855041504,
      4.727999687194824,
      4.599999904632568,
      4.5167999267578125,
      4.53439998626709,
      4.524799823760986,
      4.415999889373779,
      4.319999694824219,
      4.241919994354248,
      4.178879737854004,
      4.131519794464111,
      4.034560203552246,
      5.801918029785156,
      7.469691753387451,
      9.222116470336914,
      11.006268501281738,
      12.539586067199707,
      13.6317777633667,
      15.074519157409668,
      16.496891021728516,
      17.311979293823242,
      18.337085723876953,
      19.782438278198242,
      21.06688690185547,
      21.330978393554688,
      21.799837112426758,
      21.591854095458984,
      21.617267608642578,
      20.956510543823242,
      20.122961044311523,
      19.347856521606445,
      19.228593826293945,
      19.036678314208984,
      18.859216690063477,
      17.475399017333984,
      15.974605560302734,
      15.373852729797363,
      13.46033000946045,
      13.03570556640625,
      12.851369857788086,
      12.047504425048828,
      12.239413261413574,
      11.779294967651367,
      11.20086669921875,
      11.363694190979004,
      11.54455852508545,
      11.192337989807129,
      10.339958190917969,
      10.367323875427246,
      10.070924758911133,
      8.445317268371582,
      8.009987831115723,
      8.003432273864746,
      7.585000514984131,
      8.374799728393555,
      8.813088417053223,
      8.810688018798828,
      8.316871643066406,
      9.117534637451172,
      8.632166862487793,
      7.412232875823975,
      8.976746559143066,
      0.0
    )

    @JvmStatic
    fun main(args: Array<String>) {
      launch(*args)
    }

    @Nonnull
    private fun createSceneWithParallelCamera(@Nonnull resetViewButton: ButtonBase): SubScene {
      val root = Group()
      val gridXZPane = createGridXZPane(GridYOffset)
      root.children.addAll(gridXZPane)
      val gridXZLabels = createGridXZLabels(GridYOffset)
      root.children.addAll(gridXZLabels)
      installInverseRotate(root, gridXZLabels)
      val sensorBox = createSensorBox()
      val detectionBox = createDetectionBox()
      val detectionRod = createDetectionRod()
      val soundClub = createSoundClub()
      val innerSoundClub = createInnerSoundClub()
      val soundClubLines = createSoundClubLines(soundClub.mesh as TriangleMesh)
      bindAll(soundClub, soundClubLines)
      val soundClubShadow = createSoundClubShadow(GridYOffset)
      val innerSoundClubShadow = createInnerSoundClubShadow(GridYOffset)
      root.children.addAll(innerSoundClubShadow, soundClubShadow, sensorBox, detectionRod, detectionBox, innerSoundClub, soundClub, soundClubLines)
      val lights = createLights(GridYOffset.toDouble())
      root.children.addAll(lights)
      for (light in lights) {
        light.scope.setAll(sensorBox, detectionRod, soundClub, innerSoundClub, detectionBox, innerSoundClubShadow, soundClubShadow)
      }

      // correct!
      root.transforms.add(Rotate(-45.0, Rotate.Y_AXIS))
      root.transforms.add(Rotate(25.0, Point3D(1.0, 0.0, -1.0)))
      root.transforms.add(Translate(350.0, 300.0))
      val subScene = SubScene(root, 1024.0, 768.0, true, SceneAntialiasing.BALANCED)
      val camera = PerspectiveCamera()
      camera.fieldOfView = 1.0
      camera.translateX = -512.0
      camera.translateY = -384.0
      subScene.camera = camera
      val support = SubSceneInputEventSupport(subScene)
      support.install(root)
      resetViewButton.onAction = EventHandler { event: ActionEvent? -> support.resetAll() }
      return subScene
    }

    @Nonnull
    private fun createSensorBox(): Shape3D {
      val sensorBox = Box(24.0, 24.0, 10.0)
      sensorBox.material = PhongMaterial(Color.rgb(0, 180, 255))
      sensorBox.translateZ = -sensorBox.depth * 0.5
      return sensorBox
    }

    @Nonnull
    private fun createDetectionBox(): Shape3D {
      val detectionBox = Box(80.0, 150.0, 10.0)
      detectionBox.material = PhongMaterial(Color.rgb(0, 100, 180))
      detectionBox.translateX = detectionBox.width * 0.5
      detectionBox.translateY = -detectionBox.height * 0.5 + GridYOffset
      detectionBox.translateZ = 0.5 * GridCellCountZ * GridCellDepth
      detectionBox.rotationAxis = Rotate.Y_AXIS
      detectionBox.rotate = 15.0
      return detectionBox
    }

    @Nonnull
    private fun createDetectionRod(): Shape3D {
      val frustumMesh = FrustumMesh(5.0, 5.0, 150.0)
      frustumMesh.material = PhongMaterial(ORANGE)
      frustumMesh.translateX = -0.5 * GridCellWidth
      frustumMesh.translateY = -frustumMesh.height * 0.5 + GridYOffset
      frustumMesh.translateZ = 0.35 * GridCellCountZ * GridCellDepth
      return frustumMesh
    }

    @Nonnull
    private fun createSoundClub(): MeshView {
      val xCoordinates = Arrays.copyOf(SOUND_CLUB_X_COORDINATES, SOUND_CLUB_X_COORDINATES.size)
      val yCoordinates = Arrays.copyOf(SOUND_CLUB_Y_COORDINATES, SOUND_CLUB_Y_COORDINATES.size)

      // FIXME: use values from sensor
      for (i in xCoordinates.indices) {
        xCoordinates[i] *= 0.15
        yCoordinates[i] *= 2.2
      }
      val points = generatePoints(xCoordinates, yCoordinates, SoundClubDivisions)
      val meshView = MeshView(generateMeshFrom3DPoints(points, SoundClubDivisions, xCoordinates.size))
      meshView.material = PhongMaterial(BLUE)
      meshView.cullFace = CullFace.NONE
      meshView.transforms.add(Rotate(-90.0, Rotate.Y_AXIS))
      return meshView
    }

    @Nonnull
    private fun createInnerSoundClub(): Shape3D {
      val xCoordinates = Arrays.copyOf(SOUND_CLUB_X_COORDINATES, SOUND_CLUB_X_COORDINATES.size)
      val yCoordinates = Arrays.copyOf(SOUND_CLUB_Y_COORDINATES, SOUND_CLUB_Y_COORDINATES.size)

      // FIXME: use values from sensor
      for (i in xCoordinates.indices) {
        xCoordinates[i] *= 0.15
        yCoordinates[i] *= 1.6
      }
      val points = generatePoints(xCoordinates, yCoordinates, SoundClubDivisions)
      val meshView = MeshView(generateMeshFrom3DPoints(points, SoundClubDivisions, xCoordinates.size))
      meshView.material = PhongMaterial(ORANGE)
      meshView.cullFace = CullFace.NONE
      meshView.transforms.add(Rotate(-90.0, Rotate.Y_AXIS))
      return meshView
    }

    @Nonnull
    private fun createSoundClubLines(@Nonnull mesh: TriangleMesh): Node {
      return MeshLines(mesh, SOUND_CLUB_LINE_COLOR, PolyLine3D.LineType.TRIANGLE, 2.0f, 1.01f, SoundClubDivisions)
    }

    @Nonnull
    private fun createSoundClubShadow(yPos: Float): Node {
      val xCoordinates = Arrays.copyOf(SOUND_CLUB_X_COORDINATES, SOUND_CLUB_X_COORDINATES.size)
      val yCoordinates = Arrays.copyOf(SOUND_CLUB_Y_COORDINATES, SOUND_CLUB_Y_COORDINATES.size)
      val numberOfCoordinates = xCoordinates.size
      for (i in 0 until numberOfCoordinates) {
        xCoordinates[i] *= 0.15
        yCoordinates[i] *= 2.2
      }
      return createShadow(yPos - 0.1f, xCoordinates, yCoordinates, BLUE)
    }

    @Nonnull
    private fun createInnerSoundClubShadow(yPos: Float): Node {
      val xCoordinates = Arrays.copyOf(SOUND_CLUB_X_COORDINATES, SOUND_CLUB_X_COORDINATES.size)
      val yCoordinates = Arrays.copyOf(SOUND_CLUB_Y_COORDINATES, SOUND_CLUB_Y_COORDINATES.size)
      val numberOfCoordinates = xCoordinates.size
      for (i in 0 until numberOfCoordinates) {
        xCoordinates[i] *= 0.15
        yCoordinates[i] *= 1.6
      }
      return createShadow(yPos - 0.15f, xCoordinates, yCoordinates, Color.rgb(220, 120, 40, 0.65))
    }

    @Nonnull
    private fun createShadow(
      yPos: Float,
      @Nonnull xCoordinates: DoubleArray,
      @Nonnull yCoordinates: DoubleArray,
      @Nonnull shadowColor: Color,
    ): Node {
      val numberOfCoordinates = xCoordinates.size
      val points3D: MutableList<Point3D> = ArrayList()
      for (i in 0 until numberOfCoordinates) {
        val x2D = xCoordinates[i]
        val y2D = yCoordinates[i]
        points3D.add(Point3D(x2D, y2D, 0.0))
        points3D.add(Point3D(x2D, -y2D, 0.0))
      }
      val points = flatten(points3D)

      // FIXME why does this not work?
      val textCoords = floatArrayOf(
        0.5f, 0.0f,  // t0
        0.0f, 1.0f,  // t1
        1.0f, 1.0f // t2
      )
      val faces: MutableCollection<Int> = ArrayList()
      var i = 0
      var k = 0
      while (i < numberOfCoordinates - 3) {
        addTriangle(faces, k + 2, k, k + 1)
        addTriangle(faces, k + 2, k + 1, k + 3)
        i++
        k += 2
      }
      val triangleMesh = TriangleMesh()
      triangleMesh.points.addAll(*points)
      triangleMesh.texCoords.addAll(*textCoords)
      triangleMesh.faces.addAll(*faces.stream().mapToInt { i: Int? -> i!! }.toArray())
      val meshView = MeshView(triangleMesh)
      meshView.cullFace = CullFace.BACK
      meshView.material = PhongMaterial(shadowColor)
      meshView.transforms.add(Rotate(-90.0, Rotate.Y_AXIS))
      meshView.transforms.add(Rotate(-90.0, Rotate.X_AXIS))
      meshView.translateY = yPos.toDouble()
      return meshView
    }

    /**
     * Vertices are supposed to be in counter-clockwise order.
     */
    private fun addTriangle(
      @Nonnull faces: MutableCollection<in Int>,
      v0: Int,
      v1: Int,
      v2: Int,
    ) {
      // front face, vertices counter-clockwise, start vertex does not matter
      faces.add(v0)
      faces.add(0)
      faces.add(v1)
      faces.add(1)
      faces.add(v2)
      faces.add(2)

      // back face, vertices clockwise, start vertex does not matter
      faces.add(v0)
      faces.add(0)
      faces.add(v2)
      faces.add(2)
      faces.add(v1)
      faces.add(1)
    }

    @Nonnull
    private fun createGridXZPane(yPos: Float): Collection<Node> {
      val thickLine = 3.0f
      val thinLine = 1.0f
      val gridNodes: MutableList<Node> = ArrayList()
      val halfCellCountX = GridCellCountX / 2
      for (x in -halfCellCountX until halfCellCountX + 1) {
        val startPoint = org.fxyz3d.geometry.Point3D((x * GridCellWidth).toFloat(), yPos, 0f)
        val endPoint = org.fxyz3d.geometry.Point3D((x * GridCellWidth).toFloat(), yPos, (GridCellCountZ * GridCellDepth).toFloat())
        gridNodes.add(PolyLine3D(ImmutableList.of(startPoint, endPoint), if (x % 5 == 0) thickLine else thinLine, Color.GRAY, PolyLine3D.LineType.TRIANGLE))
      }
      for (z in 0 until GridCellCountZ + 1) {
        val startPoint = org.fxyz3d.geometry.Point3D((-halfCellCountX * GridCellWidth).toFloat(), yPos, (z * GridCellDepth).toFloat())
        val endPoint = org.fxyz3d.geometry.Point3D((halfCellCountX * GridCellWidth).toFloat(), yPos, (z * GridCellDepth).toFloat())
        gridNodes.add(PolyLine3D(ImmutableList.of(startPoint, endPoint), if (z % 5 == 0) thickLine else thinLine, Color.GRAY, PolyLine3D.LineType.TRIANGLE))
      }
      return gridNodes
    }

    @Nonnull
    private fun createGridXZLabels(yPos: Float): Collection<Node> {
      val labels: MutableCollection<Node> = ArrayList()
      val halfCellCountX = GridCellCountX / 2
      val maxZValue = 6400 // FIXME correct value from sensor
      val zCellsPerLabel = 5.0 / 2.0 // a label every 2.5 cells
      val totalDepth = (GridCellCountZ * GridCellDepth).toDouble()
      val zDistance = GridCellDepth * zCellsPerLabel
      val zValueOffset = (maxZValue / GridCellCountZ * zCellsPerLabel).toInt()
      var zValue = 0
      var zPos = 0.0
      while (zPos <= totalDepth + 1) { // + 1 due to rounding errors
        val label: Node = Label(Integer.toString(zValue))
        labels.add(label)
        label.translateX = (-halfCellCountX * GridCellWidth - 50).toDouble()
        label.translateY = (yPos - FontHeight).toDouble()
        label.translateZ = zPos
        zPos += zDistance
        zValue += zValueOffset
      }
      val maxXValue = 2400 // FIXME correct value from sensor
      val xCellsPerLabel = 5.0 / 3.0 // a label every 1.666 cells
      val halfWidth = (halfCellCountX * GridCellWidth).toDouble()
      val xDistance = GridCellWidth * xCellsPerLabel
      val xValueOffset = (maxXValue / GridCellCountX * xCellsPerLabel).toInt() * 2
      var xValue = -maxXValue
      var xPos = -halfWidth
      while (xPos <= halfWidth + 1) { // + 1 due to rounding errors
        val label: Node = Label(Integer.toString(Math.abs(xValue)))
        labels.add(label)
        label.translateX = xPos
        label.translateY = (yPos - FontHeight).toDouble()
        label.translateZ = totalDepth + 50
        xPos += xDistance
        xValue += xValueOffset
      }
      return labels
    }

    @Nonnull
    private fun createLights(yPos: Double): Collection<LightBase> {
      val light1 = PointLight(Color.color(0.5, 0.5, 0.5))
      light1.translateX = -3000.0
      light1.translateY = -500.0
      light1.translateZ = GridCellCountZ * GridCellDepth * 0.5
      val light2 = PointLight(Color.color(0.6, 0.6, 0.6))
      light2.translateX = 0.0
      light2.translateY = -600.0
      light2.translateZ = -500.0
      return ImmutableList.of(light1, light2, AmbientLight(Color.color(0.4, 0.4, 0.4)))
    }
  }
}
