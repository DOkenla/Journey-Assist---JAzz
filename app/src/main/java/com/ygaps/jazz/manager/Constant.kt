package com.ygaps.jazz.manager

import android.graphics.Color

object Constant {
    const val tokenExpire = 24*60*60
    const val BASE_URL = "http://192.168.0.13:3000"
    const val ggServerClientID = "669170260445-autdoperohvfbf0d24nq3p713m77l4ml.apps.googleusercontent.com"
    const val ggclientSecret = "fjVoYy4505lqYHrsUf4mTdEH"
    const val ggMapApiKey = "AIzaSyCVUhoqFfF-bNHpeMbAUJJ-0ZtfxuGgy40"
    val cityList: ArrayList<String> = arrayListOf("London",
        "Portsmouth",
        "Manchester",
        "Liverpool",
        "Blackpool",
        "Brighton",
        "Leeds",
        "Reading",
        "Bristol",
        "Great Yarmouth",
        "Edinburgh",
        "Coventry",
        "Gosport  ",
        "Southampton",
        "Bognor Regis",
        "Dublin",
        "Cork",
        "York",
        "Glasgow",
        "Woking")

    val colors = intArrayOf(
        Color.parseColor("#0e9d58"),
        Color.parseColor("#bfd047"),
        Color.parseColor("#ffc105"),
        Color.parseColor("#ef7e14"),
        Color.parseColor("#d36259")
    )
}