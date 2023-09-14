package com.dertefter.wearfiles

import android.animation.ObjectAnimator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.WearableRecyclerView
import org.json.JSONArray


class FileListAdapter(private val dataSet: ArrayList<JSONArray>) :
    RecyclerView.Adapter<FileListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemName: TextView
        val itemIcon: ImageView

        init {
            itemName = view.findViewById(R.id.item_name)
            itemIcon = view.findViewById(R.id.item_icon)
        }
    }


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        if (viewType == 0){
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.file_item, viewGroup, false)
            return ViewHolder(view)
        }else if (viewType == 1){
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.back_button, viewGroup, false)
            return ViewHolder(view)
        }else if (viewType == 2){
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.more_button, viewGroup, false)
            return ViewHolder(view)
        } else if (viewType == 4){
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.paste_button, viewGroup, false)
            return ViewHolder(view)
        }else{
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.path_view, viewGroup, false)
            return ViewHolder(view)
        }

    }
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        if (dataSet[position].get(0) == "path"){
            viewHolder.itemName.text = dataSet[position].get(1).toString()
            Log.e("pathHolder", dataSet[position].get(1).toString())
        } else if (dataSet[position].get(0) == "action") {
            if (dataSet[position].get(1) == "back") {
                viewHolder.itemIcon.setImageResource(R.drawable.arrow_back)
                viewHolder.itemName.setText(R.string.back)
                viewHolder.itemView.setOnClickListener {
                    if (dataSet[position].get(2) == false){
                        WearFiles.fileManagerInstance.goBack()
                        MainActivity.instance.instance?.animateFileList(true)
                        this.dataSet.clear()
                        this.dataSet.addAll(WearFiles.fileManagerInstance.currentFileList)
                        notifyDataSetChanged()
                    }else{
                        Log.e("FileListAdapter", "File clicked")

                    }
                }
            }
            else if (dataSet[position].get(1) == "paste") {
                viewHolder.itemIcon.setImageResource(R.drawable.paste)
                viewHolder.itemName.setText(R.string.paste)
                viewHolder.itemView.setOnClickListener {
                    WearFiles.fileManagerInstance.pasteFolderOrFile(WearFiles.fileManagerInstance.currentPath)
                    dataSet.clear()
                    dataSet.addAll(WearFiles.fileManagerInstance.getFilesList())
                    notifyDataSetChanged()
                    MainActivity.instance.instance!!.animateFileList()
                }
            }
            else if (dataSet[position].get(1) == "move") {
                viewHolder.itemIcon.setImageResource(R.drawable.paste)
                viewHolder.itemName.setText(R.string.paste)
                viewHolder.itemView.setOnClickListener {
                    WearFiles.fileManagerInstance.moveFolderOrFile(WearFiles.fileManagerInstance.currentPath)
                    dataSet.clear()
                    dataSet.addAll(WearFiles.fileManagerInstance.getFilesList())
                    notifyDataSetChanged()
                    MainActivity.instance.instance!!.animateFileList()
                }
            }


            else if (dataSet[position].get(1) == "more") {
                viewHolder.itemIcon.setImageResource(R.drawable.menu)
                viewHolder.itemName.setText("")
                viewHolder.itemView.findViewById<CardView>(R.id.more_button).setOnClickListener {
                    if (dataSet[position].get(2) == false){
                        Log.e("FileListAdapter", "More clicked")
                        MainActivity.instance.instance!!.showMoreMenu()

                    }else{
                        Log.e("FileListAdapter", "File clicked")

                    }
                }
            }

        } else{
            viewHolder.itemName.text = dataSet[position].get(0).toString()
            if (dataSet[position].get(2) == false){
                viewHolder.itemIcon.setImageResource(R.drawable.folder)
            }else{
                viewHolder.itemIcon.setImageResource(R.drawable.draft)
                val n = dataSet[position].get(0).toString()
                var type = n.split(".")[n.split(".").size - 1]
                when (type){
                    "txt" -> viewHolder.itemIcon.setImageResource(R.drawable.file)
                    "apk" -> viewHolder.itemIcon.setImageResource(R.drawable.apk_document)
                    "png" -> viewHolder.itemIcon.setImageResource(R.drawable.image)
                    "jpg" -> viewHolder.itemIcon.setImageResource(R.drawable.image)
                    "jpeg" -> viewHolder.itemIcon.setImageResource(R.drawable.image)
                    "gif" -> viewHolder.itemIcon.setImageResource(R.drawable.image)
                    "mp4" -> viewHolder.itemIcon.setImageResource(R.drawable.video)
                    "avi" -> viewHolder.itemIcon.setImageResource(R.drawable.video)
                    "mkv" -> viewHolder.itemIcon.setImageResource(R.drawable.video)
                    "webm" -> viewHolder.itemIcon.setImageResource(R.drawable.video)
                    "mp3" -> viewHolder.itemIcon.setImageResource(R.drawable.music)
                    "ogg" -> viewHolder.itemIcon.setImageResource(R.drawable.music)
                    "wav" -> viewHolder.itemIcon.setImageResource(R.drawable.music)
                    "flac" -> viewHolder.itemIcon.setImageResource(R.drawable.music)
                    "m4a" -> viewHolder.itemIcon.setImageResource(R.drawable.music)
                    "aac" -> viewHolder.itemIcon.setImageResource(R.drawable.music)
                    "opus" -> viewHolder.itemIcon.setImageResource(R.drawable.music)
                    "wma" -> viewHolder.itemIcon.setImageResource(R.drawable.music)
                    "mka" -> viewHolder.itemIcon.setImageResource(R.drawable.music)
                    "m3u" -> viewHolder.itemIcon.setImageResource(R.drawable.music)
                    "pdf" -> viewHolder.itemIcon.setImageResource(R.drawable.pdf)


                }
            }
            viewHolder.itemView.setOnClickListener {
                if (dataSet[position].get(2) == false){
                    WearFiles.fileManagerInstance.openFolder(dataSet[position].get(1).toString())
                    this.dataSet.clear()
                    this.dataSet.addAll(WearFiles.fileManagerInstance.currentFileList)
                    MainActivity.instance.instance!!.animateFileList()
                    notifyDataSetChanged()
                }else{
                    Log.e("FileListAdapter", "File clicked")
                    WearFiles.fileManagerInstance.openFile(dataSet[position].get(1).toString())
                }
            }
            viewHolder.itemView.setOnLongClickListener {
                WearFiles.fileManagerInstance.currentPath = dataSet[position].get(1).toString()
                MainActivity.instance.instance!!.showMoreMenu(dataSet[position].get(1).toString())
                true
            }
        }

    }
    override fun getItemCount() = dataSet.size

    override fun getItemViewType(position: Int): Int {
        if (dataSet[position].get(0) == "action") {
            return if (dataSet[position].get(1) == "back" ) {
                1
            }else if (dataSet[position].get(1) == "move" || dataSet[position].get(1) == "paste") {
                4
            } else {
                2
            }
        } else if (dataSet[position].get(0) == "path"){
            return 3
        }

        else{
            return 0
        }
    }
}
