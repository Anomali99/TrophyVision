package id.my.trophyvision

import android.app.Activity
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

class CustomListAdapter(
    private val context: Activity,
    private val classifyResult: List<Pair<Bitmap, Pair<String, String?>>>
) : ArrayAdapter<Pair<Bitmap, Pair<String, String?>>>(context, R.layout.custom_list, classifyResult) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val rowView: View
        val viewHolder: ViewHolder

        if (convertView == null) {
            val inflater = context.layoutInflater
            rowView = inflater.inflate(R.layout.custom_list, parent, false)

            viewHolder = ViewHolder()
            viewHolder.title = rowView.findViewById(R.id.text)
            viewHolder.subtitle = rowView.findViewById(R.id.txt)
            viewHolder.imageView = rowView.findViewById(R.id.img)
            viewHolder.noText = rowView.findViewById(R.id.no)

            rowView.tag = viewHolder
        } else {
            rowView = convertView
            viewHolder = rowView.tag as ViewHolder
        }

        val (cropImg, results) = classifyResult[position]
        val (classify, text) = results

        viewHolder.title.text = classify
        viewHolder.imageView.setImageBitmap(cropImg)
        viewHolder.noText.text = (position + 1).toString()

        if (text != null){
            viewHolder.subtitle.text = text.replace("\n", " ").replace("\r", "")
        } else {
            viewHolder.subtitle.visibility = View.GONE
        }

        return rowView
    }

    private class ViewHolder {
        lateinit var title: TextView
        lateinit var subtitle: TextView
        lateinit var imageView: ImageView
        lateinit var noText: TextView
    }
}