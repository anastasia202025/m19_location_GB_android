package com.example.homework19.service

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import com.example.homework19.model.Landmark

interface OpenTripMapService {

    @GET("places/radius")
    suspend fun getLandmarks(
        @Query("radius") radius: Int = 10_000, // Радиус поиска в метрах (10 км)
        @Query("lon") longitude: Double, // Долгота центра поиска
        @Query("lat") latitude: Double, // Широта центра поиска
        @Query("kinds") kinds: String = "sightseeing", // Типы достопримечательностей
        @Query("limit") limit: Int = 10, // Максимальное количество результатов
        @Query("apikey") apiKey: String // API ключ
    ): Response<List<Landmark>>
}