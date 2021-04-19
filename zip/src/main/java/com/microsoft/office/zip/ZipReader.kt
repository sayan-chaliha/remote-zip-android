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

package com.microsoft.office.zip

import java.io.Closeable
import java.io.IOException
import java.io.InputStream

/**
 * Interface for reading ZIP files.
 *
 * Implementations of this interface can read and extract all or a subset of files from a ZIP
 * archive from either on from a [java.io.File] on disk or a [java.net.URL]. Remote ZIP files
 * from a [java.net.URL] must be hosted on a server that supports
 * [HTTP Range Requests](https://developer.mozilla.org/en-US/docs/Web/HTTP/Range_requests).
 */
interface ZipReader : Closeable {

    /**
     * List of files in the ZIP archive. Accessing this list doesn't cause the files to be extracted
     * or downloaded from the Internet (in case of a remote ZIP file).
     */
    val files: List<String>

    /**
     * Creates an [InputStream] of a file from the ZIP archive. The stream is uncompressed data of
     * the file contents. Causes the file to be downloaded and cached locally if the ZIP file is
     * remote.
     *
     * @param name Name of the file to extract
     * @return [InputStream] of file contents
     *
     * @throws [IOException]
     * @throws [ZipException]
     */
    @Throws(IOException::class)
    fun fileStream(name: String): InputStream

    /**
     * Creates [InputStream]s of a files from the ZIP archive. Streams are uncompressed data of
     * the files' contents. Causes all files in the list to be downloaded and cached locally if the
     * ZIP file is remote.
     *
     * @param names Names of files to extract
     * @return [Map] of file names to [InputStream]s for all files requested
     *
     * @throws [IOException]
     * @throws [ZipException]
     */
    @Throws(IOException::class)
    fun fileStreams(names: List<String>): Map<String, InputStream>

    /**
     * Retrieves [ZipFileMetadata] of a file in the archive.
     *
     * @param name Name of file to retrieve metadata for
     * @return [ZipFileMetadata]
     *
     * @throws [IOException]
     * @throws [ZipException]
     */
    @Throws(IOException::class)
    fun fileMetadata(name: String): ZipFileMetadata

    /**
     * Caches files locally causing them to be downloaded (in a single HTTP request) and extracted.
     * Subsequent calls to [fileStream] or [fileStreams] will make use of the local cache.
     *
     * @param names Names of files to cache
     *
     * @throws [IOException]
     * @throws [ZipException]
     */
    @Throws(IOException::class)
    fun cacheFiles(names: List<String>)
}
