package com.techenglish.app.feature.stage2

import androidx.compose.runtime.getValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.ChameleonScenarioContainer
import com.techenglish.app.core.designsystem.ComboStreakBanner
import com.techenglish.app.core.designsystem.JetBrainsMonoFontFamily
import com.techenglish.app.core.designsystem.rememberTechHaptics
import com.techenglish.domain.model.ChameleonThemeType

// =========================================================================
// ФТ-7.2 — Interactive «Debug the Log» Trainer Screen
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DebugTheLogScreen(
    onBack: () -> Unit,
    viewModel: DebugTheLogViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = rememberTechHaptics()
    val challenge = state.currentChallenge

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug the Log · Тренажёр") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        if (challenge == null) return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ComboStreakBanner(comboState = state.comboStreak)

            ChameleonScenarioContainer(
                themeType = ChameleonThemeType.TERMINAL,
                headerTitle = challenge.stackKind,
                headerSubtitle = "Задача #${challenge.id} из ${state.challenges.size}"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = challenge.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = Color(0xFF0D1117),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = challenge.rawLog,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = Color(0xFFFF7B72),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            Text(
                text = challenge.rootCauseQuestionRu,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            challenge.rootCauseOptions.forEach { option ->
                val isSelected = state.selectedRootCause == option
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable(enabled = state.evaluation == null) {
                            haptics.onWordTap()
                            viewModel.selectRootCause(option)
                        }
                ) {
                    Text(
                        text = option,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Text(
                text = challenge.reportPromptRu,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
            ) {
                Text(
                    text = state.selectedReportTokens.joinToString(" ").ifBlank {
                        "Нажимайте на фразы ниже, чтобы собрать баг-репорт для Jira / GitHub..."
                    },
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                challenge.reportTokens.forEach { token ->
                    val picked = token in state.selectedReportTokens
                    FilterChip(
                        selected = picked,
                        onClick = {
                            haptics.onWordTap()
                            viewModel.toggleReportToken(token)
                        },
                        label = { Text(token) },
                        enabled = state.evaluation == null
                    )
                }
            }

            val eval = state.evaluation
            if (eval == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = viewModel::clearReportTokens,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Очистить")
                    }
                    Button(
                        onClick = {
                            haptics.onWordTap()
                            viewModel.submitDiagnosis()
                        },
                        enabled = state.selectedRootCause != null && state.selectedReportTokens.isNotEmpty(),
                        modifier = Modifier.weight(2f)
                    ) {
                        Icon(Icons.Rounded.BugReport, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Проверить диагноз")
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (eval.isFullySolved) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (eval.isFullySolved) {
                                "Отличный диагноз! +${eval.xpEarned} XP"
                            } else {
                                "Частичный результат (+${eval.xpEarned} XP)"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Эталонный баг-репорт: ${eval.expectedReportEn}",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(text = eval.explanationRu, style = MaterialTheme.typography.bodyMedium)
                        Button(
                            onClick = {
                                haptics.onWordTap()
                                viewModel.nextChallenge()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Следующий стектрейс →")
                        }
                    }
                }
            }
        }
    }
}

