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

import java.io.File
import java.io.InputStream
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Caches [InputStream]s of files in an archive in temporary files.
 *
 * Uses extra disk space but less memory. Additionally, multiple calls to access file streams
 * will not cause the source to be accessed again.
 */
class FileCache : Cache {
    private val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()
    private val cache: MutableMap<String, File> = mutableMapOf()

    override fun get(name: String): InputStream? = lock.read { cache[name]?.inputStream() }

    override fun put(name: String, stream: InputStream) {
        val tempFile = File.createTempFile("ms-zip-${name.replace("/", "-")}", ".tmp")
        stream.use { inputStream ->
            tempFile.outputStream().use {
                it.buffered().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }

        lock.write {
            cache[name]?.delete()
            cache[name] = tempFile
        }
    }

    override fun contains(name: String): Boolean = lock.read { cache.containsKey(name) }

    override fun remove(name: String) = lock.write {
        // Remove if the temp file is deleted successfully. Otherwise, leave it till the cache
        // is cleared -- best effort to try and delete the file again.
        if (cache.containsKey(name) && cache[name]!!.delete()) {
            cache.remove(name)
        }

        return@write
    }

    override fun clear() = lock.write {
        cache.forEach { (_, file) -> file.delete() }
        cache.clear()
    }
}
