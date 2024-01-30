package com.kia.vc.message

import com.app.vc.utils.KeepModel
import com.google.gson.annotations.SerializedName

data class Part(
//    @SerializedName("bas_prc")
//    val basicPrice: String,
//    @SerializedName("hdn_yn")
//    val hdn_yn: String,
//    @SerializedName("hsn_code")
//    val hsnCode: String,
    @SerializedName("is_selected")
    var isSelected: String,
//    @SerializedName("iss_type_code")
//    val iss_type_code: String,
//    @SerializedName("mrp")
//    val mrp: String,
    @SerializedName("part_amt")
    val totalPrice: String,
//    @SerializedName("part_cmpn_no")
//    val componentNumber: String,
    @SerializedName("part_desc")
    val partDescription: String,
//    @SerializedName("part_dlr_no")
//    val dealerNumber: String,
//    @SerializedName("part_iss_no")
//    val part_iss_no: String,
//    @SerializedName("part_iss_status")
//    val part_iss_status: String,
//    @SerializedName("part_list_prce")
//    val partListPrice: String,
    @SerializedName("part_no")
    val partNumber: String,
    @SerializedName("part_qty")
    val quantity: String,
//    @SerializedName("part_tax")
//    val tax: String
): KeepModel