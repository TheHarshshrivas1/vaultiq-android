
package com.vaultiq.app.download.engine

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import com.vaultiq.app.core.common.VaultResult
import com.vaultiq.app.library.StorageAccessManager
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

data class DownloadOutput(
    val uri: Uri,
    val outputStream: OutputStream,
    val fileName: String
)

class DownloadDestinationManager(context: Context) {

    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val storageManager = StorageAccessManager(appContext)

    fun createDestination(
        fileName: String,
        mimeType: String?
    ): VaultResult<DownloadOutput> {

        val safeName = sanitizeFileName(fileName)

        if (safeName.isBlank()) {
            return VaultResult.Error("Invalid file name.")
        }

        return try {
            val selectedFolder = storageManager.getSelectedFolder()

            if (selectedFolder != null) {
                createSharedDestination(
                    selectedFolder,
                    safeName,
                    mimeType
                )
            } else {
                createPrivateDestination(safeName)
            }
        } catch (exception: Exception) {
            VaultResult.Error(
                "Unable to create download destination.",
                exception
            )
        }
    }

    private fun createPrivateDestination(
        fileName: String
    ): VaultResult<DownloadOutput> {

        val directory = when (
            val result = storageManager.getPrivateDownloadsDirectory()
        ) {
            is VaultResult.Success -> result.data
            is VaultResult.Error -> return result
            is VaultResult.Loading -> {
                return VaultResult.Error(
                    "Private storage is unavailable."
                )
            }
        }

        val file = File(directory, fileName)

        if (file.exists()) {
            return VaultResult.Error(
                "A file with this name already exists."
            )
        }

        val stream = FileOutputStream(file)

        return VaultResult.Success(
            DownloadOutput(
                uri = Uri.fromFile(file),
                outputStream = stream,
                fileName = file.name
            )
        )
    }

    private fun createSharedDestination(
        treeUri: Uri,
        fileName: String,
        mimeType: String?
    ): VaultResult<DownloadOutput> {

        val documentId =
            DocumentsContract.getTreeDocumentId(treeUri)

        val parentUri =
            DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                documentId
            )

        val resolvedMimeType = mimeType
            ?.takeIf { it.contains("/") }
            ?: MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(
                    fileName.substringAfterLast(".", "")
                        .lowercase()
                )
            ?: "application/octet-stream"

        val documentUri =
            DocumentsContract.createDocument(
                resolver,
                parentUri,
                resolvedMimeType,
                fileName
            ) ?: return VaultResult.Error(
                "Unable to create file in selected folder."
            )

        val stream = resolver.openOutputStream(
            documentUri,
            "wt"
        ) ?: return VaultResult.Error(
            "Unable to open the selected file."
        )

        return VaultResult.Success(
            DownloadOutput(
                uri = documentUri,
                outputStream = stream,
                fileName = fileName
            )
        )
    }

    private fun sanitizeFileName(name: String): String {
        return name
            .substringAfterLast("/")
            .substringAfterLast("\\")
            .replace(
                Regex("""[\u0000-\u001F<>:"|?*]"""),
                "_"
            )
            .trim()
            .take(180)
    }
}
