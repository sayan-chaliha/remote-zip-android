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

package com.microsoft.office.zip.internal

/**
 * Constants.
 */
internal object ZipConstants {
    object Signatures {
        const val LOCAL_FILE_HEADER = 0x04034b50
        const val DATA_DESCRIPTOR = 0x08074b50
        const val CENTRAL_FILE_HEADER = 0x02014b50
        const val ZIP64_END_OF_CENTRAL_DIRECTORY = 0x06064b50
        const val ZIP64_END_OF_CENTRAL_DIRECTORY_LOCATOR = 0x07064b50
        const val ZIP32_END_OF_CENTRAL_DIRECTORY = 0x06054b50
    }

    object Headers {
        const val ZIP64_EXTENDED_INFORMATION: Short = 0x0001
        const val EXTENDED_TIMESTAMP_INFORMATION: Short = 0x5455
    }

    object Sizes {
        const val ZIP32_END_OF_CENTRAL_DIRECTORY = 22
        const val ZIP64_END_OF_CENTRAL_DIRECTORY_LOCATOR = 20
        const val ZIP64_END_OF_CENTRAL_DIRECTORY = 56
        const val ZIP64_EXTENDED_INFORMATION = 32
        const val LOCAL_FILE_HEADER = 30
    }

    object Versions {
        const val ZIP32_DEFAULT_VERSION = 20
        const val ZIP64_FORMAT_EXTENSION = 45
    }

    object FileAttributes {
        const val DIRECTORY: UInt = 0x0010u
        const val HIDDEN: UInt = 0x0002u
    }

    object BitFlags {
        // Bit 0: If set indicates that the file is encrypted.
        const val UNSUPPORTED_0: UShort = 0x0001u

        // Maximum compression (-exx/-ex) otherwise normal compression (-en)
        const val DEFLATE_MAX_COMPRESS: UShort = 0x0002u

        // Fast (-ef) if Max+Fast then SuperFast (-es) compression
        const val DEFLATE_FAST_COMPRESS: UShort = 0x0004u

        // the field's crc-32 compressed and uncompressed sizes = 0 in the local header
        // the correct values are put in the data descriptor immediately following the
        // compressed data.
        const val DATA_DESCRIPTOR: UShort = 0x0008u

        const val ENHANCED_DEFLATE: UShort = 0x0010u
        const val COMPRESSED_PATCHED_DATA: UShort = 0x0020u

        // Indicates that names are UTF-8 encoded
        const val UTF8_NAMES: UShort = 0x0800u

        // Strong encryption.
        const val UNSUPPORTED_6: UShort = 0x0040u

        // Currently unused
        const val UNUSED_7: UShort = 0x0080u
        const val UNUSED_8: UShort = 0x0100u
        const val UNUSED_9: UShort = 0x0200u
        const val UNUSED_10: UShort = 0x0400u

        // Language encoding flag (EFS).  File name and comments fields MUST be encoded UTF-8
        const val ENCODING_MUST_USE_UTF8: UShort = 0x0800u

        // Reserved by PKWARE for enhanced compression
        const val UNSUPPORTED_12: UShort = 0x1000u

        // Set when encrypting the Central Directory
        const val UNSUPPORTED_13: UShort = 0x2000u

        // Reserved by PKWARE
        const val UNSUPPORTED_14: UShort = 0x4000u

        // Reserved by PKWARE
        const val UNSUPPORTED_15: UShort = 0x8000u

        val UNSUPPORTED_FLAGS_MASK: UShort = UNSUPPORTED_0
            .or(UNSUPPORTED_6)
            .or(UNSUPPORTED_12)
            .or(UNSUPPORTED_13)
            .or(UNSUPPORTED_14)
            .or(UNSUPPORTED_15)
    }
}
