package com.vaultiq.app.data.repository

import com.vaultiq.app.core.common.VaultResult
import com.vaultiq.app.data.database.DownloadDao
import com.vaultiq.app.data.database.DownloadEntity
import kotlinx.coroutines.flow.Flow

class DownloadRepository(
    private val downloadDao: DownloadDao
) {

    fun observeAll(): Flow<List<DownloadEntity>> {
        return downloadDao.observeAll()
    }

    fun observeByStatus(status: String): Flow<List<DownloadEntity>> {
        return downloadDao.observeByStatus(status)
    }

    suspend fun getById(id: Long): VaultResult<DownloadEntity> {
        return try {
            val download = downloadDao.getById(id)

            if (download != null) {
                VaultResult.Success(download)
            } else {
                VaultResult.Error("Download record not found.")
            }
        } catch (exception: Exception) {
            VaultResult.Error(
                "Unable to load download record.",
                exception
            )
        }
    }

    suspend fun add(download: DownloadEntity): VaultResult<Long> {
        return try {
            val id = downloadDao.insert(download)
            VaultResult.Success(id)
        } catch (exception: Exception) {
            VaultResult.Error(
                "Unable to save download.",
                exception
            )
        }
    }

    suspend fun update(
        download: DownloadEntity
    ): VaultResult<Unit> {
        return try {
            downloadDao.update(
                download.copy(updatedAt = System.currentTimeMillis())
            )
            VaultResult.Success(Unit)
        } catch (exception: Exception) {
            VaultResult.Error(
                "Unable to update download.",
                exception
            )
        }
    }

    suspend fun delete(id: Long): VaultResult<Unit> {
        return try {
            downloadDao.deleteById(id)
            VaultResult.Success(Unit)
        } catch (exception: Exception) {
            VaultResult.Error(
                "Unable to delete download.",
                exception
            )
        }
    }

    suspend fun clearHistory(): VaultResult<Unit> {
        return try {
            downloadDao.deleteAll()
            VaultResult.Success(Unit)
        } catch (exception: Exception) {
            VaultResult.Error(
                "Unable to clear download history.",
                exception
            )
        }
    }

    suspend fun getCount(): VaultResult<Int> {
        return try {
            VaultResult.Success(downloadDao.getCount())
        } catch (exception: Exception) {
            VaultResult.Error(
                "Unable to get download count.",
                exception
            )
        }
    }

    suspend fun getCountByStatus(
        status: String
    ): VaultResult<Int> {
        return try {
            VaultResult.Success(
                downloadDao.getCountByStatus(status)
            )
        } catch (exception: Exception) {
            VaultResult.Error(
                "Unable to get download status count.",
                exception
            )
        }
    }
}
