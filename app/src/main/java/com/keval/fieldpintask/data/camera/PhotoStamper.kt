package com.keval.fieldpintask.data.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.exifinterface.media.ExifInterface
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Burns text into the saved JPEG itself, so the stamp survives sharing and cannot be edited away. */
@Singleton
class PhotoStamper @Inject constructor() {

    suspend fun stamp(file: File, lines: List<String>) = withContext(Dispatchers.Default) {
        val bitmap = decodeUpright(file)
        draw(Canvas(bitmap), bitmap.width, bitmap.height, lines)
        // Re-encoding drops the EXIF tag, which is right because the pixels are already rotated.
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        bitmap.recycle()
    }

    private fun decodeUpright(file: File): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_DIMENSION_PX) sample *= 2

        val decoded = BitmapFactory.decodeFile(
            file.path,
            BitmapFactory.Options().apply { inSampleSize = sample; inMutable = true },
        ) ?: error("Could not decode ${file.name}")

        val rotation = when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (rotation == 0f) return decoded
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(rotation) }, true)
        decoded.recycle()
        return if (rotated.isMutable) rotated else rotated.copy(Bitmap.Config.ARGB_8888, true).also { rotated.recycle() }
    }

    private fun draw(canvas: Canvas, width: Int, height: Int, lines: List<String>) {
        val margin = width * MARGIN_RATIO
        val padding = width * PADDING_RATIO
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = width * TEXT_RATIO
        }
        // Shrink until the longest line fits, so long site names never run off the photo.
        while (lines.maxOf { text.measureText(it) } > width - 2 * (margin + padding)) text.textSize *= SHRINK_FACTOR

        val lineHeight = text.fontSpacing
        val boxWidth = lines.maxOf { text.measureText(it) } + 2 * padding
        val boxHeight = lineHeight * lines.size + 2 * padding
        val box = RectF(margin, height - margin - boxHeight, margin + boxWidth, height - margin)
        canvas.drawRoundRect(box, padding, padding, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(BOX_ALPHA, 0, 0, 0) })

        lines.forEachIndexed { index, line ->
            text.typeface = if (index == 0) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            canvas.drawText(line, box.left + padding, box.top + padding + lineHeight * index - text.ascent(), text)
        }
    }

    private companion object {
        const val MAX_DIMENSION_PX = 2560
        const val JPEG_QUALITY = 90
        const val MARGIN_RATIO = 0.03f
        const val PADDING_RATIO = 0.02f
        const val TEXT_RATIO = 0.035f
        const val SHRINK_FACTOR = 0.92f
        const val BOX_ALPHA = 170
    }
}
