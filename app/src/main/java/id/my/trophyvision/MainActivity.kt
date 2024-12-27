package id.my.trophyvision

import android.app.Dialog
import android.content.Intent
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.Manifest
import java.io.File
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider

class MainActivity : AppCompatActivity() {
    private lateinit var detector: Detector
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

        requestStoragePermissions()
        detector = Detector(baseContext)
        detector.setup()
    }

    fun openGalery(view: View){
        getContent.launch("image/*")
    }

    fun openCamera(view: View){
        val photoFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "photo.jpg")
        if (!photoFile.parentFile.exists()) {
            photoFile.parentFile.mkdirs()
        }
        val photoURI = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        startActivityForResult(intent, captureId)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == captureId && resultCode == RESULT_OK) {
            val photoFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "photo.jpg")
            if (photoFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                processing(bitmap)
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

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Minta izin WRITE_EXTERNAL_STORAGE hanya untuk Android < 10
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                1
            )
        } else {
            // Minta izin READ_EXTERNAL_STORAGE untuk Android 10+
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                1
            )
        }
    }
}

object container {
    var bitmap: Bitmap? = null
    var classify: List<Pair<Bitmap, Pair<String, String?>>>? = null
}