package mihon.core.archive

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import java.io.Closeable
import java.io.File
import java.io.InputStream

/**
 * Wrapper over ArchiveReader to load files in epub format.
 */
class EpubReader(private val reader: ArchiveReader) : Closeable by reader {

    /**
     * Path separator used by this epub.
     */
    private val pathSeparator = getPathSeparator()

    /**
     * Returns an input stream for reading the contents of the specified zip file entry.
     */
    fun getInputStream(entryName: String): InputStream? {
        return reader.getInputStream(entryName)
    }

    /**
     * Returns the path of all the images found in the epub file.
     */
    fun getImagesFromPages(): List<String> {
        val ref = getPackageHref()
        val doc = getPackageDocument(ref)
        val pages = getPagesFromDocument(doc)
        return getImagesFromPages(pages, ref)
    }

    /**
     * Returns the path to the package document.
     */
    fun getPackageHref(): String {
        val meta = getInputStream(resolveZipPath("META-INF", "container.xml"))
        if (meta != null) {
            val metaDoc = meta.use { Jsoup.parse(it, null, "", Parser.xmlParser()) }
            val path = metaDoc.getElementsByTag("rootfile").first()?.attr("full-path")
            if (path != null) {
                return path
            }
        }
        return resolveZipPath("OEBPS", "content.opf")
    }

    /**
     * Returns the package document where all the files are listed.
     */
    fun getPackageDocument(ref: String): Document {
        return getInputStream(ref)!!.use { Jsoup.parse(it, null, "", Parser.xmlParser()) }
    }

    /**
     * Returns all the pages from the epub.
     */
    private fun getPagesFromDocument(document: Document): List<String> {
        val pages = document.select("manifest > item")
            .filter { node -> "application/xhtml+xml" == node.attr("media-type") }
            .associateBy { it.attr("id") }

        val spine = document.select("spine > itemref").map { it.attr("idref") }
        return spine.mapNotNull { pages[it] }.map { it.attr("href") }
    }

    /**
     * Returns all the images contained in every page from the epub.
     */
    private fun getImagesFromPages(pages: List<String>, packageHref: String): List<String> {
        val result = mutableListOf<String>()
        val basePath = getParentDirectory(packageHref)
        pages.forEach { page ->
            val entryPath = resolveZipPath(basePath, page)
            val document = getInputStream(entryPath)!!.use { Jsoup.parse(it, null, "") }
            val imageBasePath = getParentDirectory(entryPath)

            document.allElements.forEach {
                when (it.tagName()) {
                    "img" -> result.add(resolveZipPath(imageBasePath, it.attr("src")))
                    "image" -> result.add(resolveZipPath(imageBasePath, it.attr("xlink:href")))
                }
            }
        }

        return result
    }

    /**
     * Returns the path separator used by the epub file.
     */
    private fun getPathSeparator(): String {
        val meta = getInputStream("META-INF\\container.xml")
        return if (meta != null) {
            meta.close()
            "\\"
        } else {
            "/"
        }
    }

