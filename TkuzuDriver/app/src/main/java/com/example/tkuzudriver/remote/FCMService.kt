package com.example.tkuzudriver.remote

import io.reactivex.rxjava3.core.Observable
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface FCMService {
    @Headers(
        "Content-Type:application/json",
        "Authorization:key=AAAASlDaFsc:APA91bF5i7aJqhKda6YInQw8Sh6TVYjvKIK_CYzCtZd5V3Ox0y_ehbyLS9FkZ-GRQrhDlf_rY6nqN4zXErdQqAZgPFXOaBpNVia6r5EPOsAuigpLYt82QaCZpeg1b9g75iwRxuOyyf31"
    )

    @POST("fcm/send")
    fun sendNotification(@Body body: FCMSendData?): Observable<FCMResponse>
}