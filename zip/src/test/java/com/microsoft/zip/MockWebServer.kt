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

import com.microsoft.zip.internal.input.ByteRange
import com.microsoft.zip.internal.input.FileRandomAccessInput
import java.io.File
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer

internal class FileDispatcher : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
        if (request.path != null) {
            val path = request.path!!
            if (path.startsWith("/file")) {
                val fileName = path.substring(path.lastIndexOf("/") + 1)
                val file = Resources.file(fileName)
                val input = FileRandomAccessInput(file)

                val rangeHeader = request.getHeader("Range")
                    ?: return MockResponse()
                        .setStatus("HTTP/1.1 400 Bad Request")
                        .setBody("Expected range request")

                // Range header format: bytes=<range spec1>,<range spec2>,...
                val rangeSpecs = rangeHeader
                    .substring(rangeHeader.lastIndexOf("=") + 1)
                val ranges = rangeSpecs.split(",")

                // Single range isn't a multipart request
                if (ranges.size == 1) {
                    val buffer = Buffer()
                    buffer.write(input.readFully(parseRange(ranges[0], file)))

                    return MockResponse()
                        .setStatus("HTTP/1.1 200 OK")
                        .setHeader("Content-Type", "application/octetstream")
                        .setBody(buffer)
                } else {
                    val multipartBodyBuilder = MultipartBody.Builder()

                    ranges.forEach {
                        val buffer = input.readFully(parseRange(it, file))
                        val requestBody = buffer.array()
                            .toRequestBody("application/octetstream".toMediaType())
                        val part = MultipartBody.Part.create(requestBody)
                        multipartBodyBuilder.addPart(part)
                    }

                    val multipartBody = multipartBodyBuilder.build()
                    val buffer = Buffer()

                    multipartBody.writeTo(buffer)

                    return MockResponse()
                        .setStatus("HTTP/1.1 202 Partial")
                        .setHeader(
                            "Content-Type",
                            "multipart/byteranges;boundary=${multipartBody.boundary}"
                        )
                        .setBody(buffer)
                }
            }
        }

        return MockResponse()
            .setStatus("HTTP/1.1 404 Not Found")
    }

    private fun parseRange(range: String, file: File): ByteRange {
        return if (range.startsWith("-")) {
            val bytes = range.substring(1).toULong()
            ByteRange(file.length().toULong() - bytes, bytes)
        } else {
            val idx = range.lastIndexOf("-")
            val start = range.substring(0, idx).toULong()
            val end = range.substring(idx + 1).toULong()
            ByteRange(start, end - start)
        }
    }
}
