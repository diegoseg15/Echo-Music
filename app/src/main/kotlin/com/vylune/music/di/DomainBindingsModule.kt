package com.vylune.music.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.vylune.music.core.repository.PlaylistRepositoryImpl
import com.vylune.music.core.repository.SongRepositoryImpl
import com.vylune.music.domain.repositories.PlaylistRepository
import com.vylune.music.domain.repositories.SongRepository
import com.vylune.music.musicflow.MusicFlowFeedSource
import com.vylune.music.musicflow.MusicFlowRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DomainBindingsModule {

  @Binds
  @Singleton
  abstract fun bindSongRepository(
    impl: SongRepositoryImpl
  ): SongRepository

  @Binds
  @Singleton
  abstract fun bindPlaylistRepository(
    impl: PlaylistRepositoryImpl
  ): PlaylistRepository

  @Binds
  @Singleton
  abstract fun bindMusicFlowFeedSource(
    impl: MusicFlowRepository
  ): MusicFlowFeedSource
}
