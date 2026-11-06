package com.fiesta.filevault

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class SupabaseCloud(context: Context) {
    private val prefs = context.getSharedPreferences("cloud_session", Context.MODE_PRIVATE)
    private val base = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val anon = BuildConfig.SUPABASE_ANON_KEY
    private val http = OkHttpClient.Builder().callTimeout(90, TimeUnit.SECONDS).build()
    val configured get() = base.startsWith("https://") && anon.isNotBlank() && !anon.contains("YOUR_")
    val signedIn get() = !prefs.getString("access_token", null).isNullOrBlank()
    private fun request(path: String, method: String, body: String? = null, token: String? = null): JSONObject {
        check(configured) { "Add SUPABASE_URL and SUPABASE_ANON_KEY to local.properties first." }
        val builder = Request.Builder().url(base + path).header("apikey", anon)
            .header("Content-Type", "application/json")
        if (token != null) builder.header("Authorization", "Bearer " + token)
        val rb = body?.toRequestBody("application/json".toMediaType())
        builder.method(method, if (method == "GET" || method == "DELETE") null else rb ?: ByteArray(0).toRequestBody(null))
        http.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) error(JSONObject(text.ifBlank { "{}" }).optString("msg").ifBlank { JSONObject(text.ifBlank { "{}" }).optString("message", "Cloud request failed: " + response.code) })
            return JSONObject(text.ifBlank { "{}" })
        }
    }
    fun signIn(email: String, password: String) {
        val result = request("/auth/v1/token?grant_type=password", "POST", JSONObject().put("email", email).put("password", password).toString())
        saveSession(result)
    }
    fun signUp(email: String, password: String) {
        val result = request("/auth/v1/signup", "POST", JSONObject().put("email", email).put("password", password).toString())
        if (result.has("access_token")) saveSession(result)
    }
    private fun saveSession(json: JSONObject) {
        prefs.edit().putString("access_token", json.optString("access_token"))
            .putString("user_id", json.optJSONObject("user")?.optString("id")).apply()
    }
    fun signOut() { prefs.edit().clear().apply() }
    fun upload(item: VaultItem) {
        val token = prefs.getString("access_token", null) ?: error("Sign in to cloud storage first.")
        val uid = prefs.getString("user_id", null) ?: error("Cloud user is missing.")
        val name = item.id + ".fv"
        val url = base + "/storage/v1/object/filevault/" + uid + "/" + name
        val body = item.encryptedFile.readBytes().toRequestBody("application/octet-stream".toMediaType())
        val req = Request.Builder().url(url).header("apikey", anon).header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/octet-stream").header("x-upsert", "true").post(body).build()
        http.newCall(req).execute().use { if (!it.isSuccessful) error("Cloud upload failed: " + it.code + " " + (it.body?.string().orEmpty())) }
    }
}