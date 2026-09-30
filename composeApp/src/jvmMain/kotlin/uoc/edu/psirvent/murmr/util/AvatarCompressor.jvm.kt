package uoc.edu.psirvent.murmr.util

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

actual fun compressAvatar(bytes: ByteArray, maxSidePx: Int, quality: Int): ByteArray {
    return runCatching {
        val source = ImageIO.read(ByteArrayInputStream(bytes)) ?: return@runCatching bytes
        val srcW = source.width
        val srcH = source.height
        val ratio = maxSidePx.toDouble() / maxOf(srcW, srcH)
        val (dstW, dstH) = if (ratio < 1.0) {
            (srcW * ratio).toInt() to (srcH * ratio).toInt()
        } else {
            srcW to srcH
        }
        val target = BufferedImage(dstW, dstH, BufferedImage.TYPE_INT_RGB)
        val g = target.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.drawImage(source, 0, 0, dstW, dstH, null)
        g.dispose()

        val writers = ImageIO.getImageWritersByFormatName("jpeg")
        if (!writers.hasNext()) return@runCatching bytes
        val writer = writers.next()
        val params: ImageWriteParam = writer.defaultWriteParam.apply {
            compressionMode = ImageWriteParam.MODE_EXPLICIT
            compressionQuality = (quality.coerceIn(1, 100) / 100f)
        }
        ByteArrayOutputStream().use { out ->
            ImageIO.createImageOutputStream(out).use { ios ->
                writer.output = ios
                writer.write(null, IIOImage(target, null, null), params)
                writer.dispose()
            }
            out.toByteArray()
        }
    }.getOrDefault(bytes)
}
