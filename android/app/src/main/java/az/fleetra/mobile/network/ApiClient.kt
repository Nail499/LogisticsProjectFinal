package az.fleetra.mobile.network

import az.fleetra.mobile.config.ApiConfig
import az.fleetra.mobile.data.TokenStore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    val gson: Gson = GsonBuilder().create()

    private val authInterceptor = okhttp3.Interceptor { chain ->
        val original = chain.request()
        val token = TokenStore.cachedToken
        val request = if (token != null) {
            original.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val customerApi: CustomerApi by lazy { retrofit.create(CustomerApi::class.java) }
    val driverApi: DriverApi by lazy { retrofit.create(DriverApi::class.java) }
    val profileApi: ProfileApi by lazy { retrofit.create(ProfileApi::class.java) }
    val trackingApi: TrackingApi by lazy { retrofit.create(TrackingApi::class.java) }
    val notificationApi: NotificationApi by lazy { retrofit.create(NotificationApi::class.java) }
    val pushApi: PushApi by lazy { retrofit.create(PushApi::class.java) }
    val chatApi: ChatApi by lazy { retrofit.create(ChatApi::class.java) }
}
