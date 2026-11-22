
package com.vaultiq.app.library

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultiq.app.core.common.VaultResult

@Composable
fun StorageSettingsSection(
    storageAccessManager: StorageAccessManager,
    modifier: Modifier = Modifier
) {
    var selectedFolder by remember(storageAccessManager) {
        mutableStateOf(storageAccessManager.getSelectedFolder())
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    val folderPicker = rememberStorageFolderPicker(
        storageAccessManager = storageAccessManager
    ) { result ->

        when (result) {
            null -> {
                message = "Folder selection cancelled."
            }

            is VaultResult.Success -> {
                selectedFolder = result.data
                message = "Custom storage folder connected."
            }

            is VaultResult.Error -> {
                message = result.message
            }

            is VaultResult.Loading -> {
                message = "Please wait..."
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "STORAGE",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Choose where VAULTIQ should store your downloads.",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = if (selectedFolder != null) {
                    "Current location: Custom folder"
                } else {
                    "Current location: Private app storage"
                },
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = if (selectedFolder != null) {
                    "Your selected folder access is available."
                } else {
                    "Downloads are stored privately inside VAULTIQ."
                },
                style = MaterialTheme.typography.bodySmall
            )

            Button(
                onClick = {
                    message = null
                    folderPicker()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Choose Custom Folder")
            }

            if (selectedFolder != null) {
                OutlinedButton(
                    onClick = {
                        when (
                            val result =
                                storageAccessManager.releaseSelectedFolder()
                        ) {
                            is VaultResult.Success -> {
                                selectedFolder = null
                                message = "Returned to private app storage."
                            }

                            is VaultResult.Error -> {
                                message = result.message
                            }

                            is VaultResult.Loading -> {
                                message = "Please wait..."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset to Default Storage")
                }
            }

            message?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
