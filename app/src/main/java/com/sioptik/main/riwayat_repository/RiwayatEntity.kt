package com.sioptik.main.riwayat_repository
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "riwayat")
data class RiwayatEntity (
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "apriltagId") val apriltagId: Int,
    @ColumnInfo(name = "date") val date: Date,
    @ColumnInfo(name = "jsonFileUri") val jsonFileUri: String,
    @ColumnInfo(name = "originalImageUri") val originalImageUri: String,
    @ColumnInfo(name = "annotatedImageUri") val annotatedImageUri: String
)
