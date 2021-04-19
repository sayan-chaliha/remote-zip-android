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
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Represents the ZIP64 End of Central Directory Locator record.
 */
internal data class Zip64EndOfCentralDirectoryLocator(
    val endOfCentralDirectoryOffset: ULong
) {
    companion object {

        /**
         * Parses a [Zip64EndOfCentralDirectoryLocator] record from a [ByteBuffer].
         *
         * @param byteBuffer Buffer containing the locator record
         * @return [Zip64EndOfCentralDirectoryLocator] from binary data
         *
         * @throws [RecordException] if an unexpected header value is encountered
         */
        @Throws(RecordException::class)
        fun from(byteBuffer: ByteBuffer): Zip64EndOfCentralDirectoryLocator {
            // Ensure byte buffer is little endian
            val buffer = byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

            try {
                val signature = buffer.int
                require(
                    signature == ZipConstants.Signatures.ZIP64_END_OF_CENTRAL_DIRECTORY_LOCATOR
                ) {
                    "Bad ZIP64 EOCD Locator signature: $signature (expected:" +
                        "${ZipConstants.Signatures.ZIP64_END_OF_CENTRAL_DIRECTORY_LOCATOR})"
                }

                val startDisk = buffer.int
                require(startDisk == 0) {
                    "Bad ZIP64 EOCD Locator CD start disk: $startDisk (expected: 0)"
                }

                val cdOffset = buffer.long.toULong()
                val disks = buffer.int
                require(disks == 1) {
                    "Bad ZIP64 EOCD Locator total disks: $disks (expected: 1)"
                }

                return Zip64EndOfCentralDirectoryLocator(cdOffset)
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
