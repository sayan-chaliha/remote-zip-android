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

import com.microsoft.zip.ZipException
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class Zip64ExtendedInfoRecordTest {
    companion object {
        private const val TEST_COMPRESSED_SIZE: Long = 1234
        private const val TEST_UNCOMPRESSED_SIZE: Long = 5678
        private const val TEST_LFH_OFFSET: Long = 9101112
    }

    @Mock
    lateinit var mockBuffer: ByteBuffer

    @Before
    fun onSetup() {
        `when`(mockBuffer.long).thenThrow(RuntimeException())
    }

    @Test
    fun parseRecord_isCorrect() {
        val buffer = createBuffer()
        val extInfo = Zip64ExtendedInfoRecord.from(buffer)

        assertEquals(TEST_UNCOMPRESSED_SIZE.toULong(), extInfo.uncompressedFileSize)
        assertEquals(TEST_COMPRESSED_SIZE.toULong(), extInfo.compressedFileSize)
        assertEquals(TEST_LFH_OFFSET.toULong(), extInfo.localHeaderOffset)
    }

    @Test
    fun parseRecord_startDisk_isIncorrect() {
        val buffer = createBuffer(startDisk = -1)
        try {
            Zip64ExtendedInfoRecord.from(buffer)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertThat(exception.message, containsString("start disk"))
        }
    }

    @Test
    fun parseRecord_isIncomplete() {
        val buffer = ByteBuffer.allocate(Long.SIZE_BYTES)
            .putLong(12345)

        try {
            Zip64ExtendedInfoRecord.from(buffer)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertNotNull(exception.cause)
            assertTrue(exception.cause is BufferUnderflowException)
        }
    }

    @Test
    fun parseRecord_unknownException() {
        try {
            Zip64ExtendedInfoRecord.from(mockBuffer)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertNotNull(exception.cause)
            assertTrue(exception.cause is RuntimeException)
        }
    }

    private fun createBuffer(
        uncompressedFileSize: Long = TEST_UNCOMPRESSED_SIZE,
        compressedFileSize: Long = TEST_COMPRESSED_SIZE,
        lfhOffset: Long = TEST_LFH_OFFSET,
        startDisk: Int = 0
    ): ByteBuffer {
        return ByteBuffer.allocate((Long.SIZE_BYTES * 3) + Int.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putLong(uncompressedFileSize)
            .putLong(compressedFileSize)
            .putLong(lfhOffset)
            .putInt(startDisk)
            .rewind() as ByteBuffer
    }
}
