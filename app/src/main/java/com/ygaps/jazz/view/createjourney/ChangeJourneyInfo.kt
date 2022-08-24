package com.ygaps.jazz.view.createjourney

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.ygaps.jazz.*
import com.ygaps.jazz.manager.doAsync
import com.ygaps.jazz.network.model.ApiServiceGetJourneyInfo
import com.ygaps.jazz.network.model.ApiServiceUpdateJourney
import com.ygaps.jazz.network.model.WebAccess
import com.ygaps.jazz.util.util
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_change_journey_info.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.app.Activity
import android.content.Intent




class ChangeJourneyInfo : AppCompatActivity() {

    var token : String = ""
    var journeyId : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.hide()
        setContentView(R.layout.activity_change_journey_info)
        token = intent.extras!!.getString("token", "notoken")
        journeyId = intent.extras!!.getInt("userId", 128)
        spinnerJourneyStatus.setItems("Cancel", "Open", "Started" , "Closed")
        ApiRequest()

        editChangeDateStart.setOnClickListener {
            util.setOnClickDate(editChangeDateStart,this@ChangeJourneyInfo)
        }

        editChangeDateEnd.setOnClickListener {
            util.setOnClickDate(editChangeDateEnd,this@ChangeJourneyInfo)
        }


        saveInfo.setOnClickListener {
            ApiRequestSaveUpdate()
        }

    }



    fun ApiRequest() {
        doAsync {
            val service = WebAccess.retrofit.create(ApiServiceGetJourneyInfo::class.java)
            val call = service.getJourneyInfo(token,journeyId)
            call.enqueue(object : Callback<ResponseJourneyInfo> {
                override fun onFailure(call: Call<ResponseJourneyInfo>, t: Throwable) {
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_LONG).show()
                }

                override fun onResponse(
                    call: Call<ResponseJourneyInfo>,
                    response: Response<ResponseJourneyInfo>
                ) {
                    if (response.code() != 200) {
                        Toast.makeText(applicationContext, response.errorBody().toString(), Toast.LENGTH_LONG).show()
                    } else {
                        val data = response.body()!!
                        editChangeJourneyName.setText(data.name)
                        editChangeDateStart.setText(util.longToDate(data.startDate))
                        editChangeDateEnd.setText(util.longToDate(data.endDate))
                        editChangeChildNum.setText(data.childs.toString())
                        editChangeAdultNum.setText(data.adults.toString())
                        editChangeMinCostJourney.setText(data.minCost.toString())
                        editChangeMaxCostJourney.setText(data.maxCost.toString())
                        spinnerJourneyStatus.selectedIndex = data.status + 1
                        isPrivateCheckbox.isChecked = data.isPrivate
                    }
                }
            })
        }.execute()
    }

    fun ApiRequestSaveUpdate() {
            val service = WebAccess.retrofit.create(ApiServiceUpdateJourney::class.java)
            val body = JsonObject()
            body.addProperty("id", journeyId)
            body.addProperty("status", spinnerJourneyStatus.selectedIndex-1)
            body.addProperty("name", editChangeJourneyName.text.toString())
            body.addProperty("minCost", editChangeMinCostJourney.text.toString().toLong())
            body.addProperty("maxCost", editChangeMaxCostJourney.text.toString().toLong())
            body.addProperty("startDate", util.dateToLong(editChangeDateStart.text.toString()))
            body.addProperty("endDate", util.dateToLong(editChangeDateEnd.text.toString()))
            body.addProperty("adults", editChangeAdultNum.text.toString().toInt())
            body.addProperty("childs", editChangeChildNum.text.toString().toInt())
            body.addProperty("isPrivate", isPrivateCheckbox.isChecked)

            val call = service.update(token,body)
            call.enqueue(object : Callback<ResponseUpdateJourneyInfo> {
                override fun onFailure(call: Call<ResponseUpdateJourneyInfo>, t: Throwable) {
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_LONG).show()
                }

                override fun onResponse(
                    call: Call<ResponseUpdateJourneyInfo>,
                    response: Response<ResponseUpdateJourneyInfo>
                ) {
                    if (response.code() != 200) {
                        val gson = Gson()
                        val type = object : TypeToken<ErrorResponse>() {}.type
                        var errorResponse: ErrorResponse? = gson.fromJson(response.errorBody()!!.charStream(), type)
                        Toast.makeText(applicationContext, errorResponse!!.message, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(applicationContext, "Success", Toast.LENGTH_LONG).show()
                        val returnIntent = Intent()
                        returnIntent.putExtra("id", journeyId)
                        setResult(Activity.RESULT_OK, returnIntent)
                        finish()
                    }
                }
            })
    }
}
