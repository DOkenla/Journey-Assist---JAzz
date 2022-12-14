package com.ygaps.jazz.view.journey

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.GoogleMap

import com.google.android.gms.maps.SupportMapFragment


import android.location.Location

import com.google.android.gms.maps.CameraUpdateFactory

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.IntentSender.*
import android.graphics.*
import android.graphics.drawable.*
import android.location.LocationManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.*
import android.transition.Slide
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.google.maps.android.PolyUtil
import com.makeramen.roundedimageview.RoundedTransformationBuilder
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import com.ygaps.jazz.*
import com.ygaps.jazz.R
import com.ygaps.jazz.manager.Constant
import com.ygaps.jazz.manager.doAsync
import com.ygaps.jazz.network.model.*
import com.ygaps.jazz.util.util
import kotlinx.android.synthetic.main.activity_journey_follow.*
import kotlinx.android.synthetic.main.activity_journey_follow.view.*
import kotlinx.android.synthetic.main.activity_journey_info.*
import kotlinx.android.synthetic.main.alert_to_destination.view.*
import kotlinx.android.synthetic.main.audio_play_layout.*
import kotlinx.android.synthetic.main.audio_play_layout.view.*
import kotlinx.android.synthetic.main.bottom_sheet_notifi_on_road.*
import kotlinx.android.synthetic.main.bottomsheet.view.*
import kotlinx.android.synthetic.main.fragment_explorer.view.*
import kotlinx.android.synthetic.main.item_notification_on_road.view.*
import kotlinx.android.synthetic.main.item_notification_on_road.view.notiDistance
import kotlinx.android.synthetic.main.item_notification_on_road.view.notiNote
import kotlinx.android.synthetic.main.item_notification_on_road.view.notiPolice
import kotlinx.android.synthetic.main.item_notification_on_road.view.notiProblem
import kotlinx.android.synthetic.main.item_notification_on_road.view.notiSpeed
import kotlinx.android.synthetic.main.item_notification_on_road.view.notiSpeedEdit
import kotlinx.android.synthetic.main.item_notification_on_road.view.notiSpeedView
import kotlinx.android.synthetic.main.popup_chat.view.*
import kotlinx.android.synthetic.main.popup_create_notification_on_road.*
import kotlinx.android.synthetic.main.popup_create_notification_on_road.view.*
import kotlinx.android.synthetic.main.popup_notification_onroad.view.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import java.io.File
import java.io.IOException

import kotlin.Exception
import kotlin.collections.ArrayList


class JourneyFollowActivity : AppCompatActivity(), OnMapReadyCallback {

    lateinit var mGoogleMap: GoogleMap
    lateinit var myLocation: Location
    lateinit var destinationLatLng: LatLng
    lateinit var polyLineToDestination : Polyline
    private lateinit var fusedLocationProviderClient : FusedLocationProviderClient
    var mUserId : Int = 0
    var mJourneyId : Int = 0
    var mToken : String = ""
    var mListChat = ArrayList<notification>()
    var mListMember = ArrayList<member>()
    var mChatAdapter = ChatAdapter(mListChat)


    var isPopupOpen = false
    val runningTargets = mutableListOf<Target>()
    lateinit var mChatRecyclerView: RecyclerView
    lateinit var mMessageReceiver : BroadcastReceiver
    lateinit var mLocationRequest: LocationRequest

    internal var myLocationMarker : Marker ?= null

    internal var hasInitMove = false
    internal var hasInitTimeCounter = false

    var notiOnRoad = ArrayList<notificationonroad>()
    var mNotificationOnRoadAdapter = NotificationOnRoadAdapter(notiOnRoad)
    var notiOnRoadMarker = ArrayList<Marker>()

    var currentPathPolyline = ArrayList<Polyline>()

    internal var lastLocationLatLng : LatLng ?= null

    private var output: String? = null
    private var mediaRecorder: MediaRecorder? = null
    private var state: Boolean = false
    private var isRecording: Boolean = false
    private var recordingStopped: Boolean = false

    val memberPos = ArrayList<memPosChild>()
    val memberPosMarker = ArrayList<Marker>()

    internal var reachedDestination = false
    internal var hasSendLandingTime = false

    internal var currentMemberChoosing = 0
    internal var desId = 0

    lateinit var bottomSheetBehavior: BottomSheetBehavior<View>


    internal val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            myLocation = locationResult.lastLocation
            if (myLocationMarker != null) {
                myLocationMarker?.remove()
            }

