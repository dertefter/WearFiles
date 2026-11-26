package com.dertefter.wearable.onboarding

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.dertefter.wearable.onboarding.presentation.OnBoardingScreen
import com.dertefter.wearable.onboarding.presentation.OnBoardingViewModel

@Composable
fun OnBoardingRoute(
    viewModel: OnBoardingViewModel = hiltViewModel()
) {

    OnBoardingScreen(
        onEvent = viewModel::onEvent,
        dialogState = viewModel.dialogState
    )

}
