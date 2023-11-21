package com.app.vc.models.login

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Data(
    @SerializedName("area_of_work")
    val area_of_work: String,
    @SerializedName("city_code")
    val city_code: String,
    @SerializedName("dealer_class")
    val dealer_class: String,
    @SerializedName("dealer_name")
    val dealer_name: String,
    @SerializedName("dealer_no")
    val dealer_no: String,
    @SerializedName("empNo")
    val empNo: String,
    @SerializedName("emp_cmpn_no")
    val emp_cmpn_no: String,
    @SerializedName("emp_corp_no")
    val emp_corp_no: String,
    @SerializedName("ls_enabled")
    val ls_enabled: String,
    @SerializedName("region_code")
    val region_code: String,
    @SerializedName("seq")
    val seq: String,
    @SerializedName("state_code")
    val state_code: String,
    @SerializedName("token")
    val token: String?,
    @SerializedName("user_address")
    val user_address: String,
    @SerializedName("user_id")
    val user_id: String,
    @SerializedName("user_name")
    val user_name: String,
    @SerializedName("userid")
    val userid: String,
    @SerializedName("vc_enabled")
    val vc_enabled: String,
    @SerializedName("work_area_name")
    val work_area_name: String
):Serializable