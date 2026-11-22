
package com.vaultiq.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultiq.app.ui.theme.VaultBorder
import com.vaultiq.app.ui.theme.VaultGold
import com.vaultiq.app.ui.theme.VaultIQTheme
import com.vaultiq.app.ui.theme.VaultMuted
import com.vaultiq.app.ui.theme.VaultNavy
import com.vaultiq.app.ui.theme.VaultPanel
import com.vaultiq.app.ui.theme.VaultSilver
import com.vaultiq.app.ui.theme.VaultSuccess
import com.vaultiq.app.ui.theme.VaultWhite

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VaultIQTheme {
                VaultHomeScreen()
            }
        }
    }
}

@Composable
private fun VaultHomeScreen() {

    val context = LocalContext.current
    var mediaUrl by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = VaultNavy
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {

            // Header

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "V",
                            color = VaultGold,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black
                        )

                        Text(
                            text = "AULTIQ",
                            color = VaultWhite,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }

                    Text(
                        text = "CAPTURE. COLLECT. ELEVATE.",
                        color = VaultMuted,
                        fontSize = 9.sp,
                        letterSpacing = 1.5.sp
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = VaultPanel,
                    modifier = Modifier.size(44.dp)
                ) {
                    androidx.compose.foundation.layout.Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "V",
                            color = VaultGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(34.dp))

            // Welcome

            Text(
                text = "YOUR MEDIA.",
                color = VaultSilver,
                fontSize = 13.sp,
                letterSpacing = 2.sp
            )

            Text(
                text = "Your personal\nmedia vault.",
                color = VaultWhite,
                fontSize = 34.sp,
                lineHeight = 41.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Collect and organize media you own or have permission to save.",
                color = VaultMuted,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // URL input card

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = VaultPanel,
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    VaultBorder
                )
            ) {

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Text(
                        text = "ADD TO YOUR VAULT",
                        color = VaultGold,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = mediaUrl,
                        onValueChange = { mediaUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Paste a supported media URL",
                                color = VaultMuted,
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VaultGold,
                            unfocusedBorderColor = VaultBorder,
                            focusedTextColor = VaultWhite,
                            unfocusedTextColor = VaultWhite,
                            cursorColor = VaultGold
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val url = mediaUrl.trim()

                            if (url.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "Enter a media URL first.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else if (
                                !url.startsWith("https://") &&
                                !url.startsWith("http://")
                            ) {
                                Toast.makeText(
                                    context,
                                    "Enter a valid HTTP or HTTPS URL.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Download engine is coming in the next phase.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VaultGold,
                            contentColor = VaultNavy
                        )
                    ) {
                        Text(
                            text = "ADD TO VAULT",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Overview

            SectionTitle("YOUR OVERVIEW")

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = "0",
                    label = "SAVED FILES"
                )

                StatCard(
                    modifier = Modifier.weight(1f),
                    value = "0",
                    label = "IN QUEUE"
                )

                StatCard(
                    modifier = Modifier.weight(1f),
                    value = "0 GB",
                    label = "STORAGE USED"
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Library

            SectionTitle("YOUR LIBRARY")

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = VaultPanel,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    VaultBorder
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "▤",
                        color = VaultGold,
                        fontSize = 35.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Your vault is waiting.",
                        color = VaultWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Your saved photos and videos will appear here.",
                        color = VaultMuted,
                        fontSize = 12.sp,
                        lineHeight = 19.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Footer

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VAULTIQ",
                    color = VaultGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "  •  YOUR PERSONAL MEDIA VAULT",
                    color = VaultMuted,
                    fontSize = 9.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = VaultSilver,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp
    )
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String
) {
    Surface(
        modifier = modifier,
        color = VaultPanel,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            VaultBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(13.dp)
        ) {
            Text(
                text = value,
                color = VaultGold,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = label,
                color = VaultMuted,
                fontSize = 8.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}
