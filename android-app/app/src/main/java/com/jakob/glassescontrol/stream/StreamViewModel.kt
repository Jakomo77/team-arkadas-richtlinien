package com.jakob.glassescontrol.stream

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meta.wearable.dat.camera.Stream
import com.meta.wearable.dat.camera.addStream
import com.meta.wearable.dat.camera.types.PhotoData
import com.meta.wearable.dat.camera.types.StreamConfiguration
import com.meta.wearable.dat.camera.types.StreamState
import com.meta.wearable.dat.camera.types.VideoFrame
import com.meta.wearable.dat.camera.types.VideoQuality
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.selectors.DeviceSelector
import com.meta.wearable.dat.core.session.DeviceSession
import com.meta.wearable.dat.core.session.DeviceSessionState
import com.jakob.glassescontrol.wearables.WearablesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StreamUiState(
    val streamState: StreamState = StreamState.CLOSED,
    val videoFrame: Bitmap? = null,
    val videoFrameCount: Int = 0,
    val isCapturing: Boolean = false,
    val capturedPhoto: Bitmap? = null,
)

/**
 * Owns the DAT camera session/stream lifecycle for one connected pair of glasses:
 * creating a [DeviceSession], attaching a video [Stream], forwarding decoded frames
 * to the UI, and triggering on-device photo capture.
 */
class StreamViewModel(
    application: Application,
    private val wearablesViewModel: WearablesViewModel,
) : AndroidViewModel(application) {

  companion object {
    private const val TAG = "GlassesControl:Stream"
  }

  private val deviceSelector: DeviceSelector = wearablesViewModel.deviceSelector
  private var session: DeviceSession? = null
  private var stream: Stream? = null
  private var videoJob: Job? = null
  private var sessionStateJob: Job? = null

  private val _uiState = MutableStateFlow(StreamUiState())
  val uiState: StateFlow<StreamUiState> = _uiState.asStateFlow()

  fun startStream() {
    if (session != null) return

    Wearables.createSession(deviceSelector)
        .onSuccess { createdSession ->
          session = createdSession
          session?.start()
          observeSession(createdSession)
        }
        .onFailure { error, _ ->
          Log.e(TAG, "Failed to create session: ${error.description}")
          wearablesViewModel.setRecentError(error.description)
        }
  }

  private fun observeSession(deviceSession: DeviceSession) {
    sessionStateJob =
        viewModelScope.launch {
          deviceSession.state.collect { state ->
            if (state == DeviceSessionState.STARTED && stream == null) {
              attachStream(deviceSession)
            }
          }
        }
  }

  private fun attachStream(deviceSession: DeviceSession) {
    deviceSession
        .addStream(StreamConfiguration(videoQuality = VideoQuality.MEDIUM, frameRate = 24))
        .onSuccess { addedStream ->
          stream = addedStream
          videoJob =
              viewModelScope.launch {
                addedStream.videoStream.collect { frame -> handleVideoFrame(frame) }
              }
          viewModelScope.launch {
            addedStream.state.collect { state ->
              _uiState.update { it.copy(streamState = state) }
              if (state == StreamState.CLOSED) {
                stopStream()
                wearablesViewModel.navigateToDeviceSelection()
              }
            }
          }
          addedStream.start()
        }
        .onFailure { error, _ ->
          Log.e(TAG, "Failed to add stream: ${error.description}")
          wearablesViewModel.setRecentError(error.description)
        }
  }

  fun stopStream() {
    videoJob?.cancel()
    sessionStateJob?.cancel()
    stream?.stop()
    stream = null
    session?.stop()
    session = null
    _uiState.update { StreamUiState() }
  }

  fun capturePhoto() {
    val currentStream = stream ?: return
    if (_uiState.value.isCapturing || _uiState.value.streamState != StreamState.STREAMING) return

    _uiState.update { it.copy(isCapturing = true) }
    viewModelScope.launch {
      currentStream
          .capturePhoto()
          .onSuccess { photo -> handlePhotoData(photo) }
          .onFailure { error, _ ->
            Log.e(TAG, "Photo capture failed: ${error.description}")
          }
      _uiState.update { it.copy(isCapturing = false) }
    }
  }

  fun clearCapturedPhoto() {
    _uiState.update { it.copy(capturedPhoto = null) }
  }

  private fun handleVideoFrame(frame: VideoFrame) {
    val bitmap = YuvToBitmapConverter.convert(frame.buffer, frame.width, frame.height) ?: return
    viewModelScope.launch(Dispatchers.Main) {
      _uiState.update { it.copy(videoFrame = bitmap, videoFrameCount = it.videoFrameCount + 1) }
    }
  }

  private fun handlePhotoData(photo: PhotoData) {
    val bitmap =
        when (photo) {
          is PhotoData.Bitmap -> photo.bitmap
          is PhotoData.HEIC -> {
            val bytes = ByteArray(photo.data.remaining())
            photo.data.get(bytes)
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
          }
        }
    _uiState.update { it.copy(capturedPhoto = bitmap) }
  }

  override fun onCleared() {
    super.onCleared()
    stopStream()
  }

  class Factory(
      private val application: Application,
      private val wearablesViewModel: WearablesViewModel,
  ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      @Suppress("UNCHECKED_CAST")
      return StreamViewModel(application, wearablesViewModel) as T
    }
  }
}
