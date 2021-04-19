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
import com.microsoft.zip.internal.ZipConstants
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
class Zip32EndOfCentralDirectoryRecordTest {
    companion object {
        private const val TEST_CD_ENTRIES: Short = 10
        private const val TEST_CD_SIZE: Int = 100
        private const val TEST_CD_OFFSET: Int = 12345
    }

    @Mock
    lateinit var mockBuffer: ByteBuffer

    @Before
    fun onSetup() {
        `when`(mockBuffer.int).thenThrow(RuntimeException())
    }

    @Test
    fun parseRecord_isCorrect() {
        val buffer = createBuffer()
        val eocd = Zip32EndOfCentralDirectoryRecord.from(buffer)

        assertEquals(
            "Parsed Central Directory entries is incorrect",
            TEST_CD_ENTRIES.toULong(),
            eocd.centralDirectoryEntries
        )
        assertEquals(
            "Parsed Central Directory size is incorrect",
            TEST_CD_SIZE.toULong(),
            eocd.centralDirectorySize
        )
        assertEquals(
            "Parsed Central Directory offset is incorrect",
            TEST_CD_OFFSET.toULong(),
            eocd.centralDirectoryOffset
        )
    }

    @Test
    fun parseRecord_buffer_isIncomplete() {
        val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(ZipConstants.Signatures.ZIP32_END_OF_CENTRAL_DIRECTORY)
            .order(ByteOrder.BIG_ENDIAN)
            .rewind() as ByteBuffer

        try {
            Zip32EndOfCentralDirectoryRecord.from(buffer)
            assertTrue("Should have thrown an exception!", false)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertTrue(exception.cause is BufferUnderflowException)
        }
    }

    @Test
    fun parseRecord_signature_isIncorrect() {
        val buffer = createBuffer(signature = 12345)

        try {
            Zip32EndOfCentralDirectoryRecord.from(buffer)
            assertTrue("Should have thrown an exception!", false)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertThat(exception.message, containsString("signature"))
        }
    }

    @Test
    fun parseRecord_diskNumber_isIncorrect() {
        val buffer = createBuffer(diskNumber = 2)

        try {
            Zip32EndOfCentralDirectoryRecord.from(buffer)
            assertTrue("Should have thrown an exception!", false)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertThat(exception.message, containsString("disk number"))
        }
    }

    @Test
    fun parseRecord_startDiskNumber_isIncorrect() {
        val buffer = createBuffer(startDisk = 2)
        try {
            Zip32EndOfCentralDirectoryRecord.from(buffer)
            assertTrue("Should have thrown an exception!", false)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertThat(exception.message, containsString("start disk number"))
        }
    }

    @Test
    fun parseRecord_cdEntries_isIncorrect() {
        val buffer = createBuffer(cdEntries = 1, cdEntriesDisk = -1)

        try {
            Zip32EndOfCentralDirectoryRecord.from(buffer)
            assertTrue("Should have thrown an exception!", false)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertThat(exception.message, containsString("entries in CD disk"))
        }
    }

    @Test
    fun parseRecord_isZip64() {
        val buffer = createBuffer(cdEntriesDisk = -1, cdEntries = -1, cdOffset = 0, cdSize = 0)
        val eocd = Zip32EndOfCentralDirectoryRecord.from(buffer)
        assertTrue("Failed to parse ZIP64 EOCD", eocd.isZip64)
    }

    @Test
    fun parseRecord_zip64_cdSize_isIncorrect() {
        val buffer = createBuffer(cdEntriesDisk = -1, cdEntries = -1, cdOffset = 0, cdSize = 10)
        try {
            Zip32EndOfCentralDirectoryRecord.from(buffer)
            assertTrue("Should have thrown an exception!", false)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertThat(exception.message, containsString("size of CD"))
        }
    }

    @Test
    fun parseRecord_zip64_cdOffset_isIncorrect() {
        val buffer = createBuffer(cdEntriesDisk = -1, cdEntries = -1, cdOffset = 10, cdSize = 0)
        try {
            Zip32EndOfCentralDirectoryRecord.from(buffer)
            assertTrue("Should have thrown an exception!", false)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertThat(exception.message, containsString("offset of CD"))
        }
    }

    @Test
    fun parseRecord_zip64_cdEntries_isIncorrect() {
        val buffer = createBuffer(cdOffset = -1, cdSize = -1)
        try {
            Zip32EndOfCentralDirectoryRecord.from(buffer)
            assertTrue("Should have thrown an exception!", false)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertThat(exception.message, containsString("entries in CD"))
        }
    }

    @Test
    fun parseRecord_unknownException() {
        try {
            Zip32EndOfCentralDirectoryRecord.from(mockBuffer)
            assertTrue("Should have thrown an exception!", false)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertNotNull(exception.cause)
            assertTrue(exception.cause is RuntimeException)
            assertThat(exception.message, containsString("Unknown"))
        }
    }

    private fun createBuffer(
        signature: Int = ZipConstants.Signatures.ZIP32_END_OF_CENTRAL_DIRECTORY,
        diskNumber: Short = 0,
        startDisk: Short = 0,
        cdEntriesDisk: Short = TEST_CD_ENTRIES,
        cdEntries: Short = TEST_CD_ENTRIES,
        cdSize: Int = TEST_CD_SIZE,
        cdOffset: Int = TEST_CD_OFFSET
    ): ByteBuffer {
        val buffer = ByteBuffer.allocate(ZipConstants.Sizes.ZIP32_END_OF_CENTRAL_DIRECTORY)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(signature)
            .putShort(diskNumber)
            .putShort(startDisk)
            .putShort(cdEntriesDisk)
            .putShort(cdEntries)
            .putInt(cdSize)
            .putInt(cdOffset)
            .order(ByteOrder.BIG_ENDIAN)

        buffer.rewind()

        return buffer
    }
}
