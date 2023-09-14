package com.dertefter.wearfiles

import android.content.Intent
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import org.json.JSONArray
import java.io.File


class FileManager {
    var currentPath = getStoragePath()
    var currentFileList = getFilesList()
    var moveOrCopy: String? = null
    var moveOrCopyPath: String? = null

    private fun getStoragePath(): String {
        return Environment.getExternalStorageDirectory().toString()
    }

    fun getFilesList(): ArrayList<JSONArray> {
        if (File(currentPath).isFile){
            currentPath = File(currentPath).parent!!
        }
        val files = File(currentPath).listFiles()?.sortedWith(compareBy({ it.isDirectory }, { it.name }))
        val result = arrayListOf<JSONArray>()
        if (files != null){
            for (file in files){
                result.add(JSONArray(listOf(file.name, file.absolutePath, file.isFile)))
            }
        }
        if (moveOrCopy == "copy") {
            if (result.size > 0) {
                result.add(0, JSONArray().put("action").put("paste").put(false))
            } else {
                result.add(JSONArray().put("action").put("paste").put(false))
            }
        } else if (moveOrCopy == "move") {
            if (result.size > 0) {
                result.add(0, JSONArray().put("action").put("move").put(false))
            } else {
                result.add(JSONArray().put("action").put("move").put(false))
            }
        }
        if (isAvailableGoBack()){
            if (result.size > 0){
                result.add(0, JSONArray().put("action").put("back").put(false))
            }else{
                result.add(JSONArray().put("action").put("back").put(false))
            }
        }

        result.add(JSONArray().put("action").put("more").put(false))
        Log.e("main", MainActivity.instance.instance.toString())
        val pathString = currentPath.replace(getStoragePath(), WearFiles.appContext.getString(R.string.storage))
        result.add(0, JSONArray().put("path").put(pathString).put(false))
        return result
    }

    fun openFolder(path: String){
        currentPath = path
        currentFileList = getFilesList()
    }


    fun isAvailableGoBack(): Boolean {
        return currentPath != getStoragePath() && currentPath != "/"
    }

    fun goBack(){
        if (isAvailableGoBack()){
            val path = File(currentPath).parent as String
            openFolder(path)
            MainActivity.instance.instance!!.closeMoreMenu()
            MainActivity.instance.instance!!.closeNewFolderMenu()
            MainActivity.instance.instance!!.closeRenameFolderMenu()
        }
    }

    fun getAvailableActions(spicific: String? = null): ArrayList<JSONArray>{
        if (spicific == null){
            val result = arrayListOf<JSONArray>()
            result.add(JSONArray().put("action").put("back").put(false))
            result.add(JSONArray().put("action").put("new_folder").put(false))
            Log.e("current", currentPath)
            Log.e("Storage", getStoragePath())
            if (currentPath != getStoragePath() && currentPath != "/" && currentPath != getStoragePath() + "/Android"){
                result.add(JSONArray().put("action").put("rename").put(false))
                result.add(JSONArray().put("action").put("delete").put(false))
            }
            result.add(JSONArray().put("action").put("settings").put(false))
            return result
        }else{
            currentPath = spicific
            if (File(spicific).isFile){
                val result = arrayListOf<JSONArray>()
                result.add(JSONArray().put("action").put("back_spicific").put(false))
                result.add(JSONArray().put("action").put("copy").put(false))
                result.add(JSONArray().put("action").put("cut").put(false))
                result.add(JSONArray().put("action").put("delete").put(false))
                result.add(JSONArray().put("action").put("settings").put(false))
                return result
            }
            else{
                val result = arrayListOf<JSONArray>()
                result.add(JSONArray().put("action").put("back_spicific").put(false))
                Log.e("current", currentPath)
                Log.e("Storage", getStoragePath())
                if (currentPath != getStoragePath() && currentPath != "/" && currentPath != getStoragePath() + "/Android"){
                    result.add(JSONArray().put("action").put("rename").put(false))
                    result.add(JSONArray().put("action").put("copy").put(false))
                    result.add(JSONArray().put("action").put("cut").put(false))
                    result.add(JSONArray().put("action").put("delete").put(false))
                }
                result.add(JSONArray().put("action").put("settings").put(false))
                return result
            }
        }

    }

    fun createNewFolder(name: String){
        val path = currentPath
        val file = File(path, name)
        file.mkdir()
        openFolder(path)
    }

    fun pasteFolderOrFile(toPath: String): MutableList<Any> {
        try{
            if (File(moveOrCopyPath).isFile){
                copyFile(moveOrCopyPath!!, toPath)
            }else{
                copyFolder(moveOrCopyPath!!, toPath)
            }
            moveOrCopy = null
            moveOrCopyPath = null
            return mutableListOf<Any>().apply {
                add(true)
                add("success")
            }
        }
        catch (e: kotlin.io.FileAlreadyExistsException){
            return mutableListOf<Any>().apply {
                add(false)
                add("FileAlreadyExistsException")
            }
        }

    }

