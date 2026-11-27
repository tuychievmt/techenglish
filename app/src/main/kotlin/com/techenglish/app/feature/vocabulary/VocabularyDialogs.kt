package com.techenglish.app.feature.vocabulary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.techenglish.app.core.designsystem.AudioEqualizerButton
import com.techenglish.app.core.designsystem.HighlightedExampleQuoteCard
import com.techenglish.app.core.designsystem.IpaTextStyle
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.PracticalExplanationCard
import com.techenglish.app.core.designsystem.StatusBadge
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.TermDisplayTextStyle
import com.techenglish.app.core.designsystem.TranslationContainerCard
import com.techenglish.domain.model.Category
import com.techenglish.domain.model.ProgrammingLanguage
import com.techenglish.domain.model.Word
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val reviewDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

@Composable
internal fun WordDetailSheetContent(
    word: Word,
    onSpeakTerm: () -> Unit,
    onSpeakExample: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(word.term, style = TermDisplayTextStyle)
                Text(
                    word.transcription,
                    style = IpaTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AudioEqualizerButton(onClick = onSpeakTerm, contentDescription = "Произнести", size = 40.dp)
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (word.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = "В избранное",
                        tint = if (word.isFavorite) TechColors.Amber else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusBadge(text = word.categoryName)
            word.languageName?.let { StatusBadge(text = it) }
            if (word.isCustom) StatusBadge(text = "Пользовательское")
        }

        TranslationContainerCard(translationText = word.translation)

        PracticalExplanationCard(
            title = "ПОЯСНЕНИЕ",
            explanationText = word.explanationRu
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Пример в контексте", style = MaterialTheme.typography.labelLarge)
            TextButton(onClick = onSpeakExample) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Озвучить")
            }
        }
        HighlightedExampleQuoteCard(
            exampleEn = word.example,
            targetTerm = word.term
        )

        HorizontalDivider()

        val nextReviewLabel = if (word.nextReviewAt <= System.currentTimeMillis()) {
            "Доступно сейчас"
        } else {
            Instant.ofEpochMilli(word.nextReviewAt).atZone(ZoneId.systemDefault()).format(reviewDateFormatter)
        }
        Text(
            text = "SM-2: повторений ${word.repetitions} • EF ${"%.2f".format(word.easeFactor)} • интервал ${word.intervalDays} дн. • след. показ: $nextReviewLabel",
            style = MonoTagTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (onEdit != null || onDelete != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                onEdit?.let {
                    FilledTonalButton(onClick = it, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Редактировать")
                    }
                }
                onDelete?.let {
                    TextButton(onClick = it, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Удалить", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomWordEditorDialog(
    editingWord: Word?,
    categories: List<Category>,
    languages: List<ProgrammingLanguage>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, Int?, Int?) -> Unit
) {
    var term by remember { mutableStateOf(editingWord?.term ?: "") }
    var transcription by remember { mutableStateOf(editingWord?.transcription ?: "") }
    var translation by remember { mutableStateOf(editingWord?.translation ?: "") }
    var example by remember { mutableStateOf(editingWord?.example ?: "") }
    var explanationRu by remember { mutableStateOf(editingWord?.explanationRu ?: "") }

    val defaultCatId = editingWord?.categoryId ?: categories.firstOrNull { it.isSystem }?.id ?: 11
    var selectedCategoryId by remember { mutableStateOf(defaultCatId) }
    var selectedLanguageId by remember { mutableStateOf(editingWord?.languageId) }
    var catExpanded by remember { mutableStateOf(false) }
    var langExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingWord == null) "Новый термин" else "Редактировать термин") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = term,
                    onValueChange = { term = it },
                    label = { Text("English Term *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = transcription,
                    onValueChange = { transcription = it },
                    label = { Text("Транскрипция (IPA)") },
                    placeholder = { Text("/ˈɪn.deks/") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = translation,
                    onValueChange = { translation = it },
                    label = { Text("Перевод на русский *") },
                    singleLine = true,
                    supportingText = if (term.isBlank() || translation.isBlank()) {
                        { Text("Заполните термин и перевод") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = example,
                    onValueChange = { example = it },
                    label = { Text("Пример употребления (EN)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = explanationRu,
                    onValueChange = { explanationRu = it },
                    label = { Text("Пояснение (RU)") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = !catExpanded }
                ) {
                    OutlinedTextField(
                        value = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Мои слова",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Категория") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = langExpanded,
                    onExpandedChange = { langExpanded = !langExpanded }
                ) {
                    OutlinedTextField(
                        value = languages.firstOrNull { it.id == selectedLanguageId }?.name ?: "Не указан",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Язык программирования (опционально)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = langExpanded,
                        onDismissRequest = { langExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Не указан") },
                            onClick = {
                                selectedLanguageId = null
                                langExpanded = false
                            }
                        )
                        languages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang.name) },
                                onClick = {
                                    selectedLanguageId = lang.id
                                    langExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        term,
                        transcription,
                        translation,
                        example,
                        explanationRu,
                        selectedCategoryId,
                        selectedLanguageId
                    )
                },
                enabled = term.isNotBlank() && translation.isNotBlank()
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
