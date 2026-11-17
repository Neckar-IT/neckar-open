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
package it.neckar.open.image

import it.neckar.open.resources.getResourceSafe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.io.IOException
import javax.imageio.ImageIO

/**
 *
 */
class ImageConverterTest {

  private lateinit var original: BufferedImage

  @BeforeEach
  fun setUp() {
    original = ImageIO.read(javaClass.getResourceSafe("/generated-test-photo.jpg"))
  }

  @Test
  @Throws(InterruptedException::class)
  fun testResize() {
    val resized = ImageConverter.resize(original, Dimension(72, 88))
    assertEquals(72, resized.width.toLong())
    assertEquals(88, resized.height.toLong())

    //    JFrame frame = new JFrame();
    //    frame.setContentPane( new JPanel() {
    //      @Override
    //      protected void paintComponent( Graphics g ) {
    //        super.paintComponent( g );
    //        g.drawImage( resized, 0, 0, this );
    //      }
    //    } );
    //
    //    frame.setSize( 800, 600 );
    //    frame.setVisible( true );
    //    Thread.sleep( 4000 );
  }

  @Test
  fun `a transparent pixel shows the background`() {
    val transparent = BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB)

    assertEquals(Color.WHITE.rgb, ImageConverter.resize(transparent, Dimension(5, 5), background = Color.WHITE).getRGB(2, 2))
    assertEquals(Color.BLACK.rgb, ImageConverter.resize(transparent, Dimension(5, 5)).getRGB(2, 2))
  }

  @Test
  @Throws(IOException::class)
  fun testSizes() {
    assertEquals(300, original.width.toLong())
    assertEquals(367, original.height.toLong())
    val newWidth = original.width * 72 / 300
    assertEquals(72, newWidth.toLong())
    val newHeight = original.height * 72 / 300
    assertEquals(88, newHeight.toLong())
    val dim = ImageConverter.calculateNewDimensions(original, DPI.DPI_300, DPI.DPI_072)
    assertEquals(72, dim.width.toLong())
    assertEquals(88, dim.height.toLong())
  }
}
