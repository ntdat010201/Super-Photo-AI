package com.example.superphoto.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.superphoto.R
import com.example.superphoto.model.PhotoCard

class ToolsAdapter(
    private val onItemClick: (PhotoCard) -> Unit = {}
) : ListAdapter<PhotoCard, ToolsAdapter.ToolsViewHolder>(ToolsDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int
    ): ToolsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tool_photo, parent, false)
        return ToolsViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ToolsViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }
    
    override fun onBindViewHolder(
        holder: ToolsViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            // Partial update dựa trên payload
            val item = getItem(position)
            for (payload in payloads) {
                when (payload) {
                    "title_changed" -> holder.updateTitle(item.title)
                }
            }
        }
    }

    inner class ToolsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val photoImageView: ImageView = itemView.findViewById(R.id.photoImageView)
        private val titleText: TextView = itemView.findViewById(R.id.titleText)
        
        init {
            // Tối ưu hóa ViewHolder
            itemView.isClickable = true
            itemView.isFocusable = true
        }

        fun bind(photoCard: PhotoCard) {
            titleText.text = photoCard.title

            // Sử dụng Glide để load image hiệu quả hơn với caching
            photoCard.imageResource?.let { imageRes ->
                Glide.with(itemView.context)
                    .load(imageRes)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)  // Cache để tăng hiệu suất
                    .placeholder(R.drawable.template_default_sample)  // Placeholder khi loading
                    .error(R.drawable.template_default_sample)  // Error fallback
                    .into(photoImageView)
                photoImageView.visibility = View.VISIBLE
            } ?: run {
                // Hiển thị placeholder thay vì ẩn
                Glide.with(itemView.context)
                    .load(R.drawable.template_default_sample)
                    .into(photoImageView)
                photoImageView.visibility = View.VISIBLE
            }

            itemView.setOnClickListener { onItemClick(photoCard) }
        }
        
        fun updateTitle(title: String) {
            titleText.text = title
        }
    }

    object ToolsDiffCallback : DiffUtil.ItemCallback<PhotoCard>() {
        override fun areItemsTheSame(oldItem: PhotoCard, newItem: PhotoCard): Boolean {
            // So sánh ID để xác định xem đây có phải là cùng một mục hay không
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PhotoCard, newItem: PhotoCard): Boolean {
            // So sánh chi tiết để tối ưu hiệu suất
            return oldItem.title == newItem.title &&
                    oldItem.badge == newItem.badge &&
                    oldItem.imageResource == newItem.imageResource
        }
        
        override fun getChangePayload(oldItem: PhotoCard, newItem: PhotoCard): Any? {
            // Trả về payload để update partial thay vì full rebind
            return if (oldItem.title != newItem.title) "title_changed" else null
        }
    }
}