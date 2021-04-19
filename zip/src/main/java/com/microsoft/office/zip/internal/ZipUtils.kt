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

import java.util.Calendar
import java.util.Date

/**
 * Utility functions.
 */
internal object ZipUtils {
    /**
     * Checks if a ZIP header field value indicates that the actual value is specified
     * elsewhere.
     *
     * @param T Integral type
     * @param value ZIP header field value
     * @return `true` if value is elsewhere, `false` otherwise
     */
    inline fun <reified T> valueInExtendedInfo(value: T): Boolean {
        return when (T::class) {
            UShort::class -> value == UShort.MAX_VALUE
            UInt::class -> value == UInt.MAX_VALUE
            ULong::class -> value == ULong.MAX_VALUE
            Short::class -> value == Short.MAX_VALUE
            Int::class -> value == Int.MAX_VALUE
            Long::class -> value == Long.MAX_VALUE
            else -> false
        }
    }

    /**
     * Converts MS DOS formatted date and time values to Java [Date].
     *
     * @param dosTime Time in MS DOS format
     * @return Java [Date] corresponding to the MS DOS data and time
     */
    fun dosToJavaTime(dosTime: Int): Date {
        val calendar = Calendar.getInstance()

        calendar.set(Calendar.YEAR, ((dosTime shr 25) and 0x7F) + 1980)
        calendar.set(Calendar.MONTH, ((dosTime shr 21) and 0x0F) - 1)
        calendar.set(Calendar.DATE, ((dosTime shr 16) and 0x1F))
        calendar.set(Calendar.HOUR_OF_DAY, ((dosTime shr 11) and 0x1F))
        calendar.set(Calendar.MINUTE, ((dosTime shr 5) and 0x3F))
        calendar.set(Calendar.SECOND, ((dosTime shl 1) and 0x3E))
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar.time
    }

    /**
     * Converts a [Date] to MS DOS formatted date and time values.
     *
     * @param javaTime Date/time in Java format
     * @return [Int] representing date/time in MS DOS format
     */
    fun javaToDosTime(javaTime: Date): Int {
        val calendar = Calendar.getInstance()
        calendar.time = javaTime

        val year = calendar.get(Calendar.YEAR) - 1980
        val month = calendar.get(Calendar.MONTH) + 1
        val date = calendar.get(Calendar.DATE)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND) / 2

        return (year shl 25) or
            (month shl 21) or
            (date shl 16) or
            (hour shl 11) or
            (minute shl 5) or
            second
    }
}
