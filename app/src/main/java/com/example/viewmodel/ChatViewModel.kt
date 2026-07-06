package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.Content
import com.example.data.GenerateContentRequest
import com.example.data.Part
import com.example.data.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val role: String,
    val text: String
)

class ChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        val userMessage = ChatMessage(role = "user", text = text)
        _messages.value = _messages.value + userMessage
        _isLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                
                val contents = _messages.value.map { msg ->
                    Content(role = msg.role, parts = listOf(Part(text = msg.text)))
                }

                val request = GenerateContentRequest(
                    contents = contents,
                    systemInstruction = Content(
                        role = "system",
                        parts = listOf(Part(text = "Sen bir sağlık asistanısın. Adın 'Geminal'. Kullanıcının 'Neyim var?' veya benzeri sağlık şikayetleri ile ilgili sorularına, nazikçe, anlaşılır ve empatik bir dille cevap ver. Tıbbi tavsiye vermek yerine, olası durumlar hakkında bilgi ver ve her zaman bir doktora görünmeleri gerektiğini hatırlat."))
                    )
                )

                val response = RetrofitClient.service.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                
                if (responseText != null) {
                    val botMessage = ChatMessage(role = "model", text = responseText)
                    _messages.value = _messages.value + botMessage
                } else {
                    val errorMessage = ChatMessage(role = "model", text = "Cevap alınamadı.")
                    _messages.value = _messages.value + errorMessage
                }

            } catch (e: Exception) {
                val errorMessage = ChatMessage(role = "model", text = "Bir hata oluştu: ${e.message}")
                _messages.value = _messages.value + errorMessage
            } finally {
                _isLoading.value = false
            }
        }
    }
}
