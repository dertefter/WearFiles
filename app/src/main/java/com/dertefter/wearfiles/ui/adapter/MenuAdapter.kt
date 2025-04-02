package com.dertefter.wearfiles.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.dertefter.wearfiles.R
import com.dertefter.wearfiles.common.Utils
import com.dertefter.wearfiles.model.Action
import com.dertefter.wearfiles.model.ActionType
import java.io.File

class MenuAdapter(
    private var currentFile: File,
    private var actions: List<Action> = emptyList(),
    private val onActionClick: (Action) -> Unit,
    private val onFooterClick: () -> Unit,

) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_ACTION = 0
        private const val VIEW_TYPE_FOOTER = 1
        private const val VIEW_TYPE_HEADER = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> VIEW_TYPE_HEADER
            in 1..actions.size -> VIEW_TYPE_ACTION
            else -> VIEW_TYPE_FOOTER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ACTION -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.file_item, parent, false)
                ActionViewHolder(view)
            }
            VIEW_TYPE_FOOTER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.more_button, parent, false)
                FooterViewHolder(view)
            }
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_for_action, parent, false)
                HeaderViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ActionViewHolder -> {
                val action = actions[position - 1]
                holder.bind(action, onActionClick)
            }
            is FooterViewHolder -> {
                holder.bind(onFooterClick)
            }
            is HeaderViewHolder -> {
                holder.bind(currentFile)
            }
        }
    }

    override fun getItemCount(): Int = actions.size + 2

    fun updateActions(newActions: List<Action>, newFile: File) {
        actions = newActions
        currentFile = newFile
        notifyDataSetChanged()
    }

    class ActionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.item_name)
        private val icon: ImageView = itemView.findViewById(R.id.item_icon)
        fun bind(action: Action, onActionClick: (Action) -> Unit) {
            name.text = itemView.context.getString(action.name)
            itemView.setOnClickListener { onActionClick(action) }
            when (action.type){
                ActionType.OPEN -> TODO()
                ActionType.RENAME -> {
                    icon.setImageResource(R.drawable.edit)
                }
                ActionType.DELETE -> {
                    icon.setImageResource(R.drawable.delete)
                }
                ActionType.COPY -> {
                    icon.setImageResource(R.drawable.copy)
                }
                ActionType.PASTE -> {
                    icon.setImageResource(R.drawable.paste)
                }
                ActionType.NEW_FOLDER -> {
                    icon.setImageResource(R.drawable.new_folder)
                }
                ActionType.CUT -> {
                    icon.setImageResource(R.drawable.cut)
                }
            }


        }
    }

    class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val button_more: Button = itemView.findViewById(R.id.more_button)
        private val button_back: Button = itemView.findViewById(R.id.back_button)

        fun bind(onFooterClick: () -> Unit) {
            button_back.visibility = View.VISIBLE
            button_more.visibility = View.GONE
            button_back.setOnClickListener { onFooterClick() }
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.name)
        private val icon: ImageView = itemView.findViewById(R.id.icon)

        fun bind(currentFile: File) {
            if (currentFile.isDirectory){
                name.text = currentFile.path
            }else{
                name.text = currentFile.name
            }


            icon.setImageResource(Utils.getFileIconResId(currentFile))

        }
    }
}