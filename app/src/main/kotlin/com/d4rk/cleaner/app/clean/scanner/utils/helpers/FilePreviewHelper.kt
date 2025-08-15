@file:Suppress("DEPRECATION")

package com.d4rk.cleaner.app.clean.scanner.utils.helpers

import android.content.ComponentCallbacks2
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaFormat.KEY_MAX_INPUT_SIZE
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import coil3.video.videoFramePercent
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.apps.manager.ui.components.IconLoadingPlaceholder
import com.google.common.io.Files.getFileExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.util.zip.ZipFile
import kotlin.math.abs
import kotlin.math.min

/**
 * Helper used for displaying file previews. All composables that need to render
 * a file preview should delegate to [FilePreviewHelper.Preview] so that the
 * logic for determining how a file should be presented lives in a single place.
 *
 * Extend [PreviewType] when supporting new file formats.
 */
object FilePreviewHelper {

    /**
     * Maximum bytes to allocate when generating audio waveforms. This cap
     * prevents OutOfMemory errors that can be triggered by corrupted or
     * malicious files reporting extremely large input sizes.
     */
    private const val MAX_WAVEFORM_BYTES = 1 * 1024 * 1024 // 1 MB
    private val recycleHandler by lazy { android.os.Handler(android.os.Looper.getMainLooper()) }

