package kaist.iclab.galaxyppglogger.data

import android.content.Context
import android.util.Log
import com.samsung.android.service.health.tracking.ConnectionListener
import com.samsung.android.service.health.tracking.HealthTrackerException
import com.samsung.android.service.health.tracking.HealthTrackingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class HealthTrackerService(
    private val androidContext: Context
) {
    private val TAG = javaClass.simpleName
    private val _connectionStateFlow = MutableStateFlow<ConnectionState>(ConnectionState.DISCONNECTED)
    val connectionStateFlow get() = _connectionStateFlow.asStateFlow()

    private val connectionListener: ConnectionListener = object : ConnectionListener {
        override fun onConnectionSuccess() {
            Log.d(TAG, "Connection Success")
            _connectionStateFlow.value = ConnectionState.CONNECTED
        }

        override fun onConnectionEnded() {
            Log.d(TAG, "Connection Ended")
            _connectionStateFlow.value = ConnectionState.DISCONNECTED
        }

        override fun onConnectionFailed(e: HealthTrackerException?) {
            Log.e(TAG, "Connection Failed: $e")
            _connectionStateFlow.value = ConnectionState.DISCONNECTED
        }
    }

    val healthTrackingService: HealthTrackingService by lazy {
        HealthTrackingService(connectionListener, androidContext)
    }

    fun start() {
        try {
            if (_connectionStateFlow.value != ConnectionState.CONNECTED) {
                healthTrackingService.connectService()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting service: $e")
        }
    }

    fun finish() {
        try {
            healthTrackingService.disconnectService()
            _connectionStateFlow.value = ConnectionState.DISCONNECTED
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting service: $e")
        }
    }
}