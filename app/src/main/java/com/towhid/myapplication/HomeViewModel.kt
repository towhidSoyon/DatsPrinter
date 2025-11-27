package com.towhid.myapplication

import android.app.Application
import android.bluetooth.BluetoothAdapter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val app: Application
) : ViewModel() {

    private var printer: EscPosPrinter? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage = _toastMessage

    var selectedMacAddress: String? = null
        private set

    fun setMacAddress(mac: String?) {
        selectedMacAddress = mac
    }

    fun connectToPrinter(mac: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _connectionState.value = ConnectionState.Connecting

                val device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mac)
                val connection = BluetoothConnection(device)

                printer = EscPosPrinter(connection, 203, 48f, 32)

                _connectionState.value = ConnectionState.Connected
                _toastMessage.value = "Connected to $mac"

            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Failed(e.message ?: "Unknown error")
                _toastMessage.value = "Connection failed"
            }
        }
    }

    fun disconnectPrinter() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                printer?.disconnectPrinter()
                printer = null
                _connectionState.value = ConnectionState.Idle
                _toastMessage.value = "Disconnected"
            } catch (e: Exception) {
                _toastMessage.value = "Error disconnecting"
            }
        }
    }

    fun printTestReceipt() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val p = printer ?: return@launch
                p.printFormattedText(
                    """
                    [C]<b>TEST RECEIPT</b>
                    [L]Item A        [R]10.00
                    [L]Item B        [R]20.00
                    [L]------------------------
                    [R]<b>Total: 30.00</b>
                    """.trimIndent()
                )

                _toastMessage.value = "Print Success"

            } catch (e: Exception) {
                _toastMessage.value = "Print Failed"
            }
        }
    }
}
sealed class ConnectionState {
    object Idle : ConnectionState()            // No connection
    object Connecting : ConnectionState()      // Trying to connect
    object Connected : ConnectionState()       // Connected successfully
    data class Failed(val error: String) : ConnectionState() // Connection failed
}
