package id.my.trophyvision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class Detector(
    private val context: Context
) {
    private lateinit var yoloModel: YoloDetector
    private lateinit var resnetModel: ResnetDetector
    private var boxPaint: Paint = Paint()
    private var textPaint: Paint = Paint()
    private var textBg: Paint = Paint()
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

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

                val padding = 10f
                val textBgRight = left + bounds.width() + (padding * 2)
                val textBgBottom = top + bounds.height() + (padding * 2)

                val textX = left + padding
                val textY = textBgBottom - padding

                canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), boxPaint)
                canvas.drawRect(left.toFloat(), top.toFloat(), textBgRight, textBgBottom, textBg)
                canvas.drawText(text, textX, textY, textPaint)

                val croppedBitmap =
                    Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
                croppedBitmaps.add(croppedBitmap)
            }
        }
        return Pair(mutableBitmap, croppedBitmaps)
    }

    fun predictAndClassify(bitmap: Bitmap): Pair<Bitmap, List<Pair<Bitmap, Pair<String, String?>>>> {
        val (mutableBitmap, croppedImages) = predictAndCrop(bitmap)
        val classifyResult = mutableListOf<Pair<Bitmap, Pair<String, String?>>>()

        for (img in croppedImages) {
            val result = resnetModel.classify(img)
            val text: String? = processImageSync(img)

            classifyResult.add(Pair(img, Pair(result, text)))
        }

        return Pair(mutableBitmap, classifyResult)
    }

    fun processImageSync(bitmap: Bitmap): String? {
        val image = InputImage.fromBitmap(bitmap, 0)
        var result: String? = null
        val latch = CountDownLatch(1)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                result = visionText.text
                latch.countDown()
            }
            .addOnFailureListener { e ->
                result = null
                latch.countDown()
            }

        latch.await()
        return result
    }

}