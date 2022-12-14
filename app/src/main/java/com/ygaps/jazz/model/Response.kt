package com.ygaps.jazz

import android.os.Parcel
import android.os.Parcelable
import com.ygaps.jazz.model.StopPoint
import com.ygaps.jazz.model.Journey
import kotlinx.android.parcel.Parcelize
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query


data class ErrorResponse(val message: String)

data class ErrorResponseArray(val message: ArrayList<message>)

data class ResponseLogin(val message:String, val userId:Int, val token:String)
data class ResponseOthersLogin(val message:String, val avatar:Int, val fullname:String, val token:String)
data class ResponseRegister(val error:Int, val message:ArrayList<message>)
data class ResponseListJourneys(var total:Int, var journeys:ArrayList<Journey>, var message:String)
data class ResponseListHistoryJourneys(var total:Int, var journeys:ArrayList<Journey>, var message:String)
data class ResponseListHistoryJourneysByStatus(var totalJourneysGroupedByStatus : ArrayList<journeyStatus>)
data class ResponseCreateJourney(
    val hostId: String,
    val name: String,
    val id: Int,
    val message: ArrayList<errorlist>
)

data class ResponseAddStopPoint(
    val message: String,
    val arr : ArrayList<AddStopPointResultOK>
)

data class AddStopPointResultOK(
    val journeyId: String,
    val name: String,
    val lat: Double,
    val long: Double,
    val arrivalAt: Long,
    val leaveAt: Long,
    val minCost: Long,
    val maxCost: Long,
    val serviceTypeId: Int,
    val id: Int
)

data class ResponseJourneyInfo (
    var id : Int ,
    var serviceId : Int,
    var status : Int ,
    var name : String ,
    var minCost : Long ,
    var maxCost : Long ,
    var startDate: Long ,
    var endDate: Long ,
    var adults : Int ,
    var childs : Int ,
    var isPrivate: Boolean ,
    var avatar : String,
    var stopPoints : ArrayList<StopPoint>,
    var members: ArrayList<member>,
    var comments: ArrayList<comment>
)

data class ResponseSearchUser (
    var total : Int ,
    var users: ArrayList<UserInfo>,
    var message: String
)

data class ResponseAddUserToJourney (
    var message: String
)

data class ResponseToInvitation (
    var message: String
)

data class ResponseToComment (
    var message: String
)

data class ResponseToAddReview (
    var message: String
)

data class ResponseUserNotification (
    var total: Int,
    var journeys: ArrayList<JourneyNotification>
)

data class ResponseReport(val message : String)

data class ResponseSearchDestination(
    var total: Int,
    var stopPoints: ArrayList<StopPoint>
)

data class ResponseListFeedBackService(
    var total: Int,
    var feedbackList: ArrayList<feedback>
)

data class ResponseSuggestDestination(
    var stopPoints: ArrayList<StopPoint>
)

data class ResponseGetOTP(
    val type: String,
    val expiredOn : Long,
    val userId: Int
)

data class ResponseCheckOTP(
    val message: String
)

data class ResponseChangePassword(
    val message: String
)

data class ResponseUpdateUserInfo(
    val message: String
)

data class ResponseCommentList(
    val commentList : ArrayList<comment>
)

data class ResponseUserInfo (
    var id: Int,
    var fullName: String?,
    var email: String?,
    var phone: String?,
    var address: String?,
    var dob: String?,
    var gender: Int?,
    var typeLogin: Int?,
    var createOn: Long?,
    var avatar: String?
)

data class ResponseStopPointInfo (
    var id: Long,
    var address: String?,
    var contact: String?,
    var phone: String?,
    var lat: Double?,
    var long: Double?,
    var maxCost: Long?,
    var minCost: Long?,
    var selfStarRatings: Int?,
    var name: String?,
    var serviceTypeId: Int?
)

data class ResponseStopPointRatingPoints(var pointStats : ArrayList<ratingpoint>)

data class ResponseUpdateJourneyInfo(var id : Int)

data class ResponsePutFcmToken(var message: String)

data class ResponseRemoveFcmToken(var message: String)

data class ResponseFeedbackService(var message: String)

data class ResponseSendNotice(var message: String)

data class ResponseAddNotificationOnRoad(var message: String)

data class ResponseUploadRecord(var message: String)

data class ResponseUpdateLandingTime(var message: String)

data class ResponseUpdateStopPoint(var message: String)

data class ResponseSendCoordinate(var message: String)

data class ResponseGetVerifyCode(var expiredOn: Long)

data class ResponseGetJourneyNotice(var notiList : ArrayList<notification>)

data class ResponseGetNotificationOnRoad(var notiList : ArrayList<notificationonroad>)


data class ratingpoint(var total: Int, var point : Int)

data class notification(val userId: Int, val name : String, val avatar: String, val notification : String)


data class notificationonroad(val lat : Double, val long: Double, val note : String , val notificationType : Int, val speed : Int, val createdByJourneyId : Int)



// sub class
data class message (
    val location:String,
    val param:String,
    val msg:String,
    val value:String
)


data class errorlist (
    var location: String,
    var param: String,
    var msg: String
)


data class journeyStatus(var status: Int, var total: Int)

data class ResponseGetReviewsJourney(var reviewList: ArrayList<review>)

data class review(
    var id: Int, var name:String,var review: String, var avatar: String, var point : Int, var createdOn: Long
)

data class feedback(
    var id: Int, var name:String,var feedback: String, var avatar: String, var point : Int, var createdOn: Long
)

data class member(var id: Int, var name:String,var phone: String, var avatar: String, var isHost : Boolean) :
    Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(name)
        parcel.writeString(phone)
        parcel.writeString(avatar)
        parcel.writeByte(if (isHost) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<member> {
        override fun createFromParcel(parcel: Parcel): member {
            return member(parcel)
        }

        override fun newArray(size: Int): Array<member?> {
            return arrayOfNulls(size)
        }
    }
}

@Parcelize
data class memPos(val memPos: ArrayList<memPosChild>): Parcelable
@Parcelize
data class memPosChild(val id : Int, val lat : Double, val long : Double) : Parcelable

data class comment(var id: Int, var name:String,var comment: String, var avatar: String) :
    Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(name)
        parcel.writeString(comment)
        parcel.writeString(avatar)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<comment> {
        override fun createFromParcel(parcel: Parcel): comment {
            return comment(parcel)
        }

        override fun newArray(size: Int): Array<comment?> {
            return arrayOfNulls(size)
        }
    }
}

data class UserInfo(var id : Int?, var fullName: String?, var email: String?,var phone: String?, var gender: Int?,
                var typeLogin : Int?, var avatar: String?, var dob: String?)

data class JourneyNotification(var id: Int,var status: Int,var hostId: Int,var hostName: String,
                            var hostPhone: String,var hostEmail: String,var hostAvatar: String,
                            var name: String,var minCost : Long , var maxCost : Long ,
                            var startDate: Long , var endDate: Long , var adults : Int ,
                            var childs : Int , var isPrivate: Boolean , var avatar : String,
                            var createdOn: Long)






