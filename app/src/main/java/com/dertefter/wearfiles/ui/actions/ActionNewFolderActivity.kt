package com.dertefter.wearfiles.ui.actions

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dertefter.wearfiles.R
import com.dertefter.wearfiles.databinding.ActivityActionDeleteBinding
import com.dertefter.wearfiles.databinding.ActivityActionNewFolderBinding
import com.dertefter.wearfiles.model.Action
import com.dertefter.wearfiles.model.ActionType
import com.dertefter.wearfiles.ui.adapter.FileAdapter
import com.dertefter.wearfiles.ui.adapter.MenuAdapter
import com.dertefter.wearfiles.viewmodels.FileViewModel
import java.io.File

class ActionNewFolderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityActionNewFolderBinding
    private lateinit var viewModel: FileViewModel
    private lateinit var path: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActionNewFolderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if  (intent.getStringExtra("path") == null){
            finish()
        }else{
            path = intent.getStringExtra("path")!!
        }

        binding.cancel.setOnClickListener {
            finish()
        }

        viewModel = ViewModelProvider(this).get(FileViewModel::class.java)

        binding.folderName.editText?.doOnTextChanged { text, start, before, count ->
            binding.done.isEnabled = text.toString().isNotEmpty()
        }

        binding.done.setOnClickListener {
            viewModel.createFolder(path, binding.folderName.editText?.text.toString())
            finish()
        }



    }

}
