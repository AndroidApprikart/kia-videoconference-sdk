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
import com.app.vc.utils.VCConstants.EXTRA_DEALER_CODE
import com.app.vc.utils.VCConstants.EXTRA_ROLE
import com.app.vc.utils.VCConstants.EXTRA_UNIQUE_ID
import com.app.vc.utils.VCConstants.EXTRA_USERNAME
import com.app.vc.websocketconnection.NotificationWebSocketManager
import com.app.vc.websocketconnection.SocketSessionCoordinator
import com.app.vc.virtualroomlist.VirtualRoomListActivity

class LoginKiaKrystal : AppCompatActivity() {

    private lateinit var binding: ActivityLoginKiaKrystalBinding
    private val viewModel: LoginViewModel by viewModels()
    val TAG="LoginKiaKrystal"
    private var autoLoginMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenceManager.init(applicationContext)

        // Check if user is already logged in
        if (!PreferenceManager.getAccessToken().isNullOrEmpty()) {
            navigateToRoomList()
            return 
        }

        binding = ActivityLoginKiaKrystalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupListeners()
        tryAutoLoginFromIntent()
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnLogin.isEnabled = !isLoading
        }

        viewModel.isVerified.observe(this) { isVerified ->
            if (isVerified) {
                if (!autoLoginMode) {
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                }
                navigateToRoomList()
            }
        }

        viewModel.errorMessage.observe(this) { message ->
            val normalized = message?.trim().orEmpty()
            if (normalized.isNotEmpty() && !normalized.equals("null", ignoreCase = true) && !normalized.equals("Error: null", ignoreCase = true)) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                Log.d(TAG, "setupObservers: $message")
            }
            if (autoLoginMode) {
                showManualLoginUi()
            }
        }

        viewModel.sessionExpired.observe(this) { expired ->
            if (expired == true) {
                SocketSessionCoordinator.getInstance().clearSession()
                Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show()
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

    private fun tryAutoLoginFromIntent() {
        val username = intent.getStringExtra(EXTRA_USERNAME)?.trim().orEmpty()
        val uniqueId = intent.getStringExtra(EXTRA_UNIQUE_ID)?.trim().orEmpty()
        val role = intent.getStringExtra(EXTRA_ROLE)?.trim().orEmpty()
        val dealerCode = intent.getStringExtra(EXTRA_DEALER_CODE)?.trim().orEmpty()

        if (username.isBlank() || uniqueId.isBlank() || role.isBlank() || dealerCode.isBlank()) {
            return
        }

        autoLoginMode = true
        showLoadingOnlyUi()
        viewModel.login(username, uniqueId, dealer_code = dealerCode, role = role)
    }

    private fun showLoadingOnlyUi() {
        binding.tvTitle.visibility = View.GONE
        binding.tilUsername.visibility = View.GONE
        binding.tilPassword.visibility = View.GONE
        binding.tilrole.visibility = View.GONE
        binding.tildealercode.visibility = View.GONE
        binding.btnLogin.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun showManualLoginUi() {
        autoLoginMode = false
        binding.tvTitle.visibility = View.VISIBLE
        binding.tilUsername.visibility = View.VISIBLE
        binding.tilPassword.visibility = View.VISIBLE
        binding.tilrole.visibility = View.VISIBLE
        binding.tildealercode.visibility = View.VISIBLE
        binding.btnLogin.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
    }

    private fun navigateToRoomList() {
        NotificationWebSocketManager.getInstance().connectWithToken(PreferenceManager.getAccessToken())
        // FIXED: Now navigating to the List screen instead of the Chat room directly
        startActivity(Intent(this, VirtualRoomListActivity::class.java))
        finish()
    }


}