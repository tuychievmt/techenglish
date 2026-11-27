package com.techenglish.app.feature.stage3

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.techenglish.app.core.designsystem.AuroraScreenContainer
import com.techenglish.app.core.designsystem.BentoGlassCard
import com.techenglish.app.core.designsystem.DEFAULT_ACADEMIC_TERMS_COUNT
import com.techenglish.app.core.designsystem.LocalTechInteractionConfig
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.formatCountSpaced

// =========================================================================
// 0. 16+ Age Gate & Privacy Policy (ФТ-9.4)
// =========================================================================

@Composable
fun AgeGate16PlusScreen(
    onConfirm16Plus: () -> Unit
) {
    val isDark = LocalTechInteractionConfig.current.isDarkTheme
    var checked16Plus by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    AuroraScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.size(84.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CodeSlashEmblem(
                        startColor = if (isDark) TechColors.Cyan else MaterialTheme.colorScheme.primary,
                        endColor = if (isDark) TechColors.Violet else Color(0xFF4338CA),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = "TECHENGLISH · OFFLINE-FIRST",
                style = MonoTagTextStyle,
                color = if (isDark) TechColors.Cyan else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Добро пожаловать в TechEnglish",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Платформа технического английского для разработчиков (Offline-First + опциональная облачная синхронизация). " +
                    "Социальные функции, публичный профиль и лидерборды предназначены для аудитории 16+.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))

            BentoGlassCard(onClick = { checked16Plus = !checked16Plus }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked16Plus,
                        onCheckedChange = { checked16Plus = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Мне уже исполнилось 16 лет, и я ознакомлен(а) с Политикой конфиденциальности",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            TextButton(onClick = { showPrivacyDialog = true }) {
                Icon(Icons.Rounded.Policy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Открыть Политику конфиденциальности")
            }

            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onConfirm16Plus,
                enabled = checked16Plus,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Продолжить в гостевом режиме (16+)")
            }
        }
    }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
    }
}

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Политика конфиденциальности (16+)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "1. Offline-First по умолчанию: все ${formatCountSpaced(DEFAULT_ACADEMIC_TERMS_COUNT)} терминов, карточки SM-2 и локальный прогресс хранятся на устройстве в SQLite/Room.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "2. Возраст 16+: вход в облачный аккаунт, лидерборды и друзья доступны пользователям старше 16 лет.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "3. Безопасность токенов: JWT-токены шифруются через Android Keystore (AES-256-GCM) и передаются строго по HTTPS (TLS 1.2+).",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "4. Голосовые записи (Errata E5 / DEC-13): диктовка произношения и AI-интервью обрабатывается локально (0 дней хранения). Голосовые ответы в Peer Review Arena (≤ 20 с) хранятся в S3/MinIO не более 30 дней (TTL) и полностью удаляются при удалении аккаунта.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "5. Удаление аккаунта: вы можете полностью удалить аккаунт, облачные данные и голосовые записи прямо из экрана «Облако и синхронизация» или через веб-форму /v1/account/delete-form.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Понятно") }
        }
    )
}
