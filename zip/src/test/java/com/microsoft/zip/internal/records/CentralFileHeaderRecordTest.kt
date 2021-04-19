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
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Calendar
import java.util.Date
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class CentralFileHeaderRecordTest {
    companion object {
        private const val TEST_FILE_NAME = "test.txt"
        private const val TEST_FILE_COMMENT = "this is a comment"
        private const val TEST_DIR_NAME = "test/"
        private val TEST_LAST_MODIFIED_TIME = Date()
        private val TEST_LAST_ACCESS_TIME = Date()
        private val TEST_CREATION_TIME = Date()
        private val TEST_EXT_LAST_MODIFIED_TIME: Date
            get() {
                val cal = Calendar.getInstance()
                cal.time = Date()
                cal.set(Calendar.YEAR, 2020)

                return cal.time
            }

        private const val TEST_CRC32 = 123456
        private const val TEST_COMPRESSED_SIZE = 1234
        private const val TEST_UNCOMPRESSED_SIZE = 5678
        private const val TEST_LFH_OFFSET = 1234
        private const val TEST_EXT_LFH_OFFSET: ULong = 9101112U
        private const val TEST_EXT_COMPRESSED_SIZE: ULong = 13141516U
        private const val TEST_EXT_UNCOMPRESSED_SIZE: ULong = 17181920U
    }

    @Mock
    lateinit var mockBuffer: ByteBuffer

    @Before
    fun onSetup() {
        `when`(mockBuffer.int).thenThrow(RuntimeException())
    }

    @Test
    fun parseRecord_file_isCorrect() {
        val buffer = createFileBuffer()
        val cfh = CentralFileHeaderRecord.from(buffer)

        assertEquals("Parsed file name is incorrect.", TEST_FILE_NAME, cfh.fileName)

        // ZIP looses accuracy in date/time
        assertTrue(
            "Parsed last modified time is incorrect",
            TestUtil.dateAlmostEquals(TEST_LAST_MODIFIED_TIME, cfh.lastModifiedTime)
        )

        assertEquals("Parsed CRC32 is incorrect", TEST_CRC32.toUInt(), cfh.crc32)
        assertEquals(
            "Parsed compressed size is incorrect",
            TEST_COMPRESSED_SIZE.toULong(),
            cfh.compressedSize
        )
        assertEquals(
            "Parsed uncompressed size is incorrect",
            TEST_UNCOMPRESSED_SIZE.toULong(),
            cfh.uncompressedSize
        )
        assertEquals(
            "Parsed local file header offset is incorrect",
            TEST_LFH_OFFSET.toULong(),
            cfh.localFileHeaderOffset
        )
        assertEquals(
            "Parsed compression method is incorrect",
            CompressionMethod.DEFLATE,
            cfh.compressionMethod
        )
        assertEquals(
            "Parsed bit flags is incorrect",
            (ZipConstants.BitFlags.DATA_DESCRIPTOR or ZipConstants.BitFlags.UTF8_NAMES),
            cfh.bitFlags
        )
        assertTrue("Parsed bit flags is incorrect", cfh.isDataDescriptor)
        assertFalse("Parsed file name is incorrect", cfh.isDirectory)

        // These values should be null without extra fields
        assertNull("Non-null access time without extra fields specified", cfh.lastAccessTime)
        assertNull("Non-null created time without extra fields specified", cfh.creationTime)
    }

    @Test
    fun parseRecord_directory_isCorrect() {
        val buffer = createDirectoryBuffer()
        val cfh = CentralFileHeaderRecord.from(buffer)

        assertEquals("Parsed file name is incorrect.", TEST_DIR_NAME, cfh.fileName)

        assertTrue(
            "Parsed last modified time is incorrect",
            TestUtil.dateAlmostEquals(TEST_LAST_MODIFIED_TIME, cfh.lastModifiedTime)
        )

        assertEquals("Parsed CRC32 is incorrect", 0.toUInt(), cfh.crc32)
        assertEquals("Parsed compressed size is incorrect", 0.toULong(), cfh.compressedSize)
        assertEquals("Parsed uncompressed size is incorrect", 0.toULong(), cfh.uncompressedSize)
        assertEquals(
            "Parsed local file header offset is incorrect",
            0.toULong(),
            cfh.localFileHeaderOffset
        )
        assertEquals(
            "Parsed compression method is incorrect",
            CompressionMethod.STORE,
            cfh.compressionMethod
        )
        assertEquals(
            "Parsed bit flags is incorrect",
            ZipConstants.BitFlags.UTF8_NAMES,
            cfh.bitFlags
        )
        assertFalse("Parsed bit flags is incorrect", cfh.isDataDescriptor)
        assertTrue("Parsed file name is incorrect", cfh.isDirectory)

        // These values should be null without extra fields
        assertNull("Non-null access time without extra fields specified", cfh.lastAccessTime)
        assertNull("Non-null created time without extra fields specified", cfh.creationTime)
    }

    @Test
    fun parseRecord_buffer_isIncomplete() {
        val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(ZipConstants.Signatures.CENTRAL_FILE_HEADER)
            .order(ByteOrder.BIG_ENDIAN)
            .rewind() as ByteBuffer

        try {
            CentralFileHeaderRecord.from(buffer)
            assertFalse("Should have thrown an exception!", true)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertNotNull(exception.cause)
            assertTrue(exception.cause is BufferUnderflowException)
        }
    }

    @Test
    fun parseRecord_unexpectedException() {
        try {
            CentralFileHeaderRecord.from(mockBuffer)
            assertFalse("Should have thrown an exception!", true)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertNotNull(exception.cause)
            assertTrue(exception.cause is RuntimeException)
        }
    }

    @Test
    fun parseRecord_signature_isIncorrect() {
        val buffer = createFileBuffer(signature = 1245)
        try {
            CentralFileHeaderRecord.from(buffer)
            assertFalse("Should have thrown an exception!", true)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertThat(exception.message, containsString("signature"))
        }
    }

    @Test
    fun parseRecord_versionNeeded_isIncorrect() {
        val buffer = createFileBuffer(versionNeeded = 1)
        try {
            CentralFileHeaderRecord.from(buffer)
            assertFalse("Should have thrown an exception!", true)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertThat(exception.message, containsString("version needed"))
        }
    }

    @Test
    fun parseRecord_bitFlags_unsupported() {
        // All unsupported flags
        var buffer = createFileBuffer(
            bitFlags = ZipConstants.BitFlags.UNSUPPORTED_FLAGS_MASK.toShort()
        )
        try {
            CentralFileHeaderRecord.from(buffer)
            assertFalse("Should have thrown an exception!", true)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertThat(exception.message, containsString("bit flags"))
        }

        // One random unsupported flag with valid flags
        val bitFlags: Short = (
            ZipConstants.BitFlags.DATA_DESCRIPTOR or
                ZipConstants.BitFlags.UNSUPPORTED_12
            ).toShort()
        buffer = createFileBuffer(bitFlags = bitFlags)
        try {
            CentralFileHeaderRecord.from(buffer)
            assertFalse("Should have thrown an exception!", true)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertThat(exception.message, containsString("bit flags"))
        }
    }

    @Test
    fun parseRecord_compressionMethod_isUnsupported() {
        val buffer = createFileBuffer(compressionMethod = 9)
        try {
            CentralFileHeaderRecord.from(buffer)
            assertFalse("Should have thrown an exception!", true)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertThat(exception.message, containsString("compression method"))
        }
    }

    @Test
    fun parseRecord_crc32_isIncorrect() {
        val buffer = createFileBuffer(crc32 = 0)
        try {
            CentralFileHeaderRecord.from(buffer)
            assertFalse("Should have thrown an exception!", true)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertThat(exception.message, containsString("CRC32"))
        }
    }

    @Test
    fun parseRecord_fileName_isIncorrect() {
        val buffer = createFileBuffer(fileName = "")
        try {
            CentralFileHeaderRecord.from(buffer)
            assertFalse("Should have thrown an exception!", true)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertThat(exception.message, containsString("file name length"))
        }
    }

    @Test
    fun parseRecord_startDisk_isIncorrect() {
        val buffer = createFileBuffer(startDisk = 1)
        try {
            CentralFileHeaderRecord.from(buffer)
            assertFalse("Should have thrown an exception!", true)
        } catch (exception: ZipException) {
            assertTrue(exception is RecordException)
            assertThat(exception.message, containsString("start disk"))
        }
    }

    @Test
    fun parseRecord_extendedTimeInfo() {
        val extTimeInfo = ExtendedTimestampInfoRecord(
            lastAccessTime = TEST_LAST_ACCESS_TIME,
            lastModifiedTime = TEST_EXT_LAST_MODIFIED_TIME,
            creationTime = TEST_CREATION_TIME
        )

        val buffer = createFileBuffer(extraFields = arrayOf(extTimeInfo))
        val cfh = CentralFileHeaderRecord.from(buffer)

        assertTrue(
            "Parsed extended last modified time is incorrect",
            TestUtil.dateAlmostEquals(TEST_EXT_LAST_MODIFIED_TIME, cfh.lastModifiedTime)
        )
        assertTrue(
            "Parsed last access time is incorrect",
            TestUtil.dateAlmostEquals(TEST_LAST_ACCESS_TIME, cfh.lastAccessTime!!)
        )
        assertTrue(
            "Parsed creation time is incorrect",
            TestUtil.dateAlmostEquals(TEST_CREATION_TIME, cfh.creationTime!!)
        )
    }

    @Test
    fun parseRecord_zip64ExtendedInfo() {
        val zip64ExtInfo = Zip64ExtendedInfoRecord(
            compressedFileSize = TEST_EXT_COMPRESSED_SIZE,
            uncompressedFileSize = TEST_EXT_UNCOMPRESSED_SIZE,
            localHeaderOffset = TEST_EXT_LFH_OFFSET
        )

        val buffer = createFileBuffer(extraFields = arrayOf(zip64ExtInfo))
        val cfh = CentralFileHeaderRecord.from(buffer)

        assertEquals(
            "Parsed extended compressed file size is incorrect",
            TEST_EXT_COMPRESSED_SIZE,
            cfh.compressedSize
        )
        assertEquals(
            "Parsed extended uncompressed file size is incorrect",
            TEST_EXT_UNCOMPRESSED_SIZE,
            cfh.uncompressedSize
        )
        assertEquals(
            "Parsed extended local file header offset is incorrect",
            TEST_EXT_LFH_OFFSET,
            cfh.localFileHeaderOffset
        )
    }

    private fun createFileBuffer(
        signature: Int = ZipConstants.Signatures.CENTRAL_FILE_HEADER,
        versionMadeBy: Short = 1234,
        versionNeeded: Short = ZipConstants.Versions.ZIP64_FORMAT_EXTENSION.toShort(),
        bitFlags: Short = (
            ZipConstants.BitFlags.DATA_DESCRIPTOR or
                ZipConstants.BitFlags.UTF8_NAMES
            ).toShort(),
        compressionMethod: Short = 8,
        lastModifiedTime: Int = ZipUtils.javaToDosTime(TEST_LAST_MODIFIED_TIME),
        crc32: Int = TEST_CRC32,
        compressedSize: Int = TEST_COMPRESSED_SIZE,
        uncompressedSize: Int = TEST_UNCOMPRESSED_SIZE,
        fileName: String = TEST_FILE_NAME,
        extraFields: Array<ZipExtraField>? = null,
        fileComment: String? = null,
        startDisk: Short = 0,
        internalFileAttributes: Short = 0,
        externalFileAttributes: Int = 0,
        lfhOffset: Int = TEST_LFH_OFFSET
    ): ByteBuffer {
        var size = 50 + fileName.length + (fileComment?.length?.toShort() ?: 0)
        val extraFieldSize: Short = extraFields?.map { it.size }
            ?.map { (it + (Short.SIZE_BYTES * 2)).toShort() }
            ?.reduce { acc, sh -> (acc + sh).toShort() } ?: 0

        size += extraFieldSize

        val buffer = ByteBuffer.allocate(size)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(signature)
            .putShort(versionMadeBy)
            .putShort(versionNeeded)
            .putShort(bitFlags)
            .putShort(compressionMethod)
            .putInt(lastModifiedTime)
            .putInt(crc32)
            .putInt(compressedSize)
            .putInt(uncompressedSize)
            .putShort(fileName.length.toShort())
            .putShort(extraFieldSize)
            .putShort(fileComment?.length?.toShort() ?: 0)
            .putShort(startDisk)
            .putShort(internalFileAttributes)
            .putInt(externalFileAttributes)
            .putInt(lfhOffset)
            .put(fileName.toByteArray(Charsets.UTF_8))

        extraFields?.forEach { extraField ->
            buffer.put(extraField.serialize())
        }

        fileComment?.let { buffer.put(it.toByteArray(Charsets.UTF_8)) }

        return buffer.rewind() as ByteBuffer
    }

    @Test
    fun parseRecord_fileComment() {
        val buffer = createFileBuffer(fileComment = TEST_FILE_COMMENT)
        val cfh = CentralFileHeaderRecord.from(buffer)
        assertEquals("Parsed file comment is incorrect", TEST_FILE_COMMENT, cfh.fileComment)
    }

    private fun createDirectoryBuffer(
        signature: Int = ZipConstants.Signatures.CENTRAL_FILE_HEADER,
        versionMadeBy: Short = 1234,
        versionNeeded: Short = ZipConstants.Versions.ZIP64_FORMAT_EXTENSION.toShort(),
        bitFlags: Short = (ZipConstants.BitFlags.UTF8_NAMES).toShort(),
        compressionMethod: Short = 0,
        lastModifiedTime: Int = ZipUtils.javaToDosTime(TEST_LAST_MODIFIED_TIME),
        crc32: Int = 0,
        compressedSize: Int = 0,
        uncompressedSize: Int = 0,
        fileName: String = TEST_DIR_NAME,
        extraFields: Array<ZipExtraField>? = null,
        fileComment: String? = null,
        startDisk: Short = 0,
        internalFileAttributes: Short = 0,
        externalFileAttributes: Int = 0,
        lfhOffset: Int = 0
    ): ByteBuffer {
        return createFileBuffer(
            signature, versionMadeBy, versionNeeded, bitFlags, compressionMethod, lastModifiedTime,
            crc32, compressedSize, uncompressedSize, fileName, extraFields, fileComment, startDisk,
            internalFileAttributes, externalFileAttributes, lfhOffset
        )
    }

    private fun extendedTime(date: Date): Long {
        val time = date.time / 1000
        return time * 1000
    }
}
