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

import com.microsoft.office.zip.TestUtil
import com.microsoft.office.zip.ZipException
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Date
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ExtendedTimestampInfoRecordTest {
    companion object {
        // Remove accuracy from dates
        private val TEST_LAST_MODIFIED_TIME = Date()
        private val TEST_LAST_ACCESS_TIME = Date()
        private val TEST_CREATION_TIME = Date()
    }

    @Mock
    lateinit var mockBuffer: ByteBuffer

    @Before
    fun onSetup() {
        `when`(mockBuffer.get()).thenThrow(RuntimeException())
    }

    @Test
    fun parseRecord_isCorrect() {
        val buffer = ExtendedTimestampInfoRecord(
            lastModifiedTime = TEST_LAST_MODIFIED_TIME,
            lastAccessTime = TEST_LAST_ACCESS_TIME,
            creationTime = TEST_CREATION_TIME
        ).serialize()

        val extTsInfo = ZipExtraField.from(ByteBuffer.wrap(buffer))
        assertTrue(extTsInfo is ExtendedTimestampInfoRecord)

        with(extTsInfo as ExtendedTimestampInfoRecord) {
            assertTrue(
                "Parsed creation time is incorrect",
                TestUtil.dateAlmostEquals(TEST_CREATION_TIME, creationTime!!)
            )
            assertTrue(
                "Parsed modified time is incorrect",
                TestUtil.dateAlmostEquals(TEST_LAST_MODIFIED_TIME, lastModifiedTime!!)
            )
            assertTrue(
                "Parsed access time is incorrect",
                TestUtil.dateAlmostEquals(TEST_LAST_ACCESS_TIME, lastAccessTime!!)
            )
        }
    }

    @Test
    fun parseRecord_isIncomplete() {
        val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(12345)

        try {
            ExtendedTimestampInfoRecord.from(buffer)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertNotNull(exception.cause)
            assertTrue(exception.cause is BufferUnderflowException)
        }
    }

    @Test
    fun parseRecord_unknownException() {
        try {
            ExtendedTimestampInfoRecord.from(mockBuffer)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertNotNull(exception.cause)
            assertTrue(exception.cause is RuntimeException)
        }
    }
}
