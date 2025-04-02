package com.dertefter.wearfiles.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dertefter.wearfiles.R
import com.dertefter.wearfiles.common.Utils
import java.io.File

class FileAdapter(
    private var currentPath: String = "",
    private var files: List<File> = emptyList(),
    private val onFileClick: (File) -> Unit,
    private val onFooterClick: (String) -> Unit,
    private val onBackClick: () -> Unit,
    private val isBackEnabled: Boolean = false,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_FILE = 0
        private const val VIEW_TYPE_FOOTER = 1
        private const val VIEW_TYPE_HEADER = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> VIEW_TYPE_HEADER
            in 1..files.size -> VIEW_TYPE_FILE
            else -> VIEW_TYPE_FOOTER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_FILE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.file_item, parent, false)
                FileViewHolder(view)
            }
            VIEW_TYPE_FOOTER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.more_button, parent, false)
                FooterViewHolder(view)
            }
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.path_view, parent, false)
                HeaderViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is FileViewHolder -> {
                val file = files[position - 1]
                holder.bind(file, onFileClick, onFooterClick)
            }
            is FooterViewHolder -> {
                holder.bind(onFooterClick, onBackClick, isBackEnabled, currentPath)
            }
            is HeaderViewHolder -> {
                holder.bind(currentPath)
            }
        }
    }

    override fun getItemCount(): Int = files.size + 2 // +1 for header, +1 for footer

    fun updateFiles(newFiles: List<File>, newPath: String = currentPath) {
        files = newFiles
        currentPath = newPath
        notifyDataSetChanged()
    }

    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.item_name)
        private val icon: ImageView = itemView.findViewById(R.id.item_icon)





        fun bind(file: File, onFileClick: (File) -> Unit, onFooterClick: (String) -> Unit) {
            name.text = file.name
            itemView.setOnClickListener { onFileClick(file) }
            itemView.setOnLongClickListener {
                onFooterClick(file.absolutePath)
                true
            }
            if (file.isDirectory) {
                icon.setImageResource(R.drawable.folder)
            } else {
                icon.setImageResource(Utils.getFileIconResId(file))
            }
        }
    }

    class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val button_more: Button = itemView.findViewById(R.id.more_button)
        private val button_back: Button = itemView.findViewById(R.id.back_button)

        fun bind(
            onFooterClick: (path: String) -> Unit,
            onBackClick: () -> Unit,
            isBackEnabled: Boolean,
            currentPath: String
        ) {
            if (isBackEnabled){
                button_back.visibility = View.VISIBLE
            } else {
                button_back.visibility = View.GONE
            }
            button_more.setOnClickListener { onFooterClick(currentPath) }
            button_back.setOnClickListener { onBackClick() }
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val pathText: TextView = itemView.findViewById(R.id.path)

        fun bind(currentPath: String) {
            pathText.text = currentPath
        }
    }
}