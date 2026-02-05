package com.personalutil.sms_sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class SmsReceiver : BroadcastReceiver() {
    private val client = OkHttpClient()

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SmsReceiver", "onReceive called with action: ${intent.action}")
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.d("SmsReceiver", "SMS_RECEIVED_ACTION matched!")
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val prefs = context.getSharedPreferences("Config", Context.MODE_PRIVATE)

            val serverUrl = prefs.getString("server_url", "") ?: ""
            val token = prefs.getString("auth_token", "") ?: ""

            if (serverUrl.isEmpty()) return

            for (msg in messages) {
                val sender = msg.displayOriginatingAddress
                val body = msg.displayMessageBody
                sendToServer(serverUrl, token, sender, body)
            }
        }
    }

    private fun sendToServer(url: String, token: String, sender: String, body: String) {
        val json = JSONObject().apply {
            put("from", sender)
            put("content", body)
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .addHeader("X-SMS-Token", token)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SmsReceiver", "Failed to send: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                response.close()
            }
        })
    }
}