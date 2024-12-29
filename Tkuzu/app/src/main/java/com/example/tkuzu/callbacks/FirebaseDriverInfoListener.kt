package com.example.tkuzu.callbacks

import com.example.tkuzu.models.DriverGeoModel


interface FirebaseDriverInfoListener {
    fun onDriverInfoLoadSuccess(driverGeoModel: DriverGeoModel?)
}