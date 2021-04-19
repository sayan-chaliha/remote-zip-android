/*
 * Copyright (c) 2021 Microsoft Corporation.
 * All Rights Reserved
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.zip.internal.records

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Represents a generic ZIP extra field.
 */
internal class ZipGenericExtraField(
    override val headerId: Short,
    val bytes: ByteArray
) : ZipExtraField() {
    override val size: Short = bytes.size.toShort()

    companion object {

        /**
         * Parses a generic (uninterpreted) ZIP extra field.
         *
         * @param headerId Header ID of the extra field
         * @param buffer [ByteBuffer] containing the extra field
         * @return [ZipGenericExtraField]
         *
         * @throws [RecordException]
         */
        @Throws(RecordException::class)
        fun from(headerId: Short, buffer: ByteBuffer): ZipGenericExtraField {
            try {
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)

                return ZipGenericExtraField(
                    headerId = headerId,
                    bytes = bytes
                )
            } catch (exception: Exception) {
                throw RecordException("Unknown exception.", exception)
            }
        }
    }

    override fun serialize(): ByteArray {
        val byteBuffer = ByteBuffer.allocate((Short.SIZE_BYTES * 2) + size.toInt())
            .order(ByteOrder.LITTLE_ENDIAN)
            .putShort(headerId)
            .putShort(size)
            .put(bytes)
            .rewind() as ByteBuffer

        val bytes = ByteArray(byteBuffer.remaining())
        byteBuffer.get(bytes)
        return bytes
    }
}