    private val bitmapCache: LruCache<String, Bitmap> by lazy {
        val cacheSizeKb = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
        object : LruCache<String, Bitmap>(cacheSizeKb) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024

            override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
                if (evicted) {
                    if (!oldValue.isRecycled) {
                        recycleHandler.postDelayed({
                            if (!oldValue.isRecycled) {
                                oldValue.recycle()
                            }
                        }, 2000)
                    }
                }
            }
        }
    }

    private var imageExtCache: Set<String>? = null
    private var videoExtCache: Set<String>? = null
    private var apkExtCache: Set<String>? = null
    private var officeExtCache: Set<String>? = null
    private var audioExtCache: Set<String>? = null
    private var textExtCache: Set<String>? = null
    private var archiveExtCache: Set<String>? = null

    private val imageExtensions: Set<String>
        @Composable get() {
            val cached = imageExtCache
            if (cached != null) return cached
            val res = LocalContext.current.resources
            val set = res.getStringArray(R.array.image_extensions).toSet()
            imageExtCache = set
            return set
        }

    private val videoExtensions: Set<String>
        @Composable get() {
            val cached = videoExtCache
            if (cached != null) return cached
            val res = LocalContext.current.resources
            val set = res.getStringArray(R.array.video_extensions).toSet()
            videoExtCache = set
            return set
        }

    private val apkExtensions: Set<String>
        @Composable get() {
            val cached = apkExtCache
            if (cached != null) return cached
            val res = LocalContext.current.resources
            val set = res.getStringArray(R.array.apk_extensions).toSet()
            apkExtCache = set
            return set
        }

    private val officeExtensions: Set<String>
        @Composable get() {
            val cached = officeExtCache
            if (cached != null) return cached
            val res = LocalContext.current.resources
            val set = res.getStringArray(R.array.microsoft_office_extensions).toSet()
            officeExtCache = set
            return set
        }

    private val audioExtensions: Set<String>
        @Composable get() {
            val cached = audioExtCache
            if (cached != null) return cached
            val res = LocalContext.current.resources
            val set = res.getStringArray(R.array.audio_extensions).toSet()
            audioExtCache = set
            return set
        }

    private val textExtensions: Set<String>
        @Composable get() {
            val cached = textExtCache
            if (cached != null) return cached
            val res = LocalContext.current.resources
            val set = res.getStringArray(R.array.text_extensions).toSet()
            textExtCache = set
            return set
        }

    private val archiveExtensions: Set<String>
        @Composable get() {
            val cached = archiveExtCache
            if (cached != null) return cached
            val res = LocalContext.current.resources
            val set = res.getStringArray(R.array.archive_extensions).toSet()
            archiveExtCache = set
            return set
        }

    /**
     * Respond to system memory pressure. On Android 14 (API 34) and above,
     * only the UI hidden callback is dispatched, so we clear the entire cache
     * when the UI is no longer visible. On older versions we retain the
     * existing behaviour of halving the cache size under background pressure
     * and evicting everything under moderate pressure.
     */
    fun onTrimMemory(level: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
                bitmapCache.evictAll()
            }
        } else {
            when {
                level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
                    bitmapCache.evictAll()
                }
                level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> {
                    val newSize = bitmapCache.maxSize() / 2
                    bitmapCache.resize(newSize)
                }
            }
        }
    }

    private suspend fun loadAlbumArt(file: File): Bitmap? = withContext(Dispatchers.IO) {
        bitmapCache.get(file.path)?.let { cached ->
            return@withContext cached.config?.let { cached.copy(it, true) } ?: cached
        }
        val bmp = runCatching {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.path)
            val art = retriever.embeddedPicture
            retriever.release()
            art?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        }.getOrNull()
        bmp?.let {
            bitmapCache.put(file.path, it)
            return@withContext it.config?.let { config -> it.copy(config, true) } ?: it
        }
        null
    }

    private suspend fun generateWaveform(file: File, width: Int = 64, height: Int = 32): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val extractor = MediaExtractor()
            extractor.setDataSource(file.path)
            var trackIndex = 0
            while (trackIndex < extractor.trackCount) {
                val format = extractor.getTrackFormat(trackIndex)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) break else trackIndex++
            }
            if (trackIndex >= extractor.trackCount) {
                extractor.release()
                return@runCatching null
            }
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val reported = if (format.containsKey(KEY_MAX_INPUT_SIZE)) {
                format.getInteger(KEY_MAX_INPUT_SIZE)
            } else {
                64 * 1024
            }
            val maxInput = min(reported, MAX_WAVEFORM_BYTES)
            val buffer = ByteBuffer.allocate(maxInput)
            val data = IntArray(width)
            var i = 0
            while (i < width) {
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                var sum = 0
                for (b in 0 until size step 2) {
                    sum += abs(buffer.getShort(b).toInt())
                }
                data[i] = if (size > 0) sum / (size / 2) else 0
                extractor.advance()
                i++
            }
            extractor.release()
            val max = data.maxOrNull() ?: 0
            if (max == 0) return@runCatching null
            val bmp = createBitmap(width, height)
            val canvas = Canvas(bmp)
            val paint = Paint().apply { color = Color.WHITE }
            data.forEachIndexed { idx, amp ->
                val ratio = amp.toFloat() / max
                val barHeight = ratio * height
                val x = idx.toFloat()
                canvas.drawLine(x, height - barHeight, x, height.toFloat(), paint)
            }
            bmp
        }.getOrNull()?.let { it.config?.let { config -> it.copy(config, true) } ?: it }
    }

    private fun isProbablyBinary(data: ByteArray): Boolean {
        if (data.isEmpty()) return false
        var nonPrintable = 0
        for (b in data) {
            val c = b.toInt() and 0xFF
            if (c in 9..13 || c in 32..126) continue else nonPrintable++
        }
        return nonPrintable.toFloat() / data.size > 0.3f
    }

    private suspend fun loadTextSnippet(file: File, lines: Int = 3, maxBytes: Int = 2048): String? = withContext(Dispatchers.IO) {
        if (file.length() > 1024 * 1024) return@withContext null
        runCatching {
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(maxBytes)
                val read = fis.read(buffer)
                if (read <= 0) return@runCatching null
                if (isProbablyBinary(buffer.copyOf(read))) return@runCatching null
            }
            file.bufferedReader().useLines { seq ->
                seq.take(lines).joinToString("\n")
            }
        }.getOrNull()
    }

    private suspend fun getArchiveEntryCount(file: File, maxEntries: Int = 10_000): Int? = withContext(Dispatchers.IO) {
        val ext = getFileExtension(file.name).lowercase()
        runCatching {
            when (ext) {
                "zip" -> ZipFile(file).use { zip ->
                    var count = 0
                    val entries = zip.entries()
                    while (entries.hasMoreElements() && count <= maxEntries) {
                        entries.nextElement()
                        count++
                    }
                    if (count > maxEntries) null else count
                }

                "rar" -> FileInputStream(file).use { fis: FileInputStream ->
                    ArchiveStreamFactory().createArchiveInputStream<ArchiveInputStream<out ArchiveEntry>>("rar", BufferedInputStream(fis))
                        .use { ais ->
                            var count = 0
                            while (ais.nextEntry != null && count <= maxEntries) {
                                count++
                            }
                            if (count > maxEntries) null else count
                        }
                }

                "7z", "7zip" -> SevenZFile.Builder().setFile(file).get().use { sevenZ ->
                    var count = 0
                    while (sevenZ.nextEntry != null && count <= maxEntries) {
                        count++
                    }
                    if (count > maxEntries) null else count
                }

                else -> null
            }
        }.getOrNull()
    }

    /** Represents the available preview strategies for files. */
    sealed class PreviewType {
        object Directory : PreviewType()
        object Image : PreviewType()
        object Video : PreviewType()
        object Apk : PreviewType()
        object Pdf : PreviewType()
        object Office : PreviewType()
        object Audio : PreviewType()
        object Text : PreviewType()
        object Archive : PreviewType()
        object Unknown : PreviewType()
    }

    @Composable
    fun getPreviewType(file: File): PreviewType {
        if (file.isDirectory) return PreviewType.Directory
        val ext = getFileExtension(file.name).lowercase()
        return when (ext) {
            in imageExtensions -> PreviewType.Image
            in videoExtensions -> PreviewType.Video
            in apkExtensions -> PreviewType.Apk
            "pdf" -> PreviewType.Pdf
            in officeExtensions -> PreviewType.Office
            in audioExtensions -> PreviewType.Audio
            in textExtensions -> PreviewType.Text
            in archiveExtensions -> PreviewType.Archive
            else -> PreviewType.Unknown
        }
    }

    /**
     * Displays a preview of the provided [file]. Call this from any UI where a
     * preview icon or thumbnail is required.
     */
    @Composable
    fun Preview(file: File, modifier: Modifier = Modifier) {
        val context = LocalContext.current
        val type = getPreviewType(file)
        val imageLoader = LocalContext.current.imageLoader
        when (type) {
            PreviewType.Directory -> {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    modifier = modifier.size(24.dp)
                )
            }

            PreviewType.Image -> {
                AsyncImage(
                    model = remember(file) {
                        ImageRequest.Builder(context).data(file).size(64).crossfade(true).build()
                    },
                    imageLoader = imageLoader,
                    contentDescription = file.name,
                    contentScale = ContentScale.FillWidth,
                    modifier = modifier.fillMaxWidth()
                )
            }

            PreviewType.Video -> {
                AsyncImage(
                    model = remember(file) {
                        ImageRequest.Builder(context)
                            .data(file)
                            .size(64)
                            .decoderFactory { result, options, _ ->
                                VideoFrameDecoder(result.source, options)
                            }
                            .videoFramePercent(0.5)
                            .crossfade(true)
                            .build()
                    },
                    contentDescription = file.name,
                    contentScale = ContentScale.Crop,
                    modifier = modifier.fillMaxSize()
                )
            }

            PreviewType.Apk -> {
                val icon by produceState<android.graphics.drawable.Drawable?>(initialValue = null, file.path) {
                    value = withContext(Dispatchers.IO) {
                        context.packageManager.getPackageArchiveInfo(file.path, 0)?.applicationInfo?.apply {
                            sourceDir = file.path
                            publicSourceDir = file.path
                        }?.loadIcon(context.packageManager)
                    }
                }
                Box(modifier = modifier.size(24.dp), contentAlignment = Alignment.Center) {
                    IconLoadingPlaceholder(iconRes = R.drawable.ic_apk_document, modifier = Modifier.fillMaxSize())
                    icon?.let { loaded ->
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(loaded).crossfade(true).build(),
                            contentDescription = file.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            PreviewType.Pdf -> {
                val pdfBitmap by produceState<Bitmap?>(initialValue = null, file.path) {
                    value = loadPdfThumbnail(file)
                }
                val safe = pdfBitmap?.takeUnless { it.isRecycled }
                if (safe != null) {
                    DisposableEffect(safe) {
                        onDispose {
                            if (!safe.isRecycled) {
                                safe.recycle()
                            }
                        }
                    }
                    Image(
                        bitmap = safe.asImageBitmap(),
                        contentDescription = file.name,
                        contentScale = ContentScale.FillWidth,
                        modifier = modifier.fillMaxWidth()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.PictureAsPdf,
                        contentDescription = null,
                        modifier = modifier.size(24.dp)
                    )
                }
            }

            PreviewType.Office -> {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    modifier = modifier.size(24.dp)
                )
            }

            PreviewType.Audio -> {
                val bitmap by produceState<Bitmap?>(initialValue = null, file.path) {
                    value = loadAlbumArt(file) ?: generateWaveform(file)
                }
                val safe = bitmap?.takeUnless { it.isRecycled }
                if (safe != null) {
                    DisposableEffect(safe) {
                        onDispose {
                            if (!safe.isRecycled) {
                                safe.recycle()
                            }
                        }
                    }
                    Image(
                        bitmap = safe.asImageBitmap(),
                        contentDescription = file.name,
                        contentScale = ContentScale.Crop,
                        modifier = modifier.fillMaxWidth()
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_audio_file),
                        contentDescription = null,
                        modifier = modifier.size(24.dp)
                    )
                }
            }

            PreviewType.Text -> {
                val snippet by produceState<String?>(initialValue = null, file.path) {
                    value = loadTextSnippet(file)
                }
                if (snippet != null) {
                    Text(
                        text = snippet!!,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = modifier
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_unknown_document),
                        contentDescription = null,
                        modifier = modifier.size(24.dp)
                    )
                }
            }

            PreviewType.Archive -> {
                val count by produceState<Int?>(initialValue = null, file.path) {
                    value = getArchiveEntryCount(file)
                }
                val extLabel = remember(file.name) { getFileExtension(file.name).uppercase() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = modifier
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_archive_filter),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    count?.let {
                        Text(
                            text = "$extLabel (${it} files)",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            PreviewType.Unknown -> {
                val icon = remember(getFileExtension(file.name)) {
                    getFileIcon(getFileExtension(file.name), context)
                }
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    modifier = modifier.size(24.dp)
                )
            }
        }
    }
}