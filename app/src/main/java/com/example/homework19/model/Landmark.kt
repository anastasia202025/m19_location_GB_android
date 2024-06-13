package com.example.homework19.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "landmarks")
data class Landmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val photoPath: String,
    val photoDate: String,
    val latitude: Double,
    val longitude: Double,
    val description: String?
)
