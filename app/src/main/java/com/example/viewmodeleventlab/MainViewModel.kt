package com.example.viewmodeleventlab

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel: ViewModel() {

    val apiResponse = MutableLiveData<String?>(null)

    fun callSomeApi() {
        viewModelScope.launch {
            delay(3000)
            apiResponse.value = "I am some response"
        }
    }
}