    fun moveFolderOrFile(toPath: String) {

        if (File(moveOrCopyPath).isFile){
            moveFile(moveOrCopyPath!!, toPath)

        }else{
            moveFolder(moveOrCopyPath!!, toPath)
        }
        moveOrCopy = null
        moveOrCopyPath = null


    }

    fun deleteFolderOrFile(){
        val file = File(currentPath)
        try{
            if (file.isFile){
                file.delete()
                goBack()
            }else{
                file.deleteRecursively()
                goBack()
            }
        } catch (e: Exception){
            MainActivity.instance.instance!!.showAlert("error")
        }
    }

    fun copyFile(filePath: String, toPath: String){
        try{
            val file = File(filePath)
            val toFile = File(toPath, file.name)
            file.copyTo(toFile)
        } catch (e: FileAlreadyExistsException){
            MainActivity.instance.instance!!.showConfirmDialog()
            MainActivity.instance.instance!!.closeFileListMenu()
            MainActivity.instance.instance!!.confirmText?.text = WearFiles.appContext.getString(R.string.override_file)
            MainActivity.instance.instance!!.confirmButton?.setOnClickListener {
                val file = File(filePath)
                val toFile = File(toPath, file.name)
                file.copyTo(toFile, true)
                MainActivity.instance.instance!!.closeConfirmDialog()
                MainActivity.instance.instance!!.showFileListMenu()
                MainActivity.instance.instance!!.animateFileList()
                MainActivity.instance.instance!!.fileListView?.adapter = FileListAdapter(WearFiles.fileManagerInstance.getFilesList())

            }
        }

    }

    fun copyFolder(filePath: String, toPath: String){
        val file = File(filePath)
        val toFile = File(toPath, file.name)
        file.copyRecursively(toFile, overwrite = true)
    }

    fun moveFile(filePath: String, toPath: String){
        try{
            val fileAlreadyExist: Boolean = File(toPath, File(filePath).name).exists()
            if (fileAlreadyExist){
                throw FileAlreadyExistsException(File(toPath, File(filePath).name))
            }
            Log.e("moving", "eee")

        } catch (e: FileAlreadyExistsException){
            MainActivity.instance.instance!!.showConfirmDialog()
            MainActivity.instance.instance!!.closeFileListMenu()
            MainActivity.instance.instance!!.confirmText?.text = WearFiles.appContext.getString(R.string.override_file)
            MainActivity.instance.instance!!.confirmButton?.setOnClickListener {
                val file = File(filePath)
                val toFile = File(toPath, file.name)
                file.renameTo(toFile)
                MainActivity.instance.instance!!.closeConfirmDialog()
                MainActivity.instance.instance!!.closeMoreMenu()
                MainActivity.instance.instance!!.showFileListMenu()
                MainActivity.instance.instance!!.animateFileList()
                MainActivity.instance.instance!!.fileListView?.adapter = FileListAdapter(WearFiles.fileManagerInstance.getFilesList())

            }
        }

    }

    fun moveFolder(filePath: String, toPath: String){
        val file = File(filePath)
        val toFile = File(toPath, file.name)
        file.renameTo(toFile)
    }



    fun renameFolder(name: String){
        val file = File(currentPath)
        val path = file.parent
        val newFile = File(path, name)
        file.renameTo(newFile)
        openFolder(path)
    }

    fun openFile(path: String){
        try{
            val type = getType(path)
            Log.e("opening_file", type.toString())
            if (type != null){
                if (type == "application/vnd.android.package-archive"){
                    Log.e("yes", "yea")
                    MainActivity.instance.instance!!.showAlert("no_program")
                    return
                }
                val intent = Intent(Intent.ACTION_VIEW)
                val URI = FileProvider.getUriForFile(
                    MainActivity.instance.instance!!.applicationContext,
                    MainActivity.instance.instance!!.packageName + ".provider",
                    File(path)
                )
                intent.setDataAndType(URI, getType(path))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                MainActivity.instance.instance!!.startActivity(intent)
            }else{
                MainActivity.instance.instance!!.showAlert("no_program")
            }
        } catch (e: Exception){
            MainActivity.instance.instance!!.showAlert("error_opening")
            Log.e("error_opening", e.stackTraceToString())
        }


    }

    fun getType(path: String): String? {
        val extension = path.substring(path.lastIndexOf(".") + 1)
        Log.e("Ex", extension)
        val mime = MimeTypeMap.getSingleton()
        val type = mime.getMimeTypeFromExtension(extension)
        return type
    }
}