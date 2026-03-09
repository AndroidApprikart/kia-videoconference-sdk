package com.app.vc.virtualchattoken

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.app.vc.databinding.ActivityLoginKiaKrystalBinding
import com.app.vc.utils.PreferenceManager
import com.app.vc.virtualroomlist.VirtualRoomListActivity

class LoginKiaKrystal : AppCompatActivity() {

    private lateinit var binding: ActivityLoginKiaKrystalBinding
    private val viewModel: LoginViewModel by viewModels()
    val TAG="LoginKiaKrystal"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is already logged in
        if (!PreferenceManager.getAccessToken().isNullOrEmpty()) {
            navigateToRoomList()
            return 
        }

        binding = ActivityLoginKiaKrystalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnLogin.isEnabled = !isLoading
        }

        viewModel.isVerified.observe(this) { isVerified ->
            if (isVerified) {
                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                navigateToRoomList()
            }
        }

        viewModel.errorMessage.observe(this) { message ->
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                Log.d(TAG, "setupObservers: $message")
            }
        }
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val uniqueId = binding.etPassword.text.toString().trim()
            val dealercode=binding.etdealercode.text.toString().trim()
            val role=binding.etrole.text.toString().trim()
            viewModel.login(username, uniqueId, dealer_code = dealercode, role = role)
        }
    }

    private fun navigateToRoomList() {
        // FIXED: Now navigating to the List screen instead of the Chat room directly
        startActivity(Intent(this, VirtualRoomListActivity::class.java))
        finish()
    }
}