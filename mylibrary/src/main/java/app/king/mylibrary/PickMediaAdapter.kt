package app.king.mylibrary

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import app.king.mylibrary.databinding.ItemCameraBinding
import app.king.mylibrary.databinding.ItemMediaBinding

import app.king.mylibrary.ktx.Click2
import app.king.mylibrary.ktx.SimpleClick
import app.king.mylibrary.ktx.setOnSafeClickListener
import coil.load
import java.io.File

class PickMediaAdapter : BaseListAdapter<ResultMedia, RecyclerView.ViewHolder>(DIFF_PICK_MEDIA) {

    companion object {
        private const val TYPE_IMAGE = 1
        private const val TYPE_CAMERA = 2
    }

    var selectedListResultMedia = arrayListOf<ResultMedia>()
    var onClickCameraListener: SimpleClick? = null
    var onSelectFileListener: Click2<ResultMedia, Boolean>? = null
    var onSelectMediaListener: Click2<ResultMedia, ImageView>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_IMAGE -> {
                ViewHolderPickMedia(
                    ItemMediaBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            else -> {
                ViewHolderCamera(
                    ItemCameraBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            TYPE_IMAGE -> {
                (holder as ViewHolderPickMedia).bind(getItem(position))
            }
            TYPE_CAMERA -> {
                (holder as ViewHolderCamera).bind(getItem(position))
            }
        }
    }


    inner class ViewHolderPickMedia(val binding: ItemMediaBinding) :
        BaseViewHolder<ResultMedia>(binding.root) {
        private var isSelect = false

        @SuppressLint("NotifyDataSetChanged")
        override fun bind(obj: ResultMedia) {
            //  val context = binding.root.context
            binding.imageView.load(File(obj.path))
            var counter = -1
            run end@{
                selectedListResultMedia.forEachIndexed { index, resultMedia ->
                    if (resultMedia.id == obj.id) {
                        counter = index + 1
                        return@end
                    }
                }
            }

            isSelect = counter != -1
            if (isSelect) {
                select(counter)
            } else {
                unselect()
            }
            binding.root.setOnSafeClickListener {
                onSelectMediaListener?.invoke(obj, binding.imageView)
            }

            binding.btnRadio.setOnSafeClickListener {
                if (!isSelect) {
                    select(selectedListResultMedia.size + 1)
                    onSelectFileListener?.invoke(obj, true)
                } else {
                    unselect()
                    onSelectFileListener?.invoke(obj, false)
                }
                notifyDataSetChanged()
            }
        }

        private fun select(counter: Int) {
            binding.tvSelect.setBackgroundResource(R.drawable.shape_circle_primary_stroke_white)
            binding.tvSelect.text = counter.toString()
        }

        private fun unselect() {
            binding.tvSelect.setBackgroundResource(R.drawable.shape_circle_mini_transparent_stroke)
            binding.tvSelect.text = ""
        }
    }

    inner class ViewHolderCamera(val binding: ItemCameraBinding) :
        BaseViewHolder<ResultMedia>(binding.root) {
        override fun bind(obj: ResultMedia) {
            binding.root.setOnSafeClickListener {
                onClickCameraListener?.invoke()
            }
        }
    }


    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).id != -1L) {
            TYPE_IMAGE
        } else {
            TYPE_CAMERA
        }
    }

}

val DIFF_PICK_MEDIA = object : DiffUtil.ItemCallback<ResultMedia>() {
    override fun areItemsTheSame(oldItem: ResultMedia, newItem: ResultMedia): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ResultMedia, newItem: ResultMedia): Boolean {
        return oldItem == newItem
    }

}