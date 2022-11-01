package app.king.filePicker

import android.view.View
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView


abstract class BaseListAdapter<T, VH : RecyclerView.ViewHolder?>(diffUtil: DiffUtil.ItemCallback<T>) :
    ListAdapter<T, VH>(diffUtil) {

    override fun submitList(list: List<T>?) {
        super.submitList(list?.let { ArrayList(it) })
    }

    var mLastPosition = -1
    fun setScaleAnimation(viewToAnimate: View, position: Int) {
        if (position > mLastPosition) {
            val anim = ScaleAnimation(
                0.0f,
                1.0f,
                0.0f,
                1.0f,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
            )
            anim.duration =500L
            viewToAnimate.startAnimation(anim)
            mLastPosition = position
        }
    }
}


