package com.ygaps.jazz.network.model

import android.database.Observable
import com.ygaps.jazz.*
import com.ygaps.jazz.model.StopPoint
import com.ygaps.jazz.ui.home.HomeFragment
import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ApiServiceLogin {
    @POST("/user/login")
    fun postData(
        @Body body: JsonObject
    ): Call<ResponseLogin>
}

interface ApiServiceFBLogin {
    @POST("/user/login/by-facebook")
    fun postData(
        @Body body: JsonObject
    ): Call<ResponseOthersLogin>
}

interface ApiServiceGGLogin {
    @POST("/user/login/by-google")
    fun postData(
        @Body body: JsonObject
    ): Call<ResponseOthersLogin>
}

interface ApiServiceRegister {
    @POST("/user/register")
    fun postData(
        @Body body: JsonObject
    ): Call<ResponseRegister>
}


interface ApiServiceGetJourneys {
    @GET("/journey/list")
    fun getJourneys(
        @Header("Authorization") token : String,
        @Query("rowPerPage") rowPerPage: Int,
        @Query("pageNum") pageNum: Int,
        @Query("orderBy") orderBy: String?,
        @Query("isDesc") isDesc: Boolean
    ): Call<ResponseListJourneys>
}


interface ApiServiceSearchJourneys {
    @GET("/journey/search")
    fun getJourneys(
        @Header("Authorization") token : String,
        @Query("searchKey") searchKey: String,
        @Query("pageIndex") pageIndex: Int,
        @Query("pageSize") pageSize: String
    ): Call<ResponseListJourneys>
}


interface ApiServiceGetHistoryJourneys {
    @GET("/journey/history-user")
    fun getJourneys(
        @Header("Authorization") token : String,
        @Query("pageIndex") pageIndex: Int,
        @Query("pageSize") pageSize: String
    ): Call<ResponseListHistoryJourneys>
}

interface ApiServiceSearchHistoryJourneys {
    @GET("/journey/search-history-user")
    fun searchJourneys(
        @Header("Authorization") token : String,
        @Query("searchKey") searchKey: String,
        @Query("pageIndex") pageIndex: Int,
        @Query("pageSize") pageSize: String
    ): Call<ResponseListHistoryJourneys>
}

interface ApiServiceGetHistoryJourneysByStatus {
    @GET("/journey/history-user-by-status")
    fun getJourneysByStatus(
        @Header("Authorization") token : String
    ): Call<ResponseListHistoryJourneysByStatus>
}

interface ApiServiceGetJourneyInfo {
    @GET("/journey/info")
    fun getJourneyInfo(
        @Header("Authorization") token : String,
        @Query("journeyId") journeyId: Int
    ): Call<ResponseJourneyInfo>
}

interface ApiServiceCreateJourney {
    @POST("/journey/create")
    fun postData(
        @Header("Authorization") Authorization: String,
        @Body body: JsonObject
    ): Call<ResponseCreateJourney>
}

interface ApiServiceAddStopPointToJourney {
    @POST("/journey/set-stop-points")
    fun postData(
        @Header("Authorization") Authorization: String,
        @Body body: JsonObject
    ): Call<ResponseAddStopPoint>
}

interface ApiServiceGetUserList {
    @GET("/user/search")
    fun getUsers(
        @Query("searchKey") searchKey : String,
        @Query("pageIndex") pageIndex: Int,
        @Query("pageSize") pageSize: String
    ): Call<ResponseSearchUser>
}

interface ApiServiceAddUserToJourney {
    @POST("/journey/add/member")
    fun addUser(
        @Header("Authorization") Authorization: String,
        @Body body: JsonObject
    ): Call<ResponseAddUserToJourney>
}


interface ApiServiceGetNotifications {
    @GET("/journey/get/invitation")
    fun getNotif(
        @Header("Authorization") token : String,
        @Query("pageIndex") pageIndex: Int,
        @Query("pageSize") pageSize: String
    ): Call<ResponseUserNotification>
}


