package com.app.vc.baseui

import android.app.Dialog
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment

/** Created by Naghma on 18/04/23  */


open class BaseFragment : Fragment() {
    open val TAG = "BaseFragment::"

    //handler
    var progressDialog: Dialog? = null
    var majorProgressDialog: Dialog? = null
    val baseHandler = Handler(Looper.getMainLooper())
    var runnable = Runnable { /*Set runnable and use its handler in entire project*/ }

    fun showProgressDialog() {
        if (progressDialog?.isShowing?.not() == true) {
            progressDialog?.show()
        }
    }

    fun dismissProgressDialog() {
        if (progressDialog?.isShowing == true) {
            progressDialog?.dismiss()
        }
    }

    override fun onPause() {
        if (progressDialog?.isShowing == true) {
            progressDialog?.dismiss()
        }
        if (majorProgressDialog?.isShowing == true) {
            majorProgressDialog?.dismiss()
        }
        super.onPause()
    }

    override fun onDestroy() {
        if (progressDialog?.isShowing == true) {
            progressDialog?.dismiss()
        }
        if (majorProgressDialog?.isShowing == true) {
            majorProgressDialog?.dismiss()
        }
        super.onDestroy()
    }

//    fun showCustomDialog(context: Context, message: String, onOkClick: () -> Unit, onCancelClick: () -> Unit) {
//        val builder = AlertDialog.Builder(context)
//        builder.setMessage(message)
//        builder.setPositiveButton("OK") { dialog, _ ->
//            dialog.dismiss()
//            onOkClick()
//        }
////        builder.setNegativeButton("Cancel") { dialog, _ ->
////            dialog.dismiss()
////            onCancelClick()
////
////        }
//        builder.setCancelable(false)
//        builder.show()
//    }


    fun showMajorProgressDialog() {
        if (majorProgressDialog?.isShowing?.not() == true) {
            majorProgressDialog?.show()
        }
    }

    fun dismissMajorProgressDialog() {
        if (majorProgressDialog?.isShowing == true) {
            majorProgressDialog?.dismiss()
        }
    }


}