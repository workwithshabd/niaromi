package eu.kanade.tachiyomi.util.lang

import niaromi.util.lang.chop as niaromiChop

fun String.chop(count: Int, replacement: String = "…"): String = niaromiChop(count, replacement)
