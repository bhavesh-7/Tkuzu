package com.example.tkuzudriver

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.Toast
import com.example.tkuzudriver.remote.FCMSendData
import com.example.tkuzudriver.remote.FCMService
import com.example.tkuzudriver.remote.RetrofitFCM
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

object UserUtils {
    fun updateUser(
        view: View?,
        updateData: Map<String, Any>
    ) {
        FirebaseDatabase.getInstance()
            .getReference(Constants.DRIVER_INFO_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser?.uid!!)
            .updateChildren(updateData)
            .addOnSuccessListener {
                Snackbar.make(view!!,"Information updated successfully!",Snackbar.LENGTH_LONG).show()
            }.addOnFailureListener { e ->
                Snackbar.make(view!!,e.message!!,Snackbar.LENGTH_LONG).show()
            }
    }

    fun updateToken(context: Context, token: String) {
        val tokenModel = TokenModel()
        tokenModel.token = token

        FirebaseDatabase.getInstance()
            .getReference(Constants.TOKEN_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser?.uid!!)
            .setValue(tokenModel)
            .addOnFailureListener { e -> Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show() }
            .addOnSuccessListener {  }
    }

    fun sendDeclineRequest(view: View, activity: Activity?, key: String) {

        val compositeDisposable = CompositeDisposable()
        val fcmService = RetrofitFCM.instance!!.create(FCMService::class.java)
        //Get token
        FirebaseDatabase.getInstance()
            .getReference(Constants.TOKEN_REFERENCE)
            .child(key)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {

                        val tokenModel = snapshot.getValue(TokenModel::class.java)

                        val notificationData: MutableMap<String, String> = HashMap()
                        notificationData.put(Constants.NOTI_TITLE, Constants.REQUEST_DRIVER_DECLINE)
                        notificationData.put(Constants.NOTI_BODY, "Driver Declined the Request")
                        notificationData.put(Constants.DRIVER_KEY,FirebaseAuth.getInstance().currentUser!!.uid)

                        val fcmSendData = FCMSendData(tokenModel!!.token, notificationData)

                        compositeDisposable.add(fcmService.sendNotification(fcmSendData)
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ fcmResponse ->
                                if (fcmResponse.success == 0) {
                                    compositeDisposable.clear()
                                    Snackbar.make(view,activity!!.getString(R.string.send_request_to_driver_faield),
                                        Snackbar.LENGTH_LONG).show()
                                }
                            },
                                { t: Throwable? ->
                                    compositeDisposable.clear()
                                    Snackbar.make(view, t!!.message!!,Snackbar.LENGTH_LONG).show()
                                }
                            ))

                    } else {
                        Snackbar.make(
                            view,
                            activity!!.getString(R.string.token_not_found),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Snackbar.make(view, error.message, Snackbar.LENGTH_LONG).show()
                }

            })
    }

    fun driverConfirmed(view: View, activity: Activity?, key: String, time: String) {

        val compositeDisposable = CompositeDisposable()
        val fcmService = RetrofitFCM.instance!!.create(FCMService::class.java)
        //Get token
        FirebaseDatabase.getInstance()
            .getReference(Constants.TOKEN_REFERENCE)
            .child(key)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {

                        val tokenModel = snapshot.getValue(TokenModel::class.java)

                        val notificationData: MutableMap<String, String> = HashMap()
                        notificationData.put(Constants.NOTI_TITLE, Constants.tkuzuConfirmed)
                        notificationData.put(Constants.NOTI_BODY, "Driver Accepted the request and Reaching in $time")
                        notificationData.put(Constants.DRIVER_KEY,FirebaseAuth.getInstance().currentUser!!.uid)

                        val fcmSendData = FCMSendData(tokenModel!!.token, notificationData)

                        compositeDisposable.add(fcmService.sendNotification(fcmSendData)
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ fcmResponse ->
                                if (fcmResponse.success == 0) {
                                    compositeDisposable.clear()
                                    Snackbar.make(view,activity!!.getString(R.string.send_request_to_driver_faield),
                                        Snackbar.LENGTH_LONG).show()
                                }
                            },
                                { t: Throwable? ->
                                    compositeDisposable.clear()
                                    Snackbar.make(view, t!!.message!!,Snackbar.LENGTH_LONG).show()
                                }
                            ))

                    } else {
                        Snackbar.make(
                            view,
                            activity!!.getString(R.string.token_not_found),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Snackbar.make(view, error.message, Snackbar.LENGTH_LONG).show()
                }

            })
    }

    fun tkuzuConfirmed(view: View, activity: Activity?, key: String) {

        val compositeDisposable = CompositeDisposable()
        val fcmService = RetrofitFCM.instance!!.create(FCMService::class.java)
        //Get token
        FirebaseDatabase.getInstance()
            .getReference(Constants.TOKEN_REFERENCE)
            .child(key)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {

                        val tokenModel = snapshot.getValue(TokenModel::class.java)

                        val notificationData: MutableMap<String, String> = HashMap()
                        notificationData.put(Constants.NOTI_TITLE, Constants.tkuzuConfirmed)
                        notificationData.put(Constants.NOTI_BODY, "OTP Verified, The Ride is confirmed,\nHave a safe Tkuzu!")
                        notificationData.put(Constants.DRIVER_KEY,FirebaseAuth.getInstance().currentUser!!.uid)

                        val fcmSendData = FCMSendData(tokenModel!!.token, notificationData)

                        compositeDisposable.add(fcmService.sendNotification(fcmSendData)
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ fcmResponse ->
                                if (fcmResponse.success == 0) {
                                    compositeDisposable.clear()
                                    Snackbar.make(view,activity!!.getString(R.string.send_request_to_driver_faield),
                                        Snackbar.LENGTH_LONG).show()
                                }
                            },
                                { t: Throwable? ->
                                    compositeDisposable.clear()
                                    Snackbar.make(view, t!!.message!!,Snackbar.LENGTH_LONG).show()
                                }
                            ))

                    } else {
                        Snackbar.make(
                            view,
                            activity!!.getString(R.string.token_not_found),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Snackbar.make(view, error.message, Snackbar.LENGTH_LONG).show()
                }

            })
    }

    fun sendOTPRequest(view: View, activity: Activity?, key: String, otp: String) {
        val compositeDisposable = CompositeDisposable()
        val fcmService = RetrofitFCM.instance!!.create(FCMService::class.java)
        // Get token
        FirebaseDatabase.getInstance()
            .getReference(Constants.TOKEN_REFERENCE)
            .child(key)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val tokenModel = snapshot.getValue(TokenModel::class.java)

                        val notificationData: MutableMap<String, String> = HashMap()
                        notificationData.put(Constants.NOTI_TITLE, Constants.REQUEST_OTP)
                        notificationData.put(Constants.NOTI_BODY, "Your OTP is $otp")
                        notificationData.put(Constants.OTP_KEY, otp)

                        val fcmSendData = FCMSendData(tokenModel!!.token, notificationData)

                        compositeDisposable.add(fcmService.sendNotification(fcmSendData)
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ fcmResponse ->
                                if (fcmResponse.success == 0) {
                                    compositeDisposable.clear()
                                    Snackbar.make(
                                        view,
                                        activity!!.getString(R.string.send_otp_failed),
                                        Snackbar.LENGTH_LONG
                                    ).show()
                                }
                            },
                                { t: Throwable? ->
                                    compositeDisposable.clear()
                                    Snackbar.make(view, t!!.message!!, Snackbar.LENGTH_LONG).show()
                                }
                            ))
                    } else {
                        Snackbar.make(
                            view,
                            activity!!.getString(R.string.token_not_found),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Snackbar.make(view, error.message, Snackbar.LENGTH_LONG).show()
                }
            })
    }
}