package com.app.vc.message

import com.app.vc.utils.KeepModel
import com.google.gson.annotations.SerializedName
import com.kia.vc.message.Labour
import com.kia.vc.message.Part

data class ResponseModelEstimateData(
    @SerializedName("deferred_job_list")
    val deferred_job_list: List<String>?,
    @SerializedName("estimationApprovalStatus")
    var estimationApprovalStatus: String?,
    @SerializedName("labour_list")
    var labour_list: ArrayList<Labour>,
    @SerializedName("part_list")
    var part_list: ArrayList<Part>,
    @SerializedName("totalEstimate")
    val totalEstimate: Double,
    @SerializedName("totalLabourEstimate")
    val totalLabourEstimate: Double,
    @SerializedName("totalPartsEstimate")
    val totalPartsEstimate: Double,
    var selectedItemsTotal: Double = 0.0,
    var areAllItemsSelected:Boolean = false
): KeepModel