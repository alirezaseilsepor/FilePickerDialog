package app.king.mylibrary

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatTextView

class SpinnerMediaAdapter(
    context: Context,
    textViewResourceId: Int,
    val list: List<String>,
    private val onItemClick: (Pair<String, Int>) -> Unit
) : ArrayAdapter<String>(
    context,
    textViewResourceId,
    list
) {
    override fun getCount() = list.size

    override fun getItem(position: Int) = list[position]

    override fun getItemId(position: Int) = Math.random().toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return (super.getDropDownView(position, convertView, parent) as AppCompatTextView).apply {
            text = list[position]
        }
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return (super.getDropDownView(position, convertView, parent) as AppCompatTextView).apply {
            text = list[position]
            setOnClickListener {
                onItemClick(Pair(list[position], position))
            }
        }
    }
}