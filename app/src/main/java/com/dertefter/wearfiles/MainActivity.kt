package com.dertefter.wearfiles

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.cardview.widget.CardView
import androidx.core.animation.doOnEnd
import androidx.core.widget.doOnTextChanged
import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView
import com.google.android.wearable.input.RotaryEncoderHelper

class MainActivity : Activity() {
    var pm: PermissionManager = PermissionManager()
    var permissionAlert: ScrollView? = null
    var grandPermissionButton: Button? = null
    var fileListView: WearableRecyclerView? = null
    var moreMenu: WearableRecyclerView? = null
    var newFolderMenu: LinearLayout? = null
    var createFolderButton: Button? = null
    var newFolderName: EditText? = null
    var renameFolderMenu: LinearLayout? = null
    var renameFolderButton: Button? = null
    var renameFolderName: EditText? = null
    var animDuration: Long = 0
    var confirmDialog: ScrollView? = null
    var confirmText: TextView? = null
    var confirmButton: CardView? = null
    var cancelButton: CardView? = null

    override fun onBackPressed() {
        if (WearFiles.fileManagerInstance.isAvailableGoBack()){
            WearFiles.fileManagerInstance.goBack()
            fileListView?.adapter = FileListAdapter(WearFiles.fileManagerInstance.currentFileList)
            fileListView?.adapter?.notifyDataSetChanged()
            animateFileList(true)
        } else {
            super.onBackPressed()
        }

    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_SCROLL && RotaryEncoderHelper.isFromRotaryEncoder(event)) {
            val delta = -RotaryEncoderHelper.getRotaryAxisValue(event) * RotaryEncoderHelper.getScaledScrollFactor(this)
            fileListView?.smoothScrollBy(0, (delta*1.2).toInt())
            moreMenu?.smoothScrollBy(0, (delta).toInt())
            permissionAlert?.smoothScrollBy(0, (delta).toInt())
            val vibrator = applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VibrationEffect.createOneShot(10, 70))
            return true
        }
        return super.onGenericMotionEvent(event)
    }



    object instance {
        var instance: MainActivity? = null

    }

    fun getStorageName(): String{
        return getString(R.string.storage)
    }


    override fun onResume() {
        super.onResume()
        if (!pm.checkFilesPermissions(this)) {
            permissionAlert?.visibility = View.VISIBLE
            fileListView?.visibility = View.GONE
            ObjectAnimator.ofFloat(permissionAlert, "alpha", 0f, 1f).setDuration(50).start()
        }else{
            permissionAlert?.visibility = View.GONE
            showFileListMenu()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val animDurationInt: Int = resources.getInteger(R.integer.animation_duration)
        confirmDialog = findViewById(R.id.confirm_dialog)
        confirmText = findViewById(R.id.confirm_text)
        confirmButton = findViewById(R.id.confirm_button)
        cancelButton = findViewById(R.id.cancel_button)
        animDuration = animDurationInt.toLong()

        newFolderMenu = findViewById(R.id.new_folder_menu)
        createFolderButton = newFolderMenu?.findViewById<Button>(R.id.create_folder)
        newFolderName = newFolderMenu?.findViewById<EditText>(R.id.new_folder_name)

        newFolderName?.doOnTextChanged { text, start, before, count ->
            createFolderButton?.isEnabled = text.toString().isNotEmpty()
        }

        createFolderButton?.setOnClickListener {
            WearFiles.fileManagerInstance.createNewFolder(newFolderName?.text.toString())
            newFolderName?.setText("")
            closeNewFolderMenu()
            animateFileList()
        }

        renameFolderMenu = findViewById(R.id.rename_folder_menu)
        renameFolderButton = renameFolderMenu?.findViewById<Button>(R.id.rename_folder)
        renameFolderName = renameFolderMenu?.findViewById<EditText>(R.id.rename_folder_name)

        renameFolderName?.doOnTextChanged { text, start, before, count ->
            renameFolderButton?.isEnabled = text.toString().isNotEmpty()
        }

        renameFolderButton?.setOnClickListener {
            WearFiles.fileManagerInstance.renameFolder(renameFolderName?.text.toString())
            renameFolderName?.setText("")
            closeRenameFolderMenu()
            animateFileList()
        }


        moreMenu = findViewById(R.id.more_menu)
        instance.instance = this
        moreMenu?.apply {
            // To align the edge children (first and last) with the center of the screen.
            isEdgeItemsCenteringEnabled = true
            layoutManager = WearableLinearLayoutManager(this@MainActivity, CustomScrollingLayoutCallback())
        }
        moreMenu?.adapter = MoreMenuAdapter(WearFiles.fileManagerInstance.getAvailableActions())

        fileListView = findViewById(R.id.files_list_view)
        fileListView?.apply {
            isEdgeItemsCenteringEnabled = false
            layoutManager = WearableLinearLayoutManager(this@MainActivity, CustomScrollingLayoutCallback())
        }
        showFileListMenu()


        permissionAlert = findViewById(R.id.permission_alert)
        grandPermissionButton = findViewById(R.id.grand_permission_button)
        grandPermissionButton?.setOnClickListener {
            Log.e("grandPermissionButton", "click")
            pm.requestFilesPermissions(this)
        }
        Log.e("checkPermissions", pm.checkFilesPermissions(this).toString())
        if (!pm.checkFilesPermissions(this)) {
            permissionAlert?.visibility = View.VISIBLE
            closeFileListMenu()
            ObjectAnimator.ofFloat(permissionAlert, "alpha", 1f).setDuration(animDuration).start()
        }else{
            permissionAlert?.visibility = View.GONE
            showFileListMenu()
        }

    }

    fun showConfirmDialog(){
        confirmDialog?.visibility = View.VISIBLE
        ObjectAnimator.ofFloat(confirmDialog, "alpha", 1f).setDuration(animDuration).start()
    }

    fun closeConfirmDialog(){
        ObjectAnimator.ofFloat(confirmDialog, "alpha", 0f).apply {
            duration = animDuration
            doOnEnd {
                confirmDialog?.visibility = View.GONE
            }
            start()
        }

    }

    fun showMoreMenu(spicific: String? = null){
        if (spicific == null){
            moreMenu?.adapter = MoreMenuAdapter(WearFiles.fileManagerInstance.getAvailableActions())
        }else{
            moreMenu?.adapter = MoreMenuAdapter(WearFiles.fileManagerInstance.getAvailableActions(spicific))
        }
        moreMenu?.visibility = View.VISIBLE
        val anim = ObjectAnimator.ofFloat(moreMenu, "alpha", 1f).setDuration((animDuration/1.5).toLong())
        anim.start()

    }

    fun closeMoreMenu(is_spicific: Boolean? = null) {
        if (is_spicific == true){
            WearFiles.fileManagerInstance.goBack()
        }
        fileListView?.adapter = FileListAdapter(WearFiles.fileManagerInstance.getFilesList())
        val anim = ObjectAnimator.ofFloat(moreMenu, "alpha",  0f).setDuration((animDuration/1.5).toLong())
        anim.doOnEnd { moreMenu?.visibility = View.GONE }
        anim.start()
    }

    fun showFileListMenu(){
        fileListView?.visibility = View.VISIBLE
        fileListView?.adapter = null
        fileListView?.adapter = FileListAdapter(WearFiles.fileManagerInstance.getFilesList())
        val anim = ObjectAnimator.ofFloat(fileListView, "alpha",  1f).setDuration(animDuration)
        anim.start()
    }

    fun closeFileListMenu() {
        val anim = ObjectAnimator.ofFloat(fileListView, "alpha", 0f).setDuration(animDuration)
        anim.doOnEnd { fileListView?.visibility = View.GONE }
        anim.start()
    }

    fun animateFileList(isBack: Boolean = false){
        var animTransition = ObjectAnimator.ofFloat(fileListView, "translationX", 100f, 0f).setDuration(animDuration)
        if (isBack){
            animTransition = ObjectAnimator.ofFloat(fileListView, "translationX", -100f, 0f).setDuration(animDuration)
        }
        val anim = ObjectAnimator.ofFloat(fileListView, "alpha", 1f).setDuration(animDuration)
        animTransition.start()
        anim.start()

    }


    fun showNewFolderMenu() {
        moreMenu?.visibility = View.GONE
        closeFileListMenu()
        newFolderMenu?.visibility = View.VISIBLE
        val anim = ObjectAnimator.ofFloat(newFolderMenu, "alpha", 1f).setDuration(animDuration)
        anim.start()
    }
    fun closeNewFolderMenu() {
        val anim = ObjectAnimator.ofFloat(newFolderMenu, "alpha",  0f).setDuration(animDuration)
        anim.doOnEnd {
            fileListView?.adapter = FileListAdapter(WearFiles.fileManagerInstance.getFilesList())
            newFolderMenu?.visibility = View.GONE
            showFileListMenu()
        }
        anim.start()
    }

    fun showRenameFolderMenu() {
        closeFileListMenu()
        moreMenu?.visibility = View.GONE
        renameFolderMenu?.visibility = View.VISIBLE
        val anim = ObjectAnimator.ofFloat(renameFolderMenu, "alpha", 1f).setDuration(animDuration)
        anim.start()
    }
    fun closeRenameFolderMenu() {
        val anim = ObjectAnimator.ofFloat(renameFolderMenu, "alpha", 0f).setDuration(animDuration)
        anim.doOnEnd {
            showFileListMenu()
            renameFolderMenu?.visibility = View.GONE
        }
        anim.start()
    }

    fun showAlert(s: String) {
        when (s){
            "error" -> Toast.makeText(this@MainActivity, "error", Toast.LENGTH_SHORT).show()
            "no_program" -> Toast.makeText(this@MainActivity, getString(R.string.no_program_to_open), Toast.LENGTH_SHORT).show()
            "error_opening" -> Toast.makeText(this@MainActivity, getString(R.string.no_program_to_open), Toast.LENGTH_SHORT).show()
        }

    }


}