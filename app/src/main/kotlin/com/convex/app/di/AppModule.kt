package com.convex.app.di

import android.content.Context
import com.convex.app.data.ffmpeg.FfmpegRepository
import com.convex.app.data.prefs.AppPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAppPreferences(
        @ApplicationContext context: Context,
    ): AppPreferences = AppPreferences(context)

    @Provides
    @Singleton
    fun provideFfmpegRepository(
        @ApplicationContext context: Context,
    ): FfmpegRepository = FfmpegRepository(context)
}
