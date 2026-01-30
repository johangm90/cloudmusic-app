package com.jgm90.cloudmusic.core.di

import com.jgm90.cloudmusic.core.innertube.InnerTube
import com.jgm90.cloudmusic.core.innertube.YouTubeRepository
import com.jgm90.cloudmusic.core.network.RestInterface
import com.jgm90.cloudmusic.core.util.SharedUtils
import coil3.ImageLoader
import com.jgm90.cloudmusic.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(
            if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        )
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(SharedUtils.server)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideRestInterface(retrofit: Retrofit): RestInterface {
        return retrofit.create(RestInterface::class.java)
    }

    @Provides
    @Singleton
    fun provideInnerTube(client: OkHttpClient): InnerTube {
        return InnerTube(httpClient = client)
    }

    @Provides
    @Singleton
    fun provideYouTubeRepository(innerTube: InnerTube): YouTubeRepository {
        return YouTubeRepository(innerTube)
    }

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: android.content.Context): ImageLoader {
        return ImageLoader.Builder(context).build()
    }
}
