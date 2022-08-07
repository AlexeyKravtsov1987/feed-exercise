package com.lightricks.feedexercise.database

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single


@Dao
interface FeedItemDao {
    @Insert
    fun insertAll(vararg entity: FeedItemDBEntity): Completable

    @Query("DELETE FROM FeedItemDBEntity")
    fun deleteAll(): Completable

    @Query("SELECT * FROM FeedItemDBEntity")
    fun getAll(): Observable<List<FeedItemDBEntity>>

    @Query("SELECT COUNT(*) FROM FeedItemDBEntity")
    fun getCount(): Single<Integer>

}