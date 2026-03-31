package com.app.vc.virtualchattoken

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.app.vc.R
import com.app.vc.utils.PreferenceManager
import com.app.vc.utils.VCConstants.EXTRA_DEALER_CODE
import com.app.vc.utils.VCConstants.EXTRA_ROLE
import com.app.vc.utils.VCConstants.EXTRA_UNIQUE_ID
import com.app.vc.utils.VCConstants.EXTRA_USERNAME
import com.app.vc.virtualroomlist.VirtualRoomListActivity
import com.app.vc.websocketconnection.NotificationWebSocketManager
import com.app.vc.websocketconnection.SocketSessionCoordinator

class SdkAuthGateActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels()
    private lateinit var progressBar: ProgressBar
    private var hasTerminalNavigation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenceManager.init(applicationContext)
        setContentView(R.layout.activity_sdk_auth_gate)
        progressBar = findViewById(R.id.progressAuthGate)

        if (!PreferenceManager.getAccessToken().isNullOrEmpty()) {
            navigateToRoomList()
            return
        }

        setupObservers()
        startLoginFromIntent()
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.isVerified.observe(this) { isVerified ->
            if (isVerified == true) {
                navigateToRoomList()
            }
        }

        viewModel.errorMessage.observe(this) { message ->
            if (hasTerminalNavigation || !PreferenceManager.getAccessToken().isNullOrEmpty()) {
                return@observe
            }
            val normalized = message?.trim().orEmpty()
            val hasRealError = normalized.isNotEmpty() &&
                !normalized.equals("null", ignoreCase = true) &&
                !normalized.equals("Error: null", ignoreCase = true)
            if (hasRealError) {
                Toast.makeText(this, normalized, Toast.LENGTH_LONG).show()
                openManualLoginScreen()
            }
        }

        viewModel.sessionExpired.observe(this) { expired ->
            if (expired == true) {
                SocketSessionCoordinator.getInstance().clearSession()
                openManualLoginScreen()
            }
        }
    }

    private fun startLoginFromIntent() {
        val username = intent.getStringExtra(EXTRA_USERNAME)?.trim().orEmpty()
        val uniqueId = intent.getStringExtra(EXTRA_UNIQUE_ID)?.trim().orEmpty()
        val role = intent.getStringExtra(EXTRA_ROLE)?.trim().orEmpty()
        val dealerCode = intent.getStringExtra(EXTRA_DEALER_CODE)?.trim().orEmpty()

        Log.d("SdkAuthGateActivity", "startLoginFromIntent: $username, $uniqueId, $role, $dealerCode")

        if (username.isBlank() || uniqueId.isBlank() || role.isBlank() || dealerCode.isBlank()) {
            openManualLoginScreen()
            return
        }

        viewModel.login(username, uniqueId, role = role, dealer_code = dealerCode)
    }

    private fun openManualLoginScreen() {
        if (hasTerminalNavigation) return
        hasTerminalNavigation = true
        startActivity(
            Intent(this, LoginKiaKrystal::class.java).apply {
                putExtras(intent)
            }
        )
        finish()
    }

    private fun navigateToRoomList() {
        if (hasTerminalNavigation) return
        hasTerminalNavigation = true
        NotificationWebSocketManager.getInstance().connectWithToken(PreferenceManager.getAccessToken())
        startActivity(Intent(this, VirtualRoomListActivity::class.java))
        finish()
    }
}
