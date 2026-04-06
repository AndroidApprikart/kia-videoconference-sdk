package com.app.vc.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface EmployeeApiService {
    @POST("api/krystal/getEmployeeList")
    suspend fun getEmployeeList(
        @Header("X-API-Key") apiKey: String,
        @Body request: EmployeeListRequest
    ): Response<EmployeeListResponse>
}

data class EmployeeListRequest(
    @SerializedName("cmpnNo") val companyNumber: String,
    @SerializedName("corpNo") val corporateNumber: String,
    @SerializedName("dlrNo") val dealerNumber: String,
    @SerializedName("areaWorkType") val areaWorkType: String
)

data class EmployeeListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("status") val status: String?,
    @SerializedName("data") val data: List<EmployeeItem> = emptyList()
)

data class EmployeeItem(
    @SerializedName("empNo") val employeeNumber: String,
    @SerializedName("empName") val employeeName: String,
    @SerializedName("areaWorkCode") val areaWorkCode: String?,
    @SerializedName("areaWorkName") val areaWorkName: String?
)
