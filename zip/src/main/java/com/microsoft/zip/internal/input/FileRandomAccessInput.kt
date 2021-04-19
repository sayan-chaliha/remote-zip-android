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

package com.microsoft.zip.internal.input

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer

/**
 * Implementation of [RandomAccessInput] with a [RandomAccessFile] source.
 */
internal class FileRandomAccessInput
@Throws(FileNotFoundException::class)
constructor(file: File) : RandomAccessInput {

    private val randomAccessFile: RandomAccessFile = RandomAccessFile(file, "r")

    @Throws(IOException::class)
    override fun readFully(byteRange: ByteRange): ByteBuffer {
        val buffer = ByteArray(byteRange.length.toInt())

        randomAccessFile.seek(byteRange.start.toLong())
        randomAccessFile.readFully(buffer)

        return ByteBuffer.wrap(buffer)
    }

    @Throws(IOException::class)
    override fun readFully(byteRanges: ByteRanges): MultipartBody {
        require(byteRanges.isNotEmpty()) {
            "Empty byte ranges."
        }

        val parts = mutableListOf<MultipartBody.Part>()

        byteRanges.forEach { byteRange ->
            parts.add(MultipartBody.Part(readFully(byteRange)))
        }

        return object : MultipartBody {
            override val parts: List<MultipartBody.Part> = parts
        }
    }

    @Throws(IOException::class)
    override fun readLast(length: ULong): ByteBuffer {
        val offset = randomAccessFile.length() - length.toLong()
        val buffer = ByteArray(length.toInt())

        randomAccessFile.seek(offset)
        randomAccessFile.readFully(buffer)

        return ByteBuffer.wrap(buffer)
    }

    override fun close() {
        randomAccessFile.close()
    }
}
