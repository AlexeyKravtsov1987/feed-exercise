package com.lightricks.feedexercise.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class FeedItemDBEntity(
    @PrimaryKey val id: String,
    val thumbnailUrl: String,
    val isPremium: Boolean
)