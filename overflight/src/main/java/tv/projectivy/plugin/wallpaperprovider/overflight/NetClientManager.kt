package tv.projectivy.plugin.wallpaperprovider.overflight

import android.content.Context
import android.net.TrafficStats
import android.util.Log
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

object NetClientManager {

    private const val CALL_TIMEOUT_IN_S = 5L
    private const val CACHE_SIZE = 10 * 1024 * 1024L

    private val cacheControl: CacheControl
        get() = CacheControl.Builder()
            .maxAge(PreferencesManager.cacheDurationHours, TimeUnit.HOURS)
            .build()

    lateinit var httpClient: OkHttpClient

    fun init(context: Context) {
        TrafficStats.setThreadStatsTag(1906)
        httpClient = OkHttpClient.Builder()
            .cache(Cache(context.cacheDir, CACHE_SIZE))
            .callTimeout(CALL_TIMEOUT_IN_S, TimeUnit.SECONDS)
            .addNetworkInterceptor(CacheInterceptor())
            .build()
    }

    fun request(url: String): String? {
        val request: Request = Request.Builder()
            .url(url)
            .build()

        return newCall(request)
    }

    fun newCall(request: Request): String? {
        try {
            httpClient.newCall(request).execute().use { response ->
                if (response.code == 200 && response.body != null) {
                    return response.body?.string()
                }
                Log.d("NetClientManager", "Request unsuccessful. Response code:" + response.code)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    class CacheInterceptor : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
            val request: Request = chain.request()
            val response: Response = chain.proceed(request)

            return response.newBuilder()
                .header("Cache-Control", cacheControl.toString())
                .build()
        }
    }

}
