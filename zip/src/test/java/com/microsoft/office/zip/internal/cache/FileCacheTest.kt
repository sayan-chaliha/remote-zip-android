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

package com.microsoft.office.zip.internal.cache

import java.io.ByteArrayInputStream
import java.io.InputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class FileCacheTest {
    companion object {
        private const val TEST_NAME1 = "test1"
        private const val TEST_BYTES1 = "Random Bytes1"
        private val TEST_INPUT_STREAM1: InputStream
            get() = ByteArrayInputStream(TEST_BYTES1.toByteArray())

        private const val TEST_NAME2 = "test2"
        private const val TEST_BYTES2 = "Random Bytes2"
        private val TEST_INPUT_STREAM2: InputStream
            get() = ByteArrayInputStream(TEST_BYTES2.toByteArray())
    }

    private val cache: Cache = FileCache()

    @Test
    fun cache_works() {
        var inputStream: InputStream? = TEST_INPUT_STREAM1
        cache.put(TEST_NAME1, inputStream!!)
        assertTrue("Cache put failed", cache.contains(TEST_NAME1))

        // FileCache drains the input stream; unlike MemoryCache
        assertEquals("Cache put failed; input stream not drained", 0, inputStream.available())

        inputStream = cache.get(TEST_NAME1)
        assertNotNull("Cache get failed", inputStream)
        assertEquals(
            "Cache get returned bad data",
            TEST_BYTES1,
            inputStream!!.bufferedReader().readLine()
        )
        assertTrue("File cache semantics failed", cache.contains(TEST_NAME1))
    }

    @Test
    fun cache_get_nonExistentKey() {
        assertNull(cache.get("RandomKey"))
    }

    @Test
    fun cache_remove_works() {
        cache.put(TEST_NAME1, TEST_INPUT_STREAM1)
        assertTrue("Cache put failed", cache.contains(TEST_NAME1))

        cache.remove(TEST_NAME1)
        assertFalse("Cache remove failed", cache.contains(TEST_NAME1))

        // Check if removing non-existing name is a no-op
        cache.remove(TEST_NAME1)
    }

    @Test
    fun cache_replaces_entries() {
        cache.put(TEST_NAME1, TEST_INPUT_STREAM1)
        assertTrue("Cache put failed", cache.contains(TEST_NAME1))

        cache.put(TEST_NAME1, TEST_INPUT_STREAM2)
        assertTrue("Cache put failed", cache.contains(TEST_NAME1))

        val inputStream = cache.get(TEST_NAME1)
        assertNotNull("Cache get failed", inputStream)
        assertEquals(
            "Cache get returned bad data",
            TEST_BYTES2,
            inputStream!!.bufferedReader().readLine()
        )
    }

    @Test
    fun cache_clear_works() {
        cache.put(TEST_NAME1, TEST_INPUT_STREAM1)
        assertTrue("Cache put failed", cache.contains(TEST_NAME1))

        cache.put(TEST_NAME2, TEST_INPUT_STREAM2)
        assertTrue("Cache put failed", cache.contains(TEST_NAME2))

        cache.clear()
        assertFalse("Cache clear failed", cache.contains(TEST_NAME1))
        assertFalse("Cache clear failed", cache.contains(TEST_NAME2))
    }
}
