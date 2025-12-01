package com.towhid.myapplication

import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val receivedMacAddress by savedStateHandle
        ?.getStateFlow<String?>("selected_mac", null)
        ?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(null) }

    LaunchedEffect(receivedMacAddress) {
        receivedMacAddress?.let { mac ->
            Log.d("HomeScreen", "Received MAC: $mac")
            viewModel.setMacAddress(mac)
            savedStateHandle?.set("selected_mac", null)
        }
    }

    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val macAddress = viewModel.selectedMacAddress

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    viewModel.printLargeBitmap(bitmap)
                } else {
                    Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text("POS Printer", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(32.dp))

        // CONNECTION STATUS CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (connectionState) {
                    is ConnectionState.Connected -> MaterialTheme.colorScheme.primaryContainer
                    is ConnectionState.Connecting -> MaterialTheme.colorScheme.secondaryContainer
                    is ConnectionState.Failed -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Status", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (connectionState) {
                        is ConnectionState.Connecting -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp), strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Connecting...")
                        }
                        is ConnectionState.Connected -> {
                            Text("✓ Connected", style = MaterialTheme.typography.bodyLarge)
                        }
                        is ConnectionState.Failed -> {
                            Text(
                                "✗ Failed: ${(connectionState as ConnectionState.Failed).error}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        else -> {
                            Text("Disconnected", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // SELECTED DEVICE CARD for mac id show
        if (macAddress != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Selected Device", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(macAddress, style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No device selected")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // SELECT DEVICE BUTTON
        Button(
            onClick = { navController.navigate("bluetooth") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (macAddress == null) "Select Bluetooth Device" else "Change Device")
        }

        // CONNECT / DISCONNECT BUTTON
        if (macAddress != null) {
            Spacer(Modifier.height(8.dp))

            when (connectionState) {
                is ConnectionState.Connecting -> {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Connecting…")
                    }
                }
                is ConnectionState.Connected -> {
                    OutlinedButton(
                        onClick = { viewModel.disconnectPrinter() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Disconnect")
                    }
                }
                else -> {
                    Button(
                        onClick = { viewModel.connectToPrinter(macAddress) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Connect")
                    }
                }
            }
        }

        // PRINT BUTTONS (Only when connected)
        if (connectionState is ConnectionState.Connected) {
            Spacer(Modifier.height(16.dp))

            // Simple Test Print
            Button(
                onClick = { viewModel.printTestReceipt() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text("Print Simple Test")
            }

            Spacer(Modifier.height(8.dp))

            // Print with Logo
            Button(
                onClick = { viewModel.printReceiptWithLogo() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("📄 Print Receipt with Logo")
            }

            Spacer(Modifier.height(8.dp))

            // Print Image from URL
            Button(
                onClick = {
                    // Example URLs - replace with your own
                    viewModel.printImageFromUrl("https://picsum.photos/800/600")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("🌐 Print Image from URL")
            }

            Spacer(Modifier.height(8.dp))

            // Print Large Image from Gallery
            Button(
                onClick = {
                    imagePickerLauncher.launch("image/*")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("🖼️ Print Large Image (5000px)")
            }

            Spacer(Modifier.height(8.dp))

            // Print Product with URL Image
            Button(
                onClick = {
                    viewModel.printProductReceipt(
                        productName = "Samsung Galaxy Phone",
                        productPrice = 999.99,
                        productImageUrl = "https://picsum.photos/400/400"
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("📦 Print Product (URL Image)")
            }

            Spacer(Modifier.height(8.dp))

            // Print QR Code
            Button(
                onClick = { viewModel.printQRCode("https://yourstore.com") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text("🔲 Print QR Code")
            }

            Spacer(Modifier.height(8.dp))

            // Print Barcode
            Button(
                onClick = { viewModel.printBarcode("1234567890128") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("📊 Print Barcode")
            }
        }
    }
}
