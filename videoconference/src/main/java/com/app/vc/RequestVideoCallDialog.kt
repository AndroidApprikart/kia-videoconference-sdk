package com.app.vc

import android.app.Dialog
import android.content.Context
import android.os.Bundle

import android.view.ViewGroup
import com.app.vc.databinding.DialogRequestVideoCallBinding


class RequestVideoCallDialog(
    context: Context) : Dialog(context)  {

    private lateinit var binding: DialogRequestVideoCallBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DialogRequestVideoCallBinding.inflate(layoutInflater)

        setContentView(binding.root)

        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )


    }
}