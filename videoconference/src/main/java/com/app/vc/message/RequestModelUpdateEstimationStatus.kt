package com.app.vc.message

import com.google.gson.annotations.SerializedName

data class RequestModelUpdateEstimationStatus(
    @SerializedName("cust_code")
    val customerCode: String,
    @SerializedName("emp_no")
    val employeeNumber: String,
    @SerializedName("est_status")
    val estimationStatus: String,
    @SerializedName("labours")
    val labourListCodes: String,
    @SerializedName("parts")
    val partListCodes: String,
    @SerializedName("ro_no")
    val roNo: String,
    @SerializedName("dealer_no")
    val dealerNumber: String
):java.io.Serializable