interface ApiServiceResponseInvitaion {
    @POST("/journey/response/invitation")
    fun response(
        @Header("Authorization") Authorization: String,
        @Body body: JsonObject
    ): Call<ResponseToInvitation>
}

interface ApiServiceJourneyComment {
    @POST("/journey/comment")
    fun comment(
        @Header("Authorization") Authorization: String,
        @Body body: JsonObject
    ): Call<ResponseToComment>
}

interface ApiServiceGetUserInfo {
    @GET("/user/info")
    fun getInfo(
        @Header("Authorization") token : String
    ): Call<ResponseUserInfo>
}

interface ApiServiceGetCommentList {
    @GET("/journey/comment-list")
    fun getList(
        @Header("Authorization") token : String,
        @Query("journeyId") journeyId: String,
        @Query("pageIndex") pageIndex: Int,
        @Query("pageSize") pageSize: String
    ): Call<ResponseCommentList>
}

interface ApiServiceGetVerifyCode {
    @GET("/user/send-active")
    fun getVerify(
        @Header("Authorization") token : String,
        @Query("userId") userId : Int,
        @Query("type") type : String
    ): Call<ResponseGetVerifyCode>
}

interface ApiServiceUpdateUserInfo {
    @POST("/user/edit-info")
    fun updateInfo(
        @Header("Authorization") token : String,
        @Body body: JsonObject
    ): Call<ResponseUpdateUserInfo>
}

interface ApiServiceGetJourneyReview {
    @GET("/journey/get/review-list")
    fun getReview(
        @Header("Authorization") token : String,
        @Query("journeyId") journeyId : Int,
        @Query("pageIndex") pageIndex: Int,
        @Query("pageSize") pageSize : String
    ): Call<ResponseGetReviewsJourney>
}

interface ApiServiceAddReview {
    @POST("/journey/add/review")
    fun addReview(
        @Header("Authorization") Authorization: String,
        @Body body: JsonObject
    ): Call<ResponseToAddReview>
}

interface ApiServiceGetStopPointInfo {
    @GET("/journey/get/service-detail")
    fun getJourneyInfo(
        @Header("Authorization") token : String,
        @Query("serviceId") serviceId: Int
    ): Call<ResponseStopPointInfo>
}

interface ApiServiceGetStopPointPoints {
    @GET("/journey/get/feedback-point-stats")
    fun getPoints(
        @Header("Authorization") token : String,
        @Query("serviceId") serviceId: Int
    ): Call<ResponseStopPointRatingPoints>
}

interface ApiServiceGetJourneyNotices {
    @GET("/journey/notification-list")
    fun getNotices(
        @Header("Authorization") token : String,
        @Query("journeyId") journeyId: Int,
        @Query("pageIndex") pageIndex: Int,
        @Query("pageSize") pageSize: String
    ): Call<ResponseGetJourneyNotice>
}




interface ApiServiceSearchDestination {
    @GET("/journey/search/service")
    fun search(
        @Header("Authorization") token : String,
        @Query("searchKey") searchKey: String,
        @Query("pageIndex") pageIndex: Int,
        @Query("pageSize") pageSize: String
    ): Call<ResponseSearchDestination>
}


interface ApiServiceGetServiceFeedBack {
    @GET("/journey/get/feedback-service")
    fun getFeedback(
        @Header("Authorization") token : String,
        @Query("serviceId") searchKey: Int,
        @Query("pageIndex") pageIndex: Int,
        @Query("pageSize") pageSize: String
    ): Call<ResponseListFeedBackService>
}

interface ApiServiceSuggestDestination {
    @POST("/journey/suggested-destination-list")
    fun getSuggest(
        @Header("Authorization") token : String,
        @Body body: JsonObject
    ): Call<ResponseSuggestDestination>
}

interface ApiServiceGetOTP {
    @POST("/user/request-otp-recovery")
    fun getOTP(
        @Body body: JsonObject
    ): Call<ResponseGetOTP>
}

interface ApiServiceCheckOTP {
    @POST("/user/verify-otp-recovery")
    fun checkOTP(
        @Body body: JsonObject
    ): Call<ResponseCheckOTP>
}


