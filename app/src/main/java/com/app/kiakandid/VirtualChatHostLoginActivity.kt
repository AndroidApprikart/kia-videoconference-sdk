package com.app.kiakandid

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.app.vc.utils.VCConstants.EXTRA_DEALER_CODE
import com.app.vc.utils.VCConstants.EXTRA_ROLE
import com.app.vc.utils.VCConstants.EXTRA_UNIQUE_ID
import com.app.vc.utils.VCConstants.EXTRA_USERNAME
import com.app.vc.virtualchattoken.LoginKiaKrystal
import com.app.vc.virtualchattoken.SdkAuthGateActivity

class VirtualChatHostLoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_virtual_chat_host_login)

        val etUsername = findViewById<EditText>(R.id.etHostUsername)
        val etUniqueId = findViewById<EditText>(R.id.etHostUniqueId)
        val etRole = findViewById<EditText>(R.id.etHostRole)
        val etDealerCode = findViewById<EditText>(R.id.etHostDealerCode)
        val btnLogin = findViewById<Button>(R.id.btnHostLoginToSdk)

        btnLogin.setOnClickListener {
            val username = etUsername.text?.toString()?.trim().orEmpty()
            val uniqueId = etUniqueId.text?.toString()?.trim().orEmpty()
            val role = etRole.text?.toString()?.trim().orEmpty()
            val dealerCode = etDealerCode.text?.toString()?.trim().orEmpty()

            if (username.isBlank() || uniqueId.isBlank() || role.isBlank() || dealerCode.isBlank()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            startActivity(
                Intent(this, SdkAuthGateActivity::class.java).apply {
                    putExtra(EXTRA_USERNAME, username)
                    putExtra(EXTRA_UNIQUE_ID, uniqueId)
                    putExtra(EXTRA_ROLE, role)
                    putExtra(EXTRA_DEALER_CODE, dealerCode)
                }
            )
        }
    }
}
