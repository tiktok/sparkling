// Copyright 2025 TikTok Inc.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.media.utils

import java.security.MessageDigest

object Md5Utils {
    private val hexDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

    /**
     * @param string String
     * @return String?
     */
    fun hexDigest(string: String): String? {
        var res: String? = null
        try {
            res = hexDigest(string.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res
    }

    private fun hexDigest(bytes: ByteArray): String? {
        var res: String? = null
        try {
            val messageDigest = MessageDigest.getInstance("MD5")
            messageDigest.update(bytes)
            val tmp: ByteArray = messageDigest.digest()
            val charStr = CharArray(32)
            var k = 0
            for (i in 0 until 16) {
                val b = tmp[i]
                charStr[k++] = hexDigits[b.toInt() ushr 4 and 15]
                charStr[k++] = hexDigits[b.toInt() and 15]
            }
            res = charStr.toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res
    }
}
