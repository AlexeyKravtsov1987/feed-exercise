package com.lightricks.feedexercise.data

import com.lightricks.feedexercise.database.FeedDatabase
import com.lightricks.feedexercise.database.FeedItemDBEntity
import com.lightricks.feedexercise.network.FeedApiService
import com.lightricks.feedexercise.network.TemplatesMetadata
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

/**
 * This is our data layer abstraction. Users of this class don't need to know
 * where the data actually comes from (network, database or somewhere else).
 */
class FeedRepository {
    private lateinit var db: FeedDatabase
    private lateinit var feedApiService: FeedApiService
    private val urlPrefix = "https://assets.swishvideoapp.com/Android/demo/catalog/thumbnails/"
    fun setFeedApiService(feedApiService: FeedApiService){
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

    fun fetchData(): Observable<List<FeedItem>>{
        return db.feedItemDao().getAll().map { it.toFeedItems() }
    }

    private fun updateWithFeedItemList(list: List<FeedItem>) :Completable {
        return db.feedItemDao().deleteAll()
            .andThen(db.feedItemDao()
                .insertAll(*list.toFeedItemDBEntities().toTypedArray()))
    }

    private fun updateFromTemplatesMetadata(list: TemplatesMetadata) :Completable{
        val resp = list?.templatesMetadata?.map { it ->
            FeedItem(it.id,
                urlPrefix + it.templateThumbnailURI,
                it.isPremium) }
        return updateWithFeedItemList(resp!!)
    }

    private fun List<FeedItem>.toFeedItemDBEntities(): List<FeedItemDBEntity> {
        return map {
            FeedItemDBEntity(it.id, it.thumbnailUrl, it.isPremium)
        }
    }

    private fun List<FeedItemDBEntity>.toFeedItems(): List<FeedItem> {
        return map {
            FeedItem(it.id, it.thumbnailUrl, it.isPremium)
        }
    }

}