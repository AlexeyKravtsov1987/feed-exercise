package com.lightricks.feedexercise.ui.feed

import androidx.lifecycle.*
import com.lightricks.feedexercise.data.FeedItem
import com.lightricks.feedexercise.data.FeedRepository
import com.lightricks.feedexercise.util.Event
import io.reactivex.BackpressureStrategy
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.lang.IllegalArgumentException

/**
 * This view model manages the data for [FeedFragment].
 */
open class FeedViewModel(private val repository: FeedRepository) : ViewModel() {
    private val isLoading: MutableLiveData<Boolean> = MutableLiveData<Boolean>()
    private val networkErrorEvent = MutableLiveData<Event<String>>()
    private var listOffset = 0

    private val feedItems: LiveData<List<FeedItem>> =
        LiveDataReactiveStreams.fromPublisher(
            repository.fetchData().toFlowable(BackpressureStrategy.DROP))

    fun getIsLoading(): LiveData<Boolean> {
        return isLoading
    }

    fun getIsEmpty(): LiveData<Boolean> {
        return Transformations.map(feedItems) { it.isEmpty() }
    }

    fun getFeedItems(): LiveData<List<FeedItem>> {
        return Transformations.map(feedItems) {
            val size = it.size
            if (size != 0) {
                val offset = listOffset % size
                it.takeLast(size - offset) + it.take(offset)
            } else listOf()
        }
    }

    private fun handleNetworkError(error: Throwable?) {
        isLoading.postValue(false)
        networkErrorEvent.postValue(Event(error.toString()))
    }



    fun getNetworkErrorEvent(): LiveData<Event<String>> = networkErrorEvent

    init {
        refresh()
    }

    fun refresh() {
        isLoading.postValue(true)
        repository.refresh()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { isLoading.postValue(false) },
                { error->handleNetworkError(error) }
            )
        listOffset++
    }

}

/**
 * This class creates instances of [FeedViewModel].
 * It's not necessary to use this factory at this stage. But if we will need to inject
 * dependencies into [FeedViewModel] in the future, then this is the place to do it.
 */
class FeedViewModelFactory(private val repository: FeedRepository) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(FeedViewModel::class.java)) {
            throw IllegalArgumentException("factory used with a wrong class")
        }
        @Suppress("UNCHECKED_CAST")
        return FeedViewModel(repository) as T
    }
}
