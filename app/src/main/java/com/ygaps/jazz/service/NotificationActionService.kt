

package com.ygaps.jazz.service
import android.app.IntentService
import android.content.Context

import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import com.google.gson.JsonObject
import com.ygaps.jazz.ResponseToInvitation
import com.ygaps.jazz.network.model.ApiServiceResponseInvitaion
import com.ygaps.jazz.network.model.WebAccess
import org.jetbrains.anko.notificationManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.R.string.cancel
import android.app.NotificationManager

import androidx.core.app.RemoteInput
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ygaps.jazz.ResponseSendNotice
import com.ygaps.jazz.manager.doAsync
import com.ygaps.jazz.network.model.ApiServiceSendJourneyNotice


class NotificationActionService : IntentService("MyService") {

    override fun onHandleIntent(intent: Intent?) {
        val action : String = intent!!.getAction().toString()
        if ("Journey_Invitation_Accept".equals(action)) {
            Log.d("abab", "click aceept")
            ApiRequestResponseInvitation(true)
        }
        else if ("Journey_Invitation_Decline".equals(action)) {
            Log.d("abab", "click decline")
            ApiRequestResponseInvitation(false)
        }
        else if ("Journey_Follow_Reply".equals(action)) {
            Log.d("abab", "DAY NE ")
            processInlineReply(intent)
        }
        else if ("journey_follow_location".equals(action)) {
            val intenttemp = Intent("notify-new-message")
            intenttemp.putExtra("lat", intent.extras!!.getDouble("lat"))
            intenttemp.putExtra("long", intent.extras!!.getDouble("long"))
            intenttemp.putExtra("type", intent.extras!!.getString("type"))
            LocalBroadcastManager.getInstance(this).sendBroadcast(intenttemp)
        }
    }

    private fun processInlineReply(intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        if (remoteInput != null) {
            val reply = remoteInput!!.getCharSequence("chat_reply")!!.toString()
            val journeyId = intent.extras!!.getString("journeyId")!!
            ApiRequestSendNotice(reply,journeyId)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(4)
        }
    }


    fun ApiRequestResponseInvitation(isAccept: Boolean) {
        val service = WebAccess.retrofit.create(ApiServiceResponseInvitaion::class.java)
        val jsonObject = JsonObject()

        val sharePref : SharedPreferences = getSharedPreferences("logintoken", Context.MODE_PRIVATE)
        val token = sharePref.getString("token", "notoken")!!
        val journeyId = sharePref.getString("journeyId", "100")!!

        jsonObject.addProperty("journeyId", journeyId)
        jsonObject.addProperty("isAccepted", isAccept)



        val call = service.response(token,jsonObject)

        call.enqueue(object : Callback<ResponseToInvitation> {
            override fun onFailure(call: Call<ResponseToInvitation>, t: Throwable) {
                Toast.makeText(applicationContext, t.message, Toast.LENGTH_LONG).show()
            }

            override fun onResponse(
                call: Call<ResponseToInvitation>,
                response: Response<ResponseToInvitation>
            ) {
                if (response.code() != 200) {
                    Toast.makeText(applicationContext, response.raw().toString(), Toast.LENGTH_LONG).show()
                } else {
                    var result1 = "You have declined the invitation"
                    var result2 = "You have accepted the invitation"
                    if (isAccept) {
                        Toast.makeText(applicationContext, result2, Toast.LENGTH_LONG).show()
                    }
                    else {
                        Toast.makeText(applicationContext, result1, Toast.LENGTH_LONG).show()
                    }
                    notificationManager.cancel(6)
                }
            }
        })
    }


    fun ApiRequestSendNotice(notice : String, journeyId : String) {
        doAsync {
            val service = WebAccess.retrofit.create(ApiServiceSendJourneyNotice::class.java)
            val body = JsonObject()

            val sharePref : SharedPreferences = getSharedPreferences("logintoken", Context.MODE_PRIVATE)
            val token = sharePref.getString("token", "notoken")!!
            val userId = sharePref.getInt("userId", 126)

            body.addProperty("journeyId", journeyId)
            body.addProperty("userId", userId)
            body.addProperty("noti", notice)
            val call = service.sendNotice(token , body )
            call.enqueue(object : Callback<ResponseSendNotice> {
                override fun onFailure(call: Call<ResponseSendNotice>, t: Throwable) {
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_LONG).show()
                }

                override fun onResponse(
                    call: Call<ResponseSendNotice>,
                    response: Response<ResponseSendNotice>
                ) {
                    if (response.code() != 200) {
                        Toast.makeText(applicationContext, response.errorBody().toString(), Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(applicationContext, "Reply success!", Toast.LENGTH_LONG).show()
                    }
                }
            })
        }.execute()
    }
}