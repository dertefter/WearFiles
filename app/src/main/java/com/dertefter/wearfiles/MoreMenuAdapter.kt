package com.dertefter.wearfiles

import android.animation.ObjectAnimator
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.WearableRecyclerView
import org.json.JSONArray


class MoreMenuAdapter(private val dataSet: ArrayList<JSONArray>) :
    RecyclerView.Adapter<MoreMenuAdapter.ViewHolder>() {

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
        }else {
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.settings_button, viewGroup, false)
            return ViewHolder(view)
        }




    }
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        if (dataSet[position].get(1) == "back") {
            viewHolder.itemIcon.setImageResource(R.drawable.arrow_back)
            viewHolder.itemName.setText(R.string.back)
            viewHolder.itemView.setOnClickListener {
                MainActivity.instance.instance!!.closeMoreMenu()
            }


        } else if(dataSet[position].get(1) == "back_spicific") {
            viewHolder.itemIcon.setImageResource(R.drawable.arrow_back)
            viewHolder.itemName.setText(R.string.back)
            viewHolder.itemView.setOnClickListener {
                MainActivity.instance.instance!!.closeMoreMenu(is_spicific = true)
            }
        }
        else if (dataSet[position].get(1) == "copy"){
            viewHolder.itemIcon.setImageResource(R.drawable.copy)
            viewHolder.itemName.setText(R.string.copy)
            viewHolder.itemView.setOnClickListener {
                WearFiles.fileManagerInstance.moveOrCopy = "copy"
                WearFiles.fileManagerInstance.moveOrCopyPath = WearFiles.fileManagerInstance.currentPath
                WearFiles.fileManagerInstance.goBack()
                MainActivity.instance.instance!!.closeMoreMenu()
                Log.e("copy", WearFiles.fileManagerInstance.moveOrCopyPath.toString())
            }
        }
        else if (dataSet[position].get(1) == "cut"){
            viewHolder.itemIcon.setImageResource(R.drawable.cut)
            viewHolder.itemName.setText(R.string.cut)
            viewHolder.itemView.setOnClickListener {
                WearFiles.fileManagerInstance.moveOrCopy = "move"
                WearFiles.fileManagerInstance.moveOrCopyPath = WearFiles.fileManagerInstance.currentPath
                WearFiles.fileManagerInstance.goBack()
                MainActivity.instance.instance!!.closeMoreMenu()
                Log.e("cut", WearFiles.fileManagerInstance.moveOrCopyPath.toString())
            }
        }
        else if (dataSet[position].get(1) == "delete") {
            viewHolder.itemIcon.setImageResource(R.drawable.delete)
            viewHolder.itemName.setText(R.string.delete)
            viewHolder.itemView.setOnClickListener {
                WearFiles.fileManagerInstance.deleteFolderOrFile()
                MainActivity.instance.instance!!.closeMoreMenu()
            }

        } else if (dataSet[position].get(1) == "new_folder") {
            viewHolder.itemIcon.setImageResource(R.drawable.new_folder)
            viewHolder.itemName.setText(R.string.new_folder)
            viewHolder.itemView.setOnClickListener {
                MainActivity.instance.instance!!.closeMoreMenu()
                MainActivity.instance.instance!!.showNewFolderMenu()
            }
        } else if (dataSet[position].get(1) == "rename") {
            viewHolder.itemIcon.setImageResource(R.drawable.edit)
            viewHolder.itemName.setText(R.string.rename)
            viewHolder.itemView.setOnClickListener {
                MainActivity.instance.instance!!.closeMoreMenu()
                MainActivity.instance.instance!!.showRenameFolderMenu()
            }
        } else if (dataSet[position].get(1) == "settings") {
            viewHolder.itemIcon.setImageResource(R.drawable.settings)
            viewHolder.itemName.setText(R.string.settings)
            viewHolder.itemView.setOnClickListener {
                Log.e("settings", "clicked settings button")
                //val settingsIntentActivity = Intent(MainActivity.instance.instance, SettingsActivity::class.java)
                //Toast.makeText(MainActivity.instance.instance, "Coming soon...", Toast.LENGTH_SHORT).show()
            }
        }


    }
    override fun getItemCount() = dataSet.size

    override fun getItemViewType(position: Int): Int {
        return if (dataSet[position].get(1) == "settings") {
            1
        } else {
            0
        }
    }
}
