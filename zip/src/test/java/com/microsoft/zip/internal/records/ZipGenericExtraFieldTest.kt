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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ZipGenericExtraFieldTest {
    companion object {
        private const val TEST_HEADER_ID: Short = 12345
        private const val TEST_DATA: Int = 0xE110
    }

    @Mock
    lateinit var mockBuffer: ByteBuffer

    @Before
    fun onSetup() {
        mockBuffer.limit()
        `when`(mockBuffer.get(any(ByteArray::class.java))).thenThrow(RuntimeException())
    }

    @Test
    fun parseRecord_isCorrect() {
        val buffer = ZipGenericExtraField(TEST_HEADER_ID, intToByteArray(TEST_DATA))
            .serialize()

        val extraField = ZipExtraField.from(ByteBuffer.wrap(buffer))

        assertTrue(extraField is ZipGenericExtraField)
        assertEquals(
            "Parsed extra field header ID is incorrect",
            TEST_HEADER_ID,
            extraField.headerId
        )
        assertEquals(
            "Parsed extra field size is incorrect",
            Int.SIZE_BYTES.toShort(),
            extraField.size
        )
        assertEquals(
            "Parsed extra field payload is incorrect",
            TEST_DATA,
            intFromByteArray((extraField as ZipGenericExtraField).bytes)
        )
    }

    @Test
    fun parseRecord_unknownError() {
        try {
            ZipGenericExtraField.from(TEST_HEADER_ID, mockBuffer)
            assertTrue("Should have thrown an exception!", false)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertNotNull(exception.cause)
            assertTrue(exception.cause is RuntimeException)
        }
    }

    private fun intToByteArray(value: Int): ByteArray {
        val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(value)
            .rewind() as ByteBuffer

        val bytes = ByteArray(Int.SIZE_BYTES)
        buffer.get(bytes)

        return bytes
    }

    private fun intFromByteArray(bytes: ByteArray): Int {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return buffer.int
    }
}
