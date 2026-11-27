package com.techenglish.app.feature.stage4b

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.navigationBarContentPadding
import com.techenglish.app.feature.stage3.OfflineModeBanner
import com.techenglish.domain.model.DictionaryPrStatus

// =========================================================================
// 2. Squads Screen (ФТ-14.4: 5–10 devs, Weekly Goal, Non-Ranked Bonus)
// =========================================================================

@Composable
fun SquadsScreen(
    onBack: () -> Unit,
    viewModel: SquadsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    SquadsContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onCreateSquad = viewModel::createSquad,
        onJoinSquad = viewModel::joinSquad,
        onLeaveSquad = viewModel::leaveSquad
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SquadsContent(
    state: SquadsUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onCreateSquad: (String) -> Unit,
    onJoinSquad: (String) -> Unit,
    onLeaveSquad: () -> Unit
) {
    var squadName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Учебные отряды (Squads)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Обновить")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentPadding = navigationBarContentPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isOffline) {
                item { OfflineModeBanner("Офлайн-режим: управление Squad требует подключения к сети.") }
            }

            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text(
                            text = "Squad: 5–10 разработчиков, общая недельная цель XP. Множитель следующей недели НЕ влияет на рейтинговый XP лидербордов (ФТ-9.3).",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            val squad = state.squad
            if (squad == null) {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Создать новый Squad", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(
                                value = squadName,
                                onValueChange = { squadName = it },
                                label = { Text("Название отряда (например, Kotlin Guild)") },
                                supportingText = if (squadName.trim().length < 3) {
                                    { Text("Минимум 3 символа") }
                                } else null,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    onCreateSquad(squadName)
                                    squadName = ""
                                },
                                enabled = squadName.trim().length >= 3 && !state.isBusy,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Создать отряд") }

                            HorizontalDivider()

                            Text("Или вступить по коду приглашения", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(
                                value = inviteCode,
                                onValueChange = { inviteCode = it },
                                label = { Text("Код приглашения (SQ-XXXXXX)") },
                                supportingText = if (inviteCode.trim().length < 4) {
                                    { Text("Введите код от 4 символов") }
                                } else null,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            FilledTonalButton(
                                onClick = {
                                    onJoinSquad(inviteCode)
                                    inviteCode = ""
                                },
                                enabled = inviteCode.trim().length >= 4 && !state.isBusy,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Вступить в Squad") }
                        }
                    }
                }
            } else {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(squad.name, style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        text = "Код: ${squad.inviteCode} • Участников: ${squad.memberCount}/10",
                                        style = MonoTagTextStyle,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = "${squad.nextWeekBonusMultiplier}x",
                                    style = MonoTagTextStyle,
                                    fontWeight = FontWeight.Bold,
                                    color = TechColors.EmeraldDark
                                )
                            }
                            LinearProgressIndicator(
                                progress = { squad.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                            )
                            Text(
                                text = "Недельная цель: ${squad.currentWeekXp} / ${squad.weeklyGoalXp} XP" +
                                    if (!squad.isActiveForChallenge) " (нужно ≥ 5 участников для активации)" else " • Челлендж активен",
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedButton(onClick = onLeaveSquad, modifier = Modifier.fillMaxWidth()) {
                                Text("Покинуть отряд")
                            }
                        }
                    }
                }

                items(squad.members, key = { it.userId }) { m ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("@${m.username}", style = MaterialTheme.typography.titleSmall)
                                Text(m.rankTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${m.weeklyXp} XP", style = MonoTagTextStyle, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 3. Dictionary Crowdsourcing «PR в словарь» Screen (ФТ-14.3)
// =========================================================================

@Composable
fun CrowdsourcePrScreen(
    onBack: () -> Unit,
    viewModel: CrowdsourcePrViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    CrowdsourcePrContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onRefresh = viewModel::refreshPrs,
        onSubmitPr = viewModel::submitPr,
        onVotePr = viewModel::votePr,
        onReportPr = viewModel::reportPr
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrowdsourcePrContent(
    state: CrowdsourcePrUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSubmitPr: (term: String, trRu: String, trTg: String, defRu: String, sourceRef: String, licenseAccepted: Boolean) -> Unit,
    onVotePr: (String, Int) -> Unit,
    onReportPr: (String, String) -> Unit
) {
    var term by remember { mutableStateOf("") }
    var trRu by remember { mutableStateOf("") }
    var trTg by remember { mutableStateOf("") }
    var defRu by remember { mutableStateOf("") }
    var sourceRef by remember { mutableStateOf("") }
    var licenseAccepted by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("PR в словарь (Краудсорсинг)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Обновить")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentPadding = navigationBarContentPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isOffline) {
                item { OfflineModeBanner("Офлайн-режим: отправка и голосование по PR в словарь доступны онлайн.") }
            }

            // 4-Gate Eligibility Checklist (ФТ-14.3, Audit #22 & §9)
            item {
                val elig = state.eligibility
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (elig.isEligible) Icons.Rounded.WorkspacePremium else Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = if (elig.isEligible) TechColors.EmeraldDark else TechColors.AmberDark
                            )
                            Text(
                                text = if (elig.isEligible) "Допуск к «PR в словарь» открыт" else "Условия допуска Contributor (ФТ-14.3)",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        EligibilityRow("Ранг Middle Developer и выше (≥ 600 XP)", elig.meetsRankMiddlePlus)
                        EligibilityRow("Возраст аккаунта ≥ 14 дней", elig.meetsAccountAge14Days)
                        EligibilityRow("Изучено ≥ 100 терминов", elig.meetsLearnedTerms100)
                        EligibilityRow("0 подтверждённых жалоб модерации", elig.meetsZeroConfirmedReports)
                    }
                }
            }

            // Submit PR Form with Mandatory Project License Notice
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Предложить правку или перевод (RU / TJ)", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = term,
                            onValueChange = { term = it },
                            label = { Text("Термин (EN)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = trRu,
                            onValueChange = { trRu = it },
                            label = { Text("Перевод (RU)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = trTg,
                            onValueChange = { trTg = it },
                            label = { Text("Перевод на таджикский (TJ, опционально)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = defRu,
                            onValueChange = { defRu = it },
                            label = { Text("Краткое определение (опционально)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = sourceRef,
                            onValueChange = { sourceRef = it },
                            label = { Text("Источник (HTTPS URL или RFC/IEEE/NIST/ISO)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { licenseAccepted = !licenseAccepted }
                        ) {
                            Checkbox(checked = licenseAccepted, onCheckedChange = { licenseAccepted = it })
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Я соглашаюсь на публикацию вклада под открытой лицензией проекта (CC BY-SA 4.0) для получения бейджа Contributor.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (!state.eligibility.isEligible || !licenseAccepted || term.isBlank()) {
                            Text(
                                text = when {
                                    !state.eligibility.isEligible -> "Доступно после выполнения 4 условий Contributor выше"
                                    term.isBlank() -> "Укажите английский термин для PR"
                                    else -> "Подтвердите лицензию CC BY-SA 4.0 для отправки"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                onSubmitPr(term, trRu, trTg, defRu, sourceRef, licenseAccepted)
                                term = ""
                                trRu = ""
                                trTg = ""
                                defRu = ""
                                sourceRef = ""
                            },
                            enabled = state.eligibility.isEligible && licenseAccepted && term.isNotBlank() && !state.isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Отправить PR в словарь")
                        }
                    }
                }
            }

            item {
                Text("Заявки сообщества (${state.prs.size})", style = MaterialTheme.typography.titleLarge)
            }

            items(state.prs, key = { it.id }) { pr ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${pr.term} • ${pr.status.labelRu}", style = MaterialTheme.typography.titleSmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilledTonalButton(
                                    onClick = { onVotePr(pr.id, 1) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Поддержать PR", modifier = Modifier.size(16.dp))
                                    Text("${pr.upvotes}")
                                }
                                IconButton(onClick = { onReportPr(pr.id, "Недостоверный источник или спам") }) {
                                    Icon(Icons.Rounded.Flag, contentDescription = "Пожаловаться на PR", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        if (pr.proposedTranslationRu.isNotBlank()) {
                            Text("RU: ${pr.proposedTranslationRu}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (pr.proposedTranslationTg.isNotBlank()) {
                            Text("TJ: ${pr.proposedTranslationTg}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            text = "@${pr.authorUsername} • Источник: ${pr.sourceReference}",
                            style = MonoTagTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EligibilityRow(label: String, met: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (met) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
            contentDescription = null,
            tint = if (met) TechColors.EmeraldDark else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (met) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
