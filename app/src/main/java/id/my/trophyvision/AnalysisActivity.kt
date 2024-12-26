package id.my.trophyvision

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AnalysisActivity : AppCompatActivity() {
    private lateinit var imgView: ImageView
    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_analysis)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.analysis)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        imgView = findViewById(R.id.imageView)
        listView = findViewById(R.id.listView)

        imgView.setImageBitmap(container.bitmap)

        val classify:List<Pair<Bitmap, Pair<String, String?>>>? = container.classify
        if (classify != null){
            val customAdapter = CustomListAdapter(this, classify)
            listView.adapter = customAdapter
        }
    }

    fun back(view: View){
        finish()
    }
}