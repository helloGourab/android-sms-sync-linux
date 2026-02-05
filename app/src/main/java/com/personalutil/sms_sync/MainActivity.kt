package com.personalutil.sms_sync

import android.Manifest
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

    // Fallback values for Preview mode where prefs is null
    var serverUrl by remember {
        mutableStateOf(prefs?.getString("server_url", "http://laptop.local:5656/sms") ?: "http://laptop.local:5656/sms")
    }
    var authToken by remember {
        mutableStateOf(prefs?.getString("auth_token", "mysecret") ?: "mysecret")
    }

    // Permission launcher (Only runs in real app, ignored in Preview)
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
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ConfigScreenPreview() {
    Sms_syncTheme {
        Surface {
            // Passing null prefs here so the preview uses default strings
            ConfigScreen(prefs = null)
        }
    }
}