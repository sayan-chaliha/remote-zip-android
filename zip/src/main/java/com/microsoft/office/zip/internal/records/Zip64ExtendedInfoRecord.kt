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

package com.microsoft.office.zip.internal.records

import com.microsoft.office.zip.internal.ZipConstants
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A ZIP extra field containing extended info for ZIP64 files.
 */
internal data class Zip64ExtendedInfoRecord(
    val uncompressedFileSize: ULong,
    val compressedFileSize: ULong,
    val localHeaderOffset: ULong
) : ZipExtraField() {
    override val headerId: Short = ZipConstants.Headers.ZIP64_EXTENDED_INFORMATION
    override val size: Short = ((Long.SIZE_BYTES * 3) + Int.SIZE_BYTES).toShort()

    companion object {

        /**
         * Parses a ZIP64 Extended Information record from a [ByteBuffer].
         *
         * @param buffer Buffer containing the extended info record
         * @return [Zip64ExtendedInfoRecord]
         *
         * @throws [RecordException]
         */
        @Throws(RecordException::class)
        fun from(buffer: ByteBuffer): Zip64ExtendedInfoRecord {
            try {
                val uncompressedFileSize = buffer.long.toULong()
                val compressedFileSize = buffer.long.toULong()
                val lfhOffset = buffer.long.toULong()
                val startDisk = buffer.int

                require(startDisk == 0) {
                    "Bad ZIP64 Ext Info file start disk: $startDisk (expected: 0)"
                }

                return Zip64ExtendedInfoRecord(
                    uncompressedFileSize = uncompressedFileSize,
                    compressedFileSize = compressedFileSize,
                    localHeaderOffset = lfhOffset
                )
            } catch (exception: IllegalArgumentException) {
                throw RecordException(exception.message!!)
            } catch (exception: BufferUnderflowException) {
                throw RecordException("Incomplete record.", exception)
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
            .putLong(uncompressedFileSize.toLong())
            .putLong(compressedFileSize.toLong())
            .putLong(localHeaderOffset.toLong())
            .putInt(0)
            .rewind() as ByteBuffer

        val bytes = ByteArray(byteBuffer.remaining())
        byteBuffer.get(bytes)
        return bytes
    }
}
