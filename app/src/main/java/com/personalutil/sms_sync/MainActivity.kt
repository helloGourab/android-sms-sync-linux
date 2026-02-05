package com.personalutil.sms_sync

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.personalutil.sms_sync.ui.theme.Sms_syncTheme
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Sms_syncTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val context = LocalContext.current
                    val prefs = remember { context.getSharedPreferences("Config", Context.MODE_PRIVATE) }
                    ConfigScreen(prefs)
                }
            }
        }
    }
}

@Composable
fun ConfigScreen(prefs: SharedPreferences? = null) {
    val context = LocalContext.current

    var serverUrl by remember {
        mutableStateOf(prefs?.getString("server_url", "http://gourab-Vostro-15-3515.local:5656/sms") ?: "http://gourab-Vostro-15-3515.local:5656/sms")
    }
    var authToken by remember {
        mutableStateOf(prefs?.getString("auth_token", "mysecret") ?: "mysecret")
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.RECEIVE_SMS] == true) {
            Toast.makeText(context, "Permissions Granted", Toast.LENGTH_SHORT).show()
        }
    }

    if (prefs != null) {
        LaunchedEffect(Unit) {
            launcher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS))
        }
    }

    Column(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Text("SMS Forwarder Config", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text("Server URL") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("http://192.168.x.x:5656/sms") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = authToken,
            onValueChange = { authToken = it },
            label = { Text("Security Token") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                prefs?.edit()?.apply {
                    putString("server_url", serverUrl)
                    putString("auth_token", authToken)
                    apply()
                }
                Toast.makeText(context, "Saved locally!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Configuration")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // THE PING BUTTON
        OutlinedButton(
            onClick = {
                if (serverUrl.isNotEmpty()) {
                    sendManualTest(context, serverUrl, authToken)
                } else {
                    Toast.makeText(context, "URL is empty!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send Test Ping")
        }
    }
}

// Function to manually test the connection
fun sendManualTest(context: Context, url: String, token: String) {
    val client = OkHttpClient()
    val json = JSONObject().apply {
        put("from", "TEST-PHONE")
        put("content", "Testing connection to Ubuntu!")
    }

    val requestBody = json.toString().toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
        .url(url)
        .addHeader("X-SMS-Token", token)
        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, "Ping Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        override fun onResponse(call: Call, response: Response) {
            val code = response.code
            (context as? Activity)?.runOnUiThread {
                if (response.isSuccessful) {
                    Toast.makeText(context, "Ping Success! (200 OK)", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Server Replied: $code", Toast.LENGTH_LONG).show()
                }
            }
            response.close()
        }
    })
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ConfigScreenPreview() {
    Sms_syncTheme {
        Surface {
            ConfigScreen(prefs = null)
        }
    }
}