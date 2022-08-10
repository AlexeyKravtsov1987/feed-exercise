package com.lightricks.feedexercise.network

import io.reactivex.Single
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import retrofit2.http.GET

interface FeedApiService {
    @GET("feed.json")
    fun getFeed() : Single<TemplatesMetadata>

    companion object {
        var BASE_URL = "https://assets.swishvideoapp.com/Android/demo/"
    }

    object FeedApi {
        val service = Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .baseUrl(BASE_URL)
            .build()
            .create<FeedApiService>()
    }
}