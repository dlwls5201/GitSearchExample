package com.example.kotlinexample.search

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.processors.BehaviorProcessor
import java.util.*
import javax.inject.Inject

class SearchRepository @Inject constructor(
    private val remote: SearchRemoteDataSource,
    private val repoDao: RepositoryDao
) {
    private val queryProcessor = BehaviorProcessor.createDefault("Kotlin")
    private val initialLoadProcessor = BehaviorProcessor.createDefault(true)

    val initialLoad: Flowable<Boolean> = initialLoadProcessor.distinctUntilChanged()

    fun setQuery(query: String) {
        queryProcessor.offer(query)
        initialLoadProcessor.offer(true)
    }

    fun loadItemsByQuery(): Flowable<List<Repository>> =
        queryProcessor
            .switchMapSingle {
                remote.searchRepositories(it)
                    .doOnSuccess { initialLoadProcessor.offer(false) }
            }
            .map { it.items ?: emptyList() }

    fun insertRepositoryIntoDatabase(repository: Repository): Completable = Completable.fromAction {
        repoDao.insert(repository.copy(addedTime = Calendar.getInstance().timeInMillis))
    }

    fun deleteRepositoryFromDatabase(repository: Repository): Completable = Completable.fromAction {
        repoDao.delete(repository)
    }

    fun getRepositorySingle(repositoryId: String): Single<Repository> =
        repoDao.getRepositorySingle(repositoryId)

    fun getRepositories(): Flowable<List<Repository>> = repoDao.getRepositoriesFlowable()
}