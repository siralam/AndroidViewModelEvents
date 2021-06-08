package com.example.viewmodeleventlab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel: ViewModel() {

    val apiResponse = Channel<String>(Channel.BUFFERED)

    fun callSomeApi() {
        viewModelScope.launch {
            delay(3000)
            apiResponse.send("I am some response")
        }
    }
}