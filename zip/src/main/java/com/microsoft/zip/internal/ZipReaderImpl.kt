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

package com.microsoft.zip.internal

import com.microsoft.zip.ZipException
import com.microsoft.zip.ZipFileMetadata
import com.microsoft.zip.ZipReader
import com.microsoft.zip.internal.cache.Cache
import com.microsoft.zip.internal.input.ByteRange
import com.microsoft.zip.internal.input.ByteRanges
import com.microsoft.zip.internal.input.RandomAccessInput
import com.microsoft.zip.internal.records.CentralFileHeaderRecord
import com.microsoft.zip.internal.records.EndOfCentralDirectoryRecord
import com.microsoft.zip.internal.records.LocalFileHeaderRecord
import com.microsoft.zip.internal.records.Zip32EndOfCentralDirectoryRecord
import com.microsoft.zip.internal.records.Zip64EndOfCentralDirectoryLocator
import com.microsoft.zip.internal.records.Zip64EndOfCentralDirectoryRecord
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

/**
 * Implementation of the [ZipReader] interface.
 */
internal class ZipReaderImpl
@Throws(ZipException::class)
constructor(
    private val input: RandomAccessInput,
    private val cache: Cache
) : ZipReader {
    private val centralDirectory: Map<String, CentralFileHeaderRecord>

    init {
        val mutableCentralDirectory = mutableMapOf<String, CentralFileHeaderRecord>()

        val size =
            ZipConstants.Sizes.ZIP32_END_OF_CENTRAL_DIRECTORY +
                ZipConstants.Sizes.ZIP64_END_OF_CENTRAL_DIRECTORY_LOCATOR
        var buffer = input.readLast(size.toULong())
        var eocd: EndOfCentralDirectoryRecord = Zip32EndOfCentralDirectoryRecord
            .from(
                buffer.position(ZipConstants.Sizes.ZIP64_END_OF_CENTRAL_DIRECTORY_LOCATOR)
                    as ByteBuffer
            )

        if (eocd.isZip64) {
            val locatorBuffer = Zip64EndOfCentralDirectoryLocator
                .from(buffer.position(0) as ByteBuffer)
            val eocdByteRange = ByteRange(
                locatorBuffer.endOfCentralDirectoryOffset,
                ZipConstants.Sizes.ZIP64_END_OF_CENTRAL_DIRECTORY.toULong()
            )

            buffer = input.readFully(eocdByteRange)
            eocd = Zip64EndOfCentralDirectoryRecord.from(buffer)
        }

        val cdByteRange = ByteRange(eocd.centralDirectoryOffset, eocd.centralDirectorySize)

        buffer = input.readFully(cdByteRange)

        for (i in 0.toULong() until eocd.centralDirectoryEntries) {
            val fileHeaderRecord = CentralFileHeaderRecord.from(buffer)
            mutableCentralDirectory[fileHeaderRecord.fileName] = fileHeaderRecord
        }

        centralDirectory = mutableCentralDirectory
    }

    override val files by lazy {
        centralDirectory.keys.toList()
    }

    override fun fileStream(name: String): InputStream {
        cacheFiles(listOf(name))
        return cache.get(name)!!
    }

    override fun fileStreams(names: List<String>): Map<String, InputStream> {
        cacheFiles(names)
        return names.map { it to cache.get(it)!! }.toMap()
    }

    override fun fileMetadata(name: String): ZipFileMetadata {
        val fileEntry = checkFileExists(name)

        return ZipFileMetadata(
            name = fileEntry.fileName,
            lastModifiedTime = fileEntry.lastModifiedTime,
            lastAccessTime = fileEntry.lastAccessTime,
            creationTime = fileEntry.creationTime,
            compressedSize = fileEntry.compressedSize.toLong(),
            uncompressedSize = fileEntry.uncompressedSize.toLong(),
            crc32 = fileEntry.crc32.toLong()
        )
    }

    override fun cacheFiles(names: List<String>) {
        require(names.isNotEmpty()) {
            "Empty file names"
        }

        val cfhRecords = names.map { name -> checkFileExists(name) }
            .filter { cfhRecord -> !cache.contains(cfhRecord.fileName) }

        val byteRanges = ByteRanges(cfhRecords)
        val multipartBody = input.readFully(byteRanges)

        multipartBody.parts.forEach { part ->
            val lfh = LocalFileHeaderRecord.from(part.bytes, centralDirectory)
            val compressedBytes = ByteArray(lfh.compressedSize.toInt())

            part.bytes.get(compressedBytes)

            val inputStream = when (lfh.compressionMethod) {
                CompressionMethod.STORE -> ByteArrayInputStream(compressedBytes)

                CompressionMethod.DEFLATE -> InflaterInputStream(
                    ByteArrayInputStream(compressedBytes),
                    Inflater(true),
                    compressedBytes.size
                )
            }

            cache.put(lfh.fileName, inputStream)
        }
    }

    override fun close() {
        cache.clear()
        input.close()
    }

    /**
     * Checks if a file is in the archive.
     *
     * @param name File name to check for
     * @return [CentralFileHeaderRecord] of the file
     *
     * @throws [FileNotFoundException] if the file is not in the archive
     */
    @Throws(FileNotFoundException::class)
    private fun checkFileExists(name: String): CentralFileHeaderRecord {
        if (centralDirectory.containsKey(name)) {
            return centralDirectory[name]!!
        }

        throw FileNotFoundException(name)
    }
}
