package com.ygaps.jazz

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.transition.Slide
import android.util.Log
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.ygaps.jazz.manager.doAsync
import com.ygaps.jazz.model.StopPoint
import com.ygaps.jazz.network.model.*
import com.ygaps.jazz.util.util
import com.ygaps.jazz.view.createjourney.ChangeJourneyInfo
import com.ygaps.jazz.view.member.MemberListOfJourney
import com.ygaps.jazz.view.stoppoint.StopPointInfo


import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.squareup.picasso.Picasso
import com.taufiqrahman.reviewratings.BarLabels
import com.ygaps.jazz.manager.Constant
import com.ygaps.jazz.view.stoppoint.StopPointEditActivity
import com.ygaps.jazz.view.journey.JourneyFollowActivity
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_member_list_of_journey.*
import kotlinx.android.synthetic.main.activity_stop_point_feedback.view.*
import kotlinx.android.synthetic.main.activity_stop_point_info.*
import kotlinx.android.synthetic.main.activity_journey_info.*
import kotlinx.android.synthetic.main.item_choose_destination.view.*
import kotlinx.android.synthetic.main.item_reviews_layout.view.*
import kotlinx.android.synthetic.main.layout_journey_comment.*
import kotlinx.android.synthetic.main.layout_journey_comment.view.*
import kotlinx.android.synthetic.main.layout_journey_review.view.*
import kotlinx.android.synthetic.main.popup_choose_destination.*
import kotlinx.android.synthetic.main.popup_choose_destination.view.*
import kotlinx.android.synthetic.main.popup_setting_bottom.view.*
import org.jetbrains.anko.image
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class JourneyInfoActivity : AppCompatActivity() {


    lateinit var token : String
    var journeyId : Int = 0
    var listStopPoint = ArrayList<StopPoint>()
    var listComment = ArrayList<comment>()
    var listReviews = ArrayList<review>()
    var listMembers = ArrayList<member>()
    var typeCount = arrayOf(0,0,0,0)
    var reviewCount = arrayOf(0,0,0,0,0)
    var currentUserId = 126
    lateinit var startLatLang : LatLng
    lateinit var endLatLng: LatLng
    lateinit var StpAdt : StopPointAdapter
    lateinit var CommentAdt : CommentAdapter
    lateinit var ReviewAdt : ReviewAdapter
    lateinit var CommentNumCountView : TextView
    lateinit var ReviewNumCountView : TextView
    var hasInitCommentNumCountView = false
    var journeyName : String = ""
    lateinit var commentView : RecyclerView
    var position : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_journey_info)
        token = this.intent.extras!!.getString("token","notoken")!!
        journeyId = this.intent.extras!!.getInt("journeyID",100)
        position = intent.extras!!.getInt("position", -1)
        supportActionBar!!.hide()
        journeyRating.rating = 3.2f
        Log.d("abab", journeyId.toString())

        val layoutManager = LinearLayoutManager(applicationContext)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        stopPointRecyclerView.layoutManager = layoutManager
        StpAdt = StopPointAdapter(listStopPoint)
        stopPointRecyclerView.adapter = StpAdt
        CommentAdt = CommentAdapter(listComment)
        ReviewAdt = ReviewAdapter(listReviews)

        ApiRequest()
        ApiRequestGetReviewList(journeyId)
        ApiRequestGetComment(journeyId.toString())

        val sharePref : SharedPreferences = getSharedPreferences("logintoken", Context.MODE_PRIVATE)
        currentUserId = sharePref.getInt("userId",126)

        Log.d("abab", "Curernt id : ${currentUserId}")

        journeyListMembers.setOnClickListener {
            val intent = Intent(this, MemberListOfJourney::class.java)
            intent.putExtra("journeyId",journeyId)
            intent.putExtra("token",token)
            startActivity(intent)
        }

        journeyListComments.setOnClickListener {
            popupComment()
        }

        journeyListReviews.setOnClickListener {
            popupReview()
        }

        journeyMenu.setOnClickListener {
            popupSetting()
        }


        btnStartGoingJourney.setOnClickListener {
            popupStartGoing()
        }

        serviceRatingStarSelect_journey.setOnRatingBarChangeListener { _, rating, _ ->
            if (rating != 0f) {
                serviceFeedbackEditContent_journey.visibility = View.VISIBLE
            }
        }
        btnFeedbackCancel_journey.setOnClickListener {
            serviceFeedbackEditContent_journey.visibility = View.GONE
            serviceRatingStarSelect_journey.rating = 0f
        }

        btnFeedbackSubmit_journey.setOnClickListener {
            var content = editserviceRatingContent_journey.text.toString()
            ApiRequestAddReview(journeyId, serviceRatingStarSelect_journey.rating.toInt(), content)
            serviceFeedbackEditContent_journey.visibility = View.GONE
            serviceRatingStarSelect_journey.setIsIndicator(true)
            serviceRatingStarSelect_journey.isClickable = false
        }
    }

    override fun onResume() {
        ApiRequest()
        super.onResume()
    }

    inner class StopPointAdapter(data: ArrayList<StopPoint>) :
        RecyclerView.Adapter<StopPointAdapter.RecyclerViewHolder>() {

        var data = ArrayList<StopPoint>()

        init {
            this.data = data
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.stop_point_item_view, parent, false)
            return RecyclerViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
            val item = data.get(position)
            holder.name.text = item.name
            holder.time.text = Html.fromHtml(util.longToDateTime(item.arrivalAt!!) + " <br> " + util.longToDateTime(item.leaveAt!!))
            holder.type.text = util.StopPointTypeToString(item.serviceTypeId!!)
            holder.cost.text = item.minCost.toString() + " - " + item.maxCost.toString()


            holder.menubtn.setOnClickListener {
                //creating a popup menu
                val popup = PopupMenu(applicationContext, holder.menubtn)
                //inflating menu from xml resource
                popup.inflate(R.menu.stop_point_menu)
                //adding click listener
                popup.setOnMenuItemClickListener(object:  PopupMenu.OnMenuItemClickListener {
                override fun onMenuItemClick(menuitem: MenuItem?): Boolean {
                    if (menuitem!!.itemId == R.id.edit_stoppoint) {
                        val intent = Intent(applicationContext, StopPointEditActivity::class.java)
                        intent.putExtra("journeyId", journeyId)
                        intent.putExtra("id", item.id)
                        intent.putExtra("serviceId", item.serviceId)
                        intent.putExtra("serviceTypeId", item.serviceTypeId!!)
                        intent.putExtra("name", item.name)
                        intent.putExtra("cityId", item.cityId)
                        intent.putExtra("lat", item.lat)
                        intent.putExtra("long", item.long)
                        intent.putExtra("arriveAt", item.arrivalAt!!)
                        intent.putExtra("leaveAt", item.leaveAt!!)
                        intent.putExtra("minCost", item.minCost)
                        intent.putExtra("maxCost", item.maxCost)
                        intent.putExtra("index", item.index)
                        intent.putExtra("address", item.address)
                        intent.putExtra("maxindex", data.size)
                        intent.putExtra("token", token)
                        startActivity(intent)
                    }
                    else if (menuitem.itemId == R.id.remove_stoppoint) {

                        ApiRequestRemoveStopPoint(journeyId,item.id)
                    }

                    return false
                }
            })
        //displaying the popup
                popup.show()
            }

            holder.itemView.setOnClickListener {
                val intent = Intent(applicationContext, StopPointInfo::class.java)
                intent.putExtra("token", token)
                intent.putExtra("stpid", item.serviceId)
                startActivity(intent)
            }
        }

        override fun getItemCount(): Int {
            return data.size
        }

        inner class RecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            internal var name: TextView
            internal var time: TextView
            internal var type: TextView
            internal var cost: TextView
            internal var menubtn: ImageButton

            init {
                name = itemView.findViewById(R.id.itemstpname) as TextView
                time = itemView.findViewById(R.id.itemstptime) as TextView
                type = itemView.findViewById(R.id.itemstptype) as TextView
                cost = itemView.findViewById(R.id.itemstpcost) as TextView
                menubtn = itemView.findViewById(R.id.journeyMenu) as ImageButton
            }
        }
    }


    inner class CommentAdapter(data: ArrayList<comment>) :
        RecyclerView.Adapter<CommentAdapter.RecyclerViewHolder>() {

        var data = ArrayList<comment>()

        init {
            this.data = data
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.item_comments_layout, parent, false)
            return RecyclerViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
            val item = data.get(position)
            holder.content.text = item.comment

            if (!item.name.isNullOrEmpty()) {
                holder.name.text = item.name
                return
            }
            else {
                holder.name.text = "<Không tên> : ID = ${item.id}"
            }

            if (!item.avatar.isNullOrEmpty()) {
                Picasso.get()
                    .load(item.avatar)
                    .resize(50, 50)
                    .centerCrop()
                    .into(holder.avatar)
            }




        }

        override fun getItemCount(): Int {
            return data.size
        }

        inner class RecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            internal var name: TextView
            internal var content: TextView
            internal var avatar: CircleImageView

            init {
                name = itemView.findViewById(R.id.commentName) as TextView
                content = itemView.findViewById(R.id.commentContent) as TextView
                avatar = itemView.findViewById(R.id.commentAvatar) as CircleImageView
            }
        }
    }


    inner class ReviewAdapter(data: ArrayList<review>) :
        RecyclerView.Adapter<ReviewAdapter.RecyclerViewHolder>() {

        var data = ArrayList<review>()

        init {
            this.data = data
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.item_reviews_layout, parent, false)
            return RecyclerViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
            val item = data.get(position)
            holder.content.text = item.review
            holder.rating.rating = item.point.toFloat()
            holder.date.text = util.longToDate(item.createdOn)
            if (!item.name.isNullOrEmpty()) {
                holder.name.text = item.name
            }
            else {
                holder.name.text = "ID = ${item.id}"
            }

            if (!item.avatar.isNullOrEmpty()) {
                Picasso.get()
                    .load(item.avatar)
                    .resize(40, 40)
                    .centerCrop()
                    .into(holder.avatar)
            }

            val btn = holder.itemView.findViewById<ImageButton>(R.id.btnReport)
            btn.setOnClickListener {
                val builder = android.app.AlertDialog.Builder(this@JourneyInfoActivity)
                builder.setTitle("Report this feedback")
                builder.setMessage("Are you sure to report this feedback?")
                builder.setPositiveButton("YES"){dialog, which ->
                    ApiRequestReportReview(item.id)
                }

                builder.setNegativeButton("No"){dialog,which ->
                    Toast.makeText(this@JourneyInfoActivity,"Declined",Toast.LENGTH_SHORT).show()
                }
                val dialog: android.app.AlertDialog = builder.create()
                dialog.show()
            }

        }

        override fun getItemCount(): Int {
            return data.size
        }

        inner class RecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            internal var name: TextView
            internal var content: TextView
            internal var avatar: CircleImageView
            internal var rating : RatingBar
            internal var date : TextView

            init {
                name = itemView.findViewById(R.id.reviewerName) as TextView
                content = itemView.findViewById(R.id.reviewContent) as TextView
                date = itemView.findViewById(R.id.reviewDate) as TextView
                rating = itemView.findViewById(R.id.reviewRating) as RatingBar
                avatar = itemView.findViewById(R.id.reviewerAvatar) as CircleImageView
            }
        }
    }




    inner class ChooseDestinationAdapter(data: ArrayList<StopPoint>) :
        RecyclerView.Adapter<ChooseDestinationAdapter.RecyclerViewHolder>() {

        var data = ArrayList<StopPoint>()

        init {
            this.data = data
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.item_choose_destination, parent, false)
            return RecyclerViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
            val item = data.get(position)
            holder.name.setText(Html.fromHtml("To : <b>${item.name}</b>"))
            holder.type.image = getDrawable(util.getAssetByStopPointType(item.serviceTypeId!!))

            holder.itemView.setOnClickListener {
                var intent = Intent(applicationContext, JourneyFollowActivity::class.java)
                intent.putExtra("destinationLat", item.lat!!)
                intent.putExtra("destinationLng", item.long!!)
                intent.putExtra("journeyId", journeyId)
                intent.putExtra("token", token)
                intent.putExtra("desId", item.id)

                startActivity(intent)
            }
        }

        override fun getItemCount(): Int {
            return data.size
        }

        inner class RecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            internal var name: TextView
            internal var type: ImageView

            init {
                name = itemView.destinationName
                type = itemView.destinationTypeIcon
            }
        }
    }




    fun ApiRequest() {
        doAsync {
            val service = WebAccess.retrofit.create(ApiServiceGetJourneyInfo::class.java)
            val call = service.getJourneyInfo(token,journeyId)
            call.enqueue(object : Callback<ResponseJourneyInfo> {
                override fun onFailure(call: Call<ResponseJourneyInfo>, t: Throwable) {
                    Toast.makeText(this@JourneyInfoActivity, t.message, Toast.LENGTH_LONG).show()
                }

                override fun onResponse(
                    call: Call<ResponseJourneyInfo>,
                    response: Response<ResponseJourneyInfo>
                ) {
                    if (response.code() != 200) {
                        Toast.makeText(this@JourneyInfoActivity, response.errorBody().toString(), Toast.LENGTH_LONG).show()
                    } else {
                        Log.d("abab",response.body().toString())
                        val journeyInfoName = journeyInfoName
                        val journeyInfoDate = journeyInfoDate
                        val journeyInfoPeople = journeyInfoPeople
                        val journeyInfoCost = journeyInfoCost
                        val data = response.body()!!
                        journeyInfoName.text = data.name
                        journeyName = data.name
                        var drawable = resources.getDrawable(util.getAssetByStatus(data.status))
                        journeyInfoName.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
                        journeyInfoDate.text = util.longToDate(data.startDate) + " - " + util.longToDate(data.endDate)
                        var people : String = ""
                        if (data.adults >= 0) people += data.adults.toString() + " adults"
                        if (data.childs >= 0) people += " - " + data.childs.toString() + " childs"
                        journeyInfoPeople.text = people
                        journeyInfoCost.text = data.minCost.toString() + " - " + data.maxCost
                        listStopPoint.clear()
                        listStopPoint.addAll(data.stopPoints)

                        listMembers.clear()
                        listMembers.addAll(data.members)

                        var hasJoined = false
                        for (i in listMembers) {
                            if (i.id == currentUserId) {
                                hasJoined = true
                            }
                        }

                        if (!hasJoined) {
                            btnStartGoingJourney.text = "Join this journey"
                            btnStartGoingJourney.setOnClickListener {
                                ApiRequestAddUserToJourney(journeyId, currentUserId, data.isPrivate)
                            }
                        }
                        else {
                            btnStartGoingJourney.text = "Start Going"
                            btnStartGoingJourney.setOnClickListener {
                                popupStartGoing()
                            }
                        }



                        StpAdt.notifyDataSetChanged()

                        journeyMemberNum.text = data.members.size.toString()


                        countTypeStopPoint()
                    }
                }
            })
        }.execute()
    }

    fun ApiRequestNewComment(journeyId : String, userId : String, cm : String) {
        doAsync {
            val service = WebAccess.retrofit.create(ApiServiceJourneyComment::class.java)
            val jsonObject = JsonObject()
            jsonObject.addProperty("journeyId", journeyId)
            jsonObject.addProperty("userId", userId)
            jsonObject.addProperty("comment", cm)
            val call = service.comment(token,jsonObject)
            call.enqueue(object : Callback<ResponseToComment> {
                override fun onFailure(call: Call<ResponseToComment>, t: Throwable) {
                    Toast.makeText(this@JourneyInfoActivity, t.message, Toast.LENGTH_LONG).show()
                }

                override fun onResponse(
                    call: Call<ResponseToComment>,
                    response: Response<ResponseToComment>
                ) {
                    if (response.code() != 200) {
                        Toast.makeText(this@JourneyInfoActivity, response.errorBody().toString(), Toast.LENGTH_LONG).show()
                    } else {
                        ApiRequestGetComment(journeyId)
                    }
                }
            })
        }.execute()
    }

    fun ApiRequestGetComment(journeyId : String) {
        doAsync {
            val service = WebAccess.retrofit.create(ApiServiceGetCommentList::class.java)
            val call = service.getList(token,journeyId,1,"9999")
            call.enqueue(object : Callback<ResponseCommentList> {
                override fun onFailure(call: Call<ResponseCommentList>, t: Throwable) {
                    Toast.makeText(this@JourneyInfoActivity, t.message, Toast.LENGTH_LONG).show()
                }

                override fun onResponse(
                    call: Call<ResponseCommentList>,
                    response: Response<ResponseCommentList>
                ) {
                    if (response.code() != 200) {
                        Toast.makeText(this@JourneyInfoActivity, response.errorBody().toString(), Toast.LENGTH_LONG).show()
                    } else {
                        var data = response.body()
                        listComment.clear()
                        listComment.addAll(data!!.commentList)
                        CommentAdt.notifyDataSetChanged()
                        journeyCommentNum.text = data.commentList.size.toString()
                        if (hasInitCommentNumCountView) {
                            CommentNumCountView.text = listComment.size.toString() + " comments"
                        }
                    }
                }
            })
        }.execute()
    }

    fun ApiRequestGetReviewList(journeyId : Int) {
        doAsync {
            val service = WebAccess.retrofit.create(ApiServiceGetJourneyReview::class.java)
            val call = service.getReview(token,journeyId,1,"9999")
            call.enqueue(object : Callback<ResponseGetReviewsJourney> {
                override fun onFailure(call: Call<ResponseGetReviewsJourney>, t: Throwable) {
                    Toast.makeText(this@JourneyInfoActivity, t.message, Toast.LENGTH_LONG).show()
                }
                override fun onResponse(
                    call: Call<ResponseGetReviewsJourney>,
                    response: Response<ResponseGetReviewsJourney>
                ) {
                    if (response.code() != 200) {
                        Toast.makeText(this@JourneyInfoActivity, response.errorBody().toString(), Toast.LENGTH_LONG).show()
                    } else {
                        Log.d("abab",response.body().toString())
                        listReviews.clear()
                        listReviews.addAll(response.body()!!.reviewList)
                        mainReviewCount.text = "Reviews (${listReviews.size})"
                        reviewCount = arrayOf(0,0,0,0,0)
                        for (i in listReviews) {
                            reviewCount[i.point-1]++
                        }

                        val raters = reviewCount.toIntArray()


                        var maxValue = raters.max()

                        var temp = 0
                        for (i in 0..reviewCount.size - 1 ) {
                            temp += (i+1)*reviewCount[i]
                        }

                        var ratingValue = temp.toFloat() / reviewCount.sum()
                        var ratingValueString  = "%.1f".format(ratingValue)

                        textView2_journey.text = listReviews.size.toString()
                        ratingBar_journey.rating = ratingValue
                        ratingAveragePoint_journey.text = ratingValueString
                        journeyRating.rating = ratingValue

                        rating_reviews_journey.createRatingBars(maxValue!!, BarLabels.STYPE3, Constant.colors, raters.reversedArray())
                        ReviewAdt.notifyDataSetChanged()
                    }
                }
            })
        }.execute()
    }


    fun ApiRequestRemoveStopPoint(journeyId : Int, stpid : Int) {
        doAsync {
            val service = WebAccess.retrofit.create(ApiServiceAddStopPointToJourney::class.java)
            val json = JsonObject()
            json.addProperty("journeyId", journeyId)
            var array = JsonArray()
            array.add(stpid)
            json.add("deleteIds",array)

            val call = service.postData(token, json)
            call.enqueue(object : Callback<ResponseAddStopPoint> {
                override fun onFailure(call: Call<ResponseAddStopPoint>, t: Throwable) {
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_LONG).show()
                }
                override fun onResponse(
                    call: Call<ResponseAddStopPoint>,
                    response: Response<ResponseAddStopPoint>
                ) {
                    if (response.code() != 200) {
                        Toast.makeText(applicationContext, response.errorBody().toString(), Toast.LENGTH_LONG).show()
                    } else {
                        Log.d("abab",response.body().toString())
                        Toast.makeText(applicationContext, "Đã xoá", Toast.LENGTH_LONG).show()
                        ApiRequest()
                    }
                }
            })
        }.execute()
    }


    fun ApiRequestAddReview(journeyId : Int, point: Int, review: String) {
        doAsync {
            val service = WebAccess.retrofit.create(ApiServiceAddReview::class.java)
            val jsonObject = JsonObject()
            jsonObject.addProperty("journeyId", journeyId)
            jsonObject.addProperty("point", point)
            jsonObject.addProperty("review", review)

            val call = service.addReview(token,jsonObject)

            call.enqueue(object : Callback<ResponseToAddReview> {
                override fun onFailure(call: Call<ResponseToAddReview>, t: Throwable) {
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_LONG).show()
                }
                override fun onResponse(
                    call: Call<ResponseToAddReview>,
                    response: Response<ResponseToAddReview>
                ) {
                    if (response.code() != 200) {
                        Toast.makeText(applicationContext, response.errorBody().toString(), Toast.LENGTH_LONG).show()
                    } else {
                        Log.d("abab", point.toString() + " " + review)
                        ApiRequestGetReviewList(journeyId)
                    }
                }
            })
        }.execute()
    }

    fun ApiRequestAddUserToJourney(journeyId : Int, userId: Int, isJourneyPrivate: Boolean) {
        doAsync {
            val serviceuser = WebAccess.retrofit.create(ApiServiceAddUserToJourney::class.java)
            val jsonObject = JsonObject()
            Log.d("abab", journeyId.toString() + " " + userId.toString()+ " " + isJourneyPrivate)

            jsonObject.addProperty("journeyId", journeyId.toString())
            jsonObject.addProperty("invitedUserId", "")
            jsonObject.addProperty("isInvited", isJourneyPrivate)

            val callu = serviceuser.addUser(token,jsonObject)
            callu.enqueue(object : Callback<ResponseAddUserToJourney> {
                override fun onFailure(call: Call<ResponseAddUserToJourney>, t: Throwable) {

                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_LONG).show()
                }

                override fun onResponse(
                    call: Call<ResponseAddUserToJourney>,
                    response: Response<ResponseAddUserToJourney>
                ) {
                    if (response.code() != 200) {
                        val gson = Gson()
                        val type = object : TypeToken<ErrorResponse>() {}.type
                        var errorResponse: ErrorResponse? = gson.fromJson(response.errorBody()!!.charStream(), type)
                        Toast.makeText(applicationContext, errorResponse!!.message, Toast.LENGTH_LONG).show()
                    } else {
                        Log.d("addm",response.body()!!.message)
                        Toast.makeText(applicationContext, "Join Successful!!", Toast.LENGTH_LONG).show()
                        ApiRequest()
                    }
                }
            })
        }.execute()
    }

    fun countTypeStopPoint() {

        for (i in typeCount.indices) {
            typeCount[i] = 0
        }
        for (i in listStopPoint) {
            typeCount[i.serviceTypeId!!-1]++
        }
        countRest.text = typeCount[0].toString()
        countHotel.text = typeCount[1].toString()
        countRes.text = typeCount[2].toString()
        countOthers.text = typeCount[3].toString()
    }

    fun popupComment() {
        val inflater: LayoutInflater =
            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.layout_journey_comment, null)

        commentView = view.findViewById<RecyclerView>(R.id.commentRecyclerView)
        val layoutManager = LinearLayoutManager(applicationContext)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        commentView.layoutManager = layoutManager
        commentView.adapter = CommentAdt

        if (listComment.size > 0) {
            commentView.scrollToPosition(listComment.size - 1)
        }


        var commentNumField = view.findViewById<TextView>(R.id.commentNum)
        CommentNumCountView = commentNumField
        hasInitCommentNumCountView = true
        commentNumField.text = listComment.size.toString() + " comments"

        val popupWindow = PopupWindow(
            view, // Custom view to show in popup window
            LinearLayout.LayoutParams.MATCH_PARENT, // Width of popup window
            LinearLayout.LayoutParams.MATCH_PARENT, // Window height
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
            slideOut.slideEdge = Gravity.RIGHT
            popupWindow.exitTransition = slideOut

        }

        // Get the widgets reference from custom view
        //val tv = view.findViewById<TextView>(R.id.text_view)

        val sendbtn = view.findViewById<ImageView>(R.id.send)
        val contentCM = view.findViewById<EditText>(R.id.commenttext)

        sendbtn.setOnClickListener {
            val data:String = contentCM.text.toString()
            ApiRequestNewComment(journeyId.toString(),"126",data)
            contentCM.setText("")
            contentCM.clearFocus()
        }

        // Set a dismiss listener for popup window
        popupWindow.setOnDismissListener {

        }


        // Finally, show the popup window on app
        popupWindow.showAtLocation(
            journeyInfoMainLayout, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )
    }

    fun popupReview() {
        val inflater: LayoutInflater =
            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.layout_journey_review, null)

        var reviewView = view.findViewById<RecyclerView>(R.id.reviewRecyclerView)
        val layoutManager = LinearLayoutManager(applicationContext)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        reviewView.layoutManager = layoutManager
        reviewView.adapter = ReviewAdt

        view.reviewJourneyName.text = journeyName




        val popupWindow = PopupWindow(
            view, // Custom view to show in popup window
            LinearLayout.LayoutParams.MATCH_PARENT, // Width of popup window
            LinearLayout.LayoutParams.MATCH_PARENT, // Window height
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
            slideIn.slideEdge = Gravity.END
            popupWindow.enterTransition = slideIn

            // Slide animation for popup window exit transition
            val slideOut = Slide()
            slideOut.slideEdge = Gravity.END
            popupWindow.exitTransition = slideOut

        }

        view.btnCloseReview.setOnClickListener {
            popupWindow.dismiss()
        }


        // Get the widgets reference from custom view
        //val tv = view.findViewById<TextView>(R.id.text_view)
