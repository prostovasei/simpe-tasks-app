package com.prostovasei.simpletasks.core.di

import android.content.Context
import com.prostovasei.simpletasks.core.files.ContentResolverDocumentOpener
import com.prostovasei.simpletasks.core.files.DocumentOpener
import com.prostovasei.simpletasks.core.id.IdGenerator
import com.prostovasei.simpletasks.core.id.UuidIdGenerator
import com.prostovasei.simpletasks.core.time.DayTicker
import com.prostovasei.simpletasks.core.time.SystemDayTicker
import com.prostovasei.simpletasks.core.time.SystemTimeProvider
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.core.update.AppUpdater
import com.prostovasei.simpletasks.core.update.GitHubAppUpdater
import com.prostovasei.simpletasks.data.local.db.AppDatabase
import com.prostovasei.simpletasks.data.repository.RoomChecklistRepository
import com.prostovasei.simpletasks.data.repository.RoomBackupRepository
import com.prostovasei.simpletasks.data.repository.RoomProjectRepository
import com.prostovasei.simpletasks.data.repository.RoomTaskRepository
import com.prostovasei.simpletasks.domain.repository.ChecklistRepository
import com.prostovasei.simpletasks.domain.repository.BackupRepository
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {
    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider = SystemTimeProvider()

    @Provides
    @Singleton
    fun provideDayTicker(time: TimeProvider): DayTicker = SystemDayTicker(time)

    @Provides
    @Singleton
    fun provideIdGenerator(): IdGenerator = UuidIdGenerator()

    @Provides
    @Singleton
    fun provideDocumentOpener(@ApplicationContext context: Context): DocumentOpener = ContentResolverDocumentOpener(context)

    @Provides
    @Singleton
    fun provideAppUpdater(@ApplicationContext context: Context, @IoDispatcher io: CoroutineDispatcher): AppUpdater = GitHubAppUpdater(context, io)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context, time: TimeProvider): AppDatabase =
        AppDatabase.build(context, time)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindTaskRepository(impl: RoomTaskRepository): TaskRepository

    @Binds
    abstract fun bindChecklistRepository(impl: RoomChecklistRepository): ChecklistRepository

    @Binds
    abstract fun bindProjectRepository(impl: RoomProjectRepository): ProjectRepository

    @Binds
    abstract fun bindBackupRepository(impl: RoomBackupRepository): BackupRepository
}
