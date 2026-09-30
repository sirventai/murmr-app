package uoc.edu.psirvent.murmr.util

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual fun compressAvatar(bytes: ByteArray, maxSidePx: Int, quality: Int): ByteArray {
    return runCatching {
        val nsData = bytes.toNSData()
        val source = UIImage.imageWithData(nsData) ?: return@runCatching bytes
        val srcW = source.size.useContents { width }
        val srcH = source.size.useContents { height }
        val maxSide = maxOf(srcW, srcH)
        val ratio = if (maxSide > 0.0) maxSidePx.toDouble() / maxSide else 1.0
        val dstW = if (ratio < 1.0) srcW * ratio else srcW
        val dstH = if (ratio < 1.0) srcH * ratio else srcH

        UIGraphicsBeginImageContextWithOptions(CGSizeMake(dstW, dstH), false, 1.0)
        source.drawInRect(CGRectMake(0.0, 0.0, dstW, dstH))
        val resized = UIGraphicsGetImageFromCurrentImageContext() ?: return@runCatching bytes
        UIGraphicsEndImageContext()

        val q = quality.coerceIn(1, 100) / 100.0
        val jpegData = UIImageJPEGRepresentation(resized, q) ?: return@runCatching bytes
        jpegData.toByteArray()
    }.getOrDefault(bytes)
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val len = length.toInt()
    if (len == 0) return ByteArray(0)
    val out = ByteArray(len)
    out.usePinned { pinned ->
        memcpy(pinned.addressOf(0), this.bytes, length)
    }
    return out
}
