package com.defname.localshare.util

import com.defname.localshare.domain.model.FileInfo

fun Long.sizeAsString(): String {
    var sizeDouble = this.toDouble()
    val units = listOf("Byte", "KB", "MB", "GB", "TB")
    var unitIndex = 0
    while (sizeDouble >= 1024 && unitIndex < units.size - 1) {
        sizeDouble /= 1024
        unitIndex++
    }
    return "%.2f %s".format(sizeDouble, units[unitIndex])
}