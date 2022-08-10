package com.lightricks.feedexercise.data

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import androidx.room.Room
import com.bumptech.glide.load.Transformation
import com.lightricks.feedexercise.database.FeedDatabase
import com.lightricks.feedexercise.database.FeedItemDBEntity
import com.lightricks.feedexercise.network.FeedApiService
import com.lightricks.feedexercise.network.TemplateMetadataItem
import com.lightricks.feedexercise.network.TemplatesMetadata
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

/**
 * This is our data layer abstraction. Users of this class don't need to know
 * where the data actually comes from (network, database or somewhere else).
 */

class FeedRepository(private val db: FeedDatabase,
                     private val feedApiService: FeedApiService) {
    private val urlPrefix = FeedApiService.BASE_URL + "catalog/thumbnails/"

    fun refresh(): Completable {
        return feedApiService.getFeed()
            .subscribeOn(Schedulers.io())
            .flatMapCompletable {
                updateFromTemplatesMetadata(it)
            }
    }

    fun fetchData(): LiveData<List<FeedItem>>{
        return Transformations
            .map(db.feedItemDao().getAll()){ it.toFeedItems() }
}

    private fun updateFromTemplatesMetadata(list: TemplatesMetadata): Completable {
        return db.feedItemDao().deleteAll()
            .andThen(list?.templatesMetadata?.let {
                db.feedItemDao().insertAll(it.toEntities())
            })
    }

    private fun List<FeedItemDBEntity>.toFeedItems(): List<FeedItem> {
        return map {
            FeedItem(it.id, it.thumbnailUrl, it.isPremium)
        }
    }
    private fun List<TemplateMetadataItem>.toEntities(): List<FeedItemDBEntity> {
        return map { FeedItemDBEntity(
            it.id,
            urlPrefix + it.templateThumbnailURI,
            it.isPremium)
        }
    }
    companion object {
        private lateinit var INSTANCE: FeedRepository

        fun getRepository(context: Context): FeedRepository {
            synchronized(FeedDatabase::class.java) {
                if (!::INSTANCE.isInitialized) {
                    INSTANCE = FeedRepository(
                        FeedDatabase.getDatabase(context),
                        FeedApiService.FeedApi.service)
                }
            }
            return INSTANCE
        }
        fun getRepository(): FeedRepository? { return INSTANCE }
    }
}