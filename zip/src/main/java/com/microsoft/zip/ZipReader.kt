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

package com.microsoft.zip

import com.microsoft.zip.internal.ZipReaderImpl
import com.microsoft.zip.internal.cache.Cache
import com.microsoft.zip.internal.cache.FileCache
import com.microsoft.zip.internal.cache.MemoryCache
import com.microsoft.zip.internal.input.FileRandomAccessInput
import com.microsoft.zip.internal.input.HttpRandomAccessInput
import com.microsoft.zip.internal.input.RandomAccessInput
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import okhttp3.OkHttpClient

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

    /**
     * Builder for configuring a new [ZipReader].
     */
    class Builder {
        private var file: File? = null
        private var url: URL? = null
        private var useMemoryCache = false

        /**
         * Sets the input ZIP [File].
         */
        fun file(file: File) = apply { this.file = file }

        /**
         * Sets the input ZIP [URL].
         */
        fun url(url: URL) = apply { this.url = url }

        /**
         * Enables caching in memory.
         *
         * @see [MemoryCache]
         */
        fun useMemoryCache() = apply { this.useMemoryCache = true }

        /**
         * Enables file-based caching. Useful if file streams from a remote archive are accessed
         * multiple times. This is the default behavior.
         *
         * @see [FileCache]
         */
        fun useFileCache() = apply { this.useMemoryCache = false }

        /**
         * Creates a new [ZipReader] from the configuration provided.
         *
         * @return [ZipReader]
         */
        fun build(): ZipReader {
            require(file != null || url != null) {
                "Either file or URL input must be specified."
            }

            val cache: Cache = if (useMemoryCache) MemoryCache() else FileCache()
            val input: RandomAccessInput = if (file != null) {
                FileRandomAccessInput(file!!)
            } else {
                val okHttpClient = OkHttpClient.Builder()
                    .build()
                HttpRandomAccessInput(url!!, okHttpClient)
            }

            return ZipReaderImpl(
                input = input,
                cache = cache
            )
        }
    }
}
