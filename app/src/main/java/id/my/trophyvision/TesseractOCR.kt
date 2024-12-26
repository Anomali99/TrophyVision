package id.my.trophyvision

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class TesseractOCR(
    private val context: Context,
    private val labelPath: String
) {
    private lateinit var translate: Translate
    private val tess = TessBaseAPI()

    fun setup() {
        copyTessDataFiles()
        if (!::translate.isInitialized) {
            translate = TranslateOptions.getDefaultInstance().service
        }
        val tessDataPath = context.filesDir.toString() + "/"
        tess.init(tessDataPath, "ind")
    }

    fun predict(bitmap: Bitmap): String? {
        val extractedText = extractTextFromBitmap(bitmap)
        return extractedText?.let { correctSpellingWithGoogleTranslate(it) }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }

//    private fun copyTessDataFiles() {
//        val tessdataDir = File(context.filesDir, "tessdata")
//        if (!tessdataDir.exists()) {
//            tessdataDir.mkdir()
//        }
//
//        val trainedDataFile = File(tessdataDir, "ind.traineddata")
//        if (!trainedDataFile.exists()) {
//            try {
//                val inputStream: InputStream = context.assets.open(labelPath)
//                val outputStream = FileOutputStream(trainedDataFile)
//                val buffer = ByteArray(1024)
//                var length: Int
//                while (inputStream.read(buffer).also { length = it } > 0) {
//                    outputStream.write(buffer, 0, length)
//                }
//                inputStream.close()
//                outputStream.close()
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//    }

    private fun copyTessDataFiles() {
        // Tentukan direktori tessdata di penyimpanan internal aplikasi
        val tessdataDir = File(context.filesDir, "tessdata")

        // Membuat direktori tessdata jika belum ada
        if (!tessdataDir.exists()) {
            tessdataDir.mkdir()
        }

        // Tentukan file traineddata
        val trainedDataFile = File(tessdataDir, "ind.traineddata")

        // Menyalin file hanya jika file traineddata belum ada
        if (!trainedDataFile.exists()) {
            try {
                // Membuka file dari assets
                context.assets.open(labelPath).use { inputStream ->
                    // Menulis file ke penyimpanan internal
                    FileOutputStream(trainedDataFile).use { outputStream ->
                        val buffer = ByteArray(1024)
                        var length: Int
                        // Membaca dan menulis file dalam buffer
                        while (inputStream.read(buffer).also { length = it } > 0) {
                            outputStream.write(buffer, 0, length)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace() // Mencetak error ke log jika terjadi exception
            }
        }
    }

    private fun correctSpellingWithGoogleTranslate(text: String): String {
        if (isInternetAvailable()) {
            return try {
                val translation = translate.translate(
                    text,
                    Translate.TranslateOption.sourceLanguage("id"),
                    Translate.TranslateOption.targetLanguage("id")
                )
                translation.translatedText
            } catch (e: Exception) {
                e.printStackTrace()
                text
            }
        } else{
            return text
        }
    }

    private fun extractTextFromBitmap(bitmap: Bitmap): String? {
        return try {
            tess.setImage(bitmap)
            val result = tess.utF8Text
            tess.clear()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun close() {
        tess.end()
    }
}

