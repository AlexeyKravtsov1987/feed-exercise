package com.lightricks.feedexercise.ui.feed

import androidx.lifecycle.*
import com.lightricks.feedexercise.data.FeedItem
import com.lightricks.feedexercise.data.FeedRepository
import com.lightricks.feedexercise.database.FeedDatabase
import com.lightricks.feedexercise.database.FeedItemDBEntity
import com.lightricks.feedexercise.network.FeedApiService
import com.lightricks.feedexercise.util.Event
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.lang.IllegalArgumentException

/**
 * This view model manages the data for [FeedFragment].
 */
open class FeedViewModel(private val db:FeedDatabase) : ViewModel() {
    private val stateInternal: MutableLiveData<State> = MutableLiveData<State>(DEFAULT_STATE)
    private val networkErrorEvent = MutableLiveData<Event<String>>()
    private val repository: FeedRepository = FeedRepository()
    private var listOffset = 0;

    fun getIsLoading(): LiveData<Boolean> {
        return Transformations.map(stateInternal){ it.isLoading }
    }

    fun getIsEmpty(): LiveData<Boolean> {
        return Transformations.map(stateInternal) { it.feedItems.isNullOrEmpty() }
    }

    fun getFeedItems(): LiveData<List<FeedItem>> {
        return Transformations.map(stateInternal){
            if(it.feedItems.isNullOrEmpty()) { listOf<FeedItem>() }
            else it.feedItems
        }
    }

    private fun handleNetworkError(error: Throwable?) {
        updateState { State(null,false) }
        networkErrorEvent.postValue(Event(error.toString()))
    }



    fun getNetworkErrorEvent(): LiveData<Event<String>> = networkErrorEvent

    init {
        val feedApiService = FeedApiService.create()
        repository.setFeedApiService(feedApiService)
        repository.setDB(db)
        refresh()
    }

    fun refresh() {
        repository.refresh()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({handleResponseAction()},
                {error->handleNetworkError(error)}
            )
    }

    private fun handleResponseAction() {
        var res :MutableList<FeedItem> = mutableListOf()
        db.feedItemDao().getAll()
            .map { it.toFeedItems() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                val heads = it.take(listOffset)
                val tails = it.drop(listOffset)
                res.addAll(tails+heads)
                updateState { State(res,false) }
            }
        listOffset++
    }


    private fun List<FeedItemDBEntity>.toFeedItems(): List<FeedItem> {
        return map {
            FeedItem(it.id, it.thumbnailUrl, it.isPremium)
        }
    }


    private fun updateState(transform: State.() -> State) {
        stateInternal.value = transform(getState())
    }

    private fun getState(): State {
        return stateInternal.value!!
    }

    data class State(
        val feedItems: List<FeedItem>?,
        val isLoading: Boolean)

    companion object {
        private val DEFAULT_STATE = State(
            feedItems = null,
            isLoading = false)
    }
}

/**
 * This class creates instances of [FeedViewModel].
 * It's not necessary to use this factory at this stage. But if we will need to inject
 * dependencies into [FeedViewModel] in the future, then this is the place to do it.
 */
class FeedViewModelFactory(private val db:FeedDatabase) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(FeedViewModel::class.java)) {
            throw IllegalArgumentException("factory used with a wrong class")
        }
        @Suppress("UNCHECKED_CAST")
        return FeedViewModel(db) as T
    }
}
