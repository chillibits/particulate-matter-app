/*
 * Copyright © Marc Auberer 2017 - 2021. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.shared

import io.ktor.util.sha1
import java.security.MessageDigest
import java.util.*
import kotlin.text.toCharArray

val HEX_CHARS = "0123456789ABCDEF".toCharArray()

private fun printHexBinary(data: ByteArray): String {
    val r = StringBuilder(data.size * 2)
    data.forEach { b ->
        val i = b.toInt()
        r.append(HEX_CHARS[i shr 4 and 0xF])
        r.append(HEX_CHARS[i and 0xF])
    }
    return r.toString()
}

fun hashSha1(input: String): String {
    return printHexBinary(sha1(input.toByteArray())).toLowerCase(Locale.getDefault())
}

fun hashSha256(input: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(input.toByteArray())
    return digest.fold("", { str, it -> str + "%02x".format(it) })
}