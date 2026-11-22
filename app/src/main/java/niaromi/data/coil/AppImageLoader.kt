package niaromi.data.coil

import android.content.Context
import coil3.ImageLoader
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.allowRgb565
import coil3.request.crossfade
import coil3.util.DebugLogger
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.Dispatchers
import niaromi.data.cache.CoverCache
import niaromi.domain.source.service.SourceManager
import niaromi.extension.ExtensionManager
import niaromi.util.system.DeviceUtil
import niaromi.util.system.animatorDurationScale

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

        val isLowRam = DeviceUtil.isLowRamDevice(context)
        memoryCache(
            MemoryCache.Builder()
                .maxSizePercent(context, if (isLowRam) 0.10 else 0.20)
                .build(),
        )

        crossfade((300 * context.animatorDurationScale).toInt())
        allowRgb565(isLowRam)
        if (verboseLogging) logger(DebugLogger())

        // Coil spawns a new thread for every image load by default
        fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(if (isLowRam) 4 else 8))
        decoderCoroutineContext(Dispatchers.IO.limitedParallelism(if (isLowRam) 2 else 3))
    }
        .build()
}
