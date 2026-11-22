
package com.vaultiq.app.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.vaultiq.app.core.common.VaultResult
import java.io.File

class StorageAccessManager(
    context: Context
) {

    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    private val preferences = appContext.getSharedPreferences(
        "vaultiq_storage_preferences",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_SELECTED_FOLDER =
            "selected_folder_uri"

        private const val FOLDER_NAME = "VAULTIQ"

        private const val DOWNLOADS_FOLDER = "Downloads"
    }

    /**
     * Default private storage directory.
     * No storage permission is required.
     */
    fun getPrivateDownloadsDirectory(): VaultResult<File> {

        return try {

            val directory = File(
                appContext.filesDir,
                DOWNLOADS_FOLDER
            )

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

    /**
     * Persist access to a folder selected through SAF.
     */
    fun persistSelectedFolder(
        uri: Uri,
        intentFlags: Int
    ): VaultResult<Uri> {

        if (uri.scheme != "content") {
            return VaultResult.Error(
                "Invalid folder selection."
            )
        }

        val permissionFlags = intentFlags and (
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

        if (
            permissionFlags and
                Intent.FLAG_GRANT_READ_URI_PERMISSION == 0
        ) {
            return VaultResult.Error(
                "Read access was not granted."
            )
        }

        return try {

            resolver.takePersistableUriPermission(
                uri,
                permissionFlags
            )

            preferences.edit()
                .putString(KEY_SELECTED_FOLDER, uri.toString())
                .apply()

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

    /**
     * Return the previously selected folder if access still exists.
     */
    fun getSelectedFolder(): Uri? {

        val savedValue = preferences.getString(
            KEY_SELECTED_FOLDER,
            null
        ) ?: return null

        val savedUri = runCatching {
            Uri.parse(savedValue)
        }.getOrNull() ?: return null

        if (savedUri.scheme != "content") {
            return null
        }

        val hasPermission =
            resolver.persistedUriPermissions.any { permission ->
                permission.uri == savedUri &&
                    permission.isReadPermission
            }

        return if (hasPermission) savedUri else null
    }

    /**
     * Check whether a folder has persisted read access.
     */
    fun hasSelectedFolderAccess(): Boolean {
        return getSelectedFolder() != null
    }

    /**
     * Release permission and clear the saved folder.
     */
    fun releaseSelectedFolder(): VaultResult<Unit> {

        val uri = getSelectedFolder()
            ?: return VaultResult.Success(Unit)

        return try {

            val permission =
                resolver.persistedUriPermissions.firstOrNull {
                    it.uri == uri
                }

            if (permission != null) {

                var flags = 0

                if (permission.isReadPermission) {
                    flags = flags or
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

                if (permission.isWritePermission) {
                    flags = flags or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                }

                resolver.releasePersistableUriPermission(
                    uri,
                    flags
                )
            }

            preferences.edit()
                .remove(KEY_SELECTED_FOLDER)
                .apply()

            VaultResult.Success(Unit)

        } catch (exception: Exception) {

            VaultResult.Error(
                "Unable to release folder access.",
                exception
            )
        }
    }

    /**
     * Suggested folder name for the user's selected storage.
     */
    fun getSuggestedFolderName(): String {
        return FOLDER_NAME
    }
}
