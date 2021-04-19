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

import com.microsoft.office.zip.ZipException
import com.microsoft.office.zip.internal.ZipConstants
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
class Zip64EndOfCentralDirectoryLocatorTest {
    companion object {
        private const val TEST_CD_OFFSET: Long = 12345
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
        val eocdLocator = Zip64EndOfCentralDirectoryLocator.from(buffer)

        assertEquals(
            "The parsed Central Directory offset is incorrect",
            TEST_CD_OFFSET.toULong(),
            eocdLocator.endOfCentralDirectoryOffset
        )
    }

    @Test
    fun parseRecord_buffer_isIncomplete() {
        val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(ZipConstants.Signatures.ZIP64_END_OF_CENTRAL_DIRECTORY_LOCATOR)
            .order(ByteOrder.BIG_ENDIAN)
            .rewind() as ByteBuffer

        try {
            Zip64EndOfCentralDirectoryLocator.from(buffer)
            assertTrue("Should have thrown an exception!", false)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertNotNull(exception.cause)
            assertTrue(exception.cause is BufferUnderflowException)
        }
    }

    @Test
    fun parseRecord_signature_isIncorrect() {
        val buffer = createBuffer(signature = 12345)
        try {
            Zip64EndOfCentralDirectoryLocator.from(buffer)
            assertTrue("Should have thrown an exception!", false)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertThat(exception.message, containsString("signature"))
        }
    }

    @Test
    fun parseRecord_startDisk_isIncorrect() {
        val buffer = createBuffer(startDisk = 20)
        try {
            Zip64EndOfCentralDirectoryLocator.from(buffer)
            assertTrue("Should have thrown an exception!", false)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertThat(exception.message, containsString("start disk"))
        }
    }

    @Test
    fun parseRecord_disks_isIncorrect() {
        val buffer = createBuffer(disks = 20)
        try {
            Zip64EndOfCentralDirectoryLocator.from(buffer)
            assertTrue("Should have thrown an exception!", false)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertThat(exception.message, containsString("disks"))
        }
    }

    @Test
    fun parseRecord_unknownException() {
        try {
            Zip64EndOfCentralDirectoryLocator.from(mockBuffer)
            assertTrue("Should have thrown an exception!", false)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertNotNull(exception.cause)
            assertTrue(exception.cause is RuntimeException)
        }
    }

    private fun createBuffer(
        signature: Int = ZipConstants.Signatures.ZIP64_END_OF_CENTRAL_DIRECTORY_LOCATOR,
        startDisk: Int = 0,
        cdOffset: Long = TEST_CD_OFFSET,
        disks: Int = 1
    ): ByteBuffer {
        return ByteBuffer.allocate(ZipConstants.Sizes.ZIP64_END_OF_CENTRAL_DIRECTORY_LOCATOR)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(signature)
            .putInt(startDisk)
            .putLong(cdOffset)
            .putInt(disks)
            .order(ByteOrder.BIG_ENDIAN)
            .rewind() as ByteBuffer
    }
}
