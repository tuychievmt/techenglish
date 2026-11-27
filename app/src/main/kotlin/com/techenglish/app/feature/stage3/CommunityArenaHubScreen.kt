package com.techenglish.app.feature.stage3

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.navigationBarContentPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityArenaHubScreen(
    onBack: () -> Unit,
    onOpenLeaderboards: () -> Unit,
    onOpenFriends: () -> Unit,
    onOpenCommunityDecks: () -> Unit,
    onOpenAiMentor: () -> Unit,
    onOpenSquads: () -> Unit,
    onOpenCrowdsourcePr: () -> Unit,
    onOpenThematicRooms: () -> Unit,
    onOpenPvpDuel: () -> Unit,
    onOpenPeerReview: () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Сообщество и Арена") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = navigationBarContentPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Арена, WebSockets и AI-практика", style = MaterialTheme.typography.titleMedium)
                        CommunityHubRow(
                            icon = Icons.Rounded.AutoAwesome,
                            tint = MaterialTheme.colorScheme.primary,
                            title = "AI-ментор (Интервью и PR Review)",
                            subtitle = "Через серверный прокси • Согласие, лимиты, без PII",
                            onClick = onOpenAiMentor
                        )
                        HorizontalDivider()
                        CommunityHubRow(
                            icon = Icons.Rounded.Bolt,
                            tint = TechColors.AmberDark,
                            title = "1v1 PvP-дуэли в реальном времени",
                            subtitle = "60 секунд • 8 серверных вопросов • тренировочный фолбэк 15 с",
                            onClick = onOpenPvpDuel
                        )
                        HorizontalDivider()
                        CommunityHubRow(
                            icon = Icons.Rounded.RateReview,
                            tint = TechColors.Rose,
                            title = "Peer Review Arena (Кейс дня)",
                            subtitle = "Текст или голос ≤ 20 с (TTL 30 дн. в S3/MinIO) и разбор от коллег",
                            onClick = onOpenPeerReview
                        )
                        HorizontalDivider()
                        CommunityHubRow(
                            icon = Icons.Rounded.Forum,
                            tint = TechColors.Indigo,
                            title = "Тематические комнаты (WebSockets)",
                            subtitle = "#code-review-practice, #daily-standup, #interviews, #backend-english",
                            onClick = onOpenThematicRooms
                        )
                    }
                }
            }

            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Рейтинги, друзья и вклад в словарь (16+)", style = MaterialTheme.typography.titleMedium)
                        CommunityHubRow(
                            icon = Icons.Rounded.EmojiEvents,
                            tint = TechColors.AmberDark,
                            title = "Лидерборды Top-10 и Лиги",
                            subtitle = "Неделя, Лига, Друзья, Стек • кэш работает офлайн",
                            onClick = onOpenLeaderboards
                        )
                        HorizontalDivider()
                        CommunityHubRow(
                            icon = Icons.Rounded.Group,
                            tint = TechColors.EmeraldDark,
                            title = "Друзья, QR-код и лента",
                            subtitle = "Поиск по @username, пинги 1/24ч, реакции и модерация",
                            onClick = onOpenFriends
                        )
                        HorizontalDivider()
                        CommunityHubRow(
                            icon = Icons.Rounded.Groups,
                            tint = TechColors.Sky,
                            title = "Учебные отряды (Squads 5–10)",
                            subtitle = "Недельная цель XP и нерейтинговый бонус-множитель",
                            onClick = onOpenSquads
                        )
                        HorizontalDivider()
                        CommunityHubRow(
                            icon = Icons.AutoMirrored.Rounded.LibraryBooks,
                            tint = TechColors.Violet,
                            title = "Колоды сообщества (Community Decks)",
                            subtitle = "Публикация из «Мои слова», рейтинг и скачивание в 1 тап",
                            onClick = onOpenCommunityDecks
                        )
                        HorizontalDivider()
                        CommunityHubRow(
                            icon = Icons.Rounded.WorkspacePremium,
                            tint = TechColors.EmeraldDark,
                            title = "PR в словарь (Краудсорсинг)",
                            subtitle = "Допуск Middle+ / 14 дн. / 100 слов • Бейдж Contributor",
                            onClick = onOpenCrowdsourcePr
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityHubRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = tint)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
    }
}
