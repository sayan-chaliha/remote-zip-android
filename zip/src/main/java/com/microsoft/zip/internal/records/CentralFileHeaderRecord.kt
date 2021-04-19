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

import com.microsoft.zip.internal.CompressionMethod
import com.microsoft.zip.internal.ZipConstants
import com.microsoft.zip.internal.ZipUtils
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.util.Date

/**
 * Represents a ZIP Central File Header record.
 */
internal data class CentralFileHeaderRecord(
    val fileName: String,
    val fileComment: String,
    val lastModifiedTime: Date,
    val lastAccessTime: Date? = null,
    val creationTime: Date? = null,
    val compressionMethod: CompressionMethod,
    val crc32: UInt,
    val compressedSize: ULong,
    val uncompressedSize: ULong,
    val isDataDescriptor: Boolean,
    val bitFlags: UShort,
    val localFileHeaderOffset: ULong,
    val isDirectory: Boolean
) {
    companion object {

        /**
         * Parses a [CentralFileHeaderRecord] from a [ByteBuffer].
         *
         * @param byteBuffer Buffer containing the Central File Header record
         * @return [CentralFileHeaderRecord]
         *
         * @throws [RecordException] in case an unexpected header is encountered
         */
        @Throws(RecordException::class)
        fun from(byteBuffer: ByteBuffer): CentralFileHeaderRecord {
            // Ensure little endian
            val buffer = byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

            try {
                val signature = buffer.int
                require(signature == ZipConstants.Signatures.CENTRAL_FILE_HEADER) {
                    "Bad Central File Header signature: $signature (expected: " +
                        "${ZipConstants.Signatures.CENTRAL_FILE_HEADER})"
                }

                /*val versionMadeBy =*/ buffer.short
                val versionNeeded = buffer.short
                require(
                    versionNeeded == ZipConstants.Versions.ZIP32_DEFAULT_VERSION.toShort() ||
                        versionNeeded == ZipConstants.Versions.ZIP64_FORMAT_EXTENSION.toShort()
                ) {
                    "Bad Central File Header version needed to extract: $versionNeeded " +
                        "(expected: ${ZipConstants.Versions.ZIP32_DEFAULT_VERSION} || " +
                        "${ZipConstants.Versions.ZIP64_FORMAT_EXTENSION})"
                }

                val bitFlags = buffer.short.toUShort()
                require(
                    bitFlags.and(ZipConstants.BitFlags.UNSUPPORTED_FLAGS_MASK) == 0.toUShort()
                ) {
                    "Bad Central File Header bit flags: $bitFlags (unsupported mask: " +
                        "${ZipConstants.BitFlags.UNSUPPORTED_FLAGS_MASK})"
                }

                val isDataDescriptor = bitFlags
                    .and(ZipConstants.BitFlags.DATA_DESCRIPTOR) != 0.toUShort()
                val isUtf8Names = bitFlags.and(ZipConstants.BitFlags.UTF8_NAMES) != 0.toUShort()
                val encoding = if (isUtf8Names) Charsets.UTF_8 else Charset.defaultCharset()

                // Only STORE and DEFLATE are supported
                val compressionMethod = when (val compressionMethodValue = buffer.short.toInt()) {
                    0 -> CompressionMethod.STORE
                    8 -> CompressionMethod.DEFLATE
                    else -> throw IllegalArgumentException(
                        "Bad Central File Header compression method: " +
                            "$compressionMethodValue (expected: 0 || 8)"
                    )
                }

                var lastModifiedTime = ZipUtils.dosToJavaTime(buffer.int)
                val crc32 = buffer.int.toUInt()
                var compressedSize = buffer.int.toULong()
                var uncompressedSize = buffer.int.toULong()

                val fileNameLength = buffer.short.toInt()
                require(fileNameLength != 0) {
                    "Bad Central File Header file name length: $fileNameLength (expected: !0)"
                }

                val extraFieldLength = buffer.short.toInt()
                val fileCommentLength = buffer.short.toInt()

                val startDisk = buffer.short.toInt()
                require(startDisk == 0) {
                    "Bad Central File Header start disk: $startDisk (expected: 0)"
                }

                /* val internalFileAttributes =*/ buffer.short
                /* val externalFileAttributes =*/ buffer.int
                var lfhOffset = buffer.int.toULong()

                val fileNameBuffer = ByteArray(fileNameLength)
                buffer.get(fileNameBuffer)
                val fileName = String(fileNameBuffer, encoding)
                val isDirectory = fileName.endsWith("/")

                if (isDataDescriptor && !isDirectory) {
                    // Data descriptors have a valid CRC32
                    require(crc32 > 0u && crc32 <= 0xFFFFFFFFu) {
                        "Bad Central File Header CRC32: $crc32 (expected: >0 || <= 0xFFFFFFFF)"
                    }
                }

                var creationTime: Date? = null
                var lastAccessTime: Date? = null

                if (extraFieldLength > 0) {
                    val extraFieldBytes = ByteArray(extraFieldLength)
                    buffer.get(extraFieldBytes)
                    val extraFieldBuffer = ByteBuffer.wrap(extraFieldBytes)
                    while (extraFieldBuffer.hasRemaining()) {
                        when (val extField = ZipExtraField.from(extraFieldBuffer)) {
                            is Zip64ExtendedInfoRecord -> {
                                compressedSize = extField.compressedFileSize
                                uncompressedSize = extField.uncompressedFileSize
                                lfhOffset = extField.localHeaderOffset
                            }

                            is ExtendedTimestampInfoRecord -> {
                                extField.lastModifiedTime?.let { lastModifiedTime = it }
                                creationTime = extField.creationTime
                                lastAccessTime = extField.lastAccessTime
                            }

                            is ZipGenericExtraField -> {
                                // Right now, a header I'm not handling is 0x7875 --
                                // Unix GID/UID information of a file.
                            }
                        }
                    }
                }

                var fileComment = ""
                if (fileCommentLength > 0) {
                    val fileCommentBuffer = ByteArray(fileCommentLength)
                    buffer.get(fileCommentBuffer)
                    fileComment = String(fileCommentBuffer, Charsets.UTF_8)
                }

                return CentralFileHeaderRecord(
                    fileName = fileName,
                    fileComment = fileComment,
                    lastModifiedTime = lastModifiedTime,
                    compressionMethod = compressionMethod,
                    crc32 = crc32,
                    compressedSize = compressedSize,
                    uncompressedSize = uncompressedSize,
                    localFileHeaderOffset = lfhOffset,
                    isDataDescriptor = isDataDescriptor,
                    bitFlags = bitFlags,
                    isDirectory = isDirectory,
                    creationTime = creationTime,
                    lastAccessTime = lastAccessTime
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
