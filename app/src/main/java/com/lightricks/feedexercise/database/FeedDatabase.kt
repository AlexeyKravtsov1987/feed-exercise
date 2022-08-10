package com.lightricks.feedexercise.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FeedItemDBEntity::class], version = 1)
abstract class FeedDatabase : RoomDatabase() {
    abstract fun feedItemDao(): FeedItemDao

    companion object {
        private lateinit var INSTANCE: FeedDatabase

        fun getDatabase(context: Context): FeedDatabase {
            synchronized(FeedDatabase::class.java) {
                if (!::INSTANCE.isInitialized) {
                    INSTANCE = Room.databaseBuilder(
                        context,
                        FeedDatabase::class.java, "feed-database"
                    ).build()
                }
            }
            return INSTANCE
        }
    }
}