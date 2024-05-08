package com.sioptik.main.riwayat_repository
import kotlinx.coroutines.flow.Flow

class RiwayatRepository(private val riwayatDao: RiwayatDao) {
    fun getAll(): Flow<List<RiwayatEntity>> {
        return riwayatDao.getAll()
    }
    suspend fun insert(riwayat: RiwayatEntity) {
        riwayatDao.insert(riwayat)
    }
    suspend fun delete(riwayat: RiwayatEntity) {
        riwayatDao.delete(riwayat)
    }
    suspend fun deleteAll() {
        riwayatDao.deleteAll()
    }
}