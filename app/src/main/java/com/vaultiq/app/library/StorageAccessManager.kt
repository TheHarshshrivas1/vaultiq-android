
package com.vaultiq.app.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.vaultiq.app.core.common.VaultResult
import java.io.File

class StorageAccessManager(context: Context) {

    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    private val preferences = appContext.getSharedPreferences(
        "vaultiq_storage_preferences",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_SELECTED_FOLDER = "selected_folder_uri"
        private const val FOLDER_NAME = "VAULTIQ"
        private const val DOWNLOADS_FOLDER = "Downloads"
    }

    fun getPrivateDownloadsDirectory(): VaultResult<File> {
        return try {
            val directory = File(appContext.filesDir, DOWNLOADS_FOLDER)

            if (!directory.exists() && !directory.mkdirs()) {
                return VaultResult.Error(
                    "Unable to create downloads folder."
                )
            }

            if (!directory.isDirectory || !directory.canWrite()) {
                return VaultResult.Error(
                    "Downloads folder is not writable."
                )
            }

            VaultResult.Success(directory)
        } catch (exception: Exception) {
            VaultResult.Error(
                "Unable to access private storage.",
                exception
            )
        }
    }

    fun persistSelectedFolder(
        uri: Uri,
        intentFlags: Int
    ): VaultResult<Uri> {

        if (uri.scheme != "content") {
            return VaultResult.Error("Invalid folder selection.")
        }

        val permissionFlags = intentFlags and (
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

        if (
            permissionFlags and
                Intent.FLAG_GRANT_READ_URI_PERMISSION == 0
        ) {
            return VaultResult.Error("Read access was not granted.")
        }

        return try {
            resolver.takePersistableUriPermission(
                uri,
                permissionFlags
            )

            val previousUri = getStoredUri()

            preferences.edit()
                .putString(KEY_SELECTED_FOLDER, uri.toString())
                .apply()

            if (previousUri != null && previousUri != uri) {
                releasePermission(previousUri)
            }

            VaultResult.Success(uri)
        } catch (exception: SecurityException) {
            VaultResult.Error(
                "Unable to retain folder permission.",
                exception
            )
        } catch (exception: Exception) {
            VaultResult.Error(
                "Folder access could not be saved.",
                exception
            )
        }
    }

    fun getSelectedFolder(): Uri? {
        val savedUri = getStoredUri() ?: return null

        val hasReadPermission =
            resolver.persistedUriPermissions.any { permission ->
                permission.uri == savedUri &&
                    permission.isReadPermission
            }

        if (!hasReadPermission) {
            clearStoredFolder()
            return null
        }

        return savedUri
    }

    fun hasSelectedFolderAccess(): Boolean {
        return getSelectedFolder() != null
    }

    fun releaseSelectedFolder(): VaultResult<Unit> {
        val savedUri = getStoredUri()

        if (savedUri == null) {
            clearStoredFolder()
            return VaultResult.Success(Unit)
        }

        return try {
            releasePermission(savedUri)
            clearStoredFolder()
            VaultResult.Success(Unit)
        } catch (exception: Exception) {
            VaultResult.Error(
                "Unable to release folder access.",
                exception
            )
        }
    }

    fun getSuggestedFolderName(): String = FOLDER_NAME

    private fun getStoredUri(): Uri? {
        val savedValue = preferences.getString(
            KEY_SELECTED_FOLDER,
            null
        ) ?: return null

        val uri = runCatching {
            Uri.parse(savedValue)
        }.getOrNull()

        if (uri == null || uri.scheme != "content") {
            clearStoredFolder()
            return null
        }

        return uri
    }

    private fun releasePermission(uri: Uri) {
        val permission =
            resolver.persistedUriPermissions.firstOrNull {
                it.uri == uri
            } ?: return

        var flags = 0

        if (permission.isReadPermission) {
            flags = flags or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        if (permission.isWritePermission) {
            flags = flags or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }

        if (flags != 0) {
            resolver.releasePersistableUriPermission(uri, flags)
        }
    }

    private fun clearStoredFolder() {
        preferences.edit()
            .remove(KEY_SELECTED_FOLDER)
            .apply()
    }
}
