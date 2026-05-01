
package com.example.insightku.di

import com.example.insightku.viewmodel.RootViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RootViewModelModule {

    @Provides
    @Singleton
    fun provideRootViewModel(): RootViewModel {
        return RootViewModel()
    }
}
