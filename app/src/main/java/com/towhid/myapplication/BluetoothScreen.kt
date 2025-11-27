package com.towhid.myapplication

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BluetoothScreen(
    viewModel: BluetoothViewModel = hiltViewModel(),
    navController: NavController,
    //onDeviceSelected: (String) -> Unit = {}
) {
    val permissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    val permissionState = rememberMultiplePermissionsState(permissions = permissions)
    var hasRequestedPermissions by remember { mutableStateOf(false) }

    LaunchedEffect(permissionState.allPermissionsGranted) {
        Log.d("BluetoothScreen", "All permissions granted: ${permissionState.allPermissionsGranted}")
    }

    LaunchedEffect(Unit) {
        if (!hasRequestedPermissions) {
            Log.d("BluetoothScreen", "Requesting permissions...")
            hasRequestedPermissions = true
            permissionState.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            Log.d("BluetoothScreen", "Starting Bluetooth operations...")
            viewModel.loadPairedDevices()
            viewModel.startScan()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (permissionState.allPermissionsGranted) {
            Text(
                text = "Select a Bluetooth Device",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Paired Devices Section
                item {
                    Text(
                        text = "Paired Devices",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (viewModel.pairedDevices.isEmpty()) {
                    item {
                        Text(
                            text = "No paired devices",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(viewModel.pairedDevices) { device ->
                        DeviceItem(
                            deviceName = device.name ?: "Unknown Device",
                            deviceAddress = device.address,
                            onClick = {
                                //onDeviceSelected(device.address)
                            }
                        )
                    }
                }

                // Scanned Devices Section
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Nearby Devices",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (viewModel.scannedDevices.isEmpty()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Scanning...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(viewModel.scannedDevices) { device ->
                        DeviceItem(
                            deviceName = device.name ?: "Unknown Device",
                            deviceAddress = device.address,
                            onClick = {
                                /*Log.d("macClick", device.address.toString())
                                onDeviceSelected(device.address)*/

                                Log.d("BluetoothScreen", "Device selected: ${device.address}")
                                // Set the result
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("selected_mac", device.address)
                                // Navigate back
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        } else {
            // Permission not granted UI
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⚠️ Permissions Required",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(16.dp))
                Text("Bluetooth and Location permissions are needed")
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        permissionState.launchMultiplePermissionRequest()
                    }
                ) {
                    Text("Grant Permissions")
                }
            }
        }
    }
}

@Composable
fun DeviceItem(
    deviceName: String,
    deviceAddress: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = deviceAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
