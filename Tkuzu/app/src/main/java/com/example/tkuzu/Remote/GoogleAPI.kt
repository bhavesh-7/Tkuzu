package com.example.tkuzu.Remote

import io.reactivex.rxjava3.core.Observable
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleAPI {
    @GET("maps/api/directions/json")
    fun getDirections(
        @Query("mode") mode: String?,
        @Query("transit_routing_preference") transit_routing: String?,
        @Query("origin") from: String?,
        @Query("destination") to: String?,
        @Query("key") key: String
    ): Observable<String>?
}