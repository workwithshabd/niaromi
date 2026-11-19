package tachiyomi.core.common.util.lang

import rx.Observable
import niaromi.core.common.util.lang.awaitSingle as niaromiAwaitSingle

/**
 * Backward compatibility bridge for source-api and extensions.
 */
suspend inline fun <T> Observable<T>.awaitSingle(): T = niaromiAwaitSingle()
