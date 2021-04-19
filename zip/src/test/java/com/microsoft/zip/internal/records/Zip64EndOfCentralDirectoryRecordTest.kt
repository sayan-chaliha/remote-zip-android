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
class Zip64EndOfCentralDirectoryRecordTest {
    companion object {
        private const val TEST_CD_ENTRIES: Long = 1234
        private const val TEST_CD_SIZE: Long = 5678
        private const val TEST_CD_OFFSET: Long = 9101112
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
        val eocd = Zip64EndOfCentralDirectoryRecord.from(buffer)
        assertTrue(eocd.isZip64)
        assertEquals(
            "Parsed Central Directory entries is incorrect",
            TEST_CD_ENTRIES.toULong(),
            eocd.centralDirectoryEntries
        )
        assertEquals(
            "Parsed Central Directory offset is incorrect",
            TEST_CD_OFFSET.toULong(),
            eocd.centralDirectoryOffset
        )
        assertEquals(
            "Parsed Central Directory size is incorrect",
            TEST_CD_SIZE.toULong(),
            eocd.centralDirectorySize
        )
    }

    @Test
    fun parseRecord_signature_isIncorrect() {
        val buffer = createBuffer(signature = 1234)
        try {
            Zip64EndOfCentralDirectoryRecord.from(buffer)
            assertTrue("Should have thrown an exception!", false)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertThat(exception.message, containsString("signature"))
        }
    }

    @Test
    fun parseRecord_buffer_isIncomplete() {
        val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(ZipConstants.Signatures.ZIP64_END_OF_CENTRAL_DIRECTORY)
            .order(ByteOrder.BIG_ENDIAN)
            .rewind() as ByteBuffer

        try {
            Zip64EndOfCentralDirectoryRecord.from(buffer)
            assertTrue("Should have thrown an exception!", false)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertNotNull(exception.cause)
            assertTrue(exception.cause is BufferUnderflowException)
        }
    }

    @Test
    fun parseRecord_unknownException() {
        try {
            Zip64EndOfCentralDirectoryRecord.from(mockBuffer)
            assertTrue("Should have thrown an exception!", false)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertNotNull(exception.cause)
            assertTrue(exception.cause is RuntimeException)
        }
    }

    @Test
    fun parseRecord_size_isIncorrect() {
        val buffer = createBuffer(size = 12)
        try {
            Zip64EndOfCentralDirectoryRecord.from(buffer)
            assertTrue("Should have thrown an exception!", false)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertThat(exception.message, containsString("size"))
        }
    }

    @Test
    fun parseRecord_versionNeeded_isIncorrect() {
        val buffer = createBuffer(versionNeeded = 12345)
        try {
            Zip64EndOfCentralDirectoryRecord.from(buffer)
            assertTrue("Should have thrown an exception!", false)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertThat(exception.message, containsString("version needed"))
        }
    }

    @Test
    fun parseRecord_diskNumber_isIncorrect() {
        val buffer = createBuffer(diskNumber = 12345)
        try {
            Zip64EndOfCentralDirectoryRecord.from(buffer)
            assertTrue("Should have thrown an exception!", false)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertThat(exception.message, containsString("disk number"))
        }
    }

    @Test
    fun parseRecord_startDiskNumber_isIncorrect() {
        val buffer = createBuffer(startDiskNumber = 12345)
        try {
            Zip64EndOfCentralDirectoryRecord.from(buffer)
            assertTrue("Should have thrown an exception!", false)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertThat(exception.message, containsString("start disk number"))
        }
    }

    @Test
    fun parseRecord_cdEntries_isIncorrect() {
        val buffer = createBuffer(entries = 1)
        try {
            Zip64EndOfCentralDirectoryRecord.from(buffer)
            assertTrue("Should have thrown an exception!", false)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertThat(exception.message, containsString("entries in CD"))
        }
    }

    private fun createBuffer(
        signature: Int = ZipConstants.Signatures.ZIP64_END_OF_CENTRAL_DIRECTORY,
        size: Long = ZipConstants.Sizes.ZIP64_END_OF_CENTRAL_DIRECTORY.toLong() - 12,
        versionMadeBy: Short = 12345,
        versionNeeded: Short = ZipConstants.Versions.ZIP64_FORMAT_EXTENSION.toShort(),
        diskNumber: Int = 0,
        startDiskNumber: Int = 0,
        entriesDisk: Long = TEST_CD_ENTRIES,
        entries: Long = TEST_CD_ENTRIES,
        cdSize: Long = TEST_CD_SIZE,
        cdOffset: Long = TEST_CD_OFFSET
    ): ByteBuffer {
        return ByteBuffer.allocate(ZipConstants.Sizes.ZIP64_END_OF_CENTRAL_DIRECTORY + 12)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(signature)
            .putLong(size)
            .putShort(versionMadeBy)
            .putShort(versionNeeded)
            .putInt(diskNumber)
            .putInt(startDiskNumber)
            .putLong(entriesDisk)
            .putLong(entries)
            .putLong(cdSize)
            .putLong(cdOffset)
            .order(ByteOrder.BIG_ENDIAN)
            .rewind() as ByteBuffer
    }
}
