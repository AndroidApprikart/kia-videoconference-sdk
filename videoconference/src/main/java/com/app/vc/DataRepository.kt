package com.app.vc

import com.app.vc.utils.ApiDetails
import com.app.vc.utils.ApiInterface
import com.app.vc.network.RetrofitClient

class DataRepository (
){

    //can be deleted!
    private val retrofit =
        RetrofitClient().getRetrofitClient(ApiDetails.BASE_URL)
    private val api: ApiInterface = retrofit.create(ApiInterface::class.java)

    val TAG = "DataRepository"

  /*  suspend fun doUploadVCAPICall(
        file: MultipartBody.Part,
        vc_room: String,
        user_type: String,
         who: String,
      appVersion: String
    ): kotlinx.coroutines.flow.Flow<UploadVcFileResponse> {
        Log.d(TAG, "doUploadVCAPICall: ")
        return flow {
            emit(api.uploadVcFile(file, vc_room,user_type,who,appVersion))
        }
    }*/


   /* suspend fun doValidateVCAPICallForServicePerson(
        roomId:String,passcode:String, userType:String, service_id:String,version:String
    ): Flow<ValidateVcResponse> {
        Log.d(TAG, "doValidateVCAPICallForServicePerson: ")
        return flow{
            emit(api.validateVcForServicePerson(
                room = roomId,
                authPasscode = passcode,
                userType = userType,
                servicePersonId = service_id,
                appVersion = VCConstants.version
            ))
        }
    }*/

 /*   suspend fun doValidateVCAPICallForCustomer(
        user_type: String,
        vc_room: String,
        passcode: String,
        appVersion: String
    ): Flow<ValidateVcResponse> {
        Log.d(TAG, "doValidateVCAPICallForCustomer: ")
        return flow{
            emit(api.validateVcForCustomer(
                room = vc_room,
                authPasscode = passcode,
                userType = user_type,
                appVersion = appVersion
            ))
        }
    }
*/
  /*  suspend fun doVCConfigurationAPICall(
        user_type: String,
        vc_room: String,
        who: String,
        appVersion: String
    ): Flow<VcConfigurationResponse> {
        Log.d(TAG, "doVCConfigurationAPICall: ")
        return flow{
            emit(api.getVcConfiguration(
                room = vc_room,
                who = who,
                userType = user_type,
                appVersion = appVersion
            ))
        }
    }
*/
 /*   suspend fun updateStreamIdInServer(
        display_name:String,
        stream_id:String,
        user_type: String,
        vc_room: String,
        appVersion: String
    ): Flow<UpdateStreamIdResponse> {
        Log.d(TAG, "updateStreamIdInServer: ")
        return flow{
            emit(api.updateStreamIdInServer(
                displayName = display_name,
                streamId = stream_id,
                roomId = vc_room,
                userType = user_type,
                appVersion = appVersion
            ))
        }
    }*/

   /* suspend fun getDisplayNameForStream(
        stream_id:String,
        vc_room: String,
        appVersion: String
    ): Flow<DisplayNameResponse> {
        Log.d(TAG, "getDisplayNameForStream: ")
        return flow{
            emit(api.getDisplayName(
               vc_room,
                stream_id,
                appVersion
            ))
        }
    }*/


   /* suspend fun doLoginAPICall(
        body:RequestModelLogin
    ): Flow<ResponseModelLogin> {
        Log.d(TAG, "doLoginAPICall: ")
        return flow{
            emit(api.login(body))
        }
    }*/
}
