package com.lightricks.feedexercise.network

import io.reactivex.Single
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET

/**
 * todo: add the FeedApiService interface and the Retrofit and Moshi code here
 */
interface FeedApiService {
    @GET("feed.json")
    fun getFeed() : Single<TemplatesMetadata>

    companion object {

        var BASE_URL = "https://assets.swishvideoapp.com/Android/demo/"

        fun create() : FeedApiService {

            val retrofit = Retrofit.Builder()
                .addConverterFactory(MoshiConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl(BASE_URL)
                .build()
            return retrofit.create(FeedApiService::class.java)
        }
    }
}