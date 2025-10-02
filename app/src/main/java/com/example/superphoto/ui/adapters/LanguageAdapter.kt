package com.example.superphoto.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.superphoto.R
import com.example.superphoto.data.model.Language

class LanguageAdapter(
    private val languages: List<Language>,
    private val onLanguageSelected: (Language) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    private var selectedPosition = 0 // Default to first item

    inner class LanguageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val flagImageView: ImageView = itemView.findViewById(R.id.img_flag)
        private val nameTextView: TextView = itemView.findViewById(R.id.name_language)

        fun bind(language: Language, position: Int) {
            flagImageView.setImageResource(language.flagResId)
            nameTextView.text = language.name

            // Update selection state
            language.isSelected = position == selectedPosition
            
            // Update UI based on selection
            updateSelectionUI(language.isSelected)

            itemView.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = position
                
                // Update the language selection
                languages[previousPosition].isSelected = false
                language.isSelected = true
                
                // Notify adapter of changes
                notifyItemChanged(previousPosition)
                notifyItemChanged(position)
                
                // Callback to parent
                onLanguageSelected(language)
            }
        }

        private fun updateSelectionUI(isSelected: Boolean) {
            if (isSelected) {
                itemView.setBackgroundResource(R.drawable.bg_radius_selected) // You may need to create this
                nameTextView.setTextColor(itemView.context.getColor(R.color.white))
            } else {
                itemView.setBackgroundResource(R.drawable.bg_radius_white)
                nameTextView.setTextColor(itemView.context.getColor(R.color.grey))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_language, parent, false)
        return LanguageViewHolder(view)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(languages[position], position)
    }

    override fun getItemCount(): Int = languages.size

    fun setSelectedLanguage(languageCode: String) {
        val newPosition = languages.indexOfFirst { it.code == languageCode }
        if (newPosition != -1 && newPosition != selectedPosition) {
            val previousPosition = selectedPosition
            selectedPosition = newPosition
            
            languages[previousPosition].isSelected = false
            languages[newPosition].isSelected = true
            
            notifyItemChanged(previousPosition)
            notifyItemChanged(newPosition)
        }
    }

    fun getSelectedLanguage(): Language? {
        return languages.getOrNull(selectedPosition)
    }
}