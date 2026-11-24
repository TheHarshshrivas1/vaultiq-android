
package com.vaultiq.app.feature.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultiq.app.core.common.VaultResult
import com.vaultiq.app.ui.theme.VaultGold
import com.vaultiq.app.ui.theme.VaultMuted
import com.vaultiq.app.ui.theme.VaultNavy
import com.vaultiq.app.ui.theme.VaultPanel
import com.vaultiq.app.ui.theme.VaultSilver
import com.vaultiq.app.ui.theme.VaultWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PlayScreen() {
    var selectedCategory by rememberSaveable {
        mutableStateOf("Videos")
    }

    var isImporting by remember {
        mutableStateOf(false)
    }

    var isLoadingLibrary by remember {
        mutableStateOf(false)
    }

    var importMessage by remember {
        mutableStateOf<String?>(null)
    }

    var importIsError by remember {
        mutableStateOf(false)
    }

    var libraryMessage by remember {
        mutableStateOf<String?>(null)
    }

    var allFiles by remember {
        mutableStateOf<List<VaultFile>>(emptyList())
    }

    var refreshKey by remember {
        mutableStateOf(0)
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
        isLoadingLibrary = true
        libraryMessage = null

        try {
            when (
                val result = withContext(Dispatchers.IO) {
                    LibraryManager(context).listAllFiles()
                }
            ) {
                is VaultResult.Success -> {
                    allFiles = result.data.filterNot {
                        it.isDirectory
                    }
                }

                is VaultResult.Error -> {
                    libraryMessage = result.message
                }

                is VaultResult.Loading -> {
                    libraryMessage = "Library is still loading."
                }
            }
        } catch (exception: Exception) {
            libraryMessage =
                exception.message ?: "Unable to load library."
        } finally {
            isLoadingLibrary = false
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && !isImporting) {
            scope.launch {
                isImporting = true
                importMessage = null
                importIsError = false

                try {
                    when (
                        val result = MediaImportManager(context)
                            .importMedia(uri)
                    ) {
                        is VaultResult.Success -> {
                            val imported = result.data

                            selectedCategory = getMediaCategory(
                                imported.mimeType,
                                imported.fileName
                            )

                            importMessage =
                                "Imported: ${imported.fileName}\n" +
                                "Size: ${formatImportSize(imported.sizeBytes)}"

                            refreshKey++
                        }

                        is VaultResult.Error -> {
                            importIsError = true
                            importMessage = result.message
                        }

                        is VaultResult.Loading -> {
                            importIsError = true
                            importMessage = "Import is still processing."
                        }
                    }
                } catch (
                    exception: kotlinx.coroutines.CancellationException
                ) {
                    throw exception
                } catch (exception: Exception) {
                    importIsError = true
                    importMessage =
                        exception.message ?: "Media import failed."
                } finally {
                    isImporting = false
                }
            }
        }
    }

    val categories = listOf(
        "Videos",
        "Music",
        "Images",
        "Files"
    )

    val visibleFiles = allFiles.filter { file ->
        getMediaCategory(file.mimeType, file.name) == selectedCategory
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultNavy)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 20.dp)
    ) {
        Text(
            text = "PLAY",
            color = VaultWhite,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "YOUR PERSONAL MEDIA VAULT",
            color = VaultMuted,
            fontSize = 10.sp,
            letterSpacing = 1.4.sp
        )

        Spacer(modifier = Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                CategoryChip(
                    title = category,
                    selected = selectedCategory == category,
                    onClick = {
                        selectedCategory = category
                        importMessage = null
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Button(
            onClick = {
                filePicker.launch(
                    arrayOf("video/*", "audio/*", "image/*")
                )
            },
            enabled = !isImporting,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VaultGold,
                contentColor = VaultNavy,
                disabledContainerColor = VaultPanel,
                disabledContentColor = VaultMuted
            )
        ) {
            if (isImporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = VaultNavy,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.size(9.dp))
                Text("IMPORTING MEDIA")
            } else {
                Text(
                    text = "＋  IMPORT FROM PHONE",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Your imported and downloaded media appears here.",
            color = VaultMuted,
            fontSize = 10.sp
        )

        if (importMessage != null) {
            Spacer(modifier = Modifier.height(14.dp))

            MessageCard(
                message = importMessage!!,
                isError = importIsError
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = selectedCategory.uppercase(),
                color = VaultWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${visibleFiles.size} ITEMS",
                color = VaultMuted,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            isLoadingLibrary -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 35.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = VaultGold,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            libraryMessage != null -> {
                MessageCard(
                    message = libraryMessage!!,
                    isError = true
                )
            }

            visibleFiles.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 35.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "▣",
                        color = VaultGold,
                        fontSize = 40.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "No ${selectedCategory.lowercase()} yet",
                        color = VaultWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Import media or download a supported file.",
                        color = VaultMuted,
                        fontSize = 11.sp
                    )
                }
            }

            else -> {
                visibleFiles.forEach { file ->
                    LibraryFileCard(file = file)
                    Spacer(modifier = Modifier.height(9.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(25.dp))
    }
}

@Composable
private fun LibraryFileCard(file: VaultFile) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = VaultPanel,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (getMediaCategory(file.mimeType, file.name)) {
                    "Videos" -> "▶"
                    "Music" -> "♫"
                    "Images" -> "▧"
                    else -> "▤"
                },
                color = VaultGold,
                fontSize = 23.sp
            )

            Spacer(modifier = Modifier.size(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.name,
                    color = VaultWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = formatImportSize(file.sizeBytes),
                    color = VaultMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun MessageCard(
    message: String,
    isError: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = VaultPanel,
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            color = if (isError) VaultSilver else VaultGold,
            fontSize = 11.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun CategoryChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) VaultGold else VaultPanel
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 9.dp
            ),
            color = if (selected) VaultNavy else VaultSilver,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun getMediaCategory(
    mimeType: String?,
    fileName: String
): String {
    val mime = mimeType?.lowercase().orEmpty()
    val extension = fileName.substringAfterLast(
        ".",
        ""
    ).lowercase()

    return when {
        mime.startsWith("video/") ||
            extension in setOf("mp4", "mkv", "webm", "mov", "avi", "3gp") ->
            "Videos"

        mime.startsWith("audio/") ||
            extension in setOf("mp3", "m4a", "aac", "wav", "ogg", "flac") ->
            "Music"

        mime.startsWith("image/") ||
            extension in setOf("jpg", "jpeg", "png", "webp", "gif", "heic") ->
            "Images"

        else -> "Files"
    }
}

private fun formatImportSize(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"

    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = -1

    while (value >= 1024.0 && index < units.lastIndex) {
        value /= 1024.0
        index++
    }

    return String.format(
        java.util.Locale.US,
        "%.2f %s",
        value,
        units[index]
    )
}
