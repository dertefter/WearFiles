package com.dertefter.wearfiles.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.WearableLinearLayoutManager
import com.dertefter.wearfiles.PermissionManager
import com.dertefter.wearfiles.R
import com.dertefter.wearfiles.common.CustomScrollingLayoutCallback
import com.dertefter.wearfiles.common.SpacingItemDecoration
import com.dertefter.wearfiles.ui.adapter.FileAdapter
import com.dertefter.wearfiles.viewmodels.FileViewModel
import java.io.File

class FileActivity : AppCompatActivity() {
    private lateinit var viewModel: FileViewModel
    private lateinit var adapter: FileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_files)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        val permissionAlert = findViewById<ScrollView>(R.id.permission_alert)
        recyclerView.layoutManager = WearableLinearLayoutManager(this, CustomScrollingLayoutCallback())
        recyclerView.addItemDecoration(SpacingItemDecoration(R.dimen.spacing, this))

        val permissionManager = PermissionManager()

        val isGranted = permissionManager.checkFilesPermissions(this)

        if (!isGranted){
            recyclerView.visibility = View.GONE
            permissionAlert.visibility = View.VISIBLE
            val button = permissionAlert.findViewById<View>(R.id.try_get_permissions)
            button.setOnClickListener {
                permissionManager.requestFilesPermissions(this)
            }
        } else {
            recyclerView.visibility = View.VISIBLE
            permissionAlert.visibility = View.GONE
        }




        viewModel = ViewModelProvider(this).get(FileViewModel::class.java)

        adapter = FileAdapter(
            currentPath = viewModel.currentPath.value?: "",
            onFileClick = { file ->
                onFileClicked(file)
            },
            onFooterClick = { path ->
                onMoreClicked(path)
            },
            onBackClick = {finish()},
            isBackEnabled = viewModel.currentPath.value != Environment.getExternalStorageDirectory().path
        )
        recyclerView.adapter = adapter

        viewModel.files.observe(this) { files ->
            adapter.updateFiles(files)
        }

    }

    override fun onResume() {
        super.onResume()
        if (!viewModel.isExists(viewModel.currentPath.value!!)){
            finish()
        }
        viewModel.loadFiles(viewModel.currentPath.value ?: Environment.getExternalStorageDirectory().path)

    }

    private fun onFileClicked(file: File) {
        if (file.isDirectory) {
            val intent = Intent(this, FileActivity::class.java)
            intent.putExtra("path", file.absolutePath)
            startActivity(intent)
        } else {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
            val mimeType = contentResolver.getType(uri) ?: "*/*"

            intent.setDataAndType(uri, mimeType)
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "Нечем открыть", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onMoreClicked(path: String) {
        val intent = Intent(this, MenuActivity::class.java)
        intent.putExtra("path", path)
        startActivity(intent)
    }
}
