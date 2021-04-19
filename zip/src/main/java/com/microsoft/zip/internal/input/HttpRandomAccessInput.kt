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

import java.io.IOException
import java.net.URL
import java.nio.ByteBuffer
import okhttp3.Call
import okhttp3.MultipartReader
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.internal.http.promisesBody

/**
 * Implementation of [RandomAccessInput] with a remote [URL] backing. The server hosting the URL
 * must support HTTP range requests for this to work. The code itself however does no explicit
 * checks for range request support.
 */
internal class HttpRandomAccessInput(
    private val url: URL,
    private val callFactory: Call.Factory
) : RandomAccessInput {

    override fun readFully(byteRange: ByteRange): ByteBuffer {
        return makeHttpRangeRequest(byteRange.toString()) { response ->
            response.byteStream().use { inputStream ->
                inputStream.buffered().use { bufferedInputStream ->
                    return@makeHttpRangeRequest ByteBuffer.wrap(bufferedInputStream.readBytes())
                }
            }
        }
    }

    override fun readFully(byteRanges: ByteRanges): MultipartBody {
        require(byteRanges.isNotEmpty()) {
            "Empty byte ranges."
        }

        if (byteRanges.size == 1) {
            val byteBuffer = readFully(byteRanges[0])
            return object : MultipartBody {
                override val parts: List<MultipartBody.Part> =
                    listOf(MultipartBody.Part(byteBuffer))
            }
        }

        val parts: MutableList<MultipartBody.Part> = mutableListOf()
        makeHttpRangeRequest(byteRanges.toString()) { response ->
            MultipartReader(response).use { reader ->
                while (true) {
                    val part = reader.nextPart() ?: break
                    part.use {
                        parts.add(MultipartBody.Part(ByteBuffer.wrap(it.body.readByteArray())))
                    }
                }
            }
        }

        return object : MultipartBody {
            override val parts: List<MultipartBody.Part> = parts
        }
    }

    override fun readLast(length: ULong): ByteBuffer {
        return makeHttpRangeRequest("-$length") { response ->
            response.byteStream().use { inputStream ->
                inputStream.buffered().use { bufferedInputStream ->
                    return@makeHttpRangeRequest ByteBuffer.wrap(bufferedInputStream.readBytes())
                }
            }
        }
    }

    override fun close() {
    }

    private fun <T> makeHttpRangeRequest(range: String, block: (ResponseBody) -> T): T {
        val request = Request.Builder()
            .get()
            .url(url)
            .header("Range", "bytes=$range")
            .build()

        callFactory.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException(response.message)
            } else if (!response.promisesBody()) {
                throw IOException("Empty response body.")
            }

            return block(response.body!!)
        }
    }
}
