package com.techenglish.app.di

import android.content.Context
import androidx.room.Room
import com.techenglish.app.core.database.TechEnglishDatabase
import com.techenglish.app.core.database.dao.AcademicTermDao
import com.techenglish.app.core.database.dao.CategoryDao
import com.techenglish.app.core.database.dao.CodeSnippetDao
import com.techenglish.app.core.database.dao.CommentsCacheDao
import com.techenglish.app.core.database.dao.CommunityDecksDao
import com.techenglish.app.core.database.dao.CustomImportedDocDao
import com.techenglish.app.core.database.dao.DocArticleDao
import com.techenglish.app.core.database.dao.OtaContentMetaDao
import com.techenglish.app.core.database.dao.ProgrammingLanguageDao
import com.techenglish.app.core.database.dao.RoomMessagesCacheDao
import com.techenglish.app.core.database.dao.ScenarioDao
import com.techenglish.app.core.database.dao.SocialCacheDao
import com.techenglish.app.core.database.dao.SquadCacheDao
import com.techenglish.app.core.database.dao.StudyLogDao
import com.techenglish.app.core.database.dao.SyncQueueDao
import com.techenglish.app.core.database.dao.TimeAttackRecordDao
import com.techenglish.app.core.database.dao.WordDao
import com.techenglish.app.core.database.dao.WorkPhraseDao
import com.techenglish.app.data.repository.AcademicVocabularyRepositoryImpl
import com.techenglish.app.data.repository.CustomDocRepositoryImpl
import com.techenglish.app.data.repository.DocumentationRepositoryImpl
import com.techenglish.app.data.repository.ProgrammingLanguageRepositoryImpl
import com.techenglish.app.data.repository.ScenarioRepositoryImpl
import com.techenglish.app.data.repository.SettingsRepositoryImpl
import com.techenglish.app.data.repository.StudyLogRepositoryImpl
import com.techenglish.app.data.repository.TimeAttackRepositoryImpl
import com.techenglish.app.data.repository.VocabularyRepositoryImpl
import com.techenglish.domain.algorithm.Sm2Engine
import com.techenglish.domain.repository.AcademicVocabularyRepository
import com.techenglish.domain.repository.CustomDocRepository
import com.techenglish.domain.repository.DocumentationRepository
import com.techenglish.domain.repository.ProgrammingLanguageRepository
import com.techenglish.domain.repository.ScenarioRepository
import com.techenglish.domain.repository.SettingsRepository
import com.techenglish.domain.repository.StudyLogRepository
import com.techenglish.domain.repository.TimeAttackRepository
import com.techenglish.domain.repository.VocabularyRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTechEnglishDatabase(
        @ApplicationContext context: Context
    ): TechEnglishDatabase = Room.databaseBuilder(
        context,
        TechEnglishDatabase::class.java,
        TechEnglishDatabase.DATABASE_NAME
    )
        .createFromAsset(TechEnglishDatabase.SEED_ASSET_PATH)
        .addMigrations(*TechEnglishDatabase.ALL_MIGRATIONS)
        .build()

    @Provides fun provideCategoryDao(db: TechEnglishDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideProgrammingLanguageDao(db: TechEnglishDatabase): ProgrammingLanguageDao = db.programmingLanguageDao()
    @Provides fun provideCodeSnippetDao(db: TechEnglishDatabase): CodeSnippetDao = db.codeSnippetDao()
    @Provides fun provideWordDao(db: TechEnglishDatabase): WordDao = db.wordDao()
    @Provides fun provideDocArticleDao(db: TechEnglishDatabase): DocArticleDao = db.docArticleDao()
    @Provides fun provideScenarioDao(db: TechEnglishDatabase): ScenarioDao = db.scenarioDao()
    @Provides fun provideWorkPhraseDao(db: TechEnglishDatabase): WorkPhraseDao = db.workPhraseDao()
    @Provides fun provideStudyLogDao(db: TechEnglishDatabase): StudyLogDao = db.studyLogDao()
    @Provides fun provideAcademicTermDao(db: TechEnglishDatabase): AcademicTermDao = db.academicTermDao()
    @Provides fun provideCustomImportedDocDao(db: TechEnglishDatabase): CustomImportedDocDao = db.customImportedDocDao()
    @Provides fun provideTimeAttackRecordDao(db: TechEnglishDatabase): TimeAttackRecordDao = db.timeAttackRecordDao()
    @Provides fun provideSyncQueueDao(db: TechEnglishDatabase): SyncQueueDao = db.syncQueueDao()
    @Provides fun provideSocialCacheDao(db: TechEnglishDatabase): SocialCacheDao = db.socialCacheDao()
    @Provides fun provideOtaContentMetaDao(db: TechEnglishDatabase): OtaContentMetaDao = db.otaContentMetaDao()
    @Provides fun provideCommentsCacheDao(db: TechEnglishDatabase): CommentsCacheDao = db.commentsCacheDao()
    @Provides fun provideCommunityDecksDao(db: TechEnglishDatabase): CommunityDecksDao = db.communityDecksDao()
    @Provides fun provideRoomMessagesCacheDao(db: TechEnglishDatabase): RoomMessagesCacheDao = db.roomMessagesCacheDao()
    @Provides fun provideSquadCacheDao(db: TechEnglishDatabase): SquadCacheDao = db.squadCacheDao()
}

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    @Provides
    @Singleton
    fun provideSm2Engine(): Sm2Engine = Sm2Engine()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindVocabularyRepository(impl: VocabularyRepositoryImpl): VocabularyRepository

    @Binds
    @Singleton
    abstract fun bindProgrammingLanguageRepository(impl: ProgrammingLanguageRepositoryImpl): ProgrammingLanguageRepository

    @Binds
    @Singleton
    abstract fun bindDocumentationRepository(impl: DocumentationRepositoryImpl): DocumentationRepository

    @Binds
    @Singleton
    abstract fun bindScenarioRepository(impl: ScenarioRepositoryImpl): ScenarioRepository

    @Binds
    @Singleton
    abstract fun bindStudyLogRepository(impl: StudyLogRepositoryImpl): StudyLogRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindAcademicVocabularyRepository(impl: AcademicVocabularyRepositoryImpl): AcademicVocabularyRepository

    @Binds
    @Singleton
    abstract fun bindCustomDocRepository(impl: CustomDocRepositoryImpl): CustomDocRepository

    @Binds
    @Singleton
    abstract fun bindTimeAttackRepository(impl: TimeAttackRepositoryImpl): TimeAttackRepository
}
