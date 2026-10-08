package com.carvision.ai

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object CarAnalyzer {
    private val client = OkHttpClient()

    // Optional AI endpoint. Set these in local.properties and rebuild:
    // CARVISION_AI_URL=https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent
    // CARVISION_AI_KEY=YOUR_KEY
    //
    // For production, call your own backend instead of shipping a provider API key in the APK.
    private val aiUrl: String? = BuildConfig.CARVISION_AI_URL.takeIf { it.isNotBlank() }
    private val aiKey: String? = BuildConfig.CARVISION_AI_KEY.takeIf { it.isNotBlank() }

    suspend fun analyze(bitmap: Bitmap): CarResult {
        val carCheck = detectCar(bitmap)
        if (!carCheck) {
            return CarResult(false, "—", "—", "—", "Low", "No confident car label was found.")
        }

        if (aiUrl.isNullOrBlank() || aiKey.isNullOrBlank()) {
            return CarResult(
                true, "Not configured", "Not configured",
                estimateColor(bitmap), "On-device car check passed",
                "Add CARVISION_AI_URL and CARVISION_AI_KEY to local.properties for AI make/model recognition."
            )
        }

        return analyzeWithVision(bitmap)
    }

    private suspend fun detectCar(bitmap: Bitmap): Boolean =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
            labeler.process(image)
                .addOnSuccessListener { labels ->
                    val found = labels.any {
                        val t = it.text.lowercase()
                        (t == "car" || t == "automobile" || t == "vehicle") && it.confidence >= 0.55f
                    }
                    cont.resume(found)
                }
                .addOnFailureListener { cont.resume(false) }
        }

    private suspend fun analyzeWithVision(bitmap: Bitmap): CarResult =
        suspendCancellableCoroutine { cont ->
            try {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                val base64 = android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)

                val prompt = """
                    Analyze this image for a vehicle identification app.
                    Return ONLY valid JSON with these keys:
                    isCar (boolean), make (string), model (string), color (string),
                    confidence (string), notes (string).
                    If the exact make/model cannot be determined, say "Unknown" rather than inventing it.
                    Color should be a simple common color such as white, black, silver, gray, red, blue, green, yellow, brown, orange.
                """.trimIndent()

                val body = JSONObject()
                    .put("contents", JSONArray().put(
                        JSONObject().put("parts", JSONArray()
                            .put(JSONObject().put("text", prompt))
                            .put(JSONObject().put("inline_data",
                                JSONObject()
                                    .put("mime_type", "image/jpeg")
                                    .put("data", base64)
                            ))
                        )
                    )).toString()

                val request = Request.Builder()
                    .url(aiUrl!!)
                    .addHeader("x-goog-api-key", aiKey!!)
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).enqueue(object : okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                        cont.resumeWithException(e)
                    }
                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                        response.use {
                            if (!it.isSuccessful) {
                                cont.resumeWithException(
                                    IllegalStateException("AI service returned HTTP ${it.code}")
                                )
                                return
                            }
                            val raw = it.body?.string().orEmpty()
                            val text = JSONObject(raw)
                                .getJSONArray("candidates").getJSONObject(0)
                                .getJSONObject("content").getJSONArray("parts")
                                .getJSONObject(0).getString("text")
                                .replace("```json", "").replace("```", "").trim()
                            val j = JSONObject(text)
                            cont.resume(
                                CarResult(
                                    j.optBoolean("isCar", true),
                                    j.optString("make", "Unknown"),
                                    j.optString("model", "Unknown"),
                                    j.optString("color", "Unknown"),
                                    j.optString("confidence", "Unknown"),
                                    j.optString("notes", "")
                                )
                            )
                        }
                    }
                })
            } catch (t: Throwable) {
                cont.resumeWithException(t)
            }
        }

    private fun estimateColor(bitmap: Bitmap): String {
        // Lightweight fallback: average pixels and map to a coarse color.
        val scaled = Bitmap.createScaledBitmap(bitmap, 40, 40, true)
        var r = 0L; var g = 0L; var b = 0L; var n = 0L
        for (y in 0 until scaled.height) for (x in 0 until scaled.width) {
            val p = scaled.getPixel(x, y)
            r += android.graphics.Color.red(p)
            g += android.graphics.Color.green(p)
            b += android.graphics.Color.blue(p)
            n++
        }
        val rr = r.toFloat()/n; val gg = g.toFloat()/n; val bb = b.toFloat()/n
        val max = maxOf(rr, gg, bb); val min = minOf(rr, gg, bb)
        if (max < 55) return "Black"
        if (min > 205) return "White"
        if (max - min < 25) return if (max < 150) "Gray" else "Silver"
        return when {
            rr > gg * 1.25 && rr > bb * 1.25 -> "Red"
            bb > rr * 1.2 && bb > gg * 1.1 -> "Blue"
            gg > rr * 1.2 && gg > bb * 1.1 -> "Green"
            rr > 150 && gg > 90 && bb < 80 -> "Orange/Yellow"
            else -> "Unknown"
        }
    }
}
