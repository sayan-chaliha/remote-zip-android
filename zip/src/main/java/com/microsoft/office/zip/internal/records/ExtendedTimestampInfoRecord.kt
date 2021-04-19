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
import java.util.Date

/**
 * A ZIP extra field with ID 0x5455 containing additional time info.
 */
internal data class ExtendedTimestampInfoRecord(
    val lastModifiedTime: Date? = null,
    val lastAccessTime: Date? = null,
    val creationTime: Date? = null
) : ZipExtraField() {
    override val headerId: Short = ZipConstants.Headers.EXTENDED_TIMESTAMP_INFORMATION
    override val size: Short = (
        Byte.SIZE_BYTES +
            (if (lastModifiedTime != null) Int.SIZE_BYTES else 0) +
            (if (lastAccessTime != null) Int.SIZE_BYTES else 0) +
            (if (creationTime != null) Int.SIZE_BYTES else 0)
        ).toShort()

    companion object {
        private const val MODIFY_TIME_BIT = 1
        private const val ACCESS_TIME_BIT = 2
        private const val CREATE_TIME_BIT = 4

        /**
         * Parses ZIP extended timestamp information from [ByteBuffer].
         *
         * @param buffer Buffer containing the extended info fields
         * @return [ExtendedTimestampInfoRecord]
         *
         * @throws [RecordException] if an invalid field value is encountered
         */
        @Throws(RecordException::class)
        fun from(buffer: ByteBuffer): ExtendedTimestampInfoRecord {
            try {
                val flags = buffer.get().toInt()
                var modifyTime: Date? = null
                var accessTime: Date? = null
                var createTime: Date? = null

                if (flags and MODIFY_TIME_BIT == MODIFY_TIME_BIT) {
                    modifyTime = Date(buffer.int.toLong() * 1000)
                }

                if (
                    flags and ACCESS_TIME_BIT == ACCESS_TIME_BIT &&
                    buffer.remaining() >= Int.SIZE_BYTES
                ) {
                    accessTime = Date(buffer.int.toLong() * 1000)
                }

                if (
                    flags and CREATE_TIME_BIT == CREATE_TIME_BIT &&
                    buffer.remaining() >= Int.SIZE_BYTES
                ) {
                    createTime = Date(buffer.int.toLong() * 1000)
                }

                return ExtendedTimestampInfoRecord(
                    lastModifiedTime = modifyTime,
                    lastAccessTime = accessTime,
                    creationTime = createTime
                )
            } catch (exception: BufferUnderflowException) {
                throw RecordException("Incomplete record.", exception)
            } catch (exception: Exception) {
                throw RecordException("Unknown exception.", exception)
            }
        }
    }

    override fun serialize(): ByteArray {
        val flags = (if (lastModifiedTime != null) MODIFY_TIME_BIT else 0) or
            (if (lastAccessTime != null) ACCESS_TIME_BIT else 0) or
            (if (creationTime != null) CREATE_TIME_BIT else 0)

        val byteBuffer = ByteBuffer.allocate((Short.SIZE_BYTES * 2) + size.toInt())
            .order(ByteOrder.LITTLE_ENDIAN)
            .putShort(headerId)
            .putShort(size)
            .put(flags.toByte())

        if (lastModifiedTime != null) {
            byteBuffer.putInt((lastModifiedTime.time / 1000).toInt())
        }

        if (lastAccessTime != null) {
            byteBuffer.putInt((lastAccessTime.time / 1000).toInt())
        }

        if (creationTime != null) {
            byteBuffer.putInt((creationTime.time / 1000).toInt())
        }

        byteBuffer.rewind()

        val bytes = ByteArray(byteBuffer.remaining())
        byteBuffer.get(bytes)
        return bytes
    }
}
