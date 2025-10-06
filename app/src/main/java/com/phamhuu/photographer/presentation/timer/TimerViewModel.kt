package com.phamhuu.photographer.presentation.timer

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamhuu.photographer.contants.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimerViewModel : ViewModel() {
    private val _elapsedTime = MutableStateFlow(0)
    val elapsedTime = _elapsedTime.asStateFlow()

    private var isRecording = false

    @SuppressLint("DefaultLocale")
    fun timeDisplayRecord(time: Int): String =
        String.format(Constants.FORMAT_TIME, time / 60, time % 60)


    fun startTimer() {
        isRecording = true
        _elapsedTime.value = 0
        viewModelScope.launch {
            while (isRecording) {
                delay(1000L)
                _elapsedTime.value++
            }
        }
    }

    fun stopTimer() {
        isRecording = false
    }
}