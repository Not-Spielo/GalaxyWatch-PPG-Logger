package kaist.iclab.galaxyppglogger.collector

import android.util.Log
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.HealthTracker.TrackerEventListener
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import com.samsung.android.service.health.tracking.data.PpgType
import kaist.iclab.galaxyppglogger.data.HealthTrackerService

abstract class AbstractCollector(
    private val healthTrackerService: HealthTrackerService,
    private val healthTrackerType: HealthTrackerType,
    private val setPpgTypes: Set<PpgType>? = null
) {
    private val TAG = javaClass.simpleName
    private var healthTracker: HealthTracker? = null

    private val healthTrackerEventListener: TrackerEventListener =
        object : TrackerEventListener {
            override fun onError(trackerError: HealthTracker.TrackerError) {
                Log.d(TAG, "onError")
                when (trackerError) {
                    HealthTracker.TrackerError.PERMISSION_ERROR -> Log.e(
                        TAG,
                        "ERROR: Permission Failed"
                    )

                    HealthTracker.TrackerError.SDK_POLICY_ERROR -> Log.e(
                        TAG,
                        "ERROR: SDK Policy Error"
                    )

                    else -> Log.e(TAG, "ERROR: Unknown ${trackerError.name}")
                }
            }

            override fun onDataReceived(p0: MutableList<DataPoint>) {
                onData(p0)
            }

            override fun onFlushCompleted() {
                Log.d(TAG, "onFlushCompleted")
            }
        }

    abstract fun onData(dataPoints: MutableList<DataPoint>)

    fun start() {
        try {
            if (healthTracker == null) {
                healthTracker = if (setPpgTypes != null) {
                    healthTrackerService.healthTrackingService.getHealthTracker(healthTrackerType, setPpgTypes)
                } else {
                    healthTrackerService.healthTrackingService.getHealthTracker(healthTrackerType)
                }
            }
            healthTracker?.setEventListener(healthTrackerEventListener)
        } catch (e: Exception) {
            Log.e(TAG, "START error: $e", e)
        }
    }

    fun stop() {
        try {
            healthTracker?.unsetEventListener()
            healthTracker = null
        } catch (e: Exception) {
            Log.e(TAG, "STOP error: $e", e)
        }
    }
}