package id.my.trophyvision

import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.Manifest
import android.graphics.Canvas

class MainActivity : AppCompatActivity() {
    private lateinit var detector: Detector
    private val REQUEST_CODE = 100
    private val captureId: Int = 73
    private var dialog: Dialog? = null

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                processing(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        checkPermissions()
        detector = Detector(baseContext)
        detector.setup()
    }

    fun openGalery(view: View){
        getContent.launch("image/*")
    }

    fun openCamera(view: View){
        val intent: Intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, captureId)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == captureId && resultCode == RESULT_OK) {
            val bitmap = data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
//                val argbBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val argbBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(argbBitmap)
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                processing(argbBitmap)
            } else {
                Toast.makeText(this, "Gagal mendapatkan gambar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun processing(bitmap: Bitmap){
        dialog = Dialog(this).apply {
            setContentView(R.layout.loading_dialog)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setCancelable(false)
            show()
        }

        Thread {
            val (bitmapResult, classifyResult) = detector.predictAndClassify(bitmap)

            runOnUiThread {
                container.bitmap = bitmapResult
                container.classify = classifyResult

                dialog?.dismiss()
                dialog = null

                val intent = Intent(this, AnalysisActivity::class.java)
                startActivity(intent)
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        dialog?.dismiss()
        dialog = null
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                REQUEST_CODE
            )
        } else {
            Toast.makeText(this, "Izin sudah diberikan!", Toast.LENGTH_SHORT).show()
        }
    }
}

object container {
    var bitmap: Bitmap? = null
    var classify: List<Pair<Bitmap, Pair<String, String?>>>? = null
}