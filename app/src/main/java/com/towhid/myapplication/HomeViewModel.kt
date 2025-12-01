package com.towhid.myapplication

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.EscPosPrinterCommands
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
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

    /**
     * Print a simple test receipt
     */
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
                _toastMessage.value = "Print Failed: ${e.message}"
            }
        }
    }

    /**
     * Print receipt with logo image from drawable
     */
    fun printReceiptWithLogo() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val p = printer ?: return@launch

                val logoBitmap = BitmapFactory.decodeResource(
                    app.resources,
                    R.drawable.logo
                )

                p.printFormattedTextAndCut(
                    """
                    [C]<img>${PrinterTextParserImg.bitmapToHexadecimalString(p, logoBitmap)}</img>
                    [C]<b>MY STORE NAME</b>
                    [C]123 Main Street
                    [C]Tel: (123) 456-7890
                    [C]================================
                    [L]
                    [L]Date: ${getCurrentDate()}
                    [L]Time: ${getCurrentTime()}
                    [L]
                    [L]<b>Items:</b>
                    [L]--------------------------------
                    [L]Coffee            [R]${'$'}3.50
                    [L]Sandwich          [R]${'$'}7.99
                    [L]Water             [R]${'$'}1.50
                    [L]--------------------------------
                    [L]Subtotal:         [R]${'$'}12.99
                    [L]Tax (10%):        [R]${'$'}1.30
                    [L]================================
                    [L]<b>TOTAL:</b>     [R]<b>${'$'}14.29</b>
                    [L]
                    [C]<b>Thank You!</b>
                    [C]Please Come Again
                    
                    
                    """.trimIndent()
                )

                _toastMessage.value = "Receipt with logo printed!"

            } catch (e: Exception) {
                e.printStackTrace()
                _toastMessage.value = "Print Failed: ${e.message}"
            }
        }
    }

    /**
     * Print image from URL
     * Example: Print product image from web
     */
    fun printImageFromUrl(imageUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _toastMessage.value = "Downloading image..."

                val p = printer ?: run {
                    _toastMessage.value = "Printer not connected"
                    return@launch
                }

                // Download image from URL using Coil
                val bitmap = loadBitmapFromUrl(imageUrl)

                if (bitmap == null) {
                    _toastMessage.value = "Failed to download image"
                    return@launch
                }

                // Resize for printer (384 pixels wide for 48mm at 203dpi)
                val resizedBitmap = resizeBitmapForPrinter(bitmap, 384)

                // Print
                p.printFormattedTextAndCut(
                    """
                    [C]<b>Image from URL</b>
                    [C]<img>${PrinterTextParserImg.bitmapToHexadecimalString(p, resizedBitmap)}</img>
                    [C]$imageUrl
                    
                    
                    """.trimIndent()
                )

                _toastMessage.value = "Image printed successfully!"

            } catch (e: Exception) {
                e.printStackTrace()
                _toastMessage.value = "Failed to print: ${e.message}"
            }
        }
    }


    /**
     * Print large bitmap (e.g., 5000x5000 pixels)
     * Automatically resizes to fit printer
     */
    fun printLargeBitmap(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _toastMessage.value = "Processing large image..."

                val p = printer ?: run {
                    _toastMessage.value = "Printer not connected"
                    return@launch
                }

                // Show original size
                _toastMessage.value = "Original: ${bitmap.width}x${bitmap.height}px"

                // Resize to printer width (384px for 48mm thermal printer)
                val resizedBitmap = resizeBitmapForPrinter(bitmap, 384)

                _toastMessage.value = "Resized to: ${resizedBitmap.width}x${resizedBitmap.height}px"

                // Print
                p.printFormattedTextAndCut(
                    """
                    [C]<b>Large Image Print</b>
                    [C]Original: ${bitmap.width}x${bitmap.height}
                    [C]Printed: ${resizedBitmap.width}x${resizedBitmap.height}
                    [C]<img>${PrinterTextParserImg.bitmapToHexadecimalString(p, resizedBitmap)}</img>
                    
                    
                    """.trimIndent()
                )

                _toastMessage.value = "Large image printed!"

            } catch (e: Exception) {
                e.printStackTrace()
                _toastMessage.value = "Failed to print: ${e.message}"
            }
        }
    }

    /**
     * Print product with image from URL
     */
    fun printProductReceipt(
        productName: String,
        productPrice: Double,
        productImageUrl: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _toastMessage.value = "Loading product image..."

                val p = printer ?: return@launch

                // Download product image
                val productBitmap = loadBitmapFromUrl(productImageUrl)

                val imageHex = if (productBitmap != null) {
                    val resized = resizeBitmapForPrinter(productBitmap, 384)
                    PrinterTextParserImg.bitmapToHexadecimalString(p, resized)
                } else {
                    "" // Skip image if failed to load
                }

                // Print receipt
                p.printFormattedTextAndCut(
                    """
                    [C]<b>PRODUCT RECEIPT</b>
                    [C]================================
                    ${if (imageHex.isNotEmpty()) "[C]<img>$imageHex</img>\n" else ""}
                    [C]<b>$productName</b>
                    [C]Price: $${"%.2f".format(productPrice)}
                    [C]================================
                    [C]Thank you!
                    
                    
                    """.trimIndent()
                )

                _toastMessage.value = "Product receipt printed!"

            } catch (e: Exception) {
                e.printStackTrace()
                _toastMessage.value = "Print failed: ${e.message}"
            }
        }
    }

    /**
     * Print QR Code
     */
    fun printQRCode(qrData: String = "https://example.com") {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val p = printer ?: return@launch

                p.printFormattedTextAndCut(
                    """
                    [C]<b>Scan QR Code</b>
                    [C]<qrcode size='20'>$qrData</qrcode>
                    [C]Thank you!
                    
                    
                    """.trimIndent()
                )

                _toastMessage.value = "QR Code printed!"

            } catch (e: Exception) {
                _toastMessage.value = "QR Print Failed: ${e.message}"
            }
        }
    }

    /**
     * Print barcode
     */
    fun printBarcode(barcodeData: String = "1234567890128") {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val p = printer ?: return@launch

                p.printFormattedTextAndCut(
                    """
                    [C]<b>Product Barcode</b>
                    [C]<barcode type='ean13' height='50'>$barcodeData</barcode>
                    
                    
                    """.trimIndent()
                )

                _toastMessage.value = "Barcode printed!"

            } catch (e: Exception) {
                _toastMessage.value = "Barcode Print Failed: ${e.message}"
            }
        }
    }

    // ===========================
    // HELPER FUNCTIONS
    // ===========================

    /**
     * Load bitmap from URL using Coil
     */
    private suspend fun loadBitmapFromUrl(url: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(app)
                val request = ImageRequest.Builder(app)
                    .data(url)
                    .allowHardware(false) // Important: Disable hardware bitmaps
                    .build()

                val result = loader.execute(request)
                if (result is SuccessResult) {
                    (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Alternative: Load bitmap from URL using standard approach
     */
    private suspend fun loadBitmapFromUrlAlternative(url: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection()
                connection.doInput = true
                connection.connect()
                val input = connection.getInputStream()
                BitmapFactory.decodeStream(input)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Resize bitmap to fit printer width while maintaining aspect ratio
     * Handles large bitmaps (5000px+) efficiently
     */
    private fun resizeBitmapForPrinter(bitmap: Bitmap, maxWidth: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Already smaller than max width
        if (width <= maxWidth) {
            return bitmap
        }

        // Calculate new dimensions
        val ratio = maxWidth.toFloat() / width.toFloat()
        val newHeight = (height * ratio).toInt()

        // For very large bitmaps, use Matrix for better performance
        return if (width > 2000 || height > 2000) {
            val matrix = Matrix().apply {
                postScale(ratio, ratio)
            }
            Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
        } else {
            Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
        }
    }

    /**
     * Convert large bitmap to grayscale and optimize for thermal printer
     */
    private fun optimizeBitmapForThermalPrinter(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Convert to grayscale and apply threshold
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xff
            val g = (pixel shr 8) and 0xff
            val b = pixel and 0xff

            // Grayscale conversion
            val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

            // Apply threshold for better thermal printing
            val bw = if (gray > 127) 0xFFFFFF else 0x000000
            pixels[i] = (0xFF shl 24) or bw
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * Get current date
     */
    private fun getCurrentDate(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
    }

    /**
     * Get current time
     */
    private fun getCurrentTime(): String {
        return java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
    }
}

sealed class ConnectionState {
    object Idle : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Failed(val error: String) : ConnectionState()
}


