package com.jakob.glassescontrol.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meta.wearable.dat.core.types.Permission
import com.meta.wearable.dat.core.types.PermissionStatus
import com.jakob.glassescontrol.wearables.WearablesViewModel
import kotlinx.coroutines.launch

@Composable
fun DeviceScreen(
    viewModel: WearablesViewModel,
    onRequestWearablesPermission: suspend (Permission) -> PermissionStatus,
    modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val activity = LocalActivity.current as ComponentActivity
  val scope = rememberCoroutineScope()

  Column(
      modifier = modifier.fillMaxSize().padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
  ) {
    Text(text = "Verbunden", style = MaterialTheme.typography.headlineMedium)
    Text(
        text =
            if (uiState.hasActiveDevice) "Brille erkannt und bereit."
            else "Warte auf Brille - stelle sicher, dass sie in Reichweite und gekoppelt ist.",
        style = MaterialTheme.typography.bodyMedium,
    )

    Button(
        enabled = uiState.hasActiveDevice,
        onClick = {
          scope.launch {
            val status = onRequestWearablesPermission(Permission.CAMERA)
            if (status == PermissionStatus.Granted) {
              viewModel.navigateToStreaming()
            } else {
              viewModel.setRecentError("Kamera-Berechtigung fuer die Brille wurde verweigert")
            }
          }
        },
    ) {
      Text("Kamera starten")
    }

    OutlinedButton(onClick = { viewModel.startUnregistration(activity) }) {
      Text("Brille trennen")
    }
  }
}
