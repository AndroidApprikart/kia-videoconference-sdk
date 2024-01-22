package com.app.vc

import android.os.Build

object VCConstants {

    // List of mandatory application permissions to be checked before starting VC conference
    const val version = "test_1.3"

    val MANDATORY_PERMISSIONS = arrayOf(
        android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
//        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_PHONE_STATE,
        android.Manifest.permission.BLUETOOTH_CONNECT,
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.INTERNET,
        android.Manifest.permission.CAMERA
    )
    val MANDATORY_PERMISSIONS_ABOVE_VERSION11 = arrayOf(
        android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
//            android.Manifest.permission.READ_EXTERNAL_STORAGE,
//            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.BLUETOOTH_CONNECT,
        android.Manifest.permission.READ_PHONE_STATE,
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.INTERNET,
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.READ_MEDIA_IMAGES,
        android.Manifest.permission.READ_MEDIA_AUDIO,
        android.Manifest.permission.READ_MEDIA_VIDEO,
    )
    const val REQUEST_PERMISSIONS_CODE = 200


    val PERMISSIONS =if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
//            android.Manifest.permission.READ_EXTERNAL_STORAGE,
//            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.CAMERA
        )
    }else{
        arrayOf(
            android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
//            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.CAMERA
        )
    }
    val PERMISSION_CODE = 101



    val MIC_UNMUTED = "MIC_UNMUTED"
    val MIC_MUTED = "MIC_MUTED"
    val CAM_TURNED_ON = "CAM_TURNED_ON"
    val CAM_TURNED_OFF = "CAM_TURNED_OFF"
    var SCREEN_SHARE_ENABLED = "SCREEN_SHARE_ENABLED"
    var SCREEN_SHARE_DISABLED = "SCREEN_SHARE_DISABLED"
    val EVENT_TYPE = "eventType"
    val STREAM_ID = "streamId"
    val UPDATE_STATUS = "UPDATE_STATUS"
    val MIC_STATUS = "mic"
    val CAMERA_STATUS = "camera"
    val SCREEN_SHARE_STATUS = "SCREEN_SHARE"

//    val COMMAND = "command"

    val SDK_CUSTOM_BROADCAST_ACTION = "sdk_broadcast_custom"
    val SDK_BROADCAST_AUDIO_DEVICE_UPDATE = "audio_device_update"
    val SDK_BROADCAST_CAMERA_DEVICE_UPDATE = "camera_device_update"
    val SDK_BROADCAST_STREAM_LIST_UPDATE = "stream_list_update"


    const val TEXT_MESSAGE = "TEXT_MESSAGE"
    const val FILE_MESSAGE = "FILE_MESSAGE"
    const val ESTIMATION_MESSAGE = "ESTIMATION_MESSAGE"
    const val FILE_NAME = "fileName"
    const val SERVER_FILE_PATH = "serverFilePath"
    const val DISPLAY_NAME = "display_names"
    const val TEXT_MESSAGE_VALUE = "TEXT_MESSAGE_VALUE"
    const val ESTIMATION_MESSAGE_VALUE = "ESTIMATION_MESSAGE_VALUE"

    const val currentTime = "CURRENT_TIME"
    const val MESSAGEID = "message_id"
    //fragment tags
    const val MESSAGE_FRAG = "msg_frag"
    const val PARTICIPANT_FRAG = "participant_frag"
    const val SCREEN_SHARE_FRAG = "screenshare_frag"
    const val SOUND_DEVICE_FRAG = "sound_device_frag"

    enum class UserType(val value: String) {
        SERVICE_PERSON("SERVICE_PERSON"),
        CUSTOMER("customer")
    }

    enum class SaveChatMessageType(val value: String) {
        TEXT("Text"),
        ESTIMATION("Estimation"),
        FILE("File")
    }

}