package com.example.insightku.feature.home.di

import com.example.insightku.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * DI for the receipt-scan (Donut/Flask) feature. The data-source, repository,
 * and network monitor all use @Inject constructors, so only the OkHttp client
 * and the two BuildConfig-backed strings need explicit providers here.
 */
@Module
@InstallIn(SingletonComponent::class)
object ReceiptScanModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        // Donut inference can take several seconds on CPU; give it room.
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Named("receiptApiBaseUrl")
    fun provideReceiptApiBaseUrl(): String = BuildConfig.RECEIPT_API_BASE_URL

    @Provides
    @Named("receiptApiKey")
    fun provideReceiptApiKey(): String = BuildConfig.RECEIPT_API_KEY
}
