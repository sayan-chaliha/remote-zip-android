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

import java.io.InputStream
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Caches [InputStream]s of files in an archive in memory.
 *
 * Only one stream is cached, so if it's used, it would need to be read from the source again.
 * May cause memory consumed to increase based on the number of files cached.
 */
internal class MemoryCache : Cache {
    private val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()
    private val cache: MutableMap<String, InputStream> = mutableMapOf()

    override fun get(name: String): InputStream? = lock.write { cache.remove(name) }
    override fun put(name: String, stream: InputStream) = lock.write { cache[name] = stream }
    override fun contains(name: String): Boolean = lock.read { cache.contains(name) }

    override fun remove(name: String) = lock.write {
        cache.remove(name)
        return@write
    }

    override fun clear() = lock.write { cache.clear() }
}
