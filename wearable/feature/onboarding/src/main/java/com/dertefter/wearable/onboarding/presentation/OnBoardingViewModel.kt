package com.dertefter.wearable.onboarding.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dertefter.wearable.onboarding.presentation.content.DialogState
import com.dertefter.wearable.onboarding.usecase.OpenLinkOnPhoneUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnBoardingViewModel @Inject constructor(
    val openLinkOnPhoneUseCase: OpenLinkOnPhoneUseCase,
) : ViewModel() {


    var dialogState by mutableStateOf(DialogState.CLOSED)
        private set


    fun onEvent(event: Event) {
        when (event) {
            is Event.OnOpenLinkOnPhone -> {
                openLinkOnPhone()
            }

            is Event.CloseDialog -> {
                dialogState = DialogState.CLOSED
            }

            is Event.ShowDialog -> {
                dialogState = if (event.isSuccessful){
                    DialogState.SUCCESS
                } else {
                    DialogState.FAILED
                }
            }
        }
    }


    private fun openLinkOnPhone(){
        viewModelScope.launch {
            openLinkOnPhoneUseCase("https://github.com/dertefter/WearFiles/blob/master/FAQ.md#storage-section")
                .onSuccess {
                    onEvent(Event.ShowDialog(isSuccessful = true))
                }
                .onFailure {
                    onEvent(Event.ShowDialog(isSuccessful = false))
                }

        }

    }

}
