package com.example.tkuzudriver.services

import com.example.tkuzudriver.Constants
import com.example.tkuzudriver.UserUtils
import com.example.tkuzudriver.model.DriverRequestReceived
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.greenrobot.eventbus.EventBus

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (FirebaseAuth.getInstance().currentUser != null) {
            UserUtils.updateToken(this, token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        if (message != null) {
            val data = message.data
            if (data[Constants.NOTI_TITLE].equals(Constants.REQUEST_DRIVER_TITLE)) {
                EventBus.getDefault()
                    .postSticky(
                        DriverRequestReceived(
                        data[Constants.RIDER_KEY]!!,
                        data[Constants.PICKUP_LOCATION]!!
                    )
                    )
            } else {
                Constants.showNotification(
                    this, kotlin.random.Random.nextInt(),
                    data[Constants.NOTI_TITLE],
                    data[Constants.NOTI_BODY],
                    null
                )
            }

        }
    }
}