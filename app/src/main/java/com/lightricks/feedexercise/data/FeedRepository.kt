package com.lightricks.feedexercise.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.Transformations
import androidx.room.Room
import com.bumptech.glide.load.Transformation
import com.lightricks.feedexercise.database.FeedDatabase
import com.lightricks.feedexercise.database.FeedItemDBEntity
import com.lightricks.feedexercise.network.FeedApiService
import com.lightricks.feedexercise.network.TemplatesMetadata
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

/**
 * This is our data layer abstraction. Users of this class don't need to know
 * where the data actually comes from (network, database or somewhere else).
 */

class FeedRepository(private var db: FeedDatabase,
                     private var feedApiService: FeedApiService) {
    private val urlPrefix = FeedApiService.BASE_URL + "catalog/thumbnails/"

    fun setFeedApiService(feedApiService: FeedApiService) {
        this.feedApiService=feedApiService
    }

    fun setDB(db: FeedDatabase) {
        this.db=db
    }

    fun refresh(): Completable {
        return feedApiService.getFeed()
            .subscribeOn(Schedulers.io())
            .flatMapCompletable {
                updateFromTemplatesMetadata(it)
            }
    }

    fun fetchData(): LiveData<List<FeedItem>>{
        return Transformations.map(db.feedItemDao().getAll()){ it.toFeedItems() }
        /*
        // to use with Observable<> DAO
        return LiveDataReactiveStreams
            .fromPublisher(
                db.feedItemDao().getAll()
                    .map { it.toFeedItems() }
                    .toFlowable(BackpressureStrategy.LATEST)
            )

         */
    }

    private fun updateFromTemplatesMetadata(list: TemplatesMetadata): Completable {
        val resp = list?.templatesMetadata?.map { it ->
            FeedItemDBEntity(
                it.id,
                urlPrefix + it.templateThumbnailURI,
                it.isPremium
            )
        }
        return db.feedItemDao().deleteAll()
            .andThen(resp?.let {
                db.feedItemDao()
                    .insertAll(it)
            })
    }

    private fun List<FeedItemDBEntity>.toFeedItems(): List<FeedItem> {
        return map {
            FeedItem(it.id, it.thumbnailUrl, it.isPremium)
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

        fun getRepository():FeedRepository?{
            return INSTANCE
        }
    }
}