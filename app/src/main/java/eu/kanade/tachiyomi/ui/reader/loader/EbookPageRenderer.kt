package eu.kanade.tachiyomi.ui.reader.loader

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import mihon.app.di.appGraph
import mihon.core.archive.EpubReader
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Editorial page renderer for text-based EPUB eBooks.
 * Paginates and renders text into high-resolution pages matching the editorial reference UI.
 */
internal class EbookPageRenderer(
    private val context: Context,
    private val reader: EpubReader,
    private val chapterTitle: String,
    private val bookTitle: String,
) {

    sealed interface PageItem {
        data class TextPage(
            val pageIndex: Int,
            val totalPages: Int,
            val chapterLabel: String,
            val chapterHeading: String,
            val bookName: String,
            val isFirstPage: Boolean,
            val startLine: Int,
            val endLine: Int,
            val dropCapChar: Char? = null,
            val dropCapWidth: Int = 0,
        ) : PageItem

        data class ImagePage(
            val pageIndex: Int,
            val totalPages: Int,
            val imagePath: String,
            val caption: String? = null,
        ) : PageItem
    }

    private val metrics = context.resources.displayMetrics
    private val screenWidth = metrics.widthPixels.coerceAtLeast(720)
    private val screenHeight = metrics.heightPixels.coerceAtLeast(1280)
    private val density = metrics.density
    private val scaledDensity = metrics.scaledDensity

    private val horizontalMargin = (screenWidth * 0.07f).toInt().coerceIn(
        (20 * density).toInt(),
        (32 * density).toInt(),
    )
    private val topMargin = (screenHeight * 0.05f).toInt().coerceIn((36 * density).toInt(), (52 * density).toInt())
    private val bottomMargin = (screenHeight * 0.045f).toInt().coerceIn((28 * density).toInt(), (44 * density).toInt())
    private val contentWidth = screenWidth - horizontalMargin * 2

    private val bodyTextSize = 15f * density
    private val bodyLineSpacingMultiplier = 1.4f
    private val chapterLabelSize = 11.5f * density
    private val chapterTitleSize = 22f * density
    private val runningHeaderSize = 10.5f * density
    private val footerSize = 10.5f * density

    private var cachedTextLayout: StaticLayout? = null
    private var cachedBodyPaint: TextPaint? = null

    /**
     * Builds and paginates the pages for the specified spine paths.
     */
    fun buildPages(spinePaths: List<String>): List<PageItem> {
        val extractedImages = mutableListOf<String>()
        val fullContent = SpannableStringBuilder()
        var extractedTitle = ""

        spinePaths.forEachIndexed { index, path ->
            try {
                reader.getInputStream(path)?.use { stream ->
                    val doc = Jsoup.parse(stream, "UTF-8", "")
                    val basePath = reader.getParentDirectory(path)

                    if (index == 0) {
                        val headingEl = doc.select("h1, h2, h3").firstOrNull()
                        if (headingEl != null) {
                            val heading = headingEl.text().trim()
                            if (heading.isNotBlank()) {
                                extractedTitle = heading
                            }
                            headingEl.remove()
                        }
                    }

                    // Extract illustration images
                    doc.select("img, image").forEach { img ->
                        val src = img.attr("src").ifBlank { img.attr("xlink:href") }
                        if (src.isNotBlank()) {
                            val resolved = reader.resolveZipPath(basePath, src)
                            if (!extractedImages.contains(resolved)) {
                                extractedImages.add(resolved)
                            }
                        }
                    }

                    // Extract body text
                    doc.body()?.let { body ->
                        appendElement(fullContent, body)
                    }
                }
            } catch (_: Exception) {
            }
        }

        val displayTitle = if (chapterTitle.isNotBlank()) chapterTitle else extractedTitle.ifBlank { "Chapter" }
        val displayLabel = deriveChapterLabel(displayTitle)

        // Trim leading and trailing whitespace
        while (fullContent.isNotEmpty() && fullContent.first().isWhitespace()) {
            fullContent.delete(0, 1)
        }
        while (fullContent.isNotEmpty() && fullContent.last().isWhitespace()) {
            fullContent.delete(fullContent.length - 1, fullContent.length)
        }

        // Drop Cap detection on the very first character
        var dropCapChar: Char? = null
        var dropCapWidth = 0
        var hasDropCap = false

        if (fullContent.isNotEmpty()) {
            val first = fullContent.first()
            if (first.isLetter()) {
                dropCapChar = first.uppercaseChar()
                val dropCapPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 42f * density
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                }
                dropCapWidth = (dropCapPaint.measureText(dropCapChar.toString()) + 8f * density).toInt()
                hasDropCap = true
                fullContent.delete(0, 1)
            }
        }

        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = bodyTextSize
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }
        cachedBodyPaint = bodyPaint

        val layoutBuilder = StaticLayout.Builder.obtain(fullContent, 0, fullContent.length, bodyPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, bodyLineSpacingMultiplier)
            .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_FULL)

        if (hasDropCap) {
            val leftIndents = intArrayOf(dropCapWidth, dropCapWidth, dropCapWidth, 0)
            layoutBuilder.setIndents(leftIndents, null)
        }

        val layout = layoutBuilder.build()
        cachedTextLayout = layout

        // Available vertical space calculation
        val chapterLabelHeight = chapterLabelSize + 8f * density
        val chapterTitleHeight = chapterTitleSize * 1.3f + 16f * density
        val firstPageHeaderHeight = (topMargin + 30f * density + chapterTitleHeight + 28f * density).toInt()
        val footerHeight = (bottomMargin + footerSize).toInt()
        val firstPageAvailableHeight = screenHeight - firstPageHeaderHeight - footerHeight
        val runningHeaderHeight = (topMargin + 14f * density + 22f * density).toInt()
        val normalPageAvailableHeight = screenHeight - runningHeaderHeight - footerHeight

        val pageList = mutableListOf<PageItem>()
        var currentLine = 0
        val totalLines = layout.lineCount

        while (currentLine < totalLines) {
            val isFirst = pageList.isEmpty()
            val targetHeight = if (isFirst) firstPageAvailableHeight else normalPageAvailableHeight
            val startLine = currentLine
            val startLineTop = layout.getLineTop(startLine)
            var endLine = startLine

            while (endLine < totalLines && (layout.getLineBottom(endLine) - startLineTop) <= targetHeight) {
                endLine++
            }

            if (endLine == startLine) {
                endLine = startLine + 1
            }

            pageList.add(
                PageItem.TextPage(
                    pageIndex = pageList.size,
                    totalPages = 0,
                    chapterLabel = displayLabel,
                    chapterHeading = displayTitle,
                    bookName = bookTitle,
                    isFirstPage = isFirst,
                    startLine = startLine,
                    endLine = endLine - 1,
                    dropCapChar = if (isFirst && hasDropCap) dropCapChar else null,
                    dropCapWidth = if (isFirst && hasDropCap) dropCapWidth else 0,
                ),
            )
            currentLine = endLine
        }

        // Add illustration images as dedicated pages if present
        extractedImages.forEach { imagePath ->
            pageList.add(
                PageItem.ImagePage(
                    pageIndex = pageList.size,
                    totalPages = 0,
                    imagePath = imagePath,
                ),
            )
        }

        if (pageList.isEmpty()) {
            pageList.add(
                PageItem.TextPage(
                    pageIndex = 0,
                    totalPages = 1,
                    chapterLabel = displayLabel,
                    chapterHeading = displayTitle,
                    bookName = bookTitle,
                    isFirstPage = true,
                    startLine = 0,
                    endLine = 0,
                ),
            )
        }

        val total = pageList.size
        return pageList.mapIndexed { index, item ->
            when (item) {
                is PageItem.TextPage -> item.copy(pageIndex = index, totalPages = total)
                is PageItem.ImagePage -> item.copy(pageIndex = index, totalPages = total)
            }
        }
    }

    /**
     * Renders a single page item into a compressed PNG stream.
     */
    fun renderToStream(item: PageItem): InputStream {
        val isDark = isDarkTheme()
        val bgColor = if (isDark) Color.rgb(0x12, 0x12, 0x12) else Color.rgb(0xFF, 0xFD, 0xF4)
        val primaryTextColor = if (isDark) Color.rgb(0xE6, 0xE3, 0xD8) else Color.rgb(0x17, 0x17, 0x17)
        val mutedTextColor = if (isDark) Color.rgb(0x8E, 0x8E, 0x93) else Color.rgb(0x75, 0x75, 0x75)
        val dividerColor = if (isDark) Color.argb(0x33, 0xFF, 0xFF, 0xFF) else Color.argb(0x20, 0x00, 0x00, 0x00)

        val bitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(bgColor)

        when (item) {
            is PageItem.TextPage -> {
                val layout = cachedTextLayout
                if (item.isFirstPage) {
                    // Chapter Label
                    val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = chapterLabelSize
                        color = mutedTextColor
                        letterSpacing = 0.12f
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                    }
                    canvas.drawText(
                        item.chapterLabel.uppercase(),
                        screenWidth / 2f,
                        topMargin + 14f * density,
                        labelPaint,
                    )

                    // Chapter Title Heading
                    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = chapterTitleSize
                        color = primaryTextColor
                        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    }
                    val titleLayout = StaticLayout.Builder.obtain(
                        item.chapterHeading,
                        0,
                        item.chapterHeading.length,
                        titlePaint,
                        contentWidth,
                    ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()

                    val titleY = topMargin + 30f * density
                    canvas.save()
                    canvas.translate(horizontalMargin.toFloat(), titleY)
                    titleLayout.draw(canvas)
                    canvas.restore()

                    val textTop = titleY + titleLayout.height + 28f * density

                    // Drop Cap
                    if (item.dropCapChar != null && layout != null && layout.lineCount > 0) {
                        val dropCapPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                            textSize = 42f * density
                            color = primaryTextColor
                            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                        }
                        val lineIndex = 2.coerceAtMost(layout.lineCount - 1)
                        val line2Baseline = layout.getLineBaseline(lineIndex)
                        val dropCapY = textTop + line2Baseline - layout.getLineTop(item.startLine)
                        canvas.drawText(item.dropCapChar.toString(), horizontalMargin.toFloat(), dropCapY, dropCapPaint)
                    }

                    // Body text
                    if (layout != null && item.startLine < layout.lineCount) {
                        canvas.save()
                        canvas.translate(horizontalMargin.toFloat(), textTop - layout.getLineTop(item.startLine))
                        canvas.clipRect(
                            0,
                            layout.getLineTop(item.startLine),
                            contentWidth,
                            layout.getLineBottom(item.endLine),
                        )
                        cachedBodyPaint?.color = primaryTextColor
                        layout.draw(canvas)
                        canvas.restore()
                    }
                } else {
                    // Running Header
                    val headerY = topMargin + 14f * density
                    val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = runningHeaderSize
                        color = mutedTextColor
                        letterSpacing = 0.08f
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                    }
                    val headerText = item.bookName.ifBlank { item.chapterHeading }
                    canvas.drawText(headerText.uppercase(), screenWidth / 2f, headerY, headerPaint)

                    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = dividerColor
                        strokeWidth = 1f * density
                    }
                    canvas.drawLine(
                        horizontalMargin.toFloat(),
                        headerY + 10f * density,
                        (screenWidth - horizontalMargin).toFloat(),
                        headerY + 10f * density,
                        linePaint,
                    )

                    val textTop = headerY + 22f * density
                    if (layout != null && item.startLine < layout.lineCount) {
                        canvas.save()
                        canvas.translate(horizontalMargin.toFloat(), textTop - layout.getLineTop(item.startLine))
                        canvas.clipRect(
                            0,
                            layout.getLineTop(item.startLine),
                            contentWidth,
                            layout.getLineBottom(item.endLine),
                        )
                        cachedBodyPaint?.color = primaryTextColor
                        layout.draw(canvas)
                        canvas.restore()
                    }
                }

                // Running Footer
                val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = footerSize
                    color = mutedTextColor
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                }
                canvas.drawText(
                    "${item.pageIndex + 1}",
                    screenWidth / 2f,
                    screenHeight - (bottomMargin * 0.45f),
                    footerPaint,
                )
            }

            is PageItem.ImagePage -> {
                try {
                    reader.getInputStream(item.imagePath)?.use { stream ->
                        val imageBitmap = BitmapFactory.decodeStream(stream)
                        if (imageBitmap != null) {
                            val maxW = (screenWidth - 48 * density).toInt()
                            val maxH = (screenHeight - 120 * density).toInt()
                            val scale =
                                minOf(maxW.toFloat() / imageBitmap.width, maxH.toFloat() / imageBitmap.height, 1.0f)
                            val destW = (imageBitmap.width * scale).toInt()
                            val destH = (imageBitmap.height * scale).toInt()
                            val destX = (screenWidth - destW) / 2
                            val destY = (screenHeight - destH) / 2
                            val srcRect = Rect(0, 0, imageBitmap.width, imageBitmap.height)
                            val destRect = Rect(destX, destY, destX + destW, destY + destH)
                            canvas.drawBitmap(imageBitmap, srcRect, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                            imageBitmap.recycle()
                        }
                    }
                } catch (_: Exception) {
                }

                val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = footerSize
                    color = mutedTextColor
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                }
                canvas.drawText(
                    "${item.pageIndex + 1}",
                    screenWidth / 2f,
                    screenHeight - (bottomMargin * 0.45f),
                    footerPaint,
                )
            }
        }

        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        bitmap.recycle()
        return ByteArrayInputStream(output.toByteArray())
    }

    private fun isDarkTheme(): Boolean {
        return try {
            val readerPref = context.appGraph.readerPreferences
            val isSystemNight = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            val themeValue = readerPref.readerTheme.get()
            when (ReaderPreferences.ReaderTheme.fromValue(themeValue)) {
                ReaderPreferences.ReaderTheme.WHITE -> false
                ReaderPreferences.ReaderTheme.GRAY -> true
                ReaderPreferences.ReaderTheme.BLACK -> isSystemNight
                ReaderPreferences.ReaderTheme.AUTOMATIC -> isSystemNight
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun deriveChapterLabel(title: String): String {
        val lower = title.lowercase()
        return when {
            lower.contains("prologue") -> "Prologue"
            lower.contains("epilogue") -> "Epilogue"
            lower.contains("introduction") -> "Introduction"
            lower.contains("preface") -> "Preface"
            lower.startsWith("chapter") -> title.substringBefore(':').substringBefore('-').trim()
            else -> "Chapter"
        }
    }

    private fun appendElement(builder: SpannableStringBuilder, element: Element) {
        for (node in element.childNodes()) {
            when (node) {
                is TextNode -> {
                    val text = node.text()
                    if (text.isNotEmpty()) {
                        builder.append(text)
                    }
                }
                is Element -> {
                    val tag = node.tagName().lowercase()
                    val start = builder.length
                    when (tag) {
                        "p", "div", "blockquote", "li" -> {
                            appendElement(builder, node)
                            builder.append("\n\n")
                        }
                        "h1", "h2", "h3", "h4", "h5", "h6" -> {
                            appendElement(builder, node)
                            val end = builder.length
                            if (end > start) {
                                builder.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                builder.setSpan(RelativeSizeSpan(1.25f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                            builder.append("\n\n")
                        }
                        "em", "i" -> {
                            appendElement(builder, node)
                            val end = builder.length
                            if (end > start) {
                                builder.setSpan(
                                    StyleSpan(Typeface.ITALIC),
                                    start,
                                    end,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                                )
                            }
                        }
                        "strong", "b" -> {
                            appendElement(builder, node)
                            val end = builder.length
                            if (end > start) {
                                builder.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                        }
                        "br" -> {
                            builder.append("\n")
                        }
                        else -> {
                            appendElement(builder, node)
                        }
                    }
                }
            }
        }
    }
}