            var src = LatLng(myLocation.latitude,myLocation.longitude)
            if (!hasInitMove) {
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(src, 15.0f))
                hasInitMove = true
            }

            if (!reachedDestination) {
                drawPath(src, destinationLatLng)
            }
            else {
                if (!hasSendLandingTime) {
                    //Inflate the dialog with custom view
                    val mDialogView = LayoutInflater.from(this@JourneyFollowActivity).inflate(R.layout.alert_to_destination, null)
                    //AlertDialogBuilder
                    val mBuilder = AlertDialog.Builder(this@JourneyFollowActivity)
                        .setView(mDialogView)
                    //show dialog
                    val  mAlertDialog = mBuilder.show()

                    //login button click of custom layout

                    //cancel button click of custom layout
                    mDialogView.buttonBackDialog.setOnClickListener {
                        //dismiss dialog
                        mAlertDialog.dismiss()
                    }
                    ApiRequestUpdateLandingTime()
                    hasSendLandingTime = true
                }


                // send api finish
            }

            ApiRequestSendCoordinate()
        }
    }



    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.hide()

        setContentView(R.layout.activity_journey_follow)

        mUserId = getSharedPreferences("logintoken", Context.MODE_PRIVATE).getInt("userId", 126)
        mJourneyId = intent.extras!!.getInt("journeyId")
        mToken = intent.extras!!.getString("token")!!

        fusedLocationProviderClient = FusedLocationProviderClient(this.applicationContext)

        destinationLatLng = LatLng(intent.extras!!.getDouble("destinationLat", 10.7629)
            ,intent.extras!!.getDouble("destinationLng", 106.6822))

        desId = intent.extras!!.getInt("desId")



        if (!checkPermissions()) {
            requestPermissions()
        }

        requestRecordPermission()


        mMessageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val onJourneyId = intent!!.extras!!.getString("journeyId", "-1")
                val notiType = intent!!.extras!!.getString("type", "0").toInt()
                Log.d("abab", notiType.toString())
                ApiRequestGetNotificationOnRoad(mJourneyId)
                if (onJourneyId!!.toInt() == mJourneyId) {
                     object :  CountDownTimer(500, 500) {
                         override fun onFinish() {
                             ApiRequestGetNotices()
                         }

                         override fun onTick(millisUntilFinished: Long) {

                         }
                     }.start()

                }

