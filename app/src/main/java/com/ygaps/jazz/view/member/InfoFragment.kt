package com.ygaps.jazz.view.member

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ygaps.jazz.R
import com.ygaps.jazz.manager.doAsync
import com.ygaps.jazz.model.StopPoint
import com.ygaps.jazz.network.model.ApiServiceGetHistoryJourneys
import com.ygaps.jazz.network.model.ApiServiceGetJourneyInfo
import com.ygaps.jazz.network.model.WebAccess
import com.ygaps.jazz.util.util
import kotlinx.android.synthetic.main.activity_create_journey.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.journeyview.*
import org.w3c.dom.Text
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.widget.ExpandableListAdapter
import com.ygaps.jazz.ResponseJourneyInfo
import com.ygaps.jazz.member


/**
 * A placeholder fragment containing a simple view.
 */
class InfoFragment : Fragment() {

    lateinit var token : String
    var journeyId : Int = 0
    var listStopPoint = ArrayList<StopPoint>()
    var listMember = ArrayList<member>()

    var serviceTypeCount = arrayOf(0,0,0,0)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_tab_info, container, false)
        token = activity!!.intent.extras!!.getString("token","notoken")!!
        journeyId = activity!!.intent.extras!!.getInt("journeyID",100)
        ApiRequest(root)

//        var stpexlv = root.findViewById<ExpandableListView>(R.id.StopPointExpandListView)
//        var stoppointexpandAdapter = StopPointExpandableListAdapter(context!!,"Stop Point",listStopPoint)
//        stpexlv.setAdapter(stoppointexpandAdapter)
//
//        stpexlv.setOnGroupClickListener(ExpandableListView.OnGroupClickListener { parent, v, groupPosition, id ->
//            setListViewHeight(parent, groupPosition)
//            false
//        })

        return root
    }
    companion object {
        fun newInstance(): InfoFragment = InfoFragment()
    }



    fun ApiRequest(root : View) {
        val service = WebAccess.retrofit.create(ApiServiceGetJourneyInfo::class.java)
        val call = service.getJourneyInfo(token,journeyId)
            call.enqueue(object : Callback<ResponseJourneyInfo> {
                override fun onFailure(call: Call<ResponseJourneyInfo>, t: Throwable) {
                    Toast.makeText(activity!!.applicationContext, t.message, Toast.LENGTH_LONG).show()
                }

                override fun onResponse(
                    call: Call<ResponseJourneyInfo>,
                    response: Response<ResponseJourneyInfo>
                ) {
                    if (response.code() != 200) {
                        Toast.makeText(activity!!.applicationContext, response.errorBody().toString(), Toast.LENGTH_LONG).show()
                    } else {
                        val journeyInfoName = activity!!.findViewById<TextView>(R.id.journeyInfoName)
                        val journeyInfoDate = root.findViewById<TextView>(R.id.journeyInfoDate)
                        val journeyInfoPeople = root.findViewById<TextView>(R.id.journeyInfoPeople)
                        val journeyInfoCost = root.findViewById<TextView>(R.id.journeyInfoCost)
                        val data = response.body()!!
                        journeyInfoName.text = data.name
                        journeyInfoDate.text = util.longToDate(data.startDate) + " - " + util.longToDate(data.endDate)
                        var people : String = ""
                        if (data.adults >= 0) people += data.adults.toString() + " adults"
                        if (data.childs >= 0) people += " - " + data.childs.toString() + " childs"
                        journeyInfoPeople.text = people
                        journeyInfoCost.text = data.minCost.toString() + " - " + data.maxCost
                        listStopPoint.addAll(data.stopPoints)
                        listMember.addAll(data.members)
                        countServiceType()
                    }
                }
            })
    }


    private fun setListViewHeight(
        listView: ExpandableListView,
        group: Int
    ) {
        val listAdapter = listView.expandableListAdapter as ExpandableListAdapter
        var totalHeight = 0
        val desiredWidth = View.MeasureSpec.makeMeasureSpec(
            listView.width,
            View.MeasureSpec.EXACTLY
        )
        for (i in 0 until listAdapter.groupCount) {
            val groupItem = listAdapter.getGroupView(i, false, null, listView)
            groupItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED)

            totalHeight += groupItem.measuredHeight

            if (listView.isGroupExpanded(i) && i != group || !listView.isGroupExpanded(i) && i == group) {
                for (j in 0 until listAdapter.getChildrenCount(i)) {
                    val listItem = listAdapter.getChildView(
                        i, j, false, null,
                        listView
                    )
                    listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED)

                    totalHeight += listItem.measuredHeight

                }
            }
        }

        val params = listView.layoutParams
        var height = totalHeight + listView.dividerHeight * (listAdapter.groupCount - 1)
        if (height < 10)
            height = 200
        params.height = height
        listView.layoutParams = params
        listView.requestLayout()
    }

    fun countServiceType() {
        for (i in listStopPoint) {
            serviceTypeCount[i.serviceTypeId!!-1]++
        }
    }

}