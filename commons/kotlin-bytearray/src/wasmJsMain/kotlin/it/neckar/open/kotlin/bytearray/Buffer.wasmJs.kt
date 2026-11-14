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
@file:Suppress("FunctionName") //actual functions must match the expect names (_sliceInt8Buffer, MemBufferAlloc, ...)

package it.neckar.open.kotlin.bytearray

/**
 * Pure-Kotlin buffer implementation for Kotlin/Wasm, backed by a [ByteArray] with
 * little-endian element access - matching the JS TypedArray and JVM native-order semantics
 * of the other targets, without paying JS-interop cost per element access.
 */
actual class MemBuffer(val data: ByteArray)

actual fun MemBufferAlloc(size: Int): MemBuffer = MemBuffer(ByteArray(size))
actual fun MemBufferAllocNoDirect(size: Int): MemBuffer = MemBuffer(ByteArray(size))
actual fun MemBufferWrap(array: ByteArray): MemBuffer = MemBuffer(array)
actual val MemBuffer.size: Int get() = data.size

actual fun MemBuffer._sliceInt8Buffer(offset: Int, size: Int): Int8Buffer = Int8Buffer(this, offset, size)
actual fun MemBuffer._sliceInt16Buffer(offset: Int, size: Int): Int16Buffer = Int16Buffer(this, offset, size)
actual fun MemBuffer._sliceInt32Buffer(offset: Int, size: Int): Int32Buffer = Int32Buffer(this, offset, size)
actual fun MemBuffer._sliceFloat32Buffer(offset: Int, size: Int): Float32Buffer = Float32Buffer(this, offset, size)
actual fun MemBuffer._sliceFloat64Buffer(offset: Int, size: Int): Float64Buffer = Float64Buffer(this, offset, size)

private fun ByteArray.getShortLE(byteIndex: Int): Short {
  return ((this[byteIndex].toInt() and 0xFF) or (this[byteIndex + 1].toInt() shl 8)).toShort()
}

private fun ByteArray.setShortLE(byteIndex: Int, value: Short) {
  this[byteIndex] = value.toByte()
  this[byteIndex + 1] = (value.toInt() shr 8).toByte()
}

private fun ByteArray.getIntLE(byteIndex: Int): Int {
  return (this[byteIndex].toInt() and 0xFF) or
    ((this[byteIndex + 1].toInt() and 0xFF) shl 8) or
    ((this[byteIndex + 2].toInt() and 0xFF) shl 16) or
    (this[byteIndex + 3].toInt() shl 24)
}

private fun ByteArray.setIntLE(byteIndex: Int, value: Int) {
  this[byteIndex] = value.toByte()
  this[byteIndex + 1] = (value shr 8).toByte()
  this[byteIndex + 2] = (value shr 16).toByte()
  this[byteIndex + 3] = (value shr 24).toByte()
}

private fun ByteArray.getLongLE(byteIndex: Int): Long {
  return (getIntLE(byteIndex).toLong() and 0xFFFFFFFFL) or (getIntLE(byteIndex + 4).toLong() shl 32)
}

private fun ByteArray.setLongLE(byteIndex: Int, value: Long) {
  setIntLE(byteIndex, value.toInt())
  setIntLE(byteIndex + 4, (value shr 32).toInt())
}

actual typealias DataBuffer = MemBuffer

actual val DataBuffer.mem: MemBuffer get() = this
actual fun MemBuffer.getData(): DataBuffer = this
actual fun DataBuffer.getByte(index: Int): Byte = data[index]
actual fun DataBuffer.setByte(index: Int, value: Byte) {
  data[index] = value
}

actual fun DataBuffer.getShort(index: Int): Short = data.getShortLE(index)
actual fun DataBuffer.setShort(index: Int, value: Short) {
  data.setShortLE(index, value)
}

actual fun DataBuffer.getInt(index: Int): Int = data.getIntLE(index)
actual fun DataBuffer.setInt(index: Int, value: Int) {
  data.setIntLE(index, value)
}

actual fun DataBuffer.getFloat(index: Int): Float = Float.fromBits(data.getIntLE(index))
actual fun DataBuffer.setFloat(index: Int, value: Float) {
  data.setIntLE(index, value.toRawBits())
}

actual fun DataBuffer.getDouble(index: Int): Double = Double.fromBits(data.getLongLE(index))
actual fun DataBuffer.setDouble(index: Int, value: Double) {
  data.setLongLE(index, value.toRawBits())
}

actual class Int8Buffer(val mbuffer: MemBuffer, val elementOffset: Int, val elementCount: Int)

actual val Int8Buffer.mem: MemBuffer get() = mbuffer
actual val Int8Buffer.offset: Int get() = elementOffset
actual val Int8Buffer.size: Int get() = elementCount
actual operator fun Int8Buffer.get(index: Int): Byte = mbuffer.data[elementOffset + index]
actual operator fun Int8Buffer.set(index: Int, value: Byte) {
  mbuffer.data[elementOffset + index] = value
}

actual class Int16Buffer(val mbuffer: MemBuffer, val elementOffset: Int, val elementCount: Int)

