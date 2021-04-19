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

import com.microsoft.zip.TestUtil
import com.microsoft.zip.ZipException
import com.microsoft.zip.internal.CompressionMethod
import com.microsoft.zip.internal.ZipConstants
import com.microsoft.zip.internal.ZipUtils
import java.lang.RuntimeException
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Date
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
class LocalFileHeaderRecordTest {

    companion object {
        private const val TEST_FILE_NAME = "test.txt"
        private val TEST_LAST_MODIFIED_TIME = Date()
        private const val TEST_COMPRESSED_SIZE = 5678
        private const val TEST_UNCOMPRESSED_SIZE = 1234
        private const val TEST_CRC32 = 9101112
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
        val cd = createCentralDirectory()

        val lfh = LocalFileHeaderRecord.from(buffer, cd)

        assertEquals("Parsed file name is incorrect", lfh.fileName, TEST_FILE_NAME)
        assertEquals(
            "Parsed compression method is incorrect",
            CompressionMethod.DEFLATE,
            lfh.compressionMethod
        )
        assertEquals(
            "Compressed size doesn't match that of the Central Directory entry",
            TEST_COMPRESSED_SIZE.toULong(),
            lfh.compressedSize
        )
        assertEquals(
            "Parsed uncompressed size is incorrect",
            TEST_UNCOMPRESSED_SIZE.toULong(),
            lfh.uncompressedSize
        )
        assertEquals(
            "CRC32 doesn't match that of the Central Directory entry",
            TEST_CRC32.toUInt(),
            lfh.crc32
        )
        assertTrue(
            "Parsed last modified time is incorrect",
            TestUtil.dateAlmostEquals(TEST_LAST_MODIFIED_TIME, lfh.lastModifiedTime)
        )
        assertEquals(
            "Parsed bit flags are incorrect",
            (ZipConstants.BitFlags.DATA_DESCRIPTOR.or(ZipConstants.BitFlags.UTF8_NAMES)),
            lfh.bitFlags
        )
        assertTrue("Parsed bit flags are not correct", lfh.isDataDescriptor)
    }

    @Test
    fun parseRecord_compressionMethod_storeIsSupported() {
        val buffer = createBuffer(compressionMethod = 0)
        val lfh = LocalFileHeaderRecord.from(buffer, createCentralDirectory())

        assertEquals(
            "Parsed compression method is incorrect",
            CompressionMethod.STORE,
            lfh.compressionMethod
        )
    }

    @Test
    fun parseRecord_buffer_isIncomplete() {
        val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(ZipConstants.Signatures.LOCAL_FILE_HEADER)
            .rewind() as ByteBuffer

        try {
            LocalFileHeaderRecord.from(buffer, createCentralDirectory())
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertNotNull(exception.cause)
            assertTrue(exception.cause is BufferUnderflowException)
        }
    }

    @Test
    fun parseRecord_signature_isIncorrect() {
        val buffer = createBuffer(signature = 1234)
        try {
            LocalFileHeaderRecord.from(buffer, createCentralDirectory())
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertNotNull(exception.message)
            assertThat(exception.message, containsString("signature"))
        }
    }

    @Test
    fun parseRecord_unknownException() {
        try {
            LocalFileHeaderRecord.from(mockBuffer, createCentralDirectory())
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertNotNull(exception.cause)
            assertTrue(exception.cause is RuntimeException)
        }
    }

    @Test
    fun parseRecord_versionNeeded_isIncorrect() {
        val buffer = createBuffer(versionNeeded = 123)
        try {
            LocalFileHeaderRecord.from(buffer, createCentralDirectory())
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertNotNull(exception.message)
            assertThat(exception.message, containsString("version needed"))
        }
    }

    @Test
    fun parseRecord_bitFlags_areUnsupported() {
        var buffer = createBuffer(bitFlags = ZipConstants.BitFlags.UNSUPPORTED_FLAGS_MASK.toShort())
        try {
            LocalFileHeaderRecord.from(buffer, createCentralDirectory())
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertNotNull(exception.message)
            assertThat(exception.message, containsString("bit flags"))
        }

        buffer = createBuffer(
            bitFlags = ZipConstants.BitFlags.DATA_DESCRIPTOR
                .or(ZipConstants.BitFlags.UNSUPPORTED_12).toShort()
        )
        try {
            LocalFileHeaderRecord.from(buffer, createCentralDirectory())
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertNotNull(exception.message)
            assertThat(exception.message, containsString("bit flags"))
        }
    }

