package com.jakob.glassescontrol.wearables

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.selectors.AutoDeviceSelector
import com.meta.wearable.dat.core.selectors.DeviceSelector
import com.meta.wearable.dat.core.types.DeviceIdentifier
import com.meta.wearable.dat.core.types.RegistrationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WearablesUiState(
    val canRegister: Boolean = false,
    val registrationState: RegistrationState = RegistrationState.AVAILABLE,
    val devices: List<DeviceIdentifier> = emptyList(),
    val hasActiveDevice: Boolean = false,
    val isStreaming: Boolean = false,
    val recentError: String? = null,
) {
  val isRegistered: Boolean
    get() = registrationState == RegistrationState.REGISTERED
}

/**
 * Wraps the app-level DAT state: registration with the Meta AI app,
 * device discovery, and the currently selected wearable device.
 */
class WearablesViewModel(application: Application) : AndroidViewModel(application) {
  private val _uiState = MutableStateFlow(WearablesUiState())
  val uiState: StateFlow<WearablesUiState> = _uiState.asStateFlow()

  // Picks the first available paired glasses automatically.
  val deviceSelector: DeviceSelector by lazy { AutoDeviceSelector() }

  private var monitoringStarted = false

  private fun startMonitoring() {
    if (monitoringStarted) return
    monitoringStarted = true

    viewModelScope.launch {
      deviceSelector.activeDeviceFlow().collect { device ->
        _uiState.update { it.copy(hasActiveDevice = device != null) }
      }
    }
    viewModelScope.launch {
      Wearables.registrationState.collect { value ->
        _uiState.update { it.copy(registrationState = value) }
      }
    }
    viewModelScope.launch {
      Wearables.devices.collect { value -> _uiState.update { it.copy(devices = value.toList()) } }
    }
  }

  fun onPermissionsResult(permissionsResult: Map<String, Boolean>, onAllGranted: () -> Unit) {
    val granted = permissionsResult.values.all { it }
    _uiState.update { it.copy(canRegister = granted) }
    if (granted) {
      onAllGranted()
      startMonitoring()
    } else {
      _uiState.update {
        it.copy(recentError = "Bitte alle Berechtigungen erlauben (Bluetooth, Kamera, Internet)")
      }
    }
  }

  fun startRegistration(activity: Activity) = Wearables.startRegistration(activity)

  fun startUnregistration(activity: Activity) = Wearables.startUnregistration(activity)

  fun navigateToStreaming() {
    _uiState.update { it.copy(isStreaming = true) }
  }

  fun navigateToDeviceSelection() {
    _uiState.update { it.copy(isStreaming = false) }
  }

  fun setRecentError(error: String) {
    _uiState.update { it.copy(recentError = error) }
  }

  fun clearRecentError() {
    _uiState.update { it.copy(recentError = null) }
  }
}
