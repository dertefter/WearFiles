package com.dertefter.wearfiles.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.WearableLinearLayoutManager
import com.dertefter.wearfiles.R
import com.dertefter.wearfiles.common.CustomScrollingLayoutCallback
import com.dertefter.wearfiles.common.SpacingItemDecoration
import com.dertefter.wearfiles.model.ActionType
import com.dertefter.wearfiles.ui.actions.ActionDeleteActivity
import com.dertefter.wearfiles.ui.actions.ActionNewFolderActivity
import com.dertefter.wearfiles.ui.actions.ActionRenameActivity
import com.dertefter.wearfiles.ui.adapter.MenuAdapter
import com.dertefter.wearfiles.viewmodels.FileViewModel
import java.io.File

class MenuActivity : AppCompatActivity() {
    private lateinit var viewModel: FileViewModel
    private lateinit var adapter: MenuAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_files)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        recyclerView.layoutManager = WearableLinearLayoutManager(this, CustomScrollingLayoutCallback())
        recyclerView.addItemDecoration(SpacingItemDecoration(R.dimen.spacing, this))

        val path = intent.getStringExtra("path")
        if (path.isNullOrEmpty()){finish()}
        val file = File(path!!)
        adapter = MenuAdapter(
            file,
            onActionClick = { action ->
                when (action.type){
                    ActionType.OPEN -> TODO()
                    ActionType.RENAME -> {
                        val intent = Intent(this, ActionRenameActivity::class.java)
                        intent.putExtra("path", path)
                        startActivity(intent)
                    }
                    ActionType.DELETE -> {
                        val intent = Intent(this, ActionDeleteActivity::class.java)
                        intent.putExtra("path", path)
                        startActivity(intent)
                    }
                    ActionType.COPY -> {
                        viewModel.copyFile(path)
                        finish()
                    }
                    ActionType.PASTE -> {
                        viewModel.pasteFile(path)
                        finish()
                    }
                    ActionType.NEW_FOLDER -> {
                        val intent = Intent(this, ActionNewFolderActivity::class.java)
                        intent.putExtra("path", path)
                        startActivity(intent)
                    }
                    ActionType.CUT -> {
                        viewModel.cutFile(path)
                        finish()
                    }
                }
            },
            onFooterClick = {
                finish()
            }
        )

        recyclerView.adapter = adapter


        viewModel = ViewModelProvider(this).get(FileViewModel::class.java)
        val actions = path?.let { viewModel.getAvailableActions(it) }
        if (actions != null) {
            adapter.updateActions(actions, file)
        }

    }

}
