package com.invatech.pockit.data.update

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Production-ready In-App Update Manager using Google Play Core API.
 * Seamlessly manages update availability, flexible background download,
 * and completion prompts.
 */
class InAppUpdateManager(private val context: Context) {

    private val appUpdateManager: AppUpdateManager by lazy {
        AppUpdateManagerFactory.create(context)
    }

    private val _isUpdateDownloaded = MutableStateFlow(false)
    val isUpdateDownloaded: StateFlow<Boolean> = _isUpdateDownloaded.asStateFlow()

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            _isUpdateDownloaded.value = true
        }
    }

    init {
        try {
            appUpdateManager.registerListener(installStateUpdatedListener)
        } catch (_: Exception) {}
    }

    fun checkForUpdate(
        activity: Activity,
        updateLauncher: ActivityResultLauncher<IntentSenderRequest>,
        forceImmediate: Boolean = false
    ) {
        try {
            val appUpdateInfoTask = appUpdateManager.appUpdateInfo
            appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    val updateType = if (forceImmediate || appUpdateInfo.updatePriority() >= 4) {
                        AppUpdateType.IMMEDIATE
                    } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        AppUpdateType.FLEXIBLE
                    } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        AppUpdateType.IMMEDIATE
                    } else {
                        null
                    }

                    updateType?.let { type ->
                        startUpdateFlow(appUpdateInfo, type, activity, updateLauncher)
                    }
                } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    _isUpdateDownloaded.value = true
                }
            }
        } catch (_: Exception) {
            // Fails gracefully in debug builds or on devices without Google Play Store
        }
    }

    private fun startUpdateFlow(
        appUpdateInfo: AppUpdateInfo,
        updateType: Int,
        activity: Activity,
        updateLauncher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        try {
            val options = AppUpdateOptions.newBuilder(updateType).build()
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activity,
                options,
                1001
            )
        } catch (_: Exception) {
            // Alternative via ActivityResultLauncher if intent sender is used
            runCatching {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    updateLauncher,
                    AppUpdateOptions.newBuilder(updateType).build()
                )
            }
        }
    }

    fun onResumeCheck(activity: Activity) {
        try {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    _isUpdateDownloaded.value = true
                }
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    // Resume immediate update
                    runCatching {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            activity,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                            1001
                        )
                    }
                }
            }
        } catch (_: Exception) {}
    }

    fun completeUpdate() {
        try {
            appUpdateManager.completeUpdate()
        } catch (_: Exception) {}
    }

    fun onDestroy() {
        try {
            appUpdateManager.unregisterListener(installStateUpdatedListener)
        } catch (_: Exception) {}
    }
}
