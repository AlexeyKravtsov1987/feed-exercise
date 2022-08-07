package com.lightricks.feedexercise.data

/**
 * This data class is our internal representation of a feed item.
 * In a real-life application this is template meta-data for a user's project.
 * The rest of the properties are left out for brevity.
 */
data class FeedItem(
    val id: String,
    val thumbnailUrl: String,
    val isPremium: Boolean
)