//        val btn = view.findViewById<Button>(R.id.addReviewBtn)
//
//        btn.setOnClickListener {
//            val ratingview = inflater.inflate(R.layout.layout_send_rating_journey, null)
//
//            val ratingPopup = PopupWindow(
//                ratingview, // Custom view to show in popup window
//                LinearLayout.LayoutParams.WRAP_CONTENT, // Width of popup window
//                LinearLayout.LayoutParams.WRAP_CONTENT, // Window height
//                true
//            )
//
//            // Set an elevation for the popup window
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                ratingPopup.elevation = 10.0F
//            }
//
//            // Set a dismiss listener for popup window
//            ratingPopup.setOnDismissListener {
//
//            }
//
//            val sendrvbutton = ratingview.findViewById<Button>(R.id.sendReviewBtn)
//            val pointrv = ratingview.findViewById<RatingBar>(R.id.journeyRatingStarSelect)
//            val reviewcontent = ratingview.findViewById<EditText>(R.id.editJourneyRatingContent)
//
//            sendrvbutton.setOnClickListener {
//                ApiRequestAddReview(journeyId,pointrv.rating.toInt(), reviewcontent.text.toString(), ratingPopup)
//            }
//
//            // Finally, show the popup window on app
//            ratingPopup.showAtLocation(
//                journeyInfoMainLayout, // Location to display popup window
//                Gravity.CENTER, // Exact position of layout to display popup
//                0, // X offset
//                0 // Y offset
//            )
//        }


        // Set a dismiss listener for popup window
        popupWindow.setOnDismissListener {

        }


        // Finally, show the popup window on app
        popupWindow.showAtLocation(
            journeyInfoMainLayout, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )
    }


    fun popupSetting() {
        val inflater: LayoutInflater =
            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        //Inflate the dialog with custom view
        val view = LayoutInflater.from(this@JourneyInfoActivity).inflate(R.layout.popup_setting_bottom, null)
        //AlertDialogBuilder
        val mAlertDialog = BottomSheetDialog(this@JourneyInfoActivity)
        mAlertDialog.setContentView(view)
        mAlertDialog.show()



        // Get the widgets reference from custom view
        //val tv = view.findViewById<TextView>(R.id.text_view)

        view.btnJourneyEditInfomation.setOnClickListener {
            mAlertDialog.dismiss()
            var editIntent = Intent(this@JourneyInfoActivity, ChangeJourneyInfo::class.java)
            editIntent.putExtra("token", token)
            editIntent.putExtra("userId", journeyId)
            startActivityForResult(editIntent,6969)

        }

        view.btnJourneySubscribe.setOnClickListener {
            FirebaseMessaging.getInstance().subscribeToTopic("journey-id-$journeyId").addOnCompleteListener { task ->
                var msg = "Subscribe successfully"
                if (!task.isSuccessful) {
                    msg = "Subscribe failed"
                }
                Toast.makeText(this@JourneyInfoActivity, msg, Toast.LENGTH_SHORT).show()
            }
            mAlertDialog.dismiss()
        }

        view.btnDeleteJourney.setOnClickListener {
            mAlertDialog.dismiss()
            ApiRequestDeleteJourney()
        }

        // Set a dismiss listener for popup window

    }

    fun popupStartGoing() {
        val inflater: LayoutInflater =
            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.popup_choose_destination, null)





        var DestinationAdapter = ChooseDestinationAdapter(listStopPoint)
        view.chooseDestinationRecycleView.adapter = DestinationAdapter
        val layoutManager = LinearLayoutManager(applicationContext)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        view.chooseDestinationRecycleView.layoutManager = layoutManager

        val popupWindow = PopupWindow(
            view, // Custom view to show in popup window
            LinearLayout.LayoutParams.MATCH_PARENT, // Width of popup window
            LinearLayout.LayoutParams.WRAP_CONTENT, // Window height
            true
        )


        popupWindow.showAsDropDown(btnStartGoingJourney)




        // Set an elevation for the popup window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.elevation = 10.0F
        }



        // Set a dismiss listener for popup window
        popupWindow.setOnDismissListener {

        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == 6969) {
            if (resultCode == Activity.RESULT_OK) {
                ApiRequest()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    fun ApiRequestReportReview(reviewId : Int) {
        doAsync {
            val service = WebAccess.retrofit.create(ApiServiceReportReview::class.java)
            val body = JsonObject()
            body.addProperty("reviewId", reviewId)
            val call = service.report(token,body)
            call.enqueue(object : Callback<ResponseReport> {
                override fun onFailure(call: Call<ResponseReport>, t: Throwable) {
                    Toast.makeText(this@JourneyInfoActivity, t.message, Toast.LENGTH_LONG).show()
                }
                override fun onResponse(
                    call: Call<ResponseReport>,
                    response: Response<ResponseReport>
                ) {
                    if (response.code() != 200) {
                        Toast.makeText(this@JourneyInfoActivity, response.message(), Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@JourneyInfoActivity, "Report success!", Toast.LENGTH_LONG).show()
                    }
                }
            })
        }.execute()
    }


    fun ApiRequestDeleteJourney() {
        val service = WebAccess.retrofit.create(ApiServiceUpdateJourney::class.java)
        val body = JsonObject()
        body.addProperty("id", journeyId)
        body.addProperty("status", -1)
        val call = service.update(token,body)
        call.enqueue(object : Callback<ResponseUpdateJourneyInfo> {
            override fun onFailure(call: Call<ResponseUpdateJourneyInfo>, t: Throwable) {
                Toast.makeText(this@JourneyInfoActivity, t.message, Toast.LENGTH_LONG).show()
            }

            override fun onResponse(
                call: Call<ResponseUpdateJourneyInfo>,
                response: Response<ResponseUpdateJourneyInfo>
            ) {
                if (response.code() != 200) {
                    val gson = Gson()
                    val type = object : TypeToken<ErrorResponse>() {}.type
                    var errorResponse: ErrorResponse? = gson.fromJson(response.errorBody()!!.charStream(), type)
                    Toast.makeText(this@JourneyInfoActivity, errorResponse!!.message, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@JourneyInfoActivity, "Delete Successfully", Toast.LENGTH_LONG).show()
                    intent = Intent()
                    intent.putExtra("position", position)
                    setResult(RESULT_OK, intent)
                    finish()
                }
            }
        })
    }
}
