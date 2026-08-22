package com.pumpwatch.app.presentation.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pumpwatch.app.data.news.NewsSource
import com.pumpwatch.app.domain.NewsItem
import com.pumpwatch.app.domain.SentimentScorer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NewsViewModel(
    private val newsSource: NewsSource
) : ViewModel() {

    private val _news = MutableStateFlow<List<NewsItem>>(emptyList())
    val news: StateFlow<List<NewsItem>> = _news.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadNews(symbol: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val rawNews = newsSource.fetchNews(symbol)
                _news.value = rawNews.map { item ->
                    item.copy(sentiment = SentimentScorer.score(item.title))
                }
            } catch (e: Exception) {
                _news.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