    @Test
    fun parseRecord_compressionMethod_isIncorrect() {
        val buffer = createBuffer(compressionMethod = 10)
        try {
            LocalFileHeaderRecord.from(buffer, createCentralDirectory())
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertNotNull(exception.message)
            assertThat(exception.message, containsString("compression method"))
        }
    }

    @Test
    fun parseRecord_fileName_isIncorrect() {
        val buffer = createBuffer(fileName = "")
        try {
            LocalFileHeaderRecord.from(buffer, createCentralDirectory())
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertNotNull(exception.message)
            assertThat(exception.message, containsString("file name length"))
        }
    }

    @Test
    fun parseRecord_extraFieldLength_isIncorrect() {
        val buffer = createBuffer(extraFieldLength = 32)
        try {
            LocalFileHeaderRecord.from(buffer, createCentralDirectory())
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertNotNull(exception.message)
            assertThat(exception.message, containsString("extra field length"))
        }
    }

    @Test
    fun parseRecord_centralDirectoryEntry_isMissing() {
        val buffer = createBuffer()
        try {
            LocalFileHeaderRecord.from(buffer, mapOf())
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertNotNull(exception.message)
            assertThat(exception.message, containsString("Central Directory"))
        }
    }

    @Test
    fun parseRecord_centralDirectoryEntryBitFlags_isInconsistent() {
        val buffer = createBuffer()
        try {
            LocalFileHeaderRecord.from(buffer, createCentralDirectory(isDataDescriptor = false))
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertNotNull(exception.message)
            assertThat(exception.message, containsString("bit flags"))
        }
    }

    @Test
    fun parseRecord_dataDescriptor_badSizeAndCrc32() {
        var buffer = createBuffer(compressedSize = 10)
        try {
            LocalFileHeaderRecord.from(buffer, createCentralDirectory())
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertNotNull(exception.message)
            assertThat(exception.message, containsString("sizes"))
        }

        buffer = createBuffer(crc32 = 10)
        try {
            LocalFileHeaderRecord.from(buffer, createCentralDirectory())
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertNotNull(exception.message)
            assertThat(exception.message, containsString("CRC32"))
        }
    }

    private fun createCentralDirectory(
        fileName: String = TEST_FILE_NAME,
        compressedSize: Int = TEST_COMPRESSED_SIZE,
        uncompressedSize: Int = TEST_UNCOMPRESSED_SIZE,
        crc32: Int = TEST_CRC32,
        isDataDescriptor: Boolean = true,
    ): Map<String, CentralFileHeaderRecord> {
        val cfh = CentralFileHeaderRecord(
            fileName = fileName,
            fileComment = "",
            lastModifiedTime = TEST_LAST_MODIFIED_TIME,
            compressionMethod = CompressionMethod.DEFLATE,
            crc32 = crc32.toUInt(),
            compressedSize = compressedSize.toULong(),
            uncompressedSize = uncompressedSize.toULong(),
            isDataDescriptor = isDataDescriptor,
            bitFlags = 0u,
            localFileHeaderOffset = 0.toULong(),
            isDirectory = false
        )

        return mapOf((fileName to cfh))
    }

    private fun createBuffer(
        signature: Int = ZipConstants.Signatures.LOCAL_FILE_HEADER,
        versionNeeded: Short = ZipConstants.Versions.ZIP32_DEFAULT_VERSION.toShort(),
        bitFlags: Short = (
            ZipConstants.BitFlags.DATA_DESCRIPTOR or
                ZipConstants.BitFlags.UTF8_NAMES
            ).toShort(),
        compressionMethod: Short = 8,
        lastModifiedTime: Int = ZipUtils.javaToDosTime(TEST_LAST_MODIFIED_TIME),
        crc32: Int = 0,
        compressedSize: Int = 0,
        uncompressedSize: Int = TEST_UNCOMPRESSED_SIZE,
        fileName: String = TEST_FILE_NAME,
        extraFieldLength: Short = 0
    ): ByteBuffer {
        return ByteBuffer.allocate(ZipConstants.Sizes.LOCAL_FILE_HEADER + fileName.length)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(signature)
            .putShort(versionNeeded)
            .putShort(bitFlags)
            .putShort(compressionMethod)
            .putInt(lastModifiedTime)
            .putInt(crc32)
            .putInt(compressedSize)
            .putInt(uncompressedSize)
            .putShort(fileName.length.toShort())
            .putShort(extraFieldLength)
            .put(fileName.toByteArray(Charsets.UTF_8))
            .order(ByteOrder.BIG_ENDIAN)
            .rewind() as ByteBuffer
    }
}
