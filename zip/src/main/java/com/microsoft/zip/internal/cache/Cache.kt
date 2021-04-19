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

package com.microsoft.zip.internal.cache

import java.io.IOException
import java.io.InputStream

/**
 * Cache interface for caching file streams.
 */
internal interface Cache {
    /**
     * Checks if a file is cached.
     *
     * @param name File name to check for
     * @return Cached [InputStream], or `null`
     */
    fun get(name: String): InputStream?

    /**
     * Caches a file stream.
     *
     * @param name File name to cache
     * @param stream [InputStream] of file
     */
    @Throws(IOException::class)
    fun put(name: String, stream: InputStream)

    /**
     * Checks if a file is cached.
     *
     * @param name Name of file to check for
     * @return `true` if file is cached, `false` otherwise
     */
    fun contains(name: String): Boolean

    /**
     * Removes a cached file stream.
     *
     * @param name Cached file to remove
     */
    fun remove(name: String)

    /**
     * Clears all cached files.
     */
    fun clear()
}