package com.example.grindlyapp1

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MicroAcademy : Fragment() {

    private lateinit var addBtn: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_micro_academy, container, false)

        addBtn = view.findViewById(R.id.fabPlus)
        addBtn.setOnClickListener {
            val intent = Intent(requireContext(), Guide::class.java)
            startActivity(intent)
        }

        return view
    }
}