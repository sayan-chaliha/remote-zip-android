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
 * Represents the ZIP64 End of Central Directory record.
 */
internal data class Zip64EndOfCentralDirectoryRecord(
    override val centralDirectoryEntries: ULong,
    override val centralDirectorySize: ULong,
    override val centralDirectoryOffset: ULong,
    override val isZip64: Boolean
) : EndOfCentralDirectoryRecord {
    companion object {

        /**
         * Parses a ZIP64 End of Central Directory record from a [ByteBuffer].
         *
         * NOTE: EOCD comments are not supported.
         *
         * @param byteBuffer Buffer containing the EOCD record
         * @return [Zip64EndOfCentralDirectoryRecord] from binary data
         *
         * @throws RecordException if an unexpected header field value is encountered
         */
        @Throws(RecordException::class)
        fun from(byteBuffer: ByteBuffer): EndOfCentralDirectoryRecord {
            // Ensure byteBuffer is little endian
            val buffer = byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

            try {
                // Validate signature
                val signature = buffer.int
                require(signature == ZipConstants.Signatures.ZIP64_END_OF_CENTRAL_DIRECTORY) {
                    "Bad ZIP64 EOCD signature: $signature (expected " +
                        "${ZipConstants.Signatures.ZIP64_END_OF_CENTRAL_DIRECTORY})"
                }

                val size = buffer.long.toULong()
                require(
                    size == (ZipConstants.Sizes.ZIP64_END_OF_CENTRAL_DIRECTORY - 12).toULong()
                ) {
                    "Bad ZIP64 EOCD size: $size (expected: " +
                        "${ZipConstants.Sizes.ZIP64_END_OF_CENTRAL_DIRECTORY - 12})"
                }

                /* val versionMadeBy =*/ buffer.short
                val versionNeeded = buffer.short
                require(
                    versionNeeded == ZipConstants.Versions.ZIP64_FORMAT_EXTENSION.toShort()
                ) {
                    "Bad ZIP64 EOCD version needed to extract: $versionNeeded (expected: " +
                        "${ZipConstants.Versions.ZIP64_FORMAT_EXTENSION})"
                }

                val diskNumber = buffer.int
                require(diskNumber == 0) {
                    "Bad ZIP64 EOCD disk number: $diskNumber (expected: 0)"
                }

                val startDiskNumber = buffer.int
                require(startDiskNumber == 0) {
                    "Bad ZIP64 EOCD start disk number: $startDiskNumber (expected: 0)"
                }

                val cdEntriesDisk = buffer.long.toULong()
                val cdEntries = buffer.long.toULong()
                require(cdEntries == cdEntriesDisk) {
                    "Bad ZIP64 EOCD entries in CD: $cdEntriesDisk (expected: $cdEntries)"
                }

                val cdSize = buffer.long.toULong()
                val cdOffset = buffer.long.toULong()

                return Zip64EndOfCentralDirectoryRecord(
                    centralDirectoryOffset = cdOffset,
                    centralDirectorySize = cdSize,
                    centralDirectoryEntries = cdEntries,
                    isZip64 = true
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
}
