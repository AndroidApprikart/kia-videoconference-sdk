package com.app.vc

import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import com.app.vc.databinding.LayoutCommonPbDialogBinding

import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*


class AndroidUtils {

    companion object {
        //added on 12th Feb 21
        const val TAG = "AndroidUtils"
        fun isNetworkOnline4(): Boolean {
            var isOnline = false
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress("8.8.8.8", 53), 3000)
                // socket.connect(new InetSocketAddress("114.114.114.114", 53), 3000);
                isOnline = true
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return isOnline
        }


        fun isNetworkAvailablenbg(context: Context): Boolean {
            if(isWifiConnectednbg( context) ) {
                if(isInternetAvailable(context)) {
                    Log.d(TAG, "onCreate: wifi: Connected: Internet: Available")
                    return true
                }else {
                    Log.d(TAG, "onCreate: wifi: Connected: Internet: Not Available")
                    return false
                }
            }else {
                if(isInternetAvailablenbg(context)) {
                    Log.d(TAG, "onCreate: wifi: NotConnected: Internet: Available")
                    return true
                }else {
                    Log.d(TAG, "onCreate: Wifi: NotConnected: Internet Not available")
                    return false
                }
            }
            return false
        }


        fun isWifiConnectednbg(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }

        fun isInternetAvailablenbg(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
                return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } else {
                val activeNetworkInfo = connectivityManager.activeNetworkInfo ?: return false
                return activeNetworkInfo.isConnected
            }
        }


        fun isNetworkOnLine(context: Context): Boolean {
            val status = false
            try {
                val cm =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
                    if (capabilities != null) {
                        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                            return true
                        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                            return true
                        }else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                            Log.d("testvpn", "isNetworkOnLine: te ${cm.getNetworkInfo(ConnectivityManager.TYPE_VPN)?.isConnectedOrConnecting}")
                            return true
                        }
                    }
                } else {
                    val activeNetwork = cm.activeNetworkInfo
                    if (activeNetwork != null) {
                        if (activeNetwork.type == ConnectivityManager.TYPE_WIFI) {
                            return true
                        } else if (activeNetwork.type == ConnectivityManager.TYPE_MOBILE) {
                            return true
                        }else if(activeNetwork.type == ConnectivityManager.TYPE_VPN) {


                            return true
                        }
                    }
                }
            } catch (e: Exception) {
                return false
            }
            return false



//            val connectivityManager =
//                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//            val network = connectivityManager.activeNetwork
//            if (network == null) {
//                return false
//            }
//
//            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
//            return networkCapabilities?.run {
//                hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
//                        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
//                        hasTransport(NetworkCapabilities.TRANSPORT_VPN)
//            } ?: false
        }

        fun hasPermissions(
            context: Context,
            permissions: Array<String>
        ): Boolean {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d(TAG, "hasPermissions: return false")
                    return false
                }
            }
            Log.d(TAG, "hasPermissions: return true")
            return true
        }

//        fun getUnsafeOkHttpClient(): OkHttpClient {
//            // Create a trust manager that does not validate certificate chains
//            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
//                override fun checkClientTrusted(
//                        chain: Array<out X509Certificate>?,
//                        authType: String?
//                ) {
//                }
//
//                override fun checkServerTrusted(
//                        chain: Array<out X509Certificate>?,
//                        authType: String?
//                ) {
//                }
//
//                override fun getAcceptedIssuers() = arrayOf<X509Certificate>()
//            })
//
//            // Install the all-trusting trust manager
//            val sslContext = SSLContext.getInstance("SSL")
//            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
//            // Create an ssl socket factory with our all-trusting manager
//            val sslSocketFactory = sslContext.socketFactory
//
//            return OkHttpClient.Builder()
//                    .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
//                    .hostnameVerifier(HostnameVerifier { _, _ -> true })
//
//                    .build()
//        }

//        fun getPicassoUnsafeCertificate(context: Context): Picasso {
//            val client = getUnsafeOkHttpClient()
//            val picasso = Picasso.Builder(context).downloader(OkHttp3Downloader(client)).build()
//            picasso.isLoggingEnabled = true
//            return picasso
//        }

        fun getCurrentTimeInMill(): Long{
            return Calendar.getInstance().timeInMillis
        }

        fun getAppVersionNumber(context: Context): String {
            return getAppVersionNumber(context, null)
        }

        fun getAppVersionNumber(
            context: Context,
            packageName: String?
        ): String {
            var packageName = packageName
            val versionName: String
            if (packageName == null) {
                packageName = context.packageName
            }
            versionName = try {
                val packageManager = context.packageManager
                val packageInfo = packageManager.getPackageInfo(packageName.toString(), 0)
                packageInfo.versionName
            } catch (e: java.lang.Exception) {
                ""
            }
            return versionName
        }




        fun isNetworkAvailable(context: Context): Boolean {
            if(isWifiConnected( context) ) {
                if(isInternetAvailable(context)) {
                    Log.d("InternetTest", "onCreate: wifi: Connected: Internet: Available")
                    return true
                }else {
                    Log.d("InternetTest", "onCreate: wifi: Connected: Internet: Not Available")
                    return false
                }
            }else {
                if(isInternetAvailable(context)) {
                    Log.d("InternetTest", "onCreate: wifi: NotConnected: Internet: Available")
                    return true
                }else {
                    Log.d("InternetTest", "onCreate: Wifi: NotConnected: Internet Not available")
                    return false
                }
            }
            return false
        }


        fun isWifiConnected(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }

        fun isInternetAvailable(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
                return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } else {
                val activeNetworkInfo = connectivityManager.activeNetworkInfo ?: return false
                return activeNetworkInfo.isConnected
            }
        }

        fun progressDialog(
            context: Context,
            message: String = context.resources.getString(R.string.please_wait),
        ): Dialog {
            val dialog = Dialog(context)
            val dialogBinding = LayoutCommonPbDialogBinding.inflate(LayoutInflater.from(context))
            dialog.setContentView(dialogBinding.root)
            dialog.setCancelable(false)
            dialog.setCanceledOnTouchOutside(false)
//        dialogBinding.pbMessage.text = message
            //dialog alignment and size code.
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(dialog.window?.attributes)
            lp.width = WindowManager.LayoutParams.MATCH_PARENT
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
            lp.gravity = Gravity.CENTER
            dialog.window?.attributes = lp
            dialog.getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
            return dialog
        }

    }
}