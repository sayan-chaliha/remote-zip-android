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

import java.io.Closeable
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Input interface to [ZipReaderImpl] -- the interface provides random access to the ZIP file.
 *
 * Implementations use on [java.io.RandomAccessFile] for file-based access and
 * [okhttp3.OkHttpClient] with HTTP Range Requests for remote access.
 */
internal interface RandomAccessInput : Closeable {

    /**
     * Reads a range of bytes from the underlying source.
     *
     * @param byteRange Range of bytes to read
     * @return [ByteBuffer] containing the requested range
     *
     * @throws [IOException]
     */
    @Throws(IOException::class)
    fun readFully(byteRange: ByteRange): ByteBuffer

    /**
     * Reads multiple ranges of bytes from the underlying source.
     *
     * @param byteRanges A list of [ByteRange]s
     * @return [MultipartBody] with parts corresponding to each byte range requested
     *
     * @throws [IOException]
     */
    @Throws(IOException::class)
    fun readFully(byteRanges: ByteRanges): MultipartBody

    /**
     * Reads the last [length] bytes of data from the underlying source.
     *
     * @param length Length of bytes to read from the end of the underlying source
     * @return [ByteBuffer] containing the requested range
     *
     * @throws [IOException]
     */
    @Throws(IOException::class)
    fun readLast(length: ULong): ByteBuffer
}
