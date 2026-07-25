cat > app/src/main/java/com/ultrastream/app/di/NetworkModule.kt <<'EOF'
package com.ultrastream.app.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ultrastream.app.network.AllDebridApi
import com.ultrastream.app.network.PremiumizeApi
import com.ultrastream.app.network.RealDebridApi
import com.ultrastream.app.network.StremioApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("User-Agent", "UltraStream/1.0 (Android)")
                    .header("Referer", "https://ultrastream.app/")
                    .method(original.method, original.body)
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://dummy.base.url/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideStremioApi(retrofit: Retrofit): StremioApi {
        return retrofit.create(StremioApi::class.java)
    }

    @Provides
    @Singleton
    fun provideRealDebridApi(retrofit: Retrofit): RealDebridApi {
        return retrofit.newBuilder()
            .baseUrl("https://api.real-debrid.com/rest/1.0/")
            .build()
            .create(RealDebridApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAllDebridApi(retrofit: Retrofit): AllDebridApi {
        return retrofit.newBuilder()
            .baseUrl("https://api.alldebrid.com/v4/")
            .build()
            .create(AllDebridApi::class.java)
    }

    @Provides
    @Singleton
    fun providePremiumizeApi(retrofit: Retrofit): PremiumizeApi {
        return retrofit.newBuilder()
            .baseUrl("https://www.premiumize.me/api/")
            .build()
            .create(PremiumizeApi::class.java)
    }
}
EOF
