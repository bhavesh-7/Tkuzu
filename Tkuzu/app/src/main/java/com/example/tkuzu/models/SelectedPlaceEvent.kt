package com.example.tkuzu.models

import com.google.android.gms.maps.model.LatLng

class SelectedPlaceEvent(var origin: LatLng, var destination:LatLng) {
    val originString: String
        get() = StringBuilder()
            .append(origin.latitude)
            .append(",")
            .append(origin.longitude)
            .toString()

    val destinationString: String
        get() = StringBuilder()
            .append(destination.latitude)
            .append(",")
            .append(destination.longitude)
            .toString()
}