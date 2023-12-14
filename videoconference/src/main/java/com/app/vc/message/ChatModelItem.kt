package com.app.vc.message

import com.google.gson.annotations.SerializedName

data class ChatModelItem(
    @SerializedName("message")
    val chat_box: String,
    @SerializedName("mes_from_id")
    val mes_from_id: String,
    @SerializedName("mes_to_id")
    val mes_to_id: String,
    @SerializedName("message_type")
    val message_type: String,
    @SerializedName("vc_id")
    val vc_id: String
):java.io.Serializable