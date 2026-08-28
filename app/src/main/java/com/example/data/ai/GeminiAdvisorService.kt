package com.example.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiAdvisorService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun queryGeminiWithBusinessContext(
        userQuestion: String,
        businessContextSummary: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = System.getenv("GEMINI_API_KEY") ?: ""

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("No API key available."))
        }

        try {
            val systemPrompt = """
                Eres el Asesor Inteligente de 'Stock AI', un sistema de gestión para pequeños negocios y comercios locales (panaderías, kioscos, restaurantes, almacenes).
                Tu objetivo es dar respuestas breves, claras, prácticas y en español amigable a los comerciantes.
                
                REGLAS CRÍTICAS:
                1. Basa tus respuestas ÚNICAMENTE en los datos reales del negocio que te son proporcionados en el contexto.
                2. NUNCA inventes números de ventas, precios, ganancias o productos que no estén en el contexto.
                3. Si la información solicitada no existe en el contexto, dilo claramente: "No dispongo de suficientes datos registrados para responder eso con certeza".
                4. Sé conciso, motivador y usa viñetas claras con formato legible.
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                val contentsArr = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArr = JSONArray().apply {
                            val promptText = "Contexto real del negocio:\n$businessContextSummary\n\nPregunta del comerciante:\n$userQuestion"
                            put(JSONObject().put("text", promptText))
                        }
                        put("parts", partsArr)
                    }
                    put(contentObj)
                }
                put("contents", contentsArr)

                val systemInstructionObj = JSONObject().apply {
                    val partsArr = JSONArray().apply {
                        put(JSONObject().put("text", systemPrompt))
                    }
                    put("parts", partsArr)
                }
                put("systemInstruction", systemInstructionObj)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error ${response.code}"))
            }

            val responseStr = response.body?.string() ?: ""
            val respJson = JSONObject(responseStr)
            val candidates = respJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val text = parts?.getJSONObject(0)?.optString("text")
                if (!text.isNullOrBlank()) {
                    return@withContext Result.success(text)
                }
            }
            Result.failure(Exception("Respuesta vacía"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
