package com.dertefter.wearable.menu.presentation.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.dertefter.wearable.design.R

@Composable
fun OperationWorking() {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .align(alignment = Alignment.Center)
        ) {

            CircularProgressIndicator(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(64.dp),
            )

            Text(
                stringResource(R.string.wait), style = MaterialTheme.typography.titleSmall
            )
        }

    }
}