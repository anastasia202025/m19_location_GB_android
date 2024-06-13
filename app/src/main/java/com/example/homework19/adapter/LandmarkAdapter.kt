package com.example.homework19.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.homework19.databinding.ItemLandmarkBinding
import com.example.homework19.model.Landmark

class LandmarkAdapter :
    ListAdapter<Landmark, LandmarkAdapter.LandmarkViewHolder>(LandmarkDiffCallback()) {

    class LandmarkViewHolder(val binding: ItemLandmarkBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LandmarkViewHolder {
        val binding = ItemLandmarkBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return LandmarkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LandmarkViewHolder, position: Int) {
        val landmark = getItem(position)
        Glide.with(holder.itemView.context)
            .load(landmark.photoPath)
            .into(holder.binding.imageViewItem)
    }
}

class LandmarkDiffCallback : DiffUtil.ItemCallback<Landmark>() {
    override fun areItemsTheSame(oldItem: Landmark, newItem: Landmark): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Landmark, newItem: Landmark): Boolean {
        return oldItem == newItem
    }
}