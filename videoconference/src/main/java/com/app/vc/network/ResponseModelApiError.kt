package com.app.vc.network

import com.app.vc.KeepModel
import com.google.gson.annotations.SerializedName
//import com.styletribute.app.usermanagement.models.ErrorResponse

/* created by Naghma 06/10/23*/


data class ResponseModelApiError(
    @SerializedName("errorKey")
    val errorKey: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("entityName")
    val entityName: String?,
    @SerializedName("status")
    val status: Int?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("params")
    val params: String?
//    @SerializedName("errors")
//    val errors: List<ErrorResponse>?
): KeepModel

