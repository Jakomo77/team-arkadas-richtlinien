package com.jakob.glassescontrol.ai

import android.app.Application
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jakob.glassescontrol.BuildConfig
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Note on scope: the DAT SDK does not expose any API to talk to, or override,
 * the glasses' built-in Meta AI assistant - that assistant is closed. What this
 * *does* do is take a photo captured from the glasses camera (via [com.jakob.glassescontrol.stream.StreamViewModel])
 * and ask Claude to describe it, giving a custom "what am I looking at" feature
 * built on top of the camera access the SDK does grant.
 */
sealed interface AiInsightState {
  data object Idle : AiInsightState

  data object Loading : AiInsightState

  data class Result(val description: String) : AiInsightState

  data class Error(val message: String) : AiInsightState
}

class AiInsightViewModel(application: Application) : AndroidViewModel(application) {
  private val _state = MutableStateFlow<AiInsightState>(AiInsightState.Idle)
  val state: StateFlow<AiInsightState> = _state.asStateFlow()

  private val client = OkHttpClient()
  private val json =
      Json {
        ignoreUnknownKeys = true
        namingStrategy = JsonNamingStrategy.SnakeCase
      }

  fun describePhoto(photo: Bitmap) {
    val apiKey = BuildConfig.ANTHROPIC_API_KEY
    if (apiKey.isBlank()) {
      _state.update {
        AiInsightState.Error("Kein Anthropic API-Key konfiguriert (local.properties).")
      }
      return
    }

    _state.update { AiInsightState.Loading }
    viewModelScope.launch {
      val result = runCatching { withContext(Dispatchers.IO) { requestDescription(photo, apiKey) } }
      result
          .onSuccess { description -> _state.update { AiInsightState.Result(description) } }
          .onFailure { error ->
            Log.e("AiInsightViewModel", "Vision request failed", error)
            _state.update { AiInsightState.Error(error.message ?: "Unbekannter Fehler") }
          }
    }
  }

  fun reset() {
    _state.update { AiInsightState.Idle }
  }

  private fun requestDescription(photo: Bitmap, apiKey: String): String {
    val base64Image = encodeJpeg(photo)
    val requestJson = json.encodeToString(AnthropicRequest.serializer(), buildRequest(base64Image))

    val request =
        Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .build()

    client.newCall(request).execute().use { response ->
      val body = response.body?.string().orEmpty()
      if (!response.isSuccessful) {
        throw IOException("Anthropic API error ${response.code}: $body")
      }
      val parsed = json.decodeFromString(AnthropicResponse.serializer(), body)
      return parsed.content.firstOrNull { it.type == "text" }?.text
          ?: "Keine Beschreibung erhalten."
    }
  }

  private fun encodeJpeg(bitmap: Bitmap): String {
    val output = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
    return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
  }

  private fun buildRequest(base64Image: String) =
      AnthropicRequest(
          model = "claude-sonnet-5",
          maxTokens = 300,
          messages =
              listOf(
                  AnthropicMessage(
                      role = "user",
                      content =
                          listOf(
                              AnthropicContent.Image(
                                  source = AnthropicImageSource(data = base64Image)
                              ),
                              AnthropicContent.Text(
                                  text =
                                      "Beschreibe kurz und praegnant auf Deutsch, was auf diesem Foto von der Brillenkamera zu sehen ist."
                              ),
                          ),
                  )
              ),
      )
}

@Serializable
private data class AnthropicRequest(
    val model: String,
    val maxTokens: Int,
    val messages: List<AnthropicMessage>,
)

@Serializable private data class AnthropicMessage(val role: String, val content: List<AnthropicContent>)

@Serializable
private sealed interface AnthropicContent {
  @Serializable
  @SerialName("text")
  data class Text(val text: String) : AnthropicContent

  @Serializable
  @SerialName("image")
  data class Image(val source: AnthropicImageSource) : AnthropicContent
}

@Serializable
private data class AnthropicImageSource(
    val type: String = "base64",
    val mediaType: String = "image/jpeg",
    val data: String,
)

@Serializable
private data class AnthropicResponse(val content: List<AnthropicResponseContent>)

@Serializable
private data class AnthropicResponseContent(val type: String, val text: String? = null)
