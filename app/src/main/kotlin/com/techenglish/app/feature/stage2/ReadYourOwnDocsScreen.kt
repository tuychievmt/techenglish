package com.techenglish.app.feature.stage2

import androidx.compose.runtime.getValue
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.JetBrainsMonoFontFamily
import com.techenglish.app.core.designsystem.TranslationContainerCard
import com.techenglish.app.core.designsystem.rememberTechHaptics

// =========================================================================
// ФТ-7.1 — Read Your Own Docs Screen
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadYourOwnDocsScreen(
    onBack: () -> Unit,
    viewModel: ReadYourOwnDocsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = rememberTechHaptics()

    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = resolveDisplayName(context, uri) ?: "imported_doc.md"
            viewModel.importFromUri(context.contentResolver, uri, fileName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.activeDoc?.title ?: "Read Your Own Docs",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (state.activeDoc != null) viewModel.closeActiveDoc() else onBack()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            state.statusMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            if (state.isScanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                Text(
                    text = "Сканируем n-граммы по словарю 20 658 терминов...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val activeDoc = state.activeDoc
            if (activeDoc != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Найдено терминов: ${activeDoc.detectedTermsCount} (нажмите на выделенный термин)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    val highlightBg = MaterialTheme.colorScheme.primaryContainer
                    val highlightFg = MaterialTheme.colorScheme.primary
                    val annotated = remember(activeDoc.content, activeDoc.detectedTerms, highlightBg, highlightFg) {
                        buildAnnotatedString {
                            append(activeDoc.content)
                            activeDoc.detectedTerms.forEachIndexed { idx, term ->
                                if (term.startOffset in 0..activeDoc.content.length &&
                                    term.endOffset in term.startOffset..activeDoc.content.length
                                ) {
                                    addStyle(
                                        style = SpanStyle(
                                            color = highlightFg,
                                            background = highlightBg.copy(alpha = 0.55f),
                                            fontWeight = FontWeight.SemiBold,
                                            textDecoration = TextDecoration.Underline
                                        ),
                                        start = term.startOffset,
                                        end = term.endOffset
                                    )
                                    addStringAnnotation(
                                        tag = "TERM_IDX",
                                        annotation = idx.toString(),
                                        start = term.startOffset,
                                        end = term.endOffset
                                    )
                                }
                            }
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        @Suppress("DEPRECATION")
                        ClickableText(
                            text = annotated,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 26.sp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            onClick = { offset ->
                                annotated.getStringAnnotations("TERM_IDX", offset, offset)
                                    .firstOrNull()
                                    ?.item
                                    ?.toIntOrNull()
                                    ?.let { idx ->
                                        activeDoc.detectedTerms.getOrNull(idx)?.let {
                                            haptics.onWordTap()
                                            viewModel.selectTerm(it)
                                        }
                                    }
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Импортируйте README, .md или .txt файл",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Приложение офлайн найдёт и подсветит сложные IT-термины, покажет перевод и добавит их в SRS.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FilledTonalButton(
                                    onClick = { openDocLauncher.launch(arrayOf("text/*", "application/octet-stream")) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.FileOpen, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Открыть .md / .txt файл с устройства")
                                }
                                HorizontalDivider()
                                OutlinedTextField(
                                    value = state.draftTitle,
                                    onValueChange = viewModel::updateDraftTitle,
                                    label = { Text("Название документа") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = state.draftText,
                                    onValueChange = viewModel::updateDraftText,
                                    label = { Text("Или вставьте английский текст / README") },
                                    minLines = 4,
                                    maxLines = 8,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        haptics.onWordTap()
                                        viewModel.scanAndSaveDraft()
                                    },
                                    enabled = !state.isScanning && state.draftText.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.Description, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Просканировать термины офлайн")
                                }
                            }
                        }
                    }

                    if (state.savedDocs.isNotEmpty()) {
                        item {
                            Text(
                                text = "Сохранённые документы (${state.savedDocs.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        items(state.savedDocs, key = { it.id }) { doc ->
                            Card(
                                onClick = { viewModel.openSavedDoc(doc) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = doc.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${doc.sourceFileName} · Найдено терминов: ${doc.detectedTermsCount}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteDoc(doc.id) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Удалить документ")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val selectedTerm = state.selectedTerm
    if (selectedTerm != null) {
        ModalBottomSheet(onDismissRequest = { viewModel.selectTerm(null) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedTerm.headword,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedTerm.transcription.isNotBlank()) {
                            Text(
                                text = selectedTerm.transcription,
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.audioController.speakQuick(selectedTerm.headword) }) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Озвучить термин")
                    }
                }

                TranslationContainerCard(
                    translationText = selectedTerm.displayTranslation(state.translationLanguage),
                    secondaryTranslationText = "${selectedTerm.domain} · Язык: ${state.translationLanguage.shortLabel}"
                )

                if (selectedTerm.explanationRu.isNotBlank()) {
                    Text(
                        text = selectedTerm.explanationRu,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = {
                        haptics.onCorrectAnswer()
                        viewModel.addSelectedTermToSrs()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить в SRS-повторение")
                }
            }
        }
    }
}

