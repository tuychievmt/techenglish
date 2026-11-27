package com.techenglish.app.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ProfileNameDialog(
    initialName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Имя профиля") },
        text = {
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Как к вам обращаться?") },
                supportingText = if (nameInput.isBlank()) {
                    { Text("Введите имя профиля") }
                } else null,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(nameInput.trim())
                    onDismiss()
                },
                enabled = nameInput.isNotBlank()
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
internal fun ProfileAboutLicensesDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("О приложении TechEnglish") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "TechEnglish — платформа изучения технического и академического английского языка для разработчиков (Offline-First).",
                    style = MaterialTheme.typography.bodyMedium
                )
                HorizontalDivider()
                Text(
                    text = "Лицензии встроенных шрифтов (res/font):",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "• Inter (Copyright © The Inter Project Authors) — распространяется по лицензии SIL Open Font License, Version 1.1 (OFL-1.1).",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• JetBrains Mono (Copyright © JetBrains s.r.o.) — распространяется по лицензии SIL Open Font License, Version 1.1 (OFL-1.1).",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Словарные источники: стандарты NIST, IETF RFC, IEEE, ISO/IEC, OWASP и официальная документация языков и СУБД.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}
