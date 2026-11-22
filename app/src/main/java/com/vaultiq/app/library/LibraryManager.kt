
package com.vaultiq.app.library

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.database.Cursor
import com.vaultiq.app.core.common.VaultResult

data class VaultFile(
    val uri: Uri,
    val name: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val lastModified: Long,
    val isDirectory: Boolean
)

class LibraryManager(
    private val context: Context
) {

    private val resolver = context.contentResolver

    private val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_SIZE,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        DocumentsContract.Document.COLUMN_FLAGS
    )

    fun listFiles(treeUri: Uri): VaultResult<List<VaultFile>> {

        if (treeUri.scheme != "content") {
            return VaultResult.Error("Invalid folder URI.")
        }

        return try {

            val rootId =
                DocumentsContract.getTreeDocumentId(treeUri)

            val childrenUri =
                DocumentsContract.buildChildDocumentsUriUsingTree(
                    treeUri,
                    rootId
                )

            val files = mutableListOf<VaultFile>()

            resolver.query(
                childrenUri,
                projection,
                null,
                null,
                "${DocumentsContract.Document.COLUMN_DISPLAY_NAME} ASC"
            )?.use { cursor ->

                while (cursor.moveToNext()) {

                    val file = cursor.toVaultFile(treeUri)

                    if (file != null) {
                        files.add(file)
                    }
                }
            } ?: return VaultResult.Error(
                "Unable to read selected folder."
            )

            VaultResult.Success(files)

        } catch (exception: Exception) {

            VaultResult.Error(
                "Unable to load library files.",
                exception
            )
        }
    }

    fun searchFiles(
        treeUri: Uri,
        query: String
    ): VaultResult<List<VaultFile>> {

        return when (val result = listFiles(treeUri)) {

            is VaultResult.Success -> {
                val filtered = result.data.filter {
                    it.name.contains(
                        query.trim(),
                        ignoreCase = true
                    )
                }

                VaultResult.Success(filtered)
            }

            is VaultResult.Error -> result

            VaultResult.Loading -> VaultResult.Loading
        }
    }

    fun renameFile(
        fileUri: Uri,
        newName: String
    ): VaultResult<Uri> {

        val name = newName.trim()

        if (name.isBlank()) {
            return VaultResult.Error("File name cannot be empty.")
        }

        if (
            name.length > 180 ||
            name.any {
                it.isISOControl() || it in "/\\"
            }
        ) {
            return VaultResult.Error("Invalid file name.")
        }

        return try {

            val renamedUri =
                DocumentsContract.renameDocument(
                    resolver,
                    fileUri,
                    name
                )

            if (renamedUri != null) {
                VaultResult.Success(renamedUri)
            } else {
                VaultResult.Error("Unable to rename file.")
            }

        } catch (exception: Exception) {

            VaultResult.Error(
                "File rename failed.",
                exception
            )
        }
    }

    fun deleteFile(fileUri: Uri): VaultResult<Unit> {

        return try {

            val deleted =
                DocumentsContract.deleteDocument(
                    resolver,
                    fileUri
                )

            if (deleted) {
                VaultResult.Success(Unit)
            } else {
                VaultResult.Error("Unable to delete file.")
            }

        } catch (exception: Exception) {

            VaultResult.Error(
                "File deletion failed.",
                exception
            )
        }
    }

    private fun Cursor.toVaultFile(
        treeUri: Uri
    ): VaultFile? {

        val documentId = getString(
            getColumnIndexOrThrow(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID
            )
        ) ?: return null

        val name = getString(
            getColumnIndexOrThrow(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            )
        ) ?: "Unnamed"

        val mimeType = getString(
            getColumnIndexOrThrow(
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )
        )

        val sizeIndex = getColumnIndex(
            DocumentsContract.Document.COLUMN_SIZE
        )

        val modifiedIndex = getColumnIndex(
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        val size = if (
            sizeIndex >= 0 && !isNull(sizeIndex)
        ) {
            getLong(sizeIndex)
        } else {
            0L
        }

        val modified = if (
            modifiedIndex >= 0 && !isNull(modifiedIndex)
        ) {
            getLong(modifiedIndex)
        } else {
            0L
        }

        val isDirectory =
            mimeType == DocumentsContract.Document.MIME_TYPE_DIR

        val uri =
            DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                documentId
            )

        return VaultFile(
            uri = uri,
            name = name,
            mimeType = mimeType,
            sizeBytes = size,
            lastModified = modified,
            isDirectory = isDirectory
        )
    }
}
