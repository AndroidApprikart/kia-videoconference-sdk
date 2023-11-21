package com.app.vc.network

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


class RetrofitClient {
    companion object {
        private const val BASE_URL = ApiDetails.BASE_URL
    }

    val okhttp = OkHttpClient().newBuilder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
//        .addInterceptor { chain ->
//            val original = chain.request()
//            val requestBuilder = if(!preferenceProvider.loginToken.isNullOrEmpty()) {
//                Log.d("test44", ": ")
//                original.newBuilder()
//                    .header("Authorization","Bearer ${preferenceProvider.loginToken}")
//            }else {
//                original.newBuilder()
//            }
//            val request = requestBuilder.build()
//            chain.proceed(request)
//        }
        .build()

    var gson = GsonBuilder()
        .setLenient()
        .serializeNulls()
        .create()

    fun <Api> buildApi(api: Class<Api>):Api {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okhttp)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(api)
    }

    fun getRetrofitClient(baseURL:String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okhttp)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
}