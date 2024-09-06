package com.example.pdinewsapp.api

import com.example.pdinewsapp.models.NewsResponse
import com.example.pdinewsapp.util.Constants.Companion.API_KEY
import com.example.pdinewsapp.util.Constants.Companion.CNN_SOURCE
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsAPI {

    @GET("v2/top-headlines")
    suspend fun getHeadlines(
        @Query("sources")
        sources: String = CNN_SOURCE,
//        @Query("country")
//        countryCode: String = "us",
        @Query("page")
        pageNumber: Int = 1,
        @Query("apiKey")
        apiKey: String = API_KEY
    ): Response<NewsResponse>

    @GET("v2/everything")
    suspend fun searchForNews(
        @Query("q")
        searchQuery: String,
        @Query("page")
        pageNumber: Int = 1,
        @Query("sources")
        sources: String = CNN_SOURCE,
        @Query("apiKey")
        apiKey: String = API_KEY
    ): Response<NewsResponse>
}