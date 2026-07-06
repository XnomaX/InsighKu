package com.example.insightku.core.di

import android.content.Context
import com.example.insightku.core.utils.ErrorBus
import com.example.insightku.core.datastore.SessionManager
import com.example.insightku.feature.home.data.DataStoreStreakPreferences
import com.example.insightku.feature.home.domain.StreakPreferences
import com.example.insightku.feature.budgeting.data.repository.AutoAllocationDataSourceImpl
import com.example.insightku.feature.budgeting.domain.engine.AutoAllocationDataSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSessionManager(@ApplicationContext context: Context): SessionManager {
        return SessionManager(context)
    }

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    /**
     * ErrorBus sudah @Singleton via @Inject constructor, tapi kita eksplisit provide-nya
     * di sini agar jelas bahwa ini adalah single instance di seluruh app.
     * Semua ViewModel yang perlu report error inject ini, bukan RootViewModel.
     */
    @Provides
    @Singleton
    fun provideErrorBus(): ErrorBus = ErrorBus()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {

    @Binds
    @Singleton
    abstract fun bindStreakPreferences(impl: DataStoreStreakPreferences): StreakPreferences

    @Binds
    @Singleton
    abstract fun bindAutoAllocationDataSource(impl: AutoAllocationDataSourceImpl): AutoAllocationDataSource
}

