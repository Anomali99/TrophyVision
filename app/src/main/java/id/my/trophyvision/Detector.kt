package id.my.trophyvision

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.content.ContextCompat
import android.Manifest

class Detector(
    private val context: Context
) {
    private lateinit var yoloModel: YoloDetector
    private lateinit var resnetModel: ResnetDetector
    private lateinit var ocrModel: TesseractOCR
    private var hasPermission: Boolean = false
    private var boxPaint: Paint = Paint()
    private var textPaint: Paint = Paint()
    private var textBg: Paint = Paint()

    fun setup(){
        val classNames = listOf(
            "Piala Akademik",
            "Piala Ekstrakulikuler",
            "Piala Olahraga",
            "Plakat Karya Ilmiah",
            "Plakat STEM"
        )

        resnetModel = ResnetDetector(context,"resnet_model.ptl", classNames)
        resnetModel.setup()
        yoloModel = YoloDetector(context, "yolo_model.tflite", "yolo_labels.txt")
        yoloModel.setup()

//        hasPermission = hasStoragePermissions()
        hasPermission =true
        if (hasPermission) {
            ocrModel = TesseractOCR(context, "ind.traineddata")
            ocrModel.setup()
        }

        boxPaint.strokeWidth = 5f
        boxPaint.style = Paint.Style.STROKE
        boxPaint.color = Color.RED

        textBg.strokeWidth = 5f
        textBg.style = Paint.Style.FILL
        textBg.color = Color.RED

        textPaint.strokeWidth = 50f
        textPaint.style = Paint.Style.FILL
        textPaint.color = Color.WHITE
        textPaint.textSize = 50f
    }

    private fun hasStoragePermissions(): Boolean {
        val readPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        val writePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        return readPermission == PackageManager.PERMISSION_GRANTED &&
                writePermission == PackageManager.PERMISSION_GRANTED
    }

    private fun predictAndCrop(bitmap: Bitmap): Pair<Bitmap, List<Bitmap>> {
        var bestBoxes: List<BoundingBox>? = yoloModel.detect(bitmap)
        val croppedBitmaps = mutableListOf<Bitmap>()

        var mutableBitmap: Bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        var canvas: Canvas = Canvas(mutableBitmap)

        if (bestBoxes != null) {
            bestBoxes.forEachIndexed { index, box ->
                val left = (box.x1 * bitmap.width).toInt()
                val top = (box.y1 * bitmap.height).toInt()
                val right = (box.x2 * bitmap.width).toInt()
                val bottom = (box.y2 * bitmap.height).toInt()

                val text = (index + 1).toString()
                val bounds = Rect()
                textPaint.getTextBounds(text, 0, text.length, bounds)

                // Padding untuk latar belakang teks
                val padding = 10f

                // Hitung posisi teks dan latar belakang
                val textBgRight = left + bounds.width() + (padding * 2)
                val textBgBottom = top + bounds.height() + (padding * 2)

                val textX = left + padding
                val textY = textBgBottom - padding

                // Gambar bounding box
                canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), boxPaint)

                // Gambar latar belakang teks
                canvas.drawRect(left.toFloat(), top.toFloat(), textBgRight, textBgBottom, textBg)

                // Gambar teks di atas latar belakang
                canvas.drawText(text, textX, textY, textPaint)

                // Crop bagian gambar berdasarkan bounding box
                val croppedBitmap =
                    Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
                croppedBitmaps.add(croppedBitmap)
            }
        }
        // Kembalikan gambar dengan bounding box yang digambar, serta array hasil crop
        return Pair(mutableBitmap, croppedBitmaps)
    }

    fun predictAndClassify(bitmap: Bitmap): Pair<Bitmap, List<Pair<Bitmap, Pair<String, String?>>>> {
        val (mutableBitmap, croppedImages) = predictAndCrop(bitmap)
        val classifyResult = mutableListOf<Pair<Bitmap, Pair<String, String?>>>()

        for (img in croppedImages){
            val result = resnetModel.classify(img)
            val text = if (hasPermission) ocrModel.predict(img) else null
            classifyResult.add(Pair(img, Pair(result, text)))
        }

        return Pair(mutableBitmap, classifyResult)
    }

}