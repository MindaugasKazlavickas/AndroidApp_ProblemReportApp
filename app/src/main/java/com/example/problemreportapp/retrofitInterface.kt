package com.example.problemreportapp

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ProblemReportApi {
    @Multipart
    @POST("report")
    fun submitProblem(
        @Part("message") message: RequestBody, // Send message as RequestBody
        @Part image: MultipartBody.Part? = null // Send image as file (optional)
    ): Call<ResponseBody>
}