package com.example.viewmodeleventlab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainViewModel: ViewModel() {

    val apiResponse = MutableStateFlow<Event<String?>>(Event(null))

    fun callSomeApi() {
        viewModelScope.launch {
            delay(3000)
            apiResponse.value = Event("I am some response")
        }
    }
}