    /**
     * Resolves and returns the cover image path from the epub file if present.
     */
    fun getCoverImage(): String? {
        return try {
            val ref = getPackageHref()
            val doc = getPackageDocument(ref)
            val basePath = getParentDirectory(ref)

            val epub3Cover = doc.select("manifest > item[properties*=cover-image]").firstOrNull()?.attr("href")
            if (!epub3Cover.isNullOrBlank()) {
                return resolveZipPath(basePath, epub3Cover)
            }

            val coverId = doc.select("metadata > meta[name=cover]").firstOrNull()?.attr("content")
            if (!coverId.isNullOrBlank()) {
                val itemHref = doc.select("manifest > item#$coverId").firstOrNull()?.attr("href")
                    ?: doc.select("manifest > item[id=$coverId]").firstOrNull()?.attr("href")
                if (!itemHref.isNullOrBlank()) {
                    return resolveZipPath(basePath, itemHref)
                }
            }

            val manifestCover = doc.select("manifest > item")
                .filter { it.attr("media-type").startsWith("image/") }
                .firstOrNull {
                    it.attr("id").contains("cover", ignoreCase = true) ||
                        it.attr("href").contains("cover", ignoreCase = true)
                }
                ?.attr("href")
            if (!manifestCover.isNullOrBlank()) {
                return resolveZipPath(basePath, manifestCover)
            }

            getImagesFromPages().firstOrNull()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Returns all spine item paths resolved relative to the package document.
     */
    fun getSpineHrefs(): List<String> {
        val ref = getPackageHref()
        val doc = getPackageDocument(ref)
        val basePath = getParentDirectory(ref)
        return getPagesFromDocument(doc).map { resolveZipPath(basePath, it) }
    }

    /**
     * Whether this epub is primarily a text ebook rather than a comic/manga.
     */
    fun isTextEbook(): Boolean {
        val images = getImagesFromPages()
        val spine = getSpineHrefs()
        if (images.isEmpty() && spine.isNotEmpty()) return true
        if (images.size < spine.size) return true
        return false
    }

    data class EpubChapterInfo(
        val title: String,
        val href: String,
        val spineIndex: Int,
    )

    /**
     * Returns list of chapters parsed from TOC or spine items.
     */
    fun getChapters(): List<EpubChapterInfo> {
        return try {
            val ref = getPackageHref()
            val doc = getPackageDocument(ref)
            val basePath = getParentDirectory(ref)
            val spinePages = getPagesFromDocument(doc)

            // Try EPUB 3 Navigation Document
            val navItem = doc.select("manifest > item[properties*=nav]").firstOrNull()
            if (navItem != null) {
                val navPath = resolveZipPath(basePath, navItem.attr("href"))
                val navDoc = getInputStream(navPath)?.use { Jsoup.parse(it, null, "") }
                val tocLinks = navDoc?.select("nav[epub:type=toc] ol li a, nav#toc ol li a")
                if (!tocLinks.isNullOrEmpty()) {
                    val chapters = mutableListOf<EpubChapterInfo>()
                    tocLinks.forEachIndexed { idx, link ->
                        val href = link.attr("href").substringBefore('#')
                        val title = link.text().trim().ifBlank { "Chapter ${idx + 1}" }
                        val spineIdx = spinePages.indexOfFirst {
                            it.endsWith(href) || href.endsWith(it)
                        }.coerceAtLeast(0)
                        chapters.add(EpubChapterInfo(title, resolveZipPath(basePath, href), spineIdx))
                    }
                    if (chapters.isNotEmpty()) return chapters
                }
            }

            // Try EPUB 2 NCX Document
            val ncxItem = doc.select(
                "manifest > item[id=ncx], manifest > item[media-type=application/x-dtbncx+xml]",
            ).firstOrNull()
            if (ncxItem != null) {
                val ncxPath = resolveZipPath(basePath, ncxItem.attr("href"))
                val ncxDoc = getInputStream(ncxPath)?.use { Jsoup.parse(it, null, "", Parser.xmlParser()) }
                val navPoints = ncxDoc?.select("navMap > navPoint")
                if (!navPoints.isNullOrEmpty()) {
                    val chapters = mutableListOf<EpubChapterInfo>()
                    navPoints.forEachIndexed { idx, np ->
                        val title = np.select("navLabel > text").text().trim().ifBlank { "Chapter ${idx + 1}" }
                        val src = np.select("content").attr("src").substringBefore('#')
                        val spineIdx = spinePages.indexOfFirst { it.endsWith(src) || src.endsWith(it) }.coerceAtLeast(0)
                        chapters.add(EpubChapterInfo(title, resolveZipPath(basePath, src), spineIdx))
                    }
                    if (chapters.isNotEmpty()) return chapters
                }
            }

            // Fallback: derive chapters from spine items
            spinePages.mapIndexed { index, pageHref ->
                val fullPath = resolveZipPath(basePath, pageHref)
                val title = try {
                    val pageDoc = getInputStream(fullPath)?.use { Jsoup.parse(it, null, "") }
                    val heading = pageDoc?.select("h1, h2, h3, title")?.firstOrNull()?.text()?.trim()
                    if (!heading.isNullOrBlank()) heading else "Chapter ${index + 1}"
                } catch (_: Exception) {
                    "Chapter ${index + 1}"
                }
                EpubChapterInfo(title, fullPath, index)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Resolves a zip path from base and relative components and a path separator.
     */
    fun resolveZipPath(basePath: String, relativePath: String): String {
        if (relativePath.startsWith(pathSeparator)) {
            // Path is absolute, so return as-is.
            return relativePath
        }

        var fixedBasePath = basePath.replace(pathSeparator, File.separator)
        if (!fixedBasePath.startsWith(File.separator)) {
            fixedBasePath = "${File.separator}$fixedBasePath"
        }

        val fixedRelativePath = relativePath.replace(pathSeparator, File.separator)
        val resolvedPath = File(fixedBasePath, fixedRelativePath).canonicalPath
        return resolvedPath.replace(File.separator, pathSeparator).substring(1)
    }

    /**
     * Gets the parent directory of a path.
     */
    fun getParentDirectory(path: String): String {
        val separatorIndex = path.lastIndexOf(pathSeparator)
        return if (separatorIndex >= 0) {
            path.substring(0, separatorIndex)
        } else {
            ""
        }
    }
}
