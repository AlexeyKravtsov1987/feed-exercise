package com.lightricks.feedexercise.data

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lightricks.feedexercise.database.FeedDatabase
import com.lightricks.feedexercise.database.FeedItemDao
import com.lightricks.feedexercise.network.FeedApiService
import com.lightricks.feedexercise.network.TemplatesMetadata
import com.lightricks.feedexercise.ui.feed.FeedViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class FeedRepositoryTest {
   //todo: add the tests here

    private lateinit var userDao: FeedItemDao
    private lateinit var db: FeedDatabase
    private lateinit var appContext: Context
    private lateinit var jsonContent: String
    private lateinit var serviceResponse: TemplatesMetadata

    @Before
    fun createDb() {
        appContext = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            appContext, FeedDatabase::class.java).build()
        userDao = db.feedItemDao()
    }

    @Before
    fun getJsonStr(){
        jsonContent = appContext.assets
            .open("get_feed_response.json")
            .bufferedReader()
            .use{ it.readText() }
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val adapter = moshi.adapter<TemplatesMetadata>(TemplatesMetadata::class.java)
        serviceResponse = adapter.fromJson(jsonContent)!!
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Test
    fun combinedTest() {

        val service = object: FeedApiService{
            override fun getFeed(): Single<TemplatesMetadata> {
                return Single.just(serviceResponse)
            }
        }

        val repository = FeedRepository()
        repository.setFeedApiService(service)
        repository.setDB(db)
        val viewModel = FeedViewModel(repository)
        var transformationDone=false
        var feedItemsList = mutableListOf<FeedItem>()
        val observerLD = object : Observer<List<FeedItem>> {
            override fun onChanged(t: List<FeedItem>?) {
                if(!t.isNullOrEmpty()) {
                    if (transformationDone) {
                        feedItemsList = t as MutableList<FeedItem>
                        viewModel.getFeedItems().removeObserver(this)
                    }
                    else transformationDone = true
                }
            }
        }
        
        viewModel.getFeedItems().observeForever (observerLD)

        val observer = repository.refresh().test()

        observer.awaitTerminalEvent()
        observer
            .assertNoErrors()
            .assertComplete()

        val len = serviceResponse.templatesMetadata.size
        checkEquals(len)

        assert(feedItemsList.size == len)
    }

    private fun checkEquals(size: Int) {
        db.feedItemDao().getCount().test().assertValues(Integer(size))
    }

}
