package com.app.vc.network

import com.app.vc.network.Status

/* created by Naghma 15/06/23*/


data class Resource<out T>(val status: Status, val data: T?, val message: String?) {
    companion object {
        fun <T> success(data: T): Resource<T & Any> = Resource(status = Status.SUCCESS, data = data, message = null)

        fun <T> error(data: T?, message: String): Resource<Nothing> =
            Resource(status = Status.ERROR, data = null, message = message)

        fun <T> loading(): Resource<T> = Resource(status = Status.LOADING, data = null, message = null)
    }
}