package com.example.grindlyapp1

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grindlyapp1.databinding.FragmentBrowseServicesBinding
import com.example.grindlyapp1.viewmodels.ServiceViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class BrowseServicesFragment : Fragment() {

    private var _binding: FragmentBrowseServicesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ServiceViewModel by viewModels()
    private lateinit var adapter: ComboAdapter
    private lateinit var addBtn: FloatingActionButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBrowseServicesBinding.inflate(inflater, container, false)

        adapter = ComboAdapter(
            onServiceClick = { service ->
                val intent = Intent(requireContext(), ServiceProfile::class.java)
                intent.putExtra("SERVICE_ID", service.id)
                startActivity(intent)
            },
            onHustlerClick = { hustler ->
                val intent = Intent(requireContext(), ServiceProfile::class.java)
                intent.putExtra("HUSTLER_ID", hustler.hustlerId)
                startActivity(intent)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        addBtn = binding.fabPlus
        addBtn.setOnClickListener {
            val intent = Intent(requireContext(), ServiceProfile::class.java)
            startActivity(intent)
        }

        viewModel.services.observe(viewLifecycleOwner) { services ->
            val hustlers = viewModel.hustlers.value ?: emptyList()
            adapter.updateData(services, hustlers)
        }

        viewModel.hustlers.observe(viewLifecycleOwner) { hustlers ->
            val services = viewModel.services.value ?: emptyList()
            adapter.updateData(services, hustlers)
        }

        viewModel.loadCombo()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
