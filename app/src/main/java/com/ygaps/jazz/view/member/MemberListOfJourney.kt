package com.ygaps.jazz.view.member

import android.animation.AnimatorListenerAdapter
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ygaps.jazz.manager.doAsync
import com.ygaps.jazz.network.model.ApiServiceGetJourneyInfo
import com.ygaps.jazz.network.model.WebAccess
import com.ygaps.jazz.util.util
import kotlinx.android.synthetic.main.activity_member_list_of_journey.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.animation.Animator
import android.opengl.ETC1.getHeight
import androidx.core.view.ViewCompat.animate
import android.R.attr.translationY
import android.content.Context
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.os.Build
import android.transition.Slide
import android.transition.TransitionManager
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.ygaps.jazz.*
import com.ygaps.jazz.model.Journey
import com.ygaps.jazz.network.model.ApiServiceAddUserToJourney
import com.ygaps.jazz.network.model.ApiServiceGetUserList
import com.google.gson.JsonObject
import com.mancj.materialsearchbar.MaterialSearchBar
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_get_coordinate.*
import kotlinx.android.synthetic.main.member_layout_search_res.view.*


class MemberListOfJourney : AppCompatActivity() {

    var memberList = ArrayList<member>()
    lateinit var token : String
    var journeyId : Int = 0
    lateinit var lv : RecyclerView
    lateinit var rcadapter : RecyclerViewAdapter

