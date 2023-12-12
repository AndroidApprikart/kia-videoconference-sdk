package com.kia.vc.message

import com.google.gson.annotations.SerializedName

data class Labour(
    @SerializedName("is_selected")
    var isSelected: String,
//    @SerializedName("issue_type_code")
//    val issueTypeCode: String,
//    @SerializedName("issue_type_name")
//    val issueTypeName: String,
//    @SerializedName("labour_amount")
//    val labourCostBeforeTax: String,
    @SerializedName("labour_code")
    val labourCode: String,
    @SerializedName("labour_desc")
    val labourDescription: String,
    @SerializedName("labour_qty")
    val labourQuantity: String,
    @SerializedName("labour_total_amount")
    val totalLabourCost: String,
//    @SerializedName("labr_tax")
//    val labourTax: String,
//    @SerializedName("tech_id")
//    val techId: String,
//    @SerializedName("tech_name")
//    val techName: String
):java.io.Serializable