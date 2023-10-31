package com.app.kiakandid

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.app.vc.VCDynamicActivity4
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var btn = findViewById<MaterialButton>(R.id.btn_vc) as MaterialButton
        btn.setOnClickListener {
            startActivity(Intent(this,VCDynamicActivity4::class.java))
        }
    }
}