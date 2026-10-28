package kaist.iclab.galaxyppglogger.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import kaist.iclab.galaxyppglogger.collector.CollectorController
import kaist.iclab.galaxyppglogger.collector.CollectorService
import kaist.iclab.galaxyppglogger.collector.PPG.PpgDao
import kaist.iclab.galaxyppglogger.data.ConnectionState
import kaist.iclab.galaxyppglogger.data.HealthTrackerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RealViewModelImpl(
    private val application: Application,
    private val collectorController: CollectorController,
    private val ppgDao: PpgDao,
    private val healthTrackerService: HealthTrackerService
) : AbstractViewModel() {

    private val TAG = javaClass.simpleName

    private val _serviceState = MutableStateFlow(ServiceState.DISCONNECTED)
    override val serviceState: StateFlow<ServiceState>
        get() = _serviceState.asStateFlow()

    private var serviceBound = false
    private var collectorServiceRunning = false

    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        viewModelScope.launch {
            healthTrackerService.connectionStateFlow.collect { connectionState ->
                updateServiceState(connectionState)
            }
        }
    }

    private fun updateServiceState(healthConnectionState: ConnectionState = healthTrackerService.connectionStateFlow.value) {
        if (healthConnectionState == ConnectionState.CONNECTED) {
            _serviceState.value = if (collectorServiceRunning) ServiceState.RUNNING else ServiceState.READY
        } else {
            _serviceState.value = ServiceState.DISCONNECTED
        }
    }

    override fun start() {
        Log.d(TAG, "start")
        collectorController.start()
        collectorServiceRunning = true
        updateServiceState()
    }

    override fun stop() {
        Log.d(TAG, "stop")
        collectorController.stop()
        collectorServiceRunning = false
        updateServiceState()
        unbindService()
    }

    override fun flush() {
        CoroutineScope(Dispatchers.IO).launch {
            ppgDao.deleteAll()
        }
        Log.d(TAG, "flush")
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as CollectorService.LocalBinder).getService()
            collectorServiceRunning = service.isRunning
            serviceBound = true
            updateServiceState()
            Log.d(TAG, "Service Bound")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
            collectorServiceRunning = false
            updateServiceState()
            Log.d(TAG, "Service Disconnected")
        }
    }

    override fun bindService() {
        val context = application.applicationContext
        val intent = Intent(context, CollectorService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun unbindService() {
        if (serviceBound) {
            application.applicationContext.unbindService(connection)
            serviceBound = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }
}
