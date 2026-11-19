package eu.kanade.tachiyomi.data.coil

import android.content.Context
import coil3.ImageLoader
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.allowRgb565
import coil3.request.crossfade
import coil3.util.DebugLogger
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.animatorDurationScale
import kotlinx.coroutines.Dispatchers
import tachiyomi.domain.source.service.SourceManager

fun createAppImageLoader(
    context: Context,
    networkHelper: Lazy<NetworkHelper>,
    coverCache: Lazy<CoverCache>,
    sourceManager: Lazy<SourceManager>,
    extensionManager: Lazy<ExtensionManager>,
    verboseLogging: Boolean,
): ImageLoader {
    return ImageLoader.Builder(context).apply {
        val callFactoryLazy = lazy { networkHelper.value.client }
        val coverCacheValue = coverCache.value
        val sourceManagerValue = sourceManager.value
        components {
            // NetworkFetcher.Factory
            add(OkHttpNetworkFetcherFactory(callFactoryLazy::value))
            // Decoder.Factory
            add(ImageDecoder.Factory())
            // Fetcher.Factory
            add(BufferedSourceFetcher.Factory())
            add(MangaCoverFetcher.MangaCoverFactory(callFactoryLazy, coverCacheValue, sourceManagerValue))
            add(MangaCoverFetcher.MangaFactory(callFactoryLazy, coverCacheValue, sourceManagerValue))
            add(ExtensionIconFetcher.ExtensionFactory(context))
            add(ExtensionIconFetcher.SourceFactory(context, extensionManager))
            // Keyer
            add(MangaCoverKeyer(coverCacheValue))
            add(MangaKeyer(coverCacheValue))
            add(InstalledExtensionIconKeyer())
            add(InstalledSourceIconKeyer())
            // Interceptor
            add(CoverRequestInterceptor())
        }

        memoryCache(
            MemoryCache.Builder()
                .maxSizePercent(context)
                .build(),
        )

        crossfade((300 * context.animatorDurationScale).toInt())
        allowRgb565(DeviceUtil.isLowRamDevice(context))
        if (verboseLogging) logger(DebugLogger())

        // Coil spawns a new thread for every image load by default
        fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(8))
        decoderCoroutineContext(Dispatchers.IO.limitedParallelism(3))
    }
        .build()
}
