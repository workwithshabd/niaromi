package eu.kanade.tachiyomi.util.system

import niaromi.util.system.dpToPx as niaromiDpToPx

inline val Int.dpToPx: Int
    get() = this.niaromiDpToPx