    lateinit var UserSearchLV : RecyclerView
    lateinit var UserSearchAdt : UserSearchAdapter
    var searchState = false
    var popupSearchState = false
    var searchUserResult = ArrayList<UserInfo>()
    var curJourneyId :Int = 0
    var curJourneyPrivate :Boolean = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_list_of_journey)
        token = this.intent.extras!!.getString("token","notoken")!!
        journeyId = this.intent.extras!!.getInt("journeyId",100)

        val layoutManager = LinearLayoutManager(applicationContext)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        lv = findViewById(R.id.listMembersInJourneys)
        rcadapter = RecyclerViewAdapter(memberList)
        lv.layoutManager = layoutManager
        lv.adapter = rcadapter
        ApiRequest()

        inviteNewMember.setOnClickListener {
            if (!searchState) {
                findAUserSearchBar.animate()
                    .alpha(1.0f)
                    .setDuration(300)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            findAUserSearchBar.visibility = View.VISIBLE
                        }
                    })
                searchState = !searchState
            }
            else {
                findAUserSearchBar.animate()
                    .alpha(0.0f)
                    .setDuration(300)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            findAUserSearchBar.visibility = View.GONE
                        }
                    })
                searchState = !searchState
            }

        }

        findAUserSearchBar.setOnSearchActionListener(object : MaterialSearchBar.OnSearchActionListener {
            override fun onButtonClicked(buttonCode: Int) {

            }

            override fun onSearchStateChanged(enabled: Boolean) {

            }

            override fun onSearchConfirmed(text: CharSequence?) {
                if (popupSearchState) return
                popupSearchState = true
                val inflater: LayoutInflater =
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val view = inflater.inflate(R.layout.recycle_search_user, null)

                UserSearchLV = view.findViewById(R.id.userResView)
                val layoutManager = LinearLayoutManager(applicationContext)
                layoutManager.orientation = LinearLayoutManager.VERTICAL
                UserSearchLV.layoutManager = layoutManager
                UserSearchAdt = UserSearchAdapter(searchUserResult)
                UserSearchLV.adapter = UserSearchAdt

                val popupWindow = PopupWindow(
                    view, // Custom view to show in popup window
                    LinearLayout.LayoutParams.MATCH_PARENT, // Width of popup window
                    LinearLayout.LayoutParams.WRAP_CONTENT // Window height
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
                val buttonPopup = view.findViewById<ImageButton>(R.id.btnCloseStopPointList)


                // Set a click listener for popup's button widget
                buttonPopup.setOnClickListener {
                    // Dismiss the popup window
                    popupWindow.dismiss()
                }

                // Set a dismiss listener for popup window
                popupWindow.setOnDismissListener {
                    Toast.makeText(applicationContext, "Popup closed", Toast.LENGTH_SHORT).show()
                    popupSearchState = false
                }


                // Finally, show the popup window on app
                popupWindow.showAtLocation(
                    member_list_layout, // Location to display popup window
                    Gravity.CENTER, // Exact position of layout to display popup
                    0, // X offset
                    0 // Y offset
                )

                ApiRequestUser(text.toString())

            }
        })

    }


    inner class RecyclerViewAdapter(data: ArrayList<member>) :
        RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewHolder>() {

        var data = ArrayList<member>()

        init {
            this.data = data
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.member_layout_in_list, parent, false)
            return RecyclerViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
            val item = data.get(position)

            holder.name.text = item.name
            holder.phone.text = item.phone
            if (item.avatar != null) {
                util.urlToImageView(item.avatar, holder.avatar, 90)
            }
            //onclick

            holder.itemView.setOnClickListener {

            }
        }

        override fun getItemCount(): Int {
            return data.size
        }


        inner class RecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            internal var name: TextView
            internal var phone: TextView
            internal var avatar: CircleImageView

            init {
                name = itemView.findViewById(R.id.MemberName) as TextView
                phone = itemView.findViewById(R.id.MemberPhone) as TextView
                avatar = itemView.findViewById(R.id.profile_image) as CircleImageView
            }
        }
    }


    inner class UserSearchAdapter(data: ArrayList<UserInfo>) :
        RecyclerView.Adapter<UserSearchAdapter.RecyclerViewHolder>() {

        var data = ArrayList<UserInfo>()

        init {
            this.data = data
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.member_layout_search_res, parent, false)
            return RecyclerViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
            val item = data.get(position)

            holder.name.text = item.fullName
            holder.phone.text = item.phone
            holder.email.text = item.email
            if (item.avatar != null) {
                util.urlToImageView(item.avatar!!, holder.avatar, 90)
            }


            holder.itemView.setOnClickListener {
                // Initialize a new instance of
                val builder = AlertDialog.Builder(this@MemberListOfJourney)

                // Set the alert dialog title
                builder.setTitle("Thêm vào Journey")

                // Display a message on alert dialog

                builder.setMessage("Bạn có muốn mời ${item.fullName} vào journey không?")

                // Set a positive button and its click listener on alert dialog
                builder.setPositiveButton("Có"){dialog, which ->
                    ApiRequestAddUserToJourney(item.id!!, curJourneyPrivate)
                }


                // Display a negative button on alert dialog
                builder.setNegativeButton("Không"){dialog,which ->
                    Toast.makeText(applicationContext,"Không thêm!",Toast.LENGTH_SHORT).show()
                }


                // Display a neutral button on alert dialog
                builder.setNeutralButton("Huỷ"){_,_ ->
                }

                // Finally, make the alert dialog using builder
                val dialog: AlertDialog = builder.create()

                // Display the alert dialog on app interface
                dialog.show()
            }
        }

        override fun getItemCount(): Int {
            return data.size
        }


        inner class RecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            internal var name: TextView
            internal var phone: TextView
            internal var email: TextView
            internal var avatar : CircleImageView


            init {
                name = itemView.findViewById(R.id.MemberName) as TextView
                phone = itemView.findViewById(R.id.MemberPhone) as TextView
                email = itemView.findViewById(R.id.MemberEmail) as TextView
                avatar = itemView.profile_image
            }
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
                        Log.d("members",response.body()!!.toString())
                        curJourneyId = response.body()!!.id
                        curJourneyPrivate = response.body()!!.isPrivate
                        memberList.clear()
                        memberList.addAll(response.body()!!.members)
                        rcadapter.notifyDataSetChanged()
                    }
                }
            })
        }.execute()
    }

    fun ApiRequestUser(query: String) {
        doAsync {
            val serviceuser = WebAccess.retrofit.create(ApiServiceGetUserList::class.java)
            val callu = serviceuser.getUsers(query,1,"9999")
            callu.enqueue(object : Callback<ResponseSearchUser> {
                override fun onFailure(call: Call<ResponseSearchUser>, t: Throwable) {
                    Log.d("members","cec")
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_LONG).show()
                }

                override fun onResponse(
                    call: Call<ResponseSearchUser>,
                    response: Response<ResponseSearchUser>
                ) {
                    if (response.code() != 200) {
                        Toast.makeText(applicationContext, response.body()!!.message, Toast.LENGTH_LONG).show()
                    } else {
                        searchUserResult.clear()
                        searchUserResult.addAll(response.body()!!.users)
                        UserSearchAdt.notifyDataSetChanged()
                    }
                }
            })
        }.execute()
    }


    fun ApiRequestAddUserToJourney(inviteUserId: Int, isJourneyPrivate: Boolean) {
        doAsync {
            val serviceuser = WebAccess.retrofit.create(ApiServiceAddUserToJourney::class.java)
            val jsonObject = JsonObject()
            jsonObject.addProperty("journeyId", curJourneyId.toString())
            jsonObject.addProperty("invitedUserId", inviteUserId.toString())
            jsonObject.addProperty("isInvited", true)

            Log.d("addm", inviteUserId.toString() + " " + journeyId.toString())

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
                        Toast.makeText(applicationContext, response.message(), Toast.LENGTH_LONG).show()
                        Log.d("addm",response.errorBody().toString())
                    } else {
                        Log.d("addm",response.body()!!.message)
                        Toast.makeText(applicationContext, "Invited Successful!!", Toast.LENGTH_LONG).show()
                        ApiRequest()
                    }
                }
            })
        }.execute()
    }
}
