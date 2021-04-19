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
import com.microsoft.zip.internal.ZipUtils
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Represents the ZIP32 End of Central Directory record.
 */
internal data class Zip32EndOfCentralDirectoryRecord(
    override val centralDirectoryEntries: ULong,
    override val centralDirectorySize: ULong,
    override val centralDirectoryOffset: ULong,
    override val isZip64: Boolean
) : EndOfCentralDirectoryRecord {
    companion object {

        /**
         * Parses a ZIP32 End of Central Directory record from a [ByteBuffer].
         *
         * NOTE: EOCD comments are not supported.
         *
         * @param byteBuffer Buffer containing the EOCD record
         * @return [Zip32EndOfCentralDirectoryRecord] from binary data
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
                require(signature == ZipConstants.Signatures.ZIP32_END_OF_CENTRAL_DIRECTORY) {
                    "Bad ZIP32 EOCD signature: $signature (expected " +
                        "${ZipConstants.Signatures.ZIP32_END_OF_CENTRAL_DIRECTORY})"
                }

                val diskNumber = buffer.short.toUShort()
                require(diskNumber == 0.toUShort() || diskNumber == UShort.MAX_VALUE) {
                    "Bad ZIP32 EOCD disk number: $diskNumber (expected: 0 || ${UShort.MAX_VALUE})"
                }

                val startDiskNumber = buffer.short.toUShort()
                require(diskNumber == startDiskNumber) {
                    "Bad ZIP32 EOCD start disk number: $startDiskNumber (expected: $diskNumber)"
                }

                val cdEntriesDisk = buffer.short.toUShort()
                val cdEntries = buffer.short.toUShort()
                require(cdEntries == cdEntriesDisk) {
                    "Bad ZIP32 EOCD entries in CD disk: $cdEntriesDisk (expected: $cdEntries)"
                }

                val cdSize = buffer.int.toUInt()
                val cdOffset = buffer.int.toUInt()

                val isZip64 = ZipUtils.valueInExtendedInfo(diskNumber) ||
                    ZipUtils.valueInExtendedInfo(startDiskNumber) ||
                    ZipUtils.valueInExtendedInfo(cdEntriesDisk) ||
                    ZipUtils.valueInExtendedInfo(cdEntries) ||
                    ZipUtils.valueInExtendedInfo(cdSize) ||
                    ZipUtils.valueInExtendedInfo(cdOffset)

                if (isZip64) {
                    require(cdEntries == 0.toUShort() || cdEntries == UShort.MAX_VALUE) {
                        "Bad ZIP32 EOCD entries in CD: $cdEntriesDisk (expected: 0 || " +
                            "${UShort.MAX_VALUE})"
                    }

                    require(cdSize == 0.toUInt() || cdSize == UInt.MAX_VALUE) {
                        "Bad ZIP32 EOCD size of CD: $cdSize (expected: 0 || ${UInt.MAX_VALUE})"
                    }

                    require(cdOffset == 0.toUInt() || cdOffset == UInt.MAX_VALUE) {
                        "Bad ZIP32 EOCD offset of CD: $cdOffset (expected: 0 || ${UInt.MAX_VALUE})"
                    }
                }

                return Zip32EndOfCentralDirectoryRecord(
                    centralDirectoryEntries = cdEntries.toULong(),
                    centralDirectorySize = cdSize.toULong(),
                    centralDirectoryOffset = cdOffset.toULong(),
                    isZip64 = isZip64
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
