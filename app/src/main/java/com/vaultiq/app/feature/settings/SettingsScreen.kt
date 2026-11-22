
package com.vaultiq.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultiq.app.library.StorageAccessManager
import com.vaultiq.app.library.StorageSettingsSection
import com.vaultiq.app.ui.designsystem.VaultInfoCard
import com.vaultiq.app.ui.theme.VaultMuted
import com.vaultiq.app.ui.theme.VaultNavy
import com.vaultiq.app.ui.theme.VaultWhite

@Composable
fun SettingsScreen() {
    val context = LocalContext.current

    val storageAccessManager = remember(context) {
        StorageAccessManager(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultNavy)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "SETTINGS",
            color = VaultWhite,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "GENERAL",
            color = VaultMuted,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp
        )

        VaultInfoCard(
            title = "VAULTIQ",
            description = "CAPTURE. COLLECT. ELEVATE. — Your personal media vault.",
            symbol = "V"
        )

        Text(
            text = "STORAGE",
            color = VaultMuted,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp
        )

        StorageSettingsSection(
            storageAccessManager = storageAccessManager
        )
    }
}
