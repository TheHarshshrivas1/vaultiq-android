package com.vaultiq.app.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "downloads",
    indices = [
        Index(value = ["url"]),
        Index(value = ["status"]),
        Index(value = ["createdAt"])
    ]
)
data class DownloadEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val url: String,

    val fileName: String,

    val mimeType: String? = null,

    val filePath: String? = null,

    val totalBytes: Long = 0,

    val downloadedBytes: Long = 0,

    val status: String = "QUEUED",

    val errorMessage: String? = null,

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis()
)
