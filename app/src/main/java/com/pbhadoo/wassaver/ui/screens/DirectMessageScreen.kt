package com.pbhadoo.wassaver.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectMessageScreen(navController: NavController) {
    val context = LocalContext.current
    var countryCode by remember { mutableStateOf("+91") }
    var phoneNumber by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var useWAB by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Direct Message", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Send without saving contact",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = countryCode,
                            onValueChange = { if (it.length <= 5) countryCode = it },
                            label = { Text("Code") },
                            modifier = Modifier.width(90.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = {
                                phoneNumber = it.filter { c -> c.isDigit() }
                                errorMsg = null
                            },
                            label = { Text("Phone Number") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            isError = errorMsg != null,
                            supportingText = errorMsg?.let { { Text(it) } },
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Message (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6
                    )

                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("Use WhatsApp Business", modifier = Modifier.weight(1f))
                        Switch(checked = useWAB, onCheckedChange = { useWAB = it })
                    }

                    val fullNumber = "${countryCode.removePrefix("+")}${phoneNumber}"

                    Button(
                        onClick = {
                            if (phoneNumber.length < 7) {
                                errorMsg = "Enter a valid phone number"
                                return@Button
                            }
                            val pkg = if (useWAB) "com.whatsapp.w4b" else "com.whatsapp"
                            val url = "https://wa.me/$fullNumber" +
                                    if (message.isNotBlank()) "?text=${Uri.encode(message)}" else ""
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                setPackage(pkg)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                errorMsg = "${if (useWAB) "WhatsApp Business" else "WhatsApp"} not installed"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Whatsapp, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open in ${if (useWAB) "WA Business" else "WhatsApp"}")
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(modifier = Modifier.padding(12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "The contact won't be saved to your phonebook.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}
