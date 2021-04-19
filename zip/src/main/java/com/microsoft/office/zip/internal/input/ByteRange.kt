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

package com.microsoft.office.zip.internal.input

import com.microsoft.office.zip.internal.ZipConstants
import com.microsoft.office.zip.internal.records.CentralFileHeaderRecord

/**
 * Encapsulates a range of bytes derived from [CentralFileHeaderRecord], containing the offset start and end of a file.
 */
internal class ByteRange {
    /**
     * Start offset a file in the ZIP archive including the Local File Header record.
     */
    val start: ULong

    /**
     * End offset of a file in the ZIP archive.
     */
    val end: ULong

    /**
     * Length of the byte range.
     */
    val length: ULong

    constructor(off: ULong, len: ULong) {
        start = off
        end = start + len
        length = end - start
    }

    constructor(centralFileHeader: CentralFileHeaderRecord) {
        start = centralFileHeader.localFileHeaderOffset
        end = start + ZipConstants.Sizes.LOCAL_FILE_HEADER.toULong() +
            centralFileHeader.fileName.length.toULong() +
            centralFileHeader.compressedSize
        length = end - start
    }

    override fun toString() = "$start-$end"
}
