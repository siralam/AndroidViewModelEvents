package com.example.viewmodeleventlab

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels

class MainActivity : AppCompatActivity() {

    private lateinit var btnCallApi: Button
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnCallApi = findViewById(R.id.btnCallApi)
        btnCallApi.setOnClickListener {
            vm.callSomeApi()
        }

        vm.apiResponse.observe(this) {
            if (!it.isNullOrBlank()) {
                showDialog(it)
            }
        }
    }

    private fun showDialog(message: String) {
        CustomDialogFragment(message).show(supportFragmentManager, null)
    }
}