//                if (notiType == 9) {
//                    memberPos.clear()
//                    for (i in memberPosMarker) i.remove()
//                    memberPosMarker.clear()
//                    val data = intent!!.extras!!.getString("data")
//                    Log.d("abab", data)
//                    val JsonArr = JSONArray(data)
//                    for (i in 0..JsonArr.length()-1) {
//                        var temp : JSONObject = JsonArr.getJSONObject(i)
//                        if (temp.getInt("id") == mUserId) continue
//                        var memPosChildTemp = memPosChild(temp.getInt("id"), temp.getDouble("lat"),temp.getDouble("long"))
//                        memberPos.add(memPosChildTemp)
//                        val marker = addMarker(mGoogleMap, LatLng(memPosChildTemp.lat, memPosChildTemp.long), memPosChildTemp.id.toString(), R.drawable.ic_person_pin_circle_black_24dp )
//                        memberPosMarker.add(marker)
//                    }
//                    currentFollowingJourney.setText(" : ${memberPos.size+1}")
//                }
                else if (notiType == 1 || notiType == 2 || notiType == 3) {
                    val lat = intent!!.extras!!.getDouble("lat")!!
                    val long = intent!!.extras!!.getDouble("long")!!
                    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat,long), 15.0f))
                }
            }



        }



        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
            IntentFilter("notify-new-message")
        )

        ApiRequestGetJourneyInfo()


        showLocationPrompt()


        fetchLocation()


        ApiRequestGetNotificationOnRoad(mJourneyId)


        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet_layout)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        bottomSheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // React to state change
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN -> {

                    }
                    BottomSheetBehavior.STATE_EXPANDED -> {
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                    }
                    BottomSheetBehavior.STATE_DRAGGING -> {
                    }
                    BottomSheetBehavior.STATE_SETTLING -> {
                    }
                    BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // React to dragging events
            }
        })



        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this@JourneyFollowActivity)

        journeyFollowChatBtn.setOnClickListener {
            popupChat()
        }

        journeyFollowNotificationBtn.setOnClickListener {
            if (::myLocation.isInitialized) {
                popupCreateNotification(LatLng(myLocation.latitude, myLocation.longitude))
            }
            else {
                Toast.makeText(applicationContext, "Cannot get current location", Toast.LENGTH_LONG).show()
            }
        }

        moveToMemberLocation.setOnClickListener {
            Log.d("abab", currentMemberChoosing.toString() + " " + memberPos.size.toString())
            if (memberPos.size > 0) {
                if (currentMemberChoosing >= memberPos.size) {
                    currentMemberChoosing = 0
                    if (::myLocation.isInitialized) {
                        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(myLocation.latitude,myLocation.longitude),15.0f))
                    }
                }
                else{
                    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(memberPos[currentMemberChoosing].lat,memberPos[currentMemberChoosing].long),15.0f))
                    currentMemberChoosing++
                }

            }
            else {
                if (::myLocation.isInitialized) {
                    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(myLocation.latitude,myLocation.longitude),15.0f))
                }
            }
        }

        listNotificationOnRoads.setOnClickListener {
            showListNotiRecyclerView.adapter = mNotificationOnRoadAdapter
            val layoutManager = LinearLayoutManager(applicationContext)
            layoutManager.orientation = LinearLayoutManager.VERTICAL
            showListNotiRecyclerView.layoutManager = layoutManager

            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        showMapFollow.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        var mainHandler = Handler(Looper.getMainLooper())
        recordBtn.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                val permissions = arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                ActivityCompat.requestPermissions(this, permissions,1212)
            }
            else {
                var currentsecond = 0
                val runnable = object : Runnable {
                    override fun run() {
                        if (!state) {
                            mainHandler.removeCallbacks(this)
                            currentsecond = 0
                        }
                        currentsecond+=1
                        recordBtnTimeCount.text = "${currentsecond/60}:${currentsecond%60}"
                        mainHandler.postDelayed(this, 1000)
                    }
                }
                if (!state) {
                    state = true
                    currentsecond = 0
                    recordBtnTimeCount.visibility = View.VISIBLE
                    if (!hasInitTimeCounter) {
                        mainHandler.post(runnable)
                        hasInitTimeCounter = true
                    }
                    startRecording()
                }
                else {
                    state = false
                    stopRecording()
                    recordBtnTimeCount.visibility = View.GONE

                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Confirm")
                    builder.setMessage("Do you want to send this record to notification?")
                    builder.setPositiveButton("YES"){dialog, which ->
                        val path = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DCIM)
                        val file = File(path, "TS.mp3")
                        if (::myLocation.isInitialized) {
                            ApiRequestUploadRecord(LatLng(myLocation.latitude,myLocation.longitude), mJourneyId, mUserId , file)
                        }
                        else {
                            Toast.makeText(this@JourneyFollowActivity, "Cant get location", Toast.LENGTH_SHORT).show()
                        }

                    }

                    builder.setNegativeButton("No"){dialog,which ->
                        Toast.makeText(applicationContext,"Declined",Toast.LENGTH_SHORT).show()
                    }
                    // Display a neutral button on alert dialog
                    builder.setNeutralButton("Open"){_,_ ->
                        //Inflate the dialog with custom view
                        val mDialogView = LayoutInflater.from(this@JourneyFollowActivity).inflate(R.layout.audio_play_layout, null)
                        //AlertDialogBuilder
                        val mBuilder = AlertDialog.Builder(this@JourneyFollowActivity)
                            .setView(mDialogView)
                        //show dialog

                        val  mAlertDialog = mBuilder.show()
                        mAlertDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        val mLayerDrawable = mDialogView.findViewById<CoordinatorLayout>(R.id.audioplaybtn).background as LayerDrawable
                        var mClipDrawable = mLayerDrawable.findDrawableByLayerId(R.id.clip_drawable) as ClipDrawable
                        var mCurrentLevel = 0

                        val path = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DCIM)
                        val file = File(path, "TS.mp3")
                        val mediaPlayer = MediaPlayer()
                        mediaPlayer.setDataSource(file.path)
                        mediaPlayer.prepare()

                        val duration = mediaPlayer.duration.toLong()
                        val increaseByTime = (10000.0f/duration)*50
                        mDialogView.audioTimeRemaining.setText(util.secondsToString((duration/1000).toInt()))
                        var currentMiliseconds = 0

                        val timer = object: CountDownTimer(mediaPlayer.duration.toLong(), 50) {
                            override fun onTick(millisUntilFinished: Long) {
                                currentMiliseconds += 50
                                mClipDrawable.level = mCurrentLevel
                                mDialogView.audioTimeRemaining.setText(util.secondsToString((millisUntilFinished/1000).toInt()))
                                if (mCurrentLevel >= 10000) {
                                    this.cancel()
                                } else {
                                    mCurrentLevel = 10000.coerceAtMost(mCurrentLevel + increaseByTime.toInt())
                                }
                            }

                            override fun onFinish() {
                                mClipDrawable.level = 10000
                            }
                        }
                        mediaPlayer.start()
                        timer.start()

//                        audioplaybtn.setOnClickListener {
//                            if (mediaPlayer.isPlaying == true) {
//                                mediaPlayer.pause()
//                                timer.
//                            }
//                        }
                    }
                    val dialog: AlertDialog = builder.create()
                    dialog.show()
                }
            }

        }

        ApiRequestGetNotices()
    }


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val PERMISSION_ID = 12

        var transformation = RoundedTransformationBuilder()
            .borderColor(Color.BLACK)
            .borderWidthDp(1f)
            .cornerRadiusDp(30f)
            .oval(false)
            .build()

        val arrayPosType = object : TypeToken<ArrayList<memPosChild>>() {}.type


    }


    override fun onNewIntent(intent: Intent?) {
        Log.d("onnewintend",intent!!.extras!!.getString("type").toString())
        super.onNewIntent(intent)
    }

    fun createDirectory() : String{
        val path = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM);
        val file = File(path, "TS.mp3")
        try {
            // Make sure the Pictures directory exists.
            path.mkdirs()
            file.createNewFile()
        } catch (e : IOException) {
            e.printStackTrace()
        }
        return file.path
    }

    override fun onPause() {
        super.onPause()
        fusedLocationProviderClient?.removeLocationUpdates(mLocationCallback)
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver)
        super.onDestroy()
    }

    private fun startRecording() {
        try {
            mediaRecorder = MediaRecorder()
            mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder?.setOutputFile(createDirectory())
            mediaRecorder?.prepare()
            mediaRecorder?.start()
            Toast.makeText(this, "Recording started!", Toast.LENGTH_SHORT).show()
            isRecording = true
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopRecording(){
        if (isRecording) {
            mediaRecorder?.stop()
            mediaRecorder?.reset()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
        }

    }

    override fun onMapReady(p0: GoogleMap?) {
        mGoogleMap = p0!!



        mLocationRequest = LocationRequest()
        mLocationRequest.interval = 10000 // two minute interval
        mLocationRequest.fastestInterval = 10000
        mLocationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                mGoogleMap.isMyLocationEnabled = true
                mGoogleMap.uiSettings.isMyLocationButtonEnabled = true
                //Location Permission already granted
                fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
                mGoogleMap.isMyLocationEnabled = true
            } else {
                //Request Location Permission
                checkPermissions()
            }
        } else {
            fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
            mGoogleMap.isMyLocationEnabled = true
        }

        mGoogleMap.setOnMarkerClickListener(object : GoogleMap.OnMarkerClickListener {
            override fun onMarkerClick(p0: Marker?): Boolean {
                if (p0?.tag != null && p0.tag.toString().contains("noti")) {
                    val index = p0.tag.toString().split(" ")[1].toInt()
                    val item = notiOnRoad[index]
                    //AlertDialogBuilder
                    val inflater: LayoutInflater =
                        getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                    val view = inflater.inflate(R.layout.popup_notification_onroad, null)
                    val mBuilder = AlertDialog.Builder(this@JourneyFollowActivity)
                        .setView(view)


                    when (item.notificationType) {
                        1 -> {
                            view.notiPolice.visibility = View.VISIBLE
                            view.notiProblem.visibility = View.GONE
                            view.notiSpeed.visibility = View.GONE
                            view.notiSpeedView.visibility = View.GONE
                        }
                        2 -> {
                            view.notiProblem.visibility = View.VISIBLE
                            view.notiPolice.visibility = View.GONE
                            view.notiSpeed.visibility = View.GONE
                            view.notiSpeedView.visibility = View.GONE
                        }
                        3 -> {
                            view.notiSpeed.visibility = View.VISIBLE
                            view.notiSpeedView.visibility = View.VISIBLE
                            view.notiPolice.visibility = View.GONE
                            view.notiProblem.visibility = View.GONE
                            view.notiSpeedEdit.text = item.speed.toString()
                        }
                    }

                    view.notiNote.text = item.note
                    if (::myLocation.isInitialized) {
                        val currentLocation = LatLng(myLocation.latitude, myLocation.longitude)
                        val notiLocation = LatLng(item.lat,item.long)
                        view.notiDistance.text = distanceBetweenTwoPoint(currentLocation, notiLocation, destinationLatLng).toInt().toString()
                    }

                    //show dialog
                    val  mAlertDialog = mBuilder.show()

                    view.dismissBtnNotiOnRoad.setOnClickListener {
                        mAlertDialog.dismiss()
                    }

                }
                return false
            }
        })

    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

        }
        if (requestCode == PERMISSION_ID) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val intent = getIntent()
                finish()
                startActivity(intent)
            }
        }

        if (requestCode == 1212) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            }
            else {
                Toast.makeText(this@JourneyFollowActivity, "No permission for record to work", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            LocationRequest.PRIORITY_HIGH_ACCURACY -> {
                if (resultCode == Activity.RESULT_OK) {

                } else {
                    Log.e("Status: ","Off")
                }
            }
        }
    }

    private fun fetchLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                val task = fusedLocationProviderClient.lastLocation
                task.addOnSuccessListener { location ->
                    if (location != null) {
                        myLocation = location
                        val mapFragment =
                            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
                        mapFragment.getMapAsync(this)
                    }
                    else {
                        requestNewLocationData()
                    }
                }
            }

        }
        else {
            requestPermissions()
        }
    }

    fun addMarker(ggMap: GoogleMap, pos: LatLng, name: String, drawable: Int): Marker {
        return ggMap.addMarker(
            MarkerOptions()
                .position(pos)
                .icon(bitmapDescriptorFromVector(applicationContext, drawable))
                .title(name)
        )
    }


    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        return ContextCompat.getDrawable(context, vectorResId)?.run {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            val bitmap =
                Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }


    fun drawPath(src: LatLng, dest: LatLng) {
        //addMarker(mGoogleMap,src,"My location",R.drawable.ic_startpoint)
        Log.d("abab", lastLocationLatLng.toString() +" - " + dest.toString())
        drawUserMarker(getAvatarFromList(mUserId), src)
        addMarker(mGoogleMap,dest,"Destination",R.drawable.ic_endpoint)
        if (lastLocationLatLng != null && lastLocationLatLng == src) return
        else lastLocationLatLng = src
        doAsync {
            val path: MutableList<List<LatLng>> = ArrayList()
            val urlDirections = "https://maps.googleapis.com/maps/api/directions/json?origin=${src.latitude},${src.longitude}&destination=${dest.latitude},${dest.longitude}&key=${Constant.ggMapApiKey}"
            val directionsRequest = object : StringRequest(Request.Method.GET, urlDirections, Response.Listener<String> {
                    response ->

                resetCurrentPolyLine(currentPathPolyline)
                val jsonResponse = JSONObject(response)
                // Get routes
                try {
                    val routes = jsonResponse.getJSONArray("routes")
                    val legs = routes.getJSONObject(0).getJSONArray("legs")
                    val steps = legs.getJSONObject(0).getJSONArray("steps")

                    val distance = legs.getJSONObject(0).getJSONObject("distance").getDouble("value")+steps.getJSONObject(0).getJSONObject("distance").getDouble("value")
                    val duration = legs.getJSONObject(0).getJSONObject("duration").getInt("value")+steps.getJSONObject(0).getJSONObject("duration").getInt("value")

                    if (distance < 200) {
                        reachedDestination = true
                    }

                    distanceToDestination.text = "%.2f".format(distance/1000)
                    timeRemainingToDestination.text = (duration/60 + 1).toString()


                    for (i in 0 until steps.length()) {
                        val points = steps.getJSONObject(i).getJSONObject("polyline").getString("points")
                        path.add(PolyUtil.decode(points))
                    }
                    for (i in 0 until path.size) {
                        var pol =  mGoogleMap.addPolyline(PolylineOptions().addAll(path[i]).color(Color.BLUE).width(5.0f))

                        currentPathPolyline.add(pol)
                    }
                }
                catch (ex : Exception) {
                    ex.printStackTrace()
                }

            }, Response.ErrorListener {
                    _ ->
            }){}
            val requestQueue = Volley.newRequestQueue(this)
            requestQueue.add(directionsRequest)
        }.execute()
    }

    fun resetCurrentPolyLine(arr : ArrayList<Polyline>) {
        for (i in arr) {
            i.remove()
        }
        arr.clear()
    }


    fun isLocationEnabled(): Boolean {
        var locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            return true
        }
        return false
    }

    fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_ID
        )
    }



    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        var mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProviderClient!!.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }


    private fun showLocationPrompt() {
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val result: Task<LocationSettingsResponse> = LocationServices.getSettingsClient(this).checkLocationSettings(builder.build())

        result.addOnCompleteListener { task ->
            try {
                val response = task.getResult(ApiException::class.java)
                // All location settings are satisfied. The client can initialize location
                // requests here.
            } catch (exception: ApiException) {
                when (exception.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        try {
                            // Cast to a resolvable exception.
                            val resolvable: ResolvableApiException = exception as ResolvableApiException
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            resolvable.startResolutionForResult(
                                this, LocationRequest.PRIORITY_HIGH_ACCURACY
                            )
                        } catch (e: SendIntentException) {
                            // Ignore the error.
                        } catch (e: ClassCastException) {
                            // Ignore, should be an impossible error.
                        }
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        // Location settings are not satisfied. But could be fixed by showing the
                        // user a dialog.

                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                    }
                }
            }
        }
    }


    fun popupChat() {
        val inflater: LayoutInflater =
            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.popup_chat, null)

        isPopupOpen = true
        mChatRecyclerView = view.chatRecyclerView
        view.chatRecyclerView.adapter = mChatAdapter
        val layoutManager = LinearLayoutManager(applicationContext)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        layoutManager.stackFromEnd = true
        view.chatRecyclerView.layoutManager = layoutManager






        val displayMetrics =getResources().getDisplayMetrics()
        val screenWidthInDp = displayMetrics.heightPixels / displayMetrics.density

        val popupWindow = PopupWindow(
            view, // Custom view to show in popup window
            LinearLayout.LayoutParams.MATCH_PARENT, // Width of popup window
            LinearLayout.LayoutParams.WRAP_CONTENT, // Window height
            true
        )

        // Set an elevation for the popup window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.elevation = 10.0F
        }


        // If API level 23 or higher then execute the code
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Create a new slide animation for popup window enter transition
            val slideIn = Slide()
            slideIn.slideEdge = Gravity.TOP
            popupWindow.enterTransition = slideIn

            // Slide animation for popup window exit transition
            val slideOut = Slide()
            slideOut.slideEdge = Gravity.TOP
            popupWindow.exitTransition = slideOut

        }

        view.chatSend.setOnClickListener {
            if (view.chatContent.text.toString().isNotEmpty()) {
                ApiRequestSendNotice(view.chatContent.text.toString())
                view.chatContent.setText("")
                view.chatContent.clearFocus()
            }
            else {
                view.chatContent.error = "Cannot be empty!!"
            }
        }





        // Set a dismiss listener for popup window
        popupWindow.setOnDismissListener {
            isPopupOpen = false
        }


        // Finally, show the popup window on app
        popupWindow.showAtLocation(
            journeyFollowMainLayout,
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )
    }


    fun ApiRequestGetNotices() {
        doAsync {
            val service = WebAccess.retrofit.create(ApiServiceGetJourneyNotices::class.java)
            val call = service.getNotices(mToken , mJourneyId, 1, "999" )
            call.enqueue(object : Callback<ResponseGetJourneyNotice> {
                override fun onFailure(call: Call<ResponseGetJourneyNotice>, t: Throwable) {
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_LONG).show()
                }

                override fun onResponse(
                    call: Call<ResponseGetJourneyNotice>,
                    response: retrofit2.Response<ResponseGetJourneyNotice>
                ) {
                    if (response.code() != 200) {
                        Toast.makeText(applicationContext, response.errorBody().toString(), Toast.LENGTH_LONG).show()
                    } else {

                        mListChat.clear()
                        mListChat.addAll(response.body()!!.notiList)
                        mChatAdapter.notifyDataSetChanged()
                        if (isPopupOpen) {
                            mChatRecyclerView.smoothScrollToPosition(mListChat.size - 1)
                        }
                    }
                }
            })
        }.execute()
    }





    fun ApiRequestSendNotice(notice : String) {
        doAsync {
            val service = WebAccess.retrofit.create(ApiServiceSendJourneyNotice::class.java)
            val body = JsonObject()


            body.addProperty("journeyId", mJourneyId)
            body.addProperty("userId", mUserId)
            body.addProperty("noti", notice)
            val call = service.sendNotice(mToken , body )
            call.enqueue(object : Callback<ResponseSendNotice> {
                override fun onFailure(call: Call<ResponseSendNotice>, t: Throwable) {
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_LONG).show()
                }

                override fun onResponse(
                    call: Call<ResponseSendNotice>,
                    response: retrofit2.Response<ResponseSendNotice>
                ) {
                    if (response.code() != 200) {
                        Toast.makeText(applicationContext, response.errorBody().toString(), Toast.LENGTH_LONG).show()
                    } else {

                        ApiRequestGetNotices()
                    }
                }
            })
        }.execute()
    }


    inner class ChatAdapter(data: ArrayList<notification>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        var data = ArrayList<notification>()

        init {
            this.data = data
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(applicationContext)
            val itemView : View
            if (viewType == 1) {
                itemView = inflater.inflate(R.layout.item_send_message, parent, false)
                return RecyclerViewHolderSend(itemView)
            }
            else {
                itemView = inflater.inflate(R.layout.item_receive_message, parent, false)
                return RecyclerViewHolderRecv(itemView)
            }

        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = data.get(position)
            if (holder.itemViewType == 1) {
                var temp = holder as RecyclerViewHolderSend
                temp.bind(item)
            }
            else {
                var temp = holder as RecyclerViewHolderRecv
                temp.bind(item)
            }
        }

        override fun getItemCount(): Int {
            return data.size
        }

        override fun getItemViewType(position: Int): Int {
            if (mUserId == data[position].userId) {
                return 1
            }
            return 0
        }

        inner class RecyclerViewHolderRecv(itemView: View) : RecyclerView.ViewHolder(itemView) {
            internal var name: TextView
            //internal var time: TextView
            internal var body: TextView
            internal var avatar: ImageView

            init {
                name = itemView.findViewById(R.id.text_message_name)
                //time = itemView.findViewById(R.id.text_message_time)
                body = itemView.findViewById(R.id.text_message_body)
                avatar = itemView.findViewById(R.id.image_message_profile)
            }

            fun bind(message : notification) {
                body.text = message.notification
                name.text = message.name
            }
        }

        inner class RecyclerViewHolderSend(itemView: View) : RecyclerView.ViewHolder(itemView) {
            internal var body: TextView

            init {
                body = itemView.findViewById(R.id.text_message_body)
            }

            fun bind(message : notification) {
                body.text = message.notification
            }
        }
    }


    inner class NotificationOnRoadAdapter(data: ArrayList<notificationonroad>) :
        RecyclerView.Adapter<NotificationOnRoadAdapter.RecyclerViewHolder>() {

        var data = ArrayList<notificationonroad>()

        init {
            this.data = data
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.item_notification_on_road, parent, false)
            return RecyclerViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
            val item = data.get(position)

            when (item.notificationType) {
                1 -> {
                    holder.itemView.notiPolice.visibility = View.VISIBLE
                    holder.itemView.notiProblem.visibility = View.GONE
                    holder.itemView.notiSpeed.visibility = View.GONE
                    holder.itemView.notiSpeedView.visibility = View.GONE
                }
                2 -> {
                    holder.itemView.notiProblem.visibility = View.VISIBLE
                    holder.itemView.notiPolice.visibility = View.GONE
                    holder.itemView.notiSpeed.visibility = View.GONE
                    holder.itemView.notiSpeedView.visibility = View.GONE
                }
                3 -> {
                    holder.itemView.notiSpeed.visibility = View.VISIBLE
                    holder.itemView.notiSpeedView.visibility = View.VISIBLE
                    holder.itemView.notiPolice.visibility = View.GONE
                    holder.itemView.notiProblem.visibility = View.GONE
                    holder.speed.text = item.speed.toString()
                }
            }

            holder.note.text = item.note
            if (::myLocation.isInitialized) {
                val currentLocation = LatLng(myLocation.latitude, myLocation.longitude)
                val notiLocation = LatLng(item.lat,item.long)
                holder.distance.text = distanceBetweenTwoPoint(currentLocation, notiLocation, destinationLatLng).toInt().toString()
            }

            holder.itemView.setOnClickListener {
                val notiLocation = LatLng(item.lat,item.long)
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(notiLocation,15.0f))
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }


        }

        override fun getItemCount(): Int {
            return data.size
        }

        inner class RecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            internal var note: TextView
            internal var distance: TextView
            internal var speed: TextView


            init {
                note = itemView.notiNote
                distance = itemView.notiDistance
                speed = itemView.notiSpeedEdit
            }
        }
    }




    fun popupCreateNotification(location : LatLng) {
        val inflater: LayoutInflater =
            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.popup_create_notification_on_road, null)



        view.spinnerType.setItems("Police Position", "Problem On Road", "Speed Limit Sign")

        view.spinnerType.setOnItemSelectedListener { _, position, id, item ->
            if (position == 2) {
                view.editSpeedLimitView.visibility = View.VISIBLE
            }
            else {
                view.editSpeedLimitView.visibility = View.GONE
            }
        }



        //AlertDialogBuilder
        val mBuilder = AlertDialog.Builder(this@JourneyFollowActivity)
            .setView(view)
        //show dialog
        val  mAlertDialog = mBuilder.show()

        //login button click of custom layout

        //cancel button click of custom layout
        view.dismissBtn.setOnClickListener {
            //dismiss dialog
            mAlertDialog.dismiss()
        }

        view.addNoti.setOnClickListener {
            if (view.spinnerType.selectedIndex == 2) {
                if (view.editSpeedLimit.text!!.isEmpty()) {
                    view.editSpeedLimit.error = "Cant be empty"
                }
                else {
                    val speed = view.editSpeedLimit.text.toString().toInt()
                    val note = view.editNote.text.toString()
                    ApiRequestAddNotificationOnRoad(LatLng(myLocation.latitude,myLocation.longitude), mJourneyId, mUserId, 3, speed, note, mAlertDialog)
                }
            }
            else {
                val note = view.editNote.text.toString()
                val type = view.spinnerType.selectedIndex + 1
                ApiRequestAddNotificationOnRoad(LatLng(myLocation.latitude,myLocation.longitude), mJourneyId, mUserId, type, -1, note, mAlertDialog)
            }
        }




    }

    fun ApiRequestGetJourneyInfo() {
        doAsync {
            val service = WebAccess.retrofit.create(ApiServiceGetJourneyInfo::class.java)
            val call = service.getJourneyInfo(mToken,mJourneyId)
            call.enqueue(object : Callback<ResponseJourneyInfo> {
                override fun onFailure(call: Call<ResponseJourneyInfo>, t: Throwable) {
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_LONG).show()
                }

                override fun onResponse(
                    call: Call<ResponseJourneyInfo>,
                    response: retrofit2.Response<ResponseJourneyInfo>
                ) {
                    if (response.code() != 200) {
                        Toast.makeText(applicationContext, response.errorBody().toString(), Toast.LENGTH_LONG).show()
                    } else {
                        mListMember.addAll(response.body()!!.members)
                        Log.d("abab", response.body()!!.members.toString())
                    }
                }
            })
        }.execute()
    }

    fun drawUserMarker(url : String, pos : LatLng) {
        val currentTarget = object : Target {
            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                //do nothing
            }

            override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
                myLocationMarker = mGoogleMap.addMarker(MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                    .position(pos)
                    .title("My position")
                )
                runningTargets.clear()
            }

            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                //do nothing
            }
        }

        runningTargets.add(currentTarget)

        if (url.isNotEmpty() && url != "null") {
            Picasso.get().load(url).transform(transformation).into(currentTarget)
        }
    }

    fun getAvatarFromList(uid : Int) : String {
        for (i in mListMember) {
            if (i.id == uid) {
                Log.d("uid ", uid.toString() + " " + i.id)
                if (i.avatar.isNullOrEmpty()) return ""
                return i.avatar
            }
        }
        return ""
    }


    fun ApiRequestAddNotificationOnRoad(pos : LatLng, journeyId : Int, UserId : Int, notificationType : Int, speed : Int = -1, note : String = "", pop : AlertDialog) {
        doAsync {
            val service = WebAccess.retrofit.create(ApiServiceCreateNotificationOnRoad::class.java)

            val body = JsonObject()
            body.addProperty("lat",pos.latitude)
            body.addProperty("long",pos.longitude)
            body.addProperty("userId",UserId)
            body.addProperty("journeyId",journeyId)
            body.addProperty("notificationType",notificationType)
            if (notificationType == 3 && speed >= 0) {
                body.addProperty("speed",speed)
            }
            if (note.isNotEmpty()) {
                body.addProperty("note",note)
            }

            val call = service.addNoti(mToken,body)
            call.enqueue(object : Callback<ResponseAddNotificationOnRoad> {
                override fun onFailure(call: Call<ResponseAddNotificationOnRoad>, t: Throwable) {
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_LONG).show()
                }

                override fun onResponse(
                    call: Call<ResponseAddNotificationOnRoad>,
                    response: retrofit2.Response<ResponseAddNotificationOnRoad>
                ) {
                    if (response.code() != 200) {
                        val gson = Gson()
                        val type = object : TypeToken<ErrorResponse>() {}.type
                        var errorResponse: ErrorResponse? = gson.fromJson(response.errorBody()!!.charStream(), type)
                        Toast.makeText(applicationContext, errorResponse!!.message, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(applicationContext, "Success!", Toast.LENGTH_LONG).show()
                        pop.dismiss()
                    }
                }
            })
        }.execute()
    }

    fun ApiRequestUploadRecord(pos : LatLng, journeyId : Int, UserId : Int, file : File) {
        Thread(Runnable {
        val service = WebAccess.retrofit.create(ApiServiceUploadRecord::class.java)



            Log.d("ababb", file.name)

        val requestBody : RequestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", file.getName(),
                RequestBody.create(MediaType.parse("audio/mp3"), file))
            .addFormDataPart("lat", pos.latitude.toString())
            .addFormDataPart("long", pos.longitude.toString())
            .addFormDataPart("journeyId", mJourneyId.toString())
            .addFormDataPart("fullName", mUserId.toString())
            .addFormDataPart("avatar", "")
            .build()



//        val requestBodyForFile = RequestBody.create(MediaType.parse("audio/*"), file)
//        val file : MultipartBody.Part = MultipartBody.Part.createFormData("file", file.getName(), requestBody)
//        val journeyId = RequestBody.create(MultipartBody.FORM, journeyId.toString())
//        val fullName = RequestBody.create(MultipartBody.FORM, mUserId.toString())
//        val avatar = RequestBody.create(MultipartBody.FORM, "")
//        val lat = RequestBody.create(MultipartBody.FORM, pos.latitude.toString())
//        val long = RequestBody.create(MultipartBody.FORM, pos.longitude.toString())
        val call = service.upRecord(mToken, requestBody)
        call.enqueue(object : Callback<ResponseUploadRecord> {
            override fun onFailure(call: Call<ResponseUploadRecord>, t: Throwable) {
                runOnUiThread {
                    Toast.makeText(
                        this@JourneyFollowActivity,
                        t.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(
                call: Call<ResponseUploadRecord>,
                response: retrofit2.Response<ResponseUploadRecord>
            ) {
                if (response.code() != 200) {
                    val gson = Gson()
                    val type = object : TypeToken<ErrorResponse>() {}.type
                    var errorResponse: ErrorResponse? =
                        gson.fromJson(response.errorBody()!!.charStream(), type)

                    runOnUiThread {
                        Toast.makeText(
                            this@JourneyFollowActivity,
                            errorResponse!!.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@JourneyFollowActivity, "Success!", Toast.LENGTH_LONG).show()
                    }
                }
            }
             })
        }).start()
    }


    fun ApiRequestGetNotificationOnRoad(journeyId : Int) {
        doAsync {
            val service = WebAccess.retrofit.create(ApiServiceGetNotificationOnRoad::class.java)

            val call = service.getdNoti(mToken, journeyId, 1, "999")
            call.enqueue(object : Callback<ResponseGetNotificationOnRoad> {
                override fun onFailure(call: Call<ResponseGetNotificationOnRoad>, t: Throwable) {
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_LONG).show()
                }

                override fun onResponse(
                    call: Call<ResponseGetNotificationOnRoad>,
                    response: retrofit2.Response<ResponseGetNotificationOnRoad>
                ) {
                    if (response.code() != 200) {
                        val gson = Gson()
                        val type = object : TypeToken<ErrorResponse>() {}.type
                        var errorResponse: ErrorResponse? = gson.fromJson(response.errorBody()!!.charStream(), type)
                        Toast.makeText(applicationContext, errorResponse!!.message, Toast.LENGTH_LONG).show()
                    } else {
                        notiOnRoad.clear()
                        notiOnRoad.addAll(response.body()!!.notiList)
                        notiOnRoadCount.text = " : " + notiOnRoad.size.toString()
                        clearMarkerInArray(notiOnRoadMarker)
                        var marker : Marker
                        for (c in 0..notiOnRoad.size-1) {
                            val i = notiOnRoad[c]
                            when(i.notificationType) {
                                1 -> {
                                    marker = addMarker(mGoogleMap, LatLng(i.lat,i.long), util.codeToTypeOfNotification(i.notificationType), R.drawable.ic_police_on_road)
                                    marker.tag = "noti ${c}"
                                    notiOnRoadMarker.add(marker)
                                }
                                2-> {
                                    marker = addMarker(mGoogleMap, LatLng(i.lat,i.long), util.codeToTypeOfNotification(i.notificationType), R.drawable.ic_problem_on_road)
                                    marker.tag = "noti ${c}"
                                    notiOnRoadMarker.add(marker)
                                }
                                3 -> {
                                    marker = addMarker(mGoogleMap, LatLng(i.lat,i.long), util.codeToTypeOfNotification(i.notificationType) +" - " + i.speed + "km", R.drawable.ic_speed_limit)
                                    marker.tag = "noti ${c}"
                                    notiOnRoadMarker.add(marker)
                                }
                            }

                        }
                    }
                }
            })
        }.execute()
    }

    fun ApiRequestSendCoordinate() {
        doAsync {
            val service = WebAccess.retrofit.create(ApiServiceSendCoordinate::class.java)
            val body = JsonObject()
            body.addProperty("userId", mUserId)
            body.addProperty("journeyId", mJourneyId)
            body.addProperty("lat", myLocation.latitude)
            body.addProperty("long", myLocation.longitude)

            val call = service.sendCoordinate(mToken, body)
            call.enqueue(object : Callback<ArrayList<memPosChild>> {
                override fun onFailure(call: Call<ArrayList<memPosChild>>, t: Throwable) {
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_LONG).show()
                }

                override fun onResponse(
                    call: Call<ArrayList<memPosChild>>,
                    response: retrofit2.Response<ArrayList<memPosChild>>
                ) {
                    if (response.code() != 200) {
                        val gson = Gson()
                        val type = object : TypeToken<ErrorResponse>() {}.type
                        var errorResponse: ErrorResponse? = gson.fromJson(response.errorBody()!!.charStream(), type)
                        Toast.makeText(this@JourneyFollowActivity, errorResponse!!.message, Toast.LENGTH_LONG).show()
                    } else {

                        memberPos.clear()
                        for (i in memberPosMarker) i.remove()
                        memberPosMarker.clear()
                        memberPos.addAll(response.body()!!)
                        for (i in memberPos) {
                            if (i.id == mUserId) continue
                            val marker = addMarker(mGoogleMap, LatLng(i.lat, i.long), i.id.toString(), R.drawable.ic_person_pin_circle_black_24dp )

                            memberPosMarker.add(marker)
                        }
                        currentFollowingJourney.setText(" : ${memberPos.size}")
                    }

                }
            })
        }.execute()
    }



    fun clearMarkerInArray(arrMarker : ArrayList<Marker>) {
        for (i in arrMarker) {
            i.remove()
        }
        arrMarker.clear()
    }


    fun ApiRequestUpdateLandingTime() {
        doAsync {
            val service = WebAccess.retrofit.create(ApiServiceUpdateLandingTime::class.java)
            val body = JsonObject()
            body.addProperty("desId", desId)
            Log.d("ababdes", desId.toString())
            val call = service.landing(mToken, body)
            call.enqueue(object : Callback<ResponseUpdateLandingTime> {
                override fun onFailure(call: Call<ResponseUpdateLandingTime>, t: Throwable) {
                    Toast.makeText(this@JourneyFollowActivity, t.message, Toast.LENGTH_LONG).show()
                }

                override fun onResponse(
                    call: Call<ResponseUpdateLandingTime>,
                    response: retrofit2.Response<ResponseUpdateLandingTime>
                ) {
                    if (response.code() != 200) {
                        val gson = Gson()
                        val type = object : TypeToken<ErrorResponse>() {}.type
                        var errorResponse: ErrorResponse? = gson.fromJson(response.errorBody()!!.charStream(), type)
                        Toast.makeText(this@JourneyFollowActivity, errorResponse!!.message, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@JourneyFollowActivity, "Update Landing Successfully", Toast.LENGTH_LONG).show()
                    }

                }
            })
        }.execute()
    }


    fun requestRecordPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            val permissions = arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(this, permissions,0)
        }
    }


    fun distanceBetweenTwoPoint(user : LatLng, point: LatLng, dest: LatLng) : Float {
        var userpoint = floatArrayOf(0f)
        Location.distanceBetween(user.latitude, user.longitude,
            point.latitude, point.longitude, userpoint)
        var userdest = floatArrayOf(0f)
        Location.distanceBetween(user.latitude, user.longitude,
            dest.latitude, dest.longitude, userdest)
        var pointdest = floatArrayOf(0f)
        Location.distanceBetween(point.latitude, point.longitude,
            dest.latitude, dest.longitude, pointdest)
        if (userdest[0] > pointdest[0])
        {
            return userpoint[0]
        }
        else {
            return 0f
        }
    }






}





