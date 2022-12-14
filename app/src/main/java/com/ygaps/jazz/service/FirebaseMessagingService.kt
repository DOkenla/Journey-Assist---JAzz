package com.ygaps.jazz.service

import android.app.*
import android.content.Context.NOTIFICATION_SERVICE
import androidx.core.content.ContextCompat.getSystemService
import android.media.RingtoneManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.FirebaseMessagingService
import java.lang.Exception
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.provider.Settings
import android.widget.Toast

import com.google.firebase.iid.FirebaseInstanceId
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.ygaps.jazz.manager.doAsync
import com.ygaps.jazz.network.model.ApiServicePutFcmToken
import com.ygaps.jazz.network.model.ApiServiceJourneyComment
import com.ygaps.jazz.network.model.WebAccess
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.content.BroadcastReceiver
import android.os.Build

import android.graphics.BitmapFactory
import androidx.core.app.RemoteInput
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ygaps.jazz.*
import com.ygaps.jazz.network.model.ApiServiceResponseInvitaion
import com.ygaps.jazz.util.util
import org.jetbrains.anko.accessibilityManager


class FirebaseMessagingService : FirebaseMessagingService() {
    internal var userToken : String ?= null
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("notifas", "NOTIFICATION")
        for (i in remoteMessage.data) {
            Log.d("notifas", i.key + " -> " + i.value )
        }
        sendNotification(remoteMessage.data)
        super.onMessageReceived(remoteMessage)

    }

    private fun sendNotification(map: Map<String, String>) {
        val channelId = getString(R.string.project_id)
        val channel = NotificationChannel(
            channelId,
            "JAZZist",
            NotificationManager.IMPORTANCE_HIGH
        )
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        val notiType = map["type"]!!.toInt()
        
        if (notiType == 5) {
            val intent = Intent(this, JourneyInfoActivity::class.java)

            if (userToken == null) {
                val sharePref : SharedPreferences = getSharedPreferences("logintoken", Context.MODE_PRIVATE)
                userToken = sharePref.getString("token", "notoken")!!
            }

            intent.putExtra("token", userToken)
            intent.putExtra("journeyID", map["journeyId"]!!.toInt())
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)


            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setLargeIcon(
                    BitmapFactory.decodeResource(
                        resources,
                        R.drawable.ic_launcher_background
                    )
                )
                .setContentTitle(map.get("userId") + " comment to " + map.get("journeyId"))
                .setContentText(map["comment"])
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)



            // Since android Oreo notification channel is needed.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                notificationManager.createNotificationChannel(channel)
            }
            notificationManager.notify(5, notificationBuilder.build())

        }
        else if (notiType == 6) {
            val acceptIntent = Intent(this, NotificationActionService::class.java).setAction("Journey_Invitation_Accept")
            val declineIntent = Intent(this, NotificationActionService::class.java).setAction("Journey_Invitation_Decline")

            val pendingIntentAccept = PendingIntent.getService(this, 0, acceptIntent, PendingIntent.FLAG_CANCEL_CURRENT)
            val pendingIntentDecline = PendingIntent.getService(this, 0, declineIntent, PendingIntent.FLAG_CANCEL_CURRENT)


            val sharePref : SharedPreferences = getSharedPreferences("logintoken", Context.MODE_PRIVATE)
            val editor = sharePref.edit()
            editor.putString("journeyId", map["id"])
            editor.apply()


            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setLargeIcon(
                    BitmapFactory.decodeResource(
                        resources,
                        R.drawable.ic_launcher_background
                    )
                )
                .setContentTitle("Journey Invitation")
                .setContentText(map.get("hostName") + " invites you to journey " + map.get("name"))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .addAction(
                    NotificationCompat.Action(
                        android.R.drawable.btn_default,
                        "Decline",
                        pendingIntentDecline
                    )
                )
                .addAction(
                    NotificationCompat.Action(
                        android.R.drawable.btn_default,
                        "Accept",
                        pendingIntentAccept
                    )
                )


            // Since android Oreo notification channel is needed.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                notificationManager.createNotificationChannel(channel)
            }
            notificationManager.notify(6, notificationBuilder.build())
        }
        else if (notiType == 4) {
            val intent = Intent(this, NotificationActionService::class.java).setAction("Journey_Follow_Reply")
            intent.putExtra("journeyId", map["journeyId"])
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)


            val resultPendingIntent: PendingIntent? = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

            val replyLabel = "Reply"
            val remoteInput = RemoteInput.Builder("chat_reply")
                .setLabel(replyLabel)
                .build()

            //notify new message
            notifyNewMessage(map["journeyId"]!!)

            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setLargeIcon(
                    BitmapFactory.decodeResource(
                        resources,
                        R.drawable.ic_launcher_background
                    )
                )
                .setContentTitle(map.get("userId") + " send a notification to " + map.get("journeyId"))
                .setContentText(map["notification"])
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentIntent(resultPendingIntent)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .addAction(
                    NotificationCompat.Action.Builder(
                        R.drawable.ic_star_gold_24dp,replyLabel ,resultPendingIntent)
                        .addRemoteInput(remoteInput)
                        .setAllowGeneratedReplies(true)
                        .build()
                    )




            // Since android Oreo notification channel is needed.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                notificationManager.createNotificationChannel(channel)
            }


            notificationManager.notify(4, notificationBuilder.build())
        }

        else if (notiType == 2) {
            val intent = Intent(this, NotificationActionService::class.java).setAction("journey_follow_location")
            intent.putExtra("lat", map["lat"]!!.toDouble())
            intent.putExtra("long", map["long"]!!.toDouble())
            intent.putExtra("type", "2")
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val resultPendingIntent: PendingIntent? = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setLargeIcon(
                    BitmapFactory.decodeResource(
                        resources,
                        R.drawable.ic_launcher_background
                    )
                )
                .setContentTitle(map.get("userId") + " send a notification to " + map.get("journeyId"))
                .setContentText("Type : Problem On Road" )
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentIntent(resultPendingIntent)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)


            // Since android Oreo notification channel is needed.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                notificationManager.createNotificationChannel(channel)
            }
            notificationManager.notify(2, notificationBuilder.build())
        }
        else if (notiType == 9) {
            val intent = Intent("notify-new-message")
            intent.putExtra("type", "9")
            intent.putExtra("data", map["memPos"].toString())

            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
        else if (notiType == 1) {
            val intent = Intent(this, NotificationActionService::class.java).setAction("journey_follow_location")
            intent.putExtra("lat", map["lat"]!!.toDouble())
            intent.putExtra("long", map["long"]!!.toDouble())
            intent.putExtra("type", "1")
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val resultPendingIntent: PendingIntent? = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setLargeIcon(
                    BitmapFactory.decodeResource(
                        resources,
                        R.drawable.ic_launcher_background
                    )
                )
                .setContentTitle(map.get("userId") + " send a notification to " + map.get("journeyId"))
                .setContentText("Type : Police Position" )
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentIntent(resultPendingIntent)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)


            // Since android Oreo notification channel is needed.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                notificationManager.createNotificationChannel(channel)
            }
            notificationManager.notify(1, notificationBuilder.build())
        }
        else if (notiType == 3) {
            val intent = Intent(this, NotificationActionService::class.java).setAction("journey_follow_location")
            intent.putExtra("lat", map["lat"]!!.toDouble())
            intent.putExtra("long", map["long"]!!.toDouble())
            intent.putExtra("type", "3")
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val resultPendingIntent: PendingIntent? = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setLargeIcon(
                    BitmapFactory.decodeResource(
                        resources,
                        R.drawable.ic_launcher_background
                    )
                )
                .setContentTitle(map.get("userId") + " send a notification to " + map.get("journeyId"))
                .setContentText("Type : Speed Limit (${map["speed"]})" )
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentIntent(resultPendingIntent)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)


            // Since android Oreo notification channel is needed.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                notificationManager.createNotificationChannel(channel)
            }
            notificationManager.notify(3, notificationBuilder.build())
        }


    }

    companion object {
        private val TAG = "MyFirebaseMsgService"
    }

    override fun onMessageSent(p0: String) {
        super.onMessageSent(p0)
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }

    override fun onSendError(p0: String, p1: Exception) {
        super.onSendError(p0, p1)
    }

    override fun onNewToken(p0: String) {
//        val sharePref : SharedPreferences = getSharedPreferences("logintoken", Context.MODE_PRIVATE)
////        val editor = sharePref.edit()
////        Log.d("onnewtoken", p0 + " - " + userToken)
////        editor.putString("fcmToken", p0)
////        editor.putBoolean("fcmTokenPushed", false)
////        editor.apply()
        super.onNewToken(p0)
    }

    private fun notifyNewMessage(journeyId : String) {
        Log.d("sender", "Broadcasting message")
        val intent = Intent("notify-new-message")
        // You can also include some extra data.
        intent.putExtra("journeyId", journeyId)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }




    fun ApiRequestPutFcmToken( FcmToken : String) {
        doAsync {
            val service = WebAccess.retrofit.create(ApiServicePutFcmToken::class.java)

            val jsonObject = JsonObject()
            var uniqueId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID)

            jsonObject.addProperty("fcmToken", FcmToken)
            jsonObject.addProperty("deviceId", uniqueId)
            jsonObject.addProperty("platform", 1)
            jsonObject.addProperty("appVersion", "1.0")

            if (userToken == null) {
                val sharePref : SharedPreferences = getSharedPreferences("logintoken", Context.MODE_PRIVATE)
                userToken = sharePref.getString("token", "notoken")!!
            }

            val call = service.putToken(userToken!!,jsonObject)
            call.enqueue(object : Callback<ResponsePutFcmToken> {
                override fun onFailure(call: Call<ResponsePutFcmToken>, t: Throwable) {
                    //Toast.makeText(applicationContext,"Fcmtoken : " + t.message, Toast.LENGTH_LONG).show()
                }
                override fun onResponse(
                    call: Call<ResponsePutFcmToken>,
                    response: Response<ResponsePutFcmToken>
                ) {
                    if (response.code() != 200) {
                        val gson = Gson()
                        val type = object : TypeToken<ErrorResponse>() {}.type
                        var errorResponse: ErrorResponse? = gson.fromJson(response.errorBody()!!.charStream(), type)
                        //Toast.makeText(applicationContext, "Fcmtoken : " + errorResponse!!.message, Toast.LENGTH_LONG).show()
                    } else {
                        //Toast.makeText(applicationContext, "Put Token OK!!", Toast.LENGTH_LONG).show()
                    }
                }
            })
        }.execute()
    }



}