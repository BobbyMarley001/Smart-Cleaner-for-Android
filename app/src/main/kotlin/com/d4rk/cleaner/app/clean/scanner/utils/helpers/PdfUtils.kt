package com.d4rk.cleaner.app.clean.scanner.utils.helpers

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

suspend fun loadPdfThumbnail(file: File): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(descriptor)
        val page = renderer.openPage(0)
        val width = page.width
        val height = page.height
        val bitmap = createBitmap(width, height).apply {
            page.render(this, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        }
        page.close()
        renderer.close()
        descriptor.close()

        val maxSide = max(width, height)
        val scaled = if (maxSide > 512) {
            val scale = 512f / maxSide
            val scaledWidth = (width * scale).roundToInt()
            val scaledHeight = (height * scale).roundToInt()
            val resized = bitmap.scale(scaledWidth, scaledHeight)
            bitmap.recycle()
            resized
        } else bitmap
        scaled.copy(scaled.config ?: Bitmap.Config.ARGB_8888, true)
    }.getOrNull()
}
