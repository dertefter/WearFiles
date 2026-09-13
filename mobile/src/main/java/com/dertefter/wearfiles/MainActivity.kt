package com.dertefter.wearfiles

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dertefter.wearfiles.data.ConnectionStatus
import com.dertefter.wearfiles.data.TransferRepository
import com.dertefter.wearfiles.data.WearNode
import com.dertefter.wearfiles.presentation.AboutScreen
import com.dertefter.wearfiles.presentation.MainViewModel
import com.dertefter.wearfiles.presentation.NodeSelectionPager
import com.dertefter.wearfiles.presentation.NotificationPermissionRequest
import com.dertefter.wearfiles.presentation.Queue
import com.dertefter.wearfiles.ui.theme.WearFilesTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen()

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            WearFilesTheme {
                val navController = rememberNavController()
                NavHost(
                    modifier = Modifier.background(MaterialTheme.colorScheme.background),
                    navController = navController,
                    startDestination = "main",
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                ) {
                    composable("main") {
                        MainScreen(
                            availableNodes = TransferRepository.availableNodes,
                            selectedNodeId = TransferRepository.selectedNodeId,
                            sharedUris = viewModel.sharedUris,
                            onNodeSelected = { viewModel.onNodeSelected(it) },
                            onMenuClick = { navController.navigate("about") }
                        )
                    }
                    composable("about") {
                        AboutScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                viewModel.updateSharedUris(listOfNotNull(uri))
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                viewModel.updateSharedUris(uris ?: emptyList())
            }
        }
    }
}

@Composable
fun MainScreen(
    availableNodes: List<WearNode>,
    selectedNodeId: String?,
    sharedUris: List<Uri>,
    onNodeSelected: (String) -> Unit,
    onMenuClick: () -> Unit = {}
) {

    val context = LocalContext.current
    val fileSender = remember { WearableFileSender(context) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            fileSender.sendFileToWear(uri)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column{
                TopAppBar(
                    scrollBehavior = scrollBehavior,
                    modifier = Modifier.fillMaxWidth(),
                    title = {},
                    colors = TopAppBarDefaults.topAppBarColors(
                        scrolledContainerColor = Color.Transparent,
                        containerColor = Color.Transparent
                    ),
                    actions = {
                        IconButton(
                            onClick = onMenuClick
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_menu),
                                contentDescription = stringResource(R.string.menu_content_description)
                            )
                        }
                    }
                )

                NodeSelectionPager(
                    modifier = Modifier
                        .padding(bottom = 8.dp),
                    nodes = availableNodes,
                    selectedNodeId = selectedNodeId,
                    onNodeSelected = onNodeSelected
                )
            }

        },
        floatingActionButtonPosition = FabPosition.Center,
        bottomBar = {

            val selectedNode = availableNodes.find { it.id == selectedNodeId }
            val isFabVisible = selectedNode?.status == ConnectionStatus.READY

            AnimatedVisibility(
                isFabVisible ,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ){
                    ExtendedFloatingActionButton(
                        onClick = { launcher.launch("*/*") },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .navigationBarsPadding()
                            .padding(vertical = 12.dp),
                    ){
                        Icon(
                            painter = painterResource(R.drawable.ic_upload),
                            contentDescription = null,

                            )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.select_files))
                    }
                }

            }

        },
    ) { contentPadding ->
        NotificationPermissionRequest()
        Queue(
            contentPadding = contentPadding,
            initialUris = sharedUris
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenPreview() {
    WearFilesTheme {
        MainScreen(
            availableNodes = emptyList(),
            selectedNodeId = "1",
            sharedUris = emptyList(),
            onNodeSelected = {}
        )
    }
}
