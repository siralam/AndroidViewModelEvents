package com.example.viewmodeleventlab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

class MainFragment : Fragment() {

    private val vm: MainViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val btnCallApi = view.findViewById<Button>(R.id.btnCallApi)
        btnCallApi.setOnClickListener {
            vm.callSomeApi()
        }

        vm.apiResponse.receiveAsFlow().onEach {
            showDialog(it)
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun showDialog(message: String) {
        CustomDialogFragment(message).show(childFragmentManager, null)
    }
}