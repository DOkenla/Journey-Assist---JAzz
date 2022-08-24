package com.ygaps.jazz.view.navigation.history

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.transition.Slide
import android.transition.TransitionManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ygaps.jazz.*
import com.ygaps.jazz.manager.doAsync
import com.ygaps.jazz.model.Journey
import com.ygaps.jazz.network.model.*
import com.ygaps.jazz.ui.home.HomeFragment
import com.ygaps.jazz.ui.home.HomeViewModel
import com.ygaps.jazz.util.util
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jaredrummler.materialspinner.MaterialSpinner
import kotlinx.android.synthetic.main.fragment_history.view.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.historyview.view.*

import org.jetbrains.anko.image
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class HistoryFragment : Fragment() {

    var token: String = ""
    var listJourney = ArrayList<Journey>()
    var rowPerPage: Int = 10
    var pageNum: Int = 1
    var orderBy : String ?= null
    var isDesc : Boolean = false
    lateinit var journeyAdapter : RecyclerViewAdapter
    lateinit var lv: RecyclerView
    lateinit var loaded: TextView
    lateinit var curJourneyCount: TextView
    var curLoaded = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_history, container, false)

        val sharePref: SharedPreferences =
            this.activity!!.getSharedPreferences("logintoken", Context.MODE_PRIVATE)
        token = sharePref.getString("token", "nnn")!!

        loaded = root.findViewById<TextView>(R.id.journeyLoaded)
        curJourneyCount = root.findViewById<TextView>(R.id.journeyNumber)
        journeyAdapter = RecyclerViewAdapter(listJourney)
        val layoutManager = LinearLayoutManager(this.context)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        lv = root.findViewById<RecyclerView>(R.id.journeyListView)
        lv.layoutManager = layoutManager
        lv.adapter = journeyAdapter

        ApiRequest(root,pageNum,rowPerPage.toString())
        ApiRequestHistoryByStatus(root)

        val addNewBtn = root.findViewById<FloatingActionButton>(R.id.floatingaddnew)
        addNewBtn.setOnClickListener {
            startActivity(Intent(activity, CreateJourneyActivity::class.java))
        }

        val sv = root.findViewById<SearchView>(R.id.searchJourneys)
        val bntext = root.findViewById<TextView>(R.id.bannerText)

        sv.setOnSearchClickListener {
            sv.maxWidth= Int.MAX_VALUE
            bntext.visibility = View.GONE
        }

        sv.setOnCloseListener(object: SearchView.OnCloseListener {
            override fun onClose(): Boolean {
                bntext.visibility = View.VISIBLE
                return false
            }
        })

        sv.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                ApiRequestSearchHistoryJourney(query!!, 1, "9999")
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })



        lv.addOnScrollListener(object : RecyclerView.OnScrollListener(){

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {


                if (!recyclerView.canScrollVertically(1) && curLoaded >= rowPerPage) {
                    pageNum++
                    ApiRequest(root,pageNum,rowPerPage.toString())
                }

                super.onScrolled(recyclerView, dx, dy)
            }
        })


        return root
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == 12321 && resultCode == Activity.RESULT_OK) {
            try {
                var position = data?.extras?.getInt("position", -1)
                Log.d("tss", position.toString())
                if (position != -1 && position != null) {
                    listJourney.removeAt(position)
                    journeyAdapter.notifyDataSetChanged()
                }
            }
            catch (ex : Exception) {
                ex.printStackTrace()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    inner class RecyclerViewAdapter(data: ArrayList<Journey>) :
        RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewHolder>() {

        var data = ArrayList<Journey>()

        init {
            this.data = data
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.historyview, parent, false)
            return RecyclerViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
            val item = data.get(position)


            holder.titleItem.text = item.name

            //date
            if (item.startDate > 0  && item.endDate > 0) {
                holder.dateItem.text =
                    (util.longToDate(item.startDate) + " - " + util.longToDate(item.endDate))
            }

            // people
            var people = ""
            if (item.adults.toString() != "null") {
                people += item.adults.toString() + " adults"
            }

            if (item.childs.toString() != "null") {
                if (!people.isEmpty()) people += " - "
                people += item.childs.toString() + " childs"
            }
            holder.peopleItem.text = people

            holder.statusItem.image = resources.getDrawable(util.getAssetByStatus(item.status))

            // cost
            holder.costItem.text = (item.minCost.toString() + " - " + item.maxCost.toString())

            holder.itemView.setOnClickListener {
                val intent = Intent(context, JourneyInfoActivity::class.java)
                intent.putExtra("token", token)
                intent.putExtra("journeyID", item.id)
                intent.putExtra("position", position)
                startActivityForResult(intent,12321)
            }
        }

        override fun getItemCount(): Int {
            return data.size
        }


        inner class RecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            internal var titleItem: TextView
            internal var dateItem: TextView
            internal var peopleItem: TextView
            internal var costItem: TextView
            internal var statusItem: ImageView

            init {
                titleItem = itemView.findViewById(R.id.titleItem) as TextView
                dateItem = itemView.findViewById(R.id.dateItem) as TextView
                peopleItem = itemView.findViewById(R.id.peopleItem) as TextView
                costItem = itemView.findViewById(R.id.costItem) as TextView
                statusItem = itemView.journeyStatusImage
            }
        }
    }


    fun ApiRequest(root: View, pageNum : Int, pageSize: String) {
        doAsync {
            val service = WebAccess.retrofit.create(ApiServiceGetHistoryJourneys::class.java)
            val call = service.getJourneys(token, pageNum, pageSize)
            call.enqueue(object : Callback<ResponseListHistoryJourneys> {
                override fun onFailure(call: Call<ResponseListHistoryJourneys>, t: Throwable) {
                    Toast.makeText(activity!!.applicationContext, t.message, Toast.LENGTH_LONG).show()
                }

                override fun onResponse(
                    call: Call<ResponseListHistoryJourneys>,
                    response: Response<ResponseListHistoryJourneys>
                ) {
                    if (response.code() != 200) {
                        Toast.makeText(
                            activity!!.applicationContext,
                            "Load list journeys failed",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {

                        listJourney.addAll(response.body()!!.journeys)
                        listJourney.removeIf {
                            it.status == -1
                        }
                        if (response.body()!!.total.toString() != "0") {
                            curJourneyCount.text = response.body()!!.total.toString()
                        }
                        journeyAdapter.notifyDataSetChanged()

                        loaded.text = listJourney.size.toString()
                        curLoaded = listJourney.size
                    }
                }
            })
        }.execute()
    }


    fun ApiRequestHistoryByStatus(view : View) {
        doAsync {
            val service = WebAccess.retrofit.create(ApiServiceGetHistoryJourneysByStatus::class.java)
            val call = service.getJourneysByStatus(token)
            call.enqueue(object : Callback<ResponseListHistoryJourneysByStatus> {
                override fun onFailure(call: Call<ResponseListHistoryJourneysByStatus>, t: Throwable) {
                    Toast.makeText(activity!!.applicationContext, t.message, Toast.LENGTH_LONG).show()
                }

                override fun onResponse(
                    call: Call<ResponseListHistoryJourneysByStatus>,
                    response: Response<ResponseListHistoryJourneysByStatus>
                ) {
                    if (response.code() != 200) {
                        Toast.makeText(
                            activity!!.applicationContext,
                            "Load list journeys failed",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        var item = response.body()!!.totalJourneysGroupedByStatus
                        view.numJourneyCancelled.text = item[0].total.toString()
                        view.numJourneyOpenned.text = item[1].total.toString()
                        view.numJourneyStarted.text = item[2].total.toString()
                        view.numJourneyClosed.text = item[3].total.toString()
                    }
                }
            })
        }.execute()
    }



    fun ApiRequestSearchHistoryJourney(searchKey : String, pageNum : Int, pageSize: String) {
        doAsync {
            val service = WebAccess.retrofit.create(ApiServiceSearchHistoryJourneys::class.java)
            val call = service.searchJourneys(token,searchKey, pageNum, pageSize)
            call.enqueue(object : Callback<ResponseListHistoryJourneys> {
                override fun onFailure(call: Call<ResponseListHistoryJourneys>, t: Throwable) {
                    Toast.makeText(activity!!.applicationContext, t.message, Toast.LENGTH_LONG).show()
                }

                override fun onResponse(
                    call: Call<ResponseListHistoryJourneys>,
                    response: Response<ResponseListHistoryJourneys>
                ) {
                    if (response.code() != 200) {
                        Toast.makeText(
                            activity!!.applicationContext,
                            "Load list journeys failed",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        listJourney.clear()
                        listJourney.addAll(response.body()!!.journeys)
                        listJourney.removeIf {
                            it.status == -1
                        }
                        curJourneyCount.text = response.body()!!.total.toString()
                        journeyAdapter.notifyDataSetChanged()
                        loaded.text = listJourney.size.toString()
                        curLoaded = listJourney.size
                    }
                }
            })
        }.execute()
    }
}