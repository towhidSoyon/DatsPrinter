package com.towhid.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateListOf
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val bluetoothAdapter: BluetoothAdapter? =
        (application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    val scannedDevices = mutableStateListOf<BluetoothDevice>()
    val pairedDevices = mutableStateListOf<BluetoothDevice>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (BluetoothDevice.ACTION_FOUND == intent?.action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    if (!scannedDevices.contains(it)) scannedDevices.add(it)
                }
            }
        }
    }

    /*@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @SuppressLint("MissingPermission")
    fun loadPairedDevices() {
        bluetoothAdapter?.bondedDevices?.let { paired ->
            pairedDevices.clear()
            pairedDevices.addAll(paired)
        }
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    @SuppressLint("MissingPermission")
    fun startScan() {
        scannedDevices.clear()
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        getApplication<Application>().registerReceiver(receiver, filter)
        bluetoothAdapter?.startDiscovery()
    }


    */

    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("MissingPermission")
    fun loadPairedDevices() {
        val hasPermission = ActivityCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return

        bluetoothAdapter?.bondedDevices?.let { paired ->
            pairedDevices.clear()
            pairedDevices.addAll(paired)
        }
    }
    @SuppressLint("MissingPermission")
    fun startScan() {

        val hasPermission = ActivityCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return

        scannedDevices.clear()

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        getApplication<Application>().registerReceiver(receiver, filter)

        bluetoothAdapter?.startDiscovery()
    }




    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(receiver)
        } catch (_: Exception) {}
    }
}