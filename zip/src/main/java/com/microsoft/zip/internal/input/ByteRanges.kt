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

import com.microsoft.zip.internal.records.CentralFileHeaderRecord

/**
 * List of [ByteRange]s. Utility to help create a HTTP range header from a list of file headers.
 */
internal class ByteRanges() : ArrayList<ByteRange>() {
    /**
     * Creates a list of byte ranges from [CentralFileHeaderRecord]s.
     */
    constructor(fileHeaders: List<CentralFileHeaderRecord>) : this() {
        fileHeaders.forEach { fh -> add(ByteRange(fh)) }
    }

    /**
     * Builds a string in the format "off1-off1end,off2-off2end,..." which can be used
     * directly in an HTTP range request header.
     *
     * @return a byte range string
     */
    override fun toString(): String {
        return this.map { it.toString() }
            .toList()
            .joinToString(",")
    }
}
