package com.lightricks.feedexercise.data

import android.accounts.NetworkErrorException
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lightricks.feedexercise.database.FeedDatabase
import com.lightricks.feedexercise.database.FeedItemDBEntity
import com.lightricks.feedexercise.database.FeedItemDao
import com.lightricks.feedexercise.network.FeedApiService
import com.lightricks.feedexercise.network.TemplatesMetadata
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.reactivex.Single
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import retrofit2.http.GET
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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


    class MockInterceptor : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
                return chain.proceed(chain.request())
                    .newBuilder()
                    .code(404)
                    .protocol(Protocol.HTTP_2)
                    .build()
        }
    }

    interface MockFeedApiService : FeedApiService {
        @GET("feed.json")
        override fun getFeed() : Single<TemplatesMetadata>

        object FeedApi {
            val service = Retrofit.Builder()
                .addConverterFactory(MoshiConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(OkHttpClient.Builder()
                    .addInterceptor(MockInterceptor()).build())
                .baseUrl("https://google.com")
                .build()
                .create<MockFeedApiService>()
        }
    }
    @Test
    fun refreshShouldFailWith404Code(){
        val mockFeedApiService = MockFeedApiService.FeedApi.service
        val repository = FeedRepository(db, mockFeedApiService)

        val observable = repository.refresh()

        val observer=observable.test()
        observer.awaitTerminalEvent()
        observer.assertError(HttpException::class.java)
    }
    
    class MockFeedApiServiceException(message: String?) : NetworkErrorException(message)


    @Test
    fun refreshShouldFailOnApiServiceError(){
        val simpleMockFeedApiService = object : FeedApiService{
            override fun getFeed(): Single<TemplatesMetadata> {
                return Single.error(MockFeedApiServiceException(""))
            }

        }
        val repository = FeedRepository(db, simpleMockFeedApiService)

        val observable = repository.refresh()

        val observer=observable.test()
        observer.awaitTerminalEvent()
        observer.assertError(MockFeedApiServiceException::class.java)
    }

    @Test
    fun refreshShouldSaveItemsToDB(){
        val service = object: FeedApiService{
            override fun getFeed(): Single<TemplatesMetadata> {
                return Single.just(serviceResponse)
            }
        }
        val itemListSize=serviceResponse.templatesMetadata.size
        val repository = FeedRepository(db, service)
        val observer = repository.refresh().test()

        observer.awaitTerminalEvent()
        observer
            .assertNoErrors()
            .assertComplete()
        val observeGetCount = userDao
            .getCount()
            .test()
            .assertResult(Integer(itemListSize))

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

        val initialContent = repository.fetchData().blockingObserve()!!
        assert(initialContent.isEmpty())
        val insertion = userDao.insertAll(DBEntries).test()
        insertion.awaitTerminalEvent()

        val updatedContent = repository.fetchData().blockingObserve()!!

        assert(updatedContent.size == DBEntries.size)
        val item = updatedContent.get(0)
        assert(item.isPremium)
        assert(item.id == mockID)
        assert(item.thumbnailUrl == mockURI)
    }

    private fun <T> LiveData<T>.blockingObserve(): T? {
        var value: T? = null
        val latch = CountDownLatch(1)
        val observer = object : Observer<T> {
            override fun onChanged(t: T) {
                value = t
                latch.countDown()
                removeObserver(this)
            }
        }

        observeForever(observer)
        latch.await(5, TimeUnit.SECONDS)
        return value
    }
}
