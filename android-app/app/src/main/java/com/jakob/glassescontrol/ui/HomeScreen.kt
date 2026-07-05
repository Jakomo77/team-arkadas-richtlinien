package com.jakob.glassescontrol.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jakob.glassescontrol.wearables.WearablesViewModel

@Composable
fun HomeScreen(viewModel: WearablesViewModel, modifier: Modifier = Modifier) {
  val activity = LocalActivity.current as ComponentActivity

  Column(
      modifier = modifier.fillMaxSize().padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
  ) {
    Text(
        text = "Brillensteuerung",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
    )
    Text(
        text =
            "Verbinde dich mit deiner Meta AI Brille, um die Kamera zu steuern und Fotos " +
                "per KI beschreiben zu lassen. Stelle sicher, dass 'Entwicklungsmodus' in der " +
                "Meta AI App aktiviert ist.",
        style = MaterialTheme.typography.bodyMedium,
    )
    Button(onClick = { viewModel.startRegistration(activity) }) { Text("Mit Brille verbinden") }
  }
}
