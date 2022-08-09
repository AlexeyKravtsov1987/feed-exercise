package com.lightricks.feedexercise.data

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lightricks.feedexercise.database.FeedDatabase
import com.lightricks.feedexercise.database.FeedItemDBEntity
import com.lightricks.feedexercise.database.FeedItemDao
import com.lightricks.feedexercise.network.FeedApiService
import com.lightricks.feedexercise.network.TemplatesMetadata
import com.lightricks.feedexercise.ui.feed.FeedViewModel
import com.lightricks.feedexercise.util.Event
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
    fun refreshShouldFailOnBadNetworkAddress(){
        FeedApiService.BASE_URL="http://badaddress.baddomain"
        val service =
            FeedApiService.FeedApi.service
        val repository = FeedRepository(db, service)
        val viewModel = FeedViewModel(repository)
        var errorString=""

        val errorObserver = object : Observer<Event<String>> {
            override fun onChanged(t: Event<String>?) {
                if (t != null)
                    errorString = t.peekContent()
                }
        }
        viewModel.getNetworkErrorEvent().observeForever (errorObserver)

        waitForEndOfLoading(viewModel)
        viewModel.getNetworkErrorEvent().removeObserver(errorObserver)
        assert(errorString != "")

    }

    @Test
    fun refreshShouldSaveItemsToDB(){
        val service = object: FeedApiService{
            override fun getFeed(): Single<TemplatesMetadata> {
                return Single.just(serviceResponse)
            }
        }
        val itemListSize=Integer(serviceResponse.templatesMetadata.size)
        val repository = FeedRepository(db, service)
        val observer = repository.refresh().test()

        observer.awaitTerminalEvent()
        observer
            .assertNoErrors()
            .assertComplete()
        userDao
            .getCount().test()
            .assertValues(itemListSize)
    }

    @Test
    fun feedItemsShouldContainDBContent() {
        val mockID = "ID"
        val mockURI = "templateThumbnailURI"
        val service = object: FeedApiService{
            override fun getFeed(): Single<TemplatesMetadata> {
                return Single.just(serviceResponse)
            }
        }

        val repository = FeedRepository(db, service)
        val DBEntries = listOf(FeedItemDBEntity(mockID,mockURI,true) )

        val viewModel = FeedViewModel(repository)
        waitForEndOfLoading(viewModel)

        val deletion = db.feedItemDao().deleteAll().test()
        deletion.awaitTerminalEvent()
        val insertion = db.feedItemDao().insertAll(DBEntries).test()
        insertion.awaitTerminalEvent()

        val property = viewModel.getFeedItems()

        val observerLD = object : Observer<List<FeedItem>> {
            override fun onChanged(t: List<FeedItem>?) {
                if (!t.isNullOrEmpty())
                    property.removeObserver(this)
            }
        }

        property.observeForever (observerLD)
        while(property.hasObservers()){ }

        assert(property.value?.size == DBEntries.size)
        val item = property.value!!.get(0)
        assert(item.isPremium)
        assert(item.id == mockID)
        assert(item.thumbnailUrl == mockURI)
    }

    private fun waitForEndOfLoading(viewModel:FeedViewModel){
        var isLoading = false
        val loadingObserver = object : Observer<Boolean> {
            override fun onChanged(t: Boolean?) {
                if (t!!)
                    isLoading = true
                if (isLoading && !t!!) {
                    viewModel.getIsLoading().removeObserver(this)
                }
            }
        }
        viewModel.getIsLoading().observeForever(loadingObserver)
        while (viewModel.getIsLoading().hasObservers()){}

    }
}
