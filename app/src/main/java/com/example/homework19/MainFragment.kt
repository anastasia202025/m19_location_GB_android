package com.example.homework19

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.homework19.adapter.LandmarkAdapter
import com.example.homework19.databinding.FragmentMainBinding
import com.example.homework19.db.AppDatabase

class MainFragment : Fragment() {
    private lateinit var database: AppDatabase
    private lateinit var adapter: LandmarkAdapter

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = AppDatabase.getDatabase(requireContext())
        binding.recyclerView.layoutManager = GridLayoutManager(context, COLUMN)

        adapter = LandmarkAdapter()
        binding.recyclerView.adapter = adapter

        database.landmarkDao().getAllLandmarksLiveData().observe(viewLifecycleOwner, Observer { landmarks ->
            adapter.submitList(landmarks)
        })

        binding.fabTakePhoto.setOnClickListener {
            val navController = findNavController()
            navController.navigate(R.id.action_mainFragment_to_takePhotoFragment)
        }

        binding.fabOpenMap.setOnClickListener {
            val navController = findNavController()
            navController.navigate(R.id.action_mainFragment_to_mapFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val COLUMN = 3
    }
}