actual val Int16Buffer.mem: MemBuffer get() = mbuffer
actual val Int16Buffer.offset: Int get() = elementOffset
actual val Int16Buffer.size: Int get() = elementCount
actual operator fun Int16Buffer.get(index: Int): Short = mbuffer.data.getShortLE((elementOffset + index) * 2)
actual operator fun Int16Buffer.set(index: Int, value: Short) {
  mbuffer.data.setShortLE((elementOffset + index) * 2, value)
}

actual class Int32Buffer(val mbuffer: MemBuffer, val elementOffset: Int, val elementCount: Int)

actual val Int32Buffer.mem: MemBuffer get() = mbuffer
actual val Int32Buffer.offset: Int get() = elementOffset
actual val Int32Buffer.size: Int get() = elementCount
actual operator fun Int32Buffer.get(index: Int): Int = mbuffer.data.getIntLE((elementOffset + index) * 4)
actual operator fun Int32Buffer.set(index: Int, value: Int) {
  mbuffer.data.setIntLE((elementOffset + index) * 4, value)
}

actual class Float32Buffer(val mbuffer: MemBuffer, val elementOffset: Int, val elementCount: Int)

actual val Float32Buffer.mem: MemBuffer get() = mbuffer
actual val Float32Buffer.offset: Int get() = elementOffset
actual val Float32Buffer.size: Int get() = elementCount
actual operator fun Float32Buffer.get(index: Int): Float = Float.fromBits(mbuffer.data.getIntLE((elementOffset + index) * 4))
actual operator fun Float32Buffer.set(index: Int, value: Float) {
  mbuffer.data.setIntLE((elementOffset + index) * 4, value.toRawBits())
}

actual class Float64Buffer(val mbuffer: MemBuffer, val elementOffset: Int, val elementCount: Int)

actual val Float64Buffer.mem: MemBuffer get() = mbuffer
actual val Float64Buffer.offset: Int get() = elementOffset
actual val Float64Buffer.size: Int get() = elementCount
actual operator fun Float64Buffer.get(index: Int): Double = Double.fromBits(mbuffer.data.getLongLE((elementOffset + index) * 8))
actual operator fun Float64Buffer.set(index: Int, value: Double) {
  mbuffer.data.setLongLE((elementOffset + index) * 8, value.toRawBits())
}

actual fun arraycopy(src: MemBuffer, srcPos: Int, dst: MemBuffer, dstPos: Int, size: Int) {
  src.data.copyInto(dst.data, dstPos, srcPos, srcPos + size)
}

actual fun arraycopy(src: ByteArray, srcPos: Int, dst: MemBuffer, dstPos: Int, size: Int) {
  src.copyInto(dst.data, dstPos, srcPos, srcPos + size)
}

actual fun arraycopy(src: MemBuffer, srcPos: Int, dst: ByteArray, dstPos: Int, size: Int) {
  src.data.copyInto(dst, dstPos, srcPos, srcPos + size)
}

actual fun arraycopy(src: ShortArray, srcPos: Int, dst: MemBuffer, dstPos: Int, size: Int) {
  for (n in 0 until size) {
    dst.data.setShortLE((dstPos + n) * 2, src[srcPos + n])
  }
}

actual fun arraycopy(src: MemBuffer, srcPos: Int, dst: ShortArray, dstPos: Int, size: Int) {
  for (n in 0 until size) {
    dst[dstPos + n] = src.data.getShortLE((srcPos + n) * 2)
  }
}

actual fun arraycopy(src: IntArray, srcPos: Int, dst: MemBuffer, dstPos: Int, size: Int) {
  for (n in 0 until size) {
    dst.data.setIntLE((dstPos + n) * 4, src[srcPos + n])
  }
}

actual fun arraycopy(src: MemBuffer, srcPos: Int, dst: IntArray, dstPos: Int, size: Int) {
  for (n in 0 until size) {
    dst[dstPos + n] = src.data.getIntLE((srcPos + n) * 4)
  }
}

actual fun arraycopy(src: FloatArray, srcPos: Int, dst: MemBuffer, dstPos: Int, size: Int) {
  for (n in 0 until size) {
    dst.data.setIntLE((dstPos + n) * 4, src[srcPos + n].toRawBits())
  }
}

actual fun arraycopy(src: MemBuffer, srcPos: Int, dst: FloatArray, dstPos: Int, size: Int) {
  for (n in 0 until size) {
    dst[dstPos + n] = Float.fromBits(src.data.getIntLE((srcPos + n) * 4))
  }
}

actual fun arraycopy(src: DoubleArray, srcPos: Int, dst: MemBuffer, dstPos: Int, size: Int) {
  for (n in 0 until size) {
    dst.data.setLongLE((dstPos + n) * 8, src[srcPos + n].toRawBits())
  }
}

actual fun arraycopy(src: MemBuffer, srcPos: Int, dst: DoubleArray, dstPos: Int, size: Int) {
  for (n in 0 until size) {
    dst[dstPos + n] = Double.fromBits(src.data.getLongLE((srcPos + n) * 8))
  }
}
