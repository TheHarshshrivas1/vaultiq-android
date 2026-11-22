
package com.vaultiq.app.ui.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultiq.app.ui.theme.VaultGold
import com.vaultiq.app.ui.theme.VaultMuted
import com.vaultiq.app.ui.theme.VaultPanel
import com.vaultiq.app.ui.theme.VaultPanelLight
import com.vaultiq.app.ui.theme.VaultSilver
import com.vaultiq.app.ui.theme.VaultWhite

@Composable
fun VaultSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = title,
            color = VaultWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp
        )

        if (subtitle != null) {
            Text(
                text = subtitle,
                color = VaultMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun VaultInfoCard(
    title: String,
    description: String,
    symbol: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = VaultPanel
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(43.dp),
                shape = RoundedCornerShape(13.dp),
                color = VaultPanelLight
            ) {
                androidx.compose.foundation.layout.Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = symbol,
                        color = VaultGold,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.size(14.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    color = VaultWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = description,
                    color = VaultMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun VaultEmptyState(
    title: String,
    description: String,
    symbol: String = "▣",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text(
            text = symbol,
            color = VaultGold,
            fontSize = 38.sp
        )

        Text(
            text = title,
            color = VaultWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Text(
            text = description,
            color = VaultMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun VaultPill(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50.dp),
        color = VaultGold.copy(alpha = 0.13f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 7.dp
            ),
            color = VaultGold,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}
