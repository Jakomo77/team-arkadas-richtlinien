package com.jakob.glassescontrol.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meta.wearable.dat.camera.types.StreamState
import com.jakob.glassescontrol.ai.AiInsightState
import com.jakob.glassescontrol.ai.AiInsightViewModel
import com.jakob.glassescontrol.stream.StreamViewModel
import com.jakob.glassescontrol.wearables.WearablesViewModel

@Composable
fun StreamScreen(
    wearablesViewModel: WearablesViewModel,
    modifier: Modifier = Modifier,
    streamViewModel: StreamViewModel =
        viewModel(
            factory =
                StreamViewModel.Factory(
                    application = (LocalActivity.current as ComponentActivity).application,
                    wearablesViewModel = wearablesViewModel,
                ),
        ),
    aiInsightViewModel: AiInsightViewModel = viewModel(),
) {
  val streamUiState by streamViewModel.uiState.collectAsStateWithLifecycle()
  val aiState by aiInsightViewModel.state.collectAsStateWithLifecycle()

  LaunchedEffect(Unit) { streamViewModel.startStream() }

  Box(modifier = modifier.fillMaxSize()) {
    streamUiState.videoFrame?.let { frame ->
      key(streamUiState.videoFrameCount) {
        Image(
            bitmap = frame.asImageBitmap(),
            contentDescription = "Live-Bild der Brillenkamera",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
      }
    }
    if (streamUiState.streamState == StreamState.STARTING) {
      CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }

    Column(
        modifier =
            Modifier.align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      streamUiState.capturedPhoto?.let { photo ->
        Card(shape = RoundedCornerShape(16.dp)) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(onClick = { aiInsightViewModel.describePhoto(photo) }) { Text("KI beschreiben") }
              OutlinedButton(
                  onClick = {
                    streamViewModel.clearCapturedPhoto()
                    aiInsightViewModel.reset()
                  }
              ) {
                Text("Foto verwerfen")
              }
            }
            when (val insight = aiState) {
              is AiInsightState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(8.dp))
              is AiInsightState.Result -> Text(insight.description, modifier = Modifier.padding(top = 8.dp))
              is AiInsightState.Error ->
                  Text(
                      "Fehler: ${insight.message}",
                      color = MaterialTheme.colorScheme.error,
                      modifier = Modifier.padding(top = 8.dp),
                  )
              AiInsightState.Idle -> Unit
            }
          }
        }
      }

      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
            onClick = {
              streamViewModel.stopStream()
              wearablesViewModel.navigateToDeviceSelection()
            },
            modifier = Modifier.weight(1f),
        ) {
          Text("Stream stoppen")
        }
        Button(
            onClick = { streamViewModel.capturePhoto() },
            enabled = !streamUiState.isCapturing && streamUiState.streamState == StreamState.STREAMING,
            modifier = Modifier.weight(1f),
        ) {
          Text(if (streamUiState.isCapturing) "..." else "Foto aufnehmen")
        }
      }
    }
  }
}
