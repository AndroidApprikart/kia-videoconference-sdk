package com.app.vc.network

internal class ApiDetails {
    companion object{
        /*const val BASE_URL = "https://toyota-lakshya-onlineassessment.in/"
const val GET_DISPLAY_NAME = "vc/get_display_name/?"*/
        const val BASE_URL = "https://kia.apprikart.com/"
        const val KIA_BASE_URL = "https://kialinkd-qa.kiaindia.net/dev/"
        const val KIA_BASE_URL_VPN = "http://10.107.11.242:7001/kiakandit/service/vcConference/"

        //        const val BASE_URL = "https://videoserver.apprikart.com/"
        const val MEDIA_BASE_URL = "https://videoserver.apprikart.com"
        const val SUB_URL = "kandid_api/v1/"
        const val SUB_URL_Testing = "kia_vc_api/v1/"
        //        const val SUB_URL_1 = "kia_vc_api/V1/"
        const val GET_DISPLAY_NAME = SUB_URL+"get_display_name/?"
        const val VALIDATE_VC = SUB_URL+"validate_vc/?"
        const val UPLOAD_VC_FILE = SUB_URL_Testing + "upload_vc_file/"
        const val UPDATE_STREAM_ID = SUB_URL+"update_stream_id/?"
        const val KICK_OUT_USER = SUB_URL+"kick_out_user/?"
        const val GET_APP_UPDATE_DETAILS = SUB_URL+"get_app_update_details/?"
        const val GET_VC_CONFIGURATION = SUB_URL+"get_vc_configuration/?"
        const val SEND_ESTIMATION = KIA_BASE_URL+"customerEstimate"
        const val UPDATE_ESTIMATION_STATUS = KIA_BASE_URL_VPN + "updateEstimateStatus"
        const val SAVE_CHAT_DETAILS = KIA_BASE_URL + "storeMessage"


        const val GET_SURVEY_QUESTIONS = "http://10.107.11.242:7001/kiakandit/service/vcConference/feedbackQuestions"
        const val POST_SURVEY_QUESTIONS = "http://10.107.11.242:7001/kiakandit/service/vcConference/createVCSurvey"


        const val VALIDATE_DEALER_CODE = BASE_URL + "kandid_api/v1/validate_license/?"

        const val UPDATE_VC_STATUS_CUSTOMER = KIA_BASE_URL_VPN + "updateVcStatus"

        const val LOGIN = KIA_BASE_URL + "LoginForm"

        const val GET_ESTIMATION_LIST_NEW = "customerEstimate"
        const val SAVE_CHAT_DETAILS_NEW = "storeMessage"

        const val GET_SURVEY_QUESTIONS_NEW = "service/vcConference/feedbackQuestions"
        const val POST_SURVEY_QUESTIONS_NEW = "service/vcConference/createVCSurvey"
        const val UPDATE_ESTIMATION_STATUS_NEW = "service/vcConference/updateEstimateStatus"
        const val UPDATE_VC_STATUS_CUSTOMER_NEW = "service/vcConference/updateVcStatus"
        const val GET_VC_LIST = "kandid_api/v1/get_vc_list/?"

        const val DELETE_BROADCAST = "delete_stream_from_room"

        const val encryptedUserName = "OjV2hfJEdrshGQ7WdAasqg=="
        const val encryptedPassword = "OjV2hfJEdrshGQ7WdAasqg=="
        const val stunUrl = "stun:vc.apprikart.com"
        const val udp = "turn:vc.apprikart.com" + ":3478?transport=udp"
        const val tcp = "turn:vc.apprikart.com" + ":3478?transport=tcp"
        const val port = "turn:vc.apprikart.com" + ":3478"


    }
}