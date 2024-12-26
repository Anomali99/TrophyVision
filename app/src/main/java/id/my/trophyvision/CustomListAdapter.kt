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

        // Use view recycling for better performance
        if (convertView == null) {
            val inflater = context.layoutInflater
            rowView = inflater.inflate(R.layout.custom_list, parent, false)

            // Create a new ViewHolder and store it in the view
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

        // Get the current pair
        val (cropImg, results) = classifyResult[position]
        val (classify, text) = results

        // Set the values to the views
        viewHolder.title.text = classify
        viewHolder.imageView.setImageBitmap(cropImg)
        viewHolder.noText.text = (position + 1).toString()

        if (text != null){
            viewHolder.subtitle.text = text
        } else {
            viewHolder.subtitle.visibility = View.GONE
        }

        return rowView
    }

    // ViewHolder pattern for better performance and cleaner code
    private class ViewHolder {
        lateinit var title: TextView
        lateinit var subtitle: TextView
        lateinit var imageView: ImageView
        lateinit var noText: TextView
    }
}