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

import com.microsoft.zip.internal.ZipConstants
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Represents an extra field supported by the ZIP format.
 * As of 1.0, only two types of extra fields are supported --
 *  1. Extended timestamp info
 *  2. ZIP64 extended info
 *
 * A ZIP extra field contains a 2-byte header ID, a 2-byte size field, followed by the payload
 * bytes interpreted based on the header ID.
 */
internal abstract class ZipExtraField {

    /**
     * Extra field header ID.
     */
    abstract val headerId: Short

    /**
     * Extra field payload size.
     */
    abstract val size: Short

    /**
     * Serializes the extra field into a [ByteArray].
     *
     * @return Serialized bytes containing the extra field
     */
    abstract fun serialize(): ByteArray

    companion object {

        /**
         * Parses a single [ZipExtraField] from a [ByteBuffer].
         *
         * @param byteBuffer Buffer containing the extra field
         * @return [ZipExtraField]
         *
         * @throws [RecordException] if parsing fails
         */
        @Throws(RecordException::class)
        fun from(byteBuffer: ByteBuffer): ZipExtraField {
            // Ensure little endian
            val buffer = byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
            val headerId = buffer.short
            val size = buffer.short.toInt()
            val bytes = ByteArray(size)
            buffer.get(bytes)

            return when (headerId) {
                ZipConstants.Headers.EXTENDED_TIMESTAMP_INFORMATION -> {
                    ExtendedTimestampInfoRecord
                        .from(ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN))
                }

                ZipConstants.Headers.ZIP64_EXTENDED_INFORMATION -> {
                    Zip64ExtendedInfoRecord
                        .from(ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN))
                }

                else -> {
                    ZipGenericExtraField
                        .from(headerId, ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN))
                }
            }
        }
    }
}
