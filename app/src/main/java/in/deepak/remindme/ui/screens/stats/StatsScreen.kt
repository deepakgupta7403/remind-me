package `in`.deepak.remindme.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.deepak.remindme.data.icons.ReminderIconPack
import `in`.deepak.remindme.domain.model.Reminder
import `in`.deepak.remindme.ui.navigation.Destination
import `in`.deepak.remindme.ui.screens.common.AppBottomNavigation
import `in`.deepak.remindme.ui.theme.BrandColors

/**
 * Stats tab — streak, weekly completion, a 12-week activity heatmap, and the
 * user's best-completed reminders. Matches `sampledata/nextstep/stats_setting.png`.
 *
 * All numbers come from [StatsViewModel], which derives them from real recorded
 * activity (fired/done). Before anything has fired we show [StatsUiState.Empty].
 */
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onTabSelected: (Destination) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BrandColors.PageBackground,
        bottomBar = {
            AppBottomNavigation(
                currentRoute = Destination.Stats.route,
                onSelect = onTabSelected,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandColors.PageBackground)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Your stats",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = BrandColors.TextHeading,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "This week's progress",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandColors.TextBody,
            )
            Spacer(Modifier.height(20.dp))

            when (val s = state) {
                StatsUiState.Loading -> Unit
                StatsUiState.Empty -> EmptyStats()
                is StatsUiState.Loaded -> LoadedStats(s)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LoadedStats(state: StatsUiState.Loaded) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricCard(
            icon = Icons.Filled.LocalFireDepartment,
            iconTint = BrandColors.Tile.DailyFg,
            label = "CURRENT STREAK",
            value = state.streakDays.toString(),
            suffix = if (state.streakDays == 1) "day" else "days",
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            icon = Icons.Filled.TrackChanges,
            iconTint = BrandColors.Tile.WeeklyFg,
            label = "COMPLETION",
            value = state.completionPct?.toString() ?: "—",
            suffix = "%",
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(Modifier.height(24.dp))
    SectionLabel("LAST 12 WEEKS")
    Spacer(Modifier.height(10.dp))
    HeatmapCard(weeks = state.weeks)

    Spacer(Modifier.height(24.dp))
    SectionLabel("TOP REMINDERS")
    Spacer(Modifier.height(10.dp))
    if (state.topReminders.isEmpty()) {
        Text(
            text = "Mark reminders as done to see your best habits here.",
            style = MaterialTheme.typography.bodyMedium,
            color = BrandColors.TextBody,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.topReminders.forEach { TopReminderRow(it) }
        }
    }
}

@Composable
private fun MetricCard(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    suffix: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandColors.SurfaceCard),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandColors.TextBody,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = BrandColors.TextHeading,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = suffix,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandColors.TextBody,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun HeatmapCard(weeks: List<List<Int>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandColors.SurfaceCard),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                weeks.forEach { column ->
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        column.forEach { level ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(heatColor(level)),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Less",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandColors.TextBody,
                )
                Spacer(Modifier.size(6.dp))
                listOf(0, 1, 2, 3, 4).forEach { level ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(heatColor(level)),
                    )
                    Spacer(Modifier.size(3.dp))
                }
                Spacer(Modifier.size(3.dp))
                Text(
                    text = "More",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandColors.TextBody,
                )
            }
        }
    }
}

@Composable
private fun TopReminderRow(stat: TopReminderStat) {
    val (tileBg, tileFg) = tileColorsFor(stat.reminder)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BrandColors.SurfaceCard),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tileBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconFor(stat.reminder),
                    contentDescription = null,
                    tint = tileFg,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.size(12.dp))
            Text(
                text = stat.reminder.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = BrandColors.TextHeading,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${stat.completionPct}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BrandColors.Tile.WeeklyFg,
            )
        }
    }
}

@Composable
private fun EmptyStats() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(BrandColors.Tile.WeeklyBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.InsertChart,
                contentDescription = null,
                tint = BrandColors.Tile.WeeklyFg,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Not enough data yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = BrandColors.TextHeading,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "When your reminders fire, mark them as done. Your streak, completion rate, and activity will start showing up here.",
            style = MaterialTheme.typography.bodyMedium,
            color = BrandColors.TextBody,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = BrandColors.TextBody,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
    )
}

// --- Per-reminder icon + colour (mirrors HomeScreen) ----------------------

private fun iconFor(reminder: Reminder): ImageVector =
    ReminderIconPack.imageOrNull(reminder.iconKey) ?: when (reminder) {
        is Reminder.Interval -> Icons.Filled.WaterDrop
        is Reminder.OneTime  -> Icons.Filled.CardGiftcard
        is Reminder.Daily    -> Icons.Filled.WbSunny
        is Reminder.Weekly   -> Icons.Filled.CalendarMonth
    }

@Composable
private fun tileColorsFor(reminder: Reminder): Pair<Color, Color> = when (reminder) {
    is Reminder.Interval -> BrandColors.Tile.IntervalBg to BrandColors.Tile.IntervalFg
    is Reminder.OneTime  -> BrandColors.Tile.OnceBg     to BrandColors.Tile.OnceFg
    is Reminder.Daily    -> BrandColors.Tile.DailyBg    to BrandColors.Tile.DailyFg
    is Reminder.Weekly   -> BrandColors.Tile.WeeklyBg   to BrandColors.Tile.WeeklyFg
}

@Composable
private fun heatColor(level: Int): Color = when (level) {
    1 -> BrandColors.Heat.Level1
    2 -> BrandColors.Heat.Level2
    3 -> BrandColors.Heat.Level3
    4 -> BrandColors.Heat.Level4
    0 -> BrandColors.Heat.Level0
    else -> Color.Transparent // -1 = future/blank cell in the current week
}