interface ApiServiceChangePassword {
    @POST("/user/update-password")
    fun changepw(
        @Header("Authorization") token : String,
        @Body body: JsonObject
    ): Call<ResponseChangePassword>
}

interface ApiServiceUpdateJourney {
    @POST("/journey/update-journey")
    fun update(
        @Header("Authorization") token : String,
        @Body body: JsonObject
    ): Call<ResponseUpdateJourneyInfo>
}

interface ApiServicePutFcmToken {
    @POST("/user/notification/put-token")
    fun putToken(
        @Header("Authorization") token : String,
        @Body body: JsonObject
    ): Call<ResponsePutFcmToken>
}


interface ApiServiceRemoveFcmToken {
    @POST("/user/notification/remove-token")
    fun removeToken(
        @Header("Authorization") token : String,
        @Body body: JsonObject
    ): Call<ResponseRemoveFcmToken>
}


interface ApiServiceSendServiceFeedback {
    @POST("/journey/add/feedback-service")
    fun sendFeedback(
        @Header("Authorization") token : String,
        @Body body: JsonObject
    ): Call<ResponseFeedbackService>
}

interface ApiServiceSendJourneyNotice {
    @POST("/journey/notification")
    fun sendNotice(
        @Header("Authorization") token : String,
        @Body body: JsonObject
    ): Call<ResponseSendNotice>
}

interface ApiServiceCreateNotificationOnRoad {
    @POST("/journey/add/notification-on-road")
    fun addNoti(
        @Header("Authorization") token : String,
        @Body body: JsonObject
    ): Call<ResponseAddNotificationOnRoad>
}

interface ApiServiceGetNotificationOnRoad {
    @GET("/journey/get/noti-on-road")
    fun getdNoti(
        @Header("Authorization") token : String,
        @Query("journeyId") journeyId : Int,
        @Query("pageIndex") pageIndex : Int,
        @Query("pageSize") pageSize : String
    ): Call<ResponseGetNotificationOnRoad>
}

interface ApiServiceUpdateStopPoint {
    @POST("/journey/set-stop-point")
    fun updateStopPoint(
        @Header("Authorization") token : String,
        @Body body: JsonObject
    ): Call<ResponseUpdateStopPoint>
}


interface ApiServiceSendCoordinate {
    @POST("/journey/current-users-coordinate")
    fun sendCoordinate(
        @Header("Authorization") token : String,
        @Body body: JsonObject
    ): Call<ArrayList<memPosChild>>
}



interface ApiServiceUploadRecord {
    @POST("/journey/recording")
    fun upRecord(
        @Header("Authorization") token : String,
        @Body body : RequestBody
    ): Call<ResponseUploadRecord>

//    @Multipart
//    @POST("/journey/recording")
//    fun upRecord(
//        @Header("Authorization") token : String,
//        @Part file : MultipartBody.Part,
//        @Part("journeyId") journeyId : RequestBody,
//        @Part("fullName") fullName : RequestBody,
//        @Part("avatar") avatar : RequestBody,
//        @Part("lat") lat : RequestBody,
//        @Part("long") long : RequestBody
//    ) : Call<ResponseUploadRecord>
}

interface ApiServiceUpdateLandingTime {
    @POST("/journey/update/landing-times-for-destination")
    fun landing(
        @Header("Authorization") token : String,
        @Body body: JsonObject
    ): Call<ResponseUpdateLandingTime>
}

interface ApiServiceCloneJourney {
    @POST("/journey/clone")
    fun clone(
        @Header("Authorization") token : String,
        @Body body: JsonObject
    ): Call<ResponseJourneyInfo>
}


interface ApiServiceReportReview {
    @POST("/journey/report/review")
    fun report(
        @Header("Authorization") token : String,
        @Body body: JsonObject
    ): Call<ResponseReport>
}

interface ApiServiceReportFeedback {
    @POST("/journey/report/feedback")
    fun report(
        @Header("Authorization") token : String,
        @Body body: JsonObject
    ): Call<ResponseReport>
}



