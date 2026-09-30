package uoc.edu.psirvent.murmr.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

actual fun compressAvatar(bytes: ByteArray, maxSidePx: Int, quality: Int): ByteArray {
    return runCatching {

        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        val srcW = opts.outWidth
        val srcH = opts.outHeight
        if (srcW <= 0 || srcH <= 0) return@runCatching bytes

        var sample = 1
        while (srcW / (sample * 2) >= maxSidePx && srcH / (sample * 2) >= maxSidePx) {
            sample *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        val sampled = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
            ?: return@runCatching bytes

        val orientation = runCatching {
            ExifInterface(ByteArrayInputStream(bytes))
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val oriented = applyExifOrientation(sampled, orientation)

        val ratio = maxSidePx.toFloat() / maxOf(oriented.width, oriented.height)
        val scaled = if (ratio < 1f) {
            val m = Matrix().apply { postScale(ratio, ratio) }
            Bitmap.createBitmap(oriented, 0, 0, oriented.width, oriented.height, m, true)
                .also { if (it !== oriented) oriented.recycle() }
        } else {
            oriented
        }

        ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            scaled.recycle()
            out.toByteArray()
        }
    }.getOrDefault(bytes)
}

private fun applyExifOrientation(src: Bitmap, orientation: Int): Bitmap {
    val m = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(270f); m.postScale(-1f, 1f) }
        else -> return src
    }
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
        .also { if (it !== src) src.recycle() }
}
