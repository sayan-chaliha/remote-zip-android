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

import com.microsoft.office.zip.internal.CompressionMethod
import com.microsoft.office.zip.internal.ZipConstants
import com.microsoft.office.zip.internal.ZipUtils
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Date

/**
 * Represents a ZIP Local File Header record.
 */
internal data class LocalFileHeaderRecord(
    val fileName: String,
    val compressionMethod: CompressionMethod,
    val lastModifiedTime: Date,
    val crc32: UInt,
    val compressedSize: ULong,
    val uncompressedSize: ULong,
    val bitFlags: UShort,
    val isDataDescriptor: Boolean
) {
    companion object {

        /**
         * Parses a Local File Header from a [ByteBuffer].
         *
         * @param byteBuffer Buffer containing the Local File Header record
         * @param centralDirectory Central Directory to entries to validate the Local File Header
         * @return [LocalFileHeaderRecord] from binary data
         *
         * @throws [RecordException] if invalid entries are found or the header is inconsistent
         *                           with the Central Directory
         */
        fun from(
            byteBuffer: ByteBuffer,
            centralDirectory: Map<String, CentralFileHeaderRecord>
        ): LocalFileHeaderRecord {
            // Ensure little endian
            val buffer = byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

            try {
                val signature = buffer.int
                require(signature == ZipConstants.Signatures.LOCAL_FILE_HEADER) {
                    "Bad Local File Header signature: $signature (expected: " +
                        "${ZipConstants.Signatures.LOCAL_FILE_HEADER})"
                }

                val versionNeeded = buffer.short.toInt()
                require(
                    versionNeeded == ZipConstants.Versions.ZIP32_DEFAULT_VERSION ||
                        versionNeeded == ZipConstants.Versions.ZIP64_FORMAT_EXTENSION
                ) {
                    "Bad Local File Header version needed to extract: $versionNeeded " +
                        "(expected: ${ZipConstants.Versions.ZIP32_DEFAULT_VERSION} || " +
                        "${ZipConstants.Versions.ZIP64_FORMAT_EXTENSION})"
                }

                val bitFlags = buffer.short.toUShort()
                require(
                    bitFlags.and(ZipConstants.BitFlags.UNSUPPORTED_FLAGS_MASK) == 0.toUShort()
                ) {
                    "Bad Local File Header bit flags: $bitFlags (unsupported mask: " +
                        "${ZipConstants.BitFlags.UNSUPPORTED_FLAGS_MASK})"
                }

                val isDataDescriptor = bitFlags
                    .and(ZipConstants.BitFlags.DATA_DESCRIPTOR) != 0.toUShort()

                val compressionMethod = when (val compressionMethodValue = buffer.short.toInt()) {
                    0 -> CompressionMethod.STORE
                    8 -> CompressionMethod.DEFLATE
                    else -> throw IllegalArgumentException(
                        "Bad Local File Header compression method: $compressionMethodValue " +
                            "(expected: 0 || 8)"
                    )
                }

                val lastModifiedTime = ZipUtils.dosToJavaTime(buffer.int)

                var crc32 = buffer.int.toUInt()
                var compressedSize = buffer.int.toULong()
                var uncompressedSize = buffer.int.toULong()

                val fileNameLength = buffer.short.toInt()
                require(fileNameLength != 0) {
                    "Bad Local File Header file name length: $fileNameLength (expected: !0)"
                }

                val extraFieldLength = buffer.short.toInt()
                require(extraFieldLength == 0) {
                    "Bad Local File Header unsupported extra field size: $extraFieldLength " +
                        "(expected: 0)"
                }

                val fileNameBuffer = ByteArray(fileNameLength)
                buffer.get(fileNameBuffer)
                val fileName = String(fileNameBuffer, Charsets.UTF_8)

                require(centralDirectory[fileName] != null) {
                    "Bad Local File Header file name not in Central Directory."
                }

                val centralFileHeader = centralDirectory[fileName]!!
                require(isDataDescriptor == centralFileHeader.isDataDescriptor) {
                    "Bad Local File Header inconsistent bit flags: $bitFlags (expected: " +
                        "${centralFileHeader.bitFlags})"
                }

                if (isDataDescriptor) {
                    // CRC32 and file compressed size are not valid if this is a data descriptor.
                    // The actual data descriptor appears after the payload.
                    require(compressedSize == 0.toULong() && crc32 == 0u) {
                        "Bad Local File Header sizes and CRC32 inconsistent with data descriptor."
                    }

                    compressedSize = centralFileHeader.compressedSize
                    uncompressedSize = centralFileHeader.uncompressedSize
                    crc32 = centralFileHeader.crc32
                }

                return LocalFileHeaderRecord(
                    fileName = fileName,
                    uncompressedSize = uncompressedSize,
                    compressedSize = compressedSize,
                    compressionMethod = compressionMethod,
                    crc32 = crc32,
                    bitFlags = bitFlags,
                    isDataDescriptor = isDataDescriptor,
                    lastModifiedTime = lastModifiedTime
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
