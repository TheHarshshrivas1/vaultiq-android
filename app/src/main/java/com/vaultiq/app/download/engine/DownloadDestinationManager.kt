
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

    fun deleteDestination(uri: Uri): VaultResult<Unit> {
        return try {
            when (uri.scheme) {
                "content" -> {
                    val deleted = DocumentsContract.deleteDocument(
                        resolver,
                        uri
                    )

                    if (!deleted) {
                        return VaultResult.Error(
                            "Unable to remove incomplete downloaded file."
                        )
                    }
                }

                "file" -> {
                    val directoryResult =
                        storageManager.getPrivateDownloadsDirectory()

                    val directory = when (directoryResult) {
                        is VaultResult.Success -> directoryResult.data
                        is VaultResult.Error -> return directoryResult
                        is VaultResult.Loading -> {
                            return VaultResult.Error(
                                "Private storage is unavailable."
                            )
                        }
                    }

                    val path = uri.path
                        ?: return VaultResult.Error(
                            "Invalid private file location."
                        )

                    val target = File(path).canonicalFile
                    val allowedDirectory = directory.canonicalFile

                    if (target.parentFile != allowedDirectory) {
                        return VaultResult.Error(
                            "File is outside VAULTIQ storage."
                        )
                    }

                    if (target.exists() && !target.delete()) {
                        return VaultResult.Error(
                            "Unable to delete incomplete private file."
                        )
                    }
                }

                else -> {
                    return VaultResult.Error(
                        "Unsupported destination location."
                    )
                }
            }

            VaultResult.Success(Unit)
        } catch (exception: Exception) {
            VaultResult.Error(
                "Unable to delete incomplete download.",
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

        val uniqueName = findUniqueName(fileName) {
            File(directory, it).exists()
        }

        val file = File(directory, uniqueName)

        if (!file.createNewFile()) {
            return VaultResult.Error(
                "Unable to reserve a unique download filename."
            )
        }

        val stream = try {
            FileOutputStream(file)
        } catch (exception: Exception) {
            file.delete()
            throw exception
        }

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

        val childrenUri =
            DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                documentId
            )

        val existingNames: Set<String> = resolver.query(
    childrenUri,
    arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
    null,
    null,
    null
)?.use { cursor ->
    buildSet<String> {
        val nameColumn = cursor.getColumnIndex(
            DocumentsContract.Document.COLUMN_DISPLAY_NAME
        )

        if (nameColumn >= 0) {
            while (cursor.moveToNext()) {
                val existingName = cursor.getString(nameColumn)
                if (existingName != null) {
                    add(existingName)
                }
            }
        }
    }
} ?: return VaultResult.Error(
    "Unable to check existing files in the selected folder."
)

        val uniqueName = findUniqueName(fileName) {
            it in existingNames
        }

        val resolvedMimeType = mimeType
            ?.takeIf { it.contains("/") }
            ?: MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(
                    uniqueName.substringAfterLast(".", "")
                        .lowercase()
                )
            ?: "application/octet-stream"

        val documentUri = DocumentsContract.createDocument(
            resolver,
            parentUri,
            resolvedMimeType,
            uniqueName
        ) ?: return VaultResult.Error(
            "Unable to create file in selected folder."
        )

        return try {
            val stream = resolver.openOutputStream(
                documentUri,
                "wt"
            ) ?: throw IllegalStateException(
                "Unable to open the selected file."
            )

            val actualName = resolver.query(
                documentUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME
                    )
                    if (index >= 0) cursor.getString(index) else null
                } else {
                    null
                }
            } ?: uniqueName

            VaultResult.Success(
                DownloadOutput(
                    uri = documentUri,
                    outputStream = stream,
                    fileName = actualName
                )
            )
        } catch (exception: Exception) {
            runCatching {
                DocumentsContract.deleteDocument(
                    resolver,
                    documentUri
                )
            }

            throw exception
        }
    }

    private fun findUniqueName(
        originalName: String,
        exists: (String) -> Boolean
    ): String {
        if (!exists(originalName)) return originalName

        val dotIndex = originalName.lastIndexOf('.')
        val hasExtension = dotIndex > 0

        val baseName = if (hasExtension) {
            originalName.substring(0, dotIndex)
        } else {
            originalName
        }

        val extension = if (hasExtension) {
            originalName.substring(dotIndex)
        } else {
            ""
        }

        for (number in 1..9999) {
            val candidate = "$baseName ($number)$extension"

            if (!exists(candidate)) {
                return candidate
            }
        }

        throw IllegalStateException(
            "Unable to find an available filename."
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
