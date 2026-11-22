
package com.vaultiq.app.library

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.vaultiq.app.core.common.VaultResult

@Composable
fun rememberStorageFolderPicker(
    storageAccessManager: StorageAccessManager,
    onResult: (VaultResult<Uri>?) -> Unit
): () -> Unit {

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->

        if (uri == null) {
            onResult(null)
        } else {

            val flags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            val result =
                storageAccessManager.persistSelectedFolder(
                    uri = uri,
                    intentFlags = flags
                )

            onResult(result)
        }
    }

    return {
        launcher.launch(null)
    }
}
