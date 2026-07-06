package com.example.coffio.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deleted_brews")
data class DeletedBrew(
    @PrimaryKey val syncKey: String
)
