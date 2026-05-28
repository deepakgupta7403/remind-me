package `in`.deepak.remindme.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.deepak.remindme.data.icons.ReminderIconPack
import `in`.deepak.remindme.domain.model.Reminder
import `in`.deepak.remindme.ui.navigation.Destination
import `in`.deepak.remindme.ui.screens.common.AppBottomNavigation
import `in`.deepak.remindme.ui.theme.BrandColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Redesigned landing screen — replaces the old `ReminderListScreen`.
 *
 * Visual structure (top → bottom):
 *   1. Greeting line + bold count + Search icon (header).
 *   2. Purple "next reminder in X" banner (hidden when no future reminder).
 *   3. "TODAY" and "UPCOMING" sections of reminder cards.
 *   4. Floating "+" action.
 *   5. Bottom navigation across the four top-level tabs.
 *
 * No `ViewModel`-side scroll/list-state; the LazyColumn owns it. Card icons
 * are derived from reminder *type* (no Category domain field yet — once that
 * lands in Phase 2, [iconFor]/[tileColorsFor] will switch to read category).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddReminder: () -> Unit,
    onEditReminder: (Long) -> Unit,
    onSearch: () -> Unit,
    onBrowseTemplates: () -> Unit,
    onTabSelected: (Destination) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BrandColors.PageBackground,
        bottomBar = {
            AppBottomNavigation(
                currentRoute = Destination.Home.route,
                onSelect = onTabSelected,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddReminder,
                containerColor = BrandColors.Primary,
                contentColor = BrandColors.OnPrimary,
                shape = CircleShape,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New reminder")
            }
        },
    ) { padding ->
        when (val s = state) {
            HomeUiState.Loading -> LoadingBox(padding)
            is HomeUiState.Loaded -> Content(
                state = s,
                padding = padding,
                onSearch = onSearch,
                onToggle = viewModel::setEnabled,
                onCardClick = onEditReminder,
                onDelete = viewModel::delete,
                onCreate = onAddReminder,
                onBrowseTemplates = onBrowseTemplates,
            )
        }
    }
}

@Composable
private fun LoadingBox(padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center,
    ) { CircularProgressIndicator(color = BrandColors.Primary) }
}

@Composable
private fun Content(
    state: HomeUiState.Loaded,
    padding: PaddingValues,
    onSearch: () -> Unit,
    onToggle: (Long, Boolean) -> Unit,
    onCardClick: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onCreate: () -> Unit,
    onBrowseTemplates: () -> Unit,
) {
    // Long-press a card → ask for confirmation before deleting. Holding the
    // pending Reminder (not just its id) so the dialog can name it.
    var pendingDelete by remember { mutableStateOf<Reminder?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandColors.PageBackground)
            .padding(padding),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Header(state = state, onSearch = onSearch) }

        if (state.nextReminder != null) {
            item {
                Spacer(Modifier.height(4.dp))
                NextReminderBanner(state.nextReminder)
            }
        }

        if (state.today.isNotEmpty()) {
            item { SectionLabel("TODAY") }
            items(state.today, key = { "today-${it.id}" }) { reminder ->
                ReminderCard(
                    reminder = reminder,
                    onClick = { onCardClick(reminder.id) },
                    onLongPress = { pendingDelete = reminder },
                    onToggle = { onToggle(reminder.id, it) },
                )
            }
        }

        if (state.upcoming.isNotEmpty()) {
            item { SectionLabel("UPCOMING") }
            items(state.upcoming, key = { "up-${it.id}" }) { reminder ->
                ReminderCard(
                    reminder = reminder,
                    onClick = { onCardClick(reminder.id) },
                    onLongPress = { pendingDelete = reminder },
                    onToggle = { onToggle(reminder.id, it) },
                )
            }
        }

        if (state.today.isEmpty() && state.upcoming.isEmpty()) {
            item { EmptyState(onCreate = onCreate, onBrowseTemplates = onBrowseTemplates) }
        }
    }

    pendingDelete?.let { reminder ->
        DeleteConfirmationDialog(
            title = reminder.title,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                onDelete(reminder.id)
                pendingDelete = null
            },
        )
    }
}

@Composable
private fun DeleteConfirmationDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete reminder?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = BrandColors.TextHeading,
            )
        },
        text = {
            Text(
                text = "“$title” will be removed and its alarm cancelled. This can't be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandColors.TextBody,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = BrandColors.Danger, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = BrandColors.TextBody)
            }
        },
        containerColor = BrandColors.SurfaceCard,
    )
}

// --- Header ---------------------------------------------------------------

@Composable
private fun Header(state: HomeUiState.Loaded, onSearch: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            greetingLine(state)?.let { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandColors.TextBody,
                )
            }
            Text(
                text = countLine(state.totalToday),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = BrandColors.TextHeading,
            )
        }
        FilledIconButton(
            onClick = onSearch,
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = BrandColors.SurfaceCard,
                contentColor = BrandColors.TextHeading,
            ),
            modifier = Modifier.size(44.dp),
        ) {
            Icon(Icons.Filled.Search, contentDescription = "Search")
        }
    }
}

/** Null when the greeting is turned off in Profile, so the header omits the line. */
private fun greetingLine(state: HomeUiState.Loaded): String? = when {
    !state.showGreeting    -> null
    state.userName.isBlank() -> state.greeting
    else                   -> "${state.greeting}, ${state.userName}"
}

private fun countLine(count: Int): String = when (count) {
    0    -> "Nothing on today"
    1    -> "1 reminder today"
    else -> "$count reminders today"
}

// --- Banner ---------------------------------------------------------------

@Composable
private fun NextReminderBanner(next: NextReminderInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandColors.Primary),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "NEXT REMINDER ${next.relativeLabel.uppercase(Locale.getDefault())}",
                style = MaterialTheme.typography.labelMedium,
                color = BrandColors.OnPrimary.copy(alpha = 0.85f),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = iconFor(next.reminder),
                    contentDescription = null,
                    tint = BrandColors.OnPrimary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = next.reminder.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = BrandColors.OnPrimary,
                )
            }
        }
    }
}

// --- Section + Card --------------------------------------------------------

@Composable
private fun SectionLabel(text: String) {
    Spacer(Modifier.height(8.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = BrandColors.TextBody,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReminderCard(
    reminder: Reminder,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val (tileBg, tileFg) = tileColorsFor(reminder)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        shape = RoundedCornerShape(16.dp),
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
                    imageVector = iconFor(reminder),
                    contentDescription = null,
                    tint = tileFg,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandColors.TextHeading,
                )
                Text(
                    text = scheduleSummary(reminder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandColors.TextBody,
                )
            }
            Switch(
                checked = reminder.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BrandColors.OnPrimary,
                    checkedTrackColor = BrandColors.Primary,
                    uncheckedThumbColor = BrandColors.SurfaceCard,
                    uncheckedTrackColor = BrandColors.SurfaceDim,
                    uncheckedBorderColor = BrandColors.BorderSubtle,
                ),
            )
        }
    }
}

@Composable
private fun EmptyState(
    onCreate: () -> Unit,
    onBrowseTemplates: () -> Unit,
) {
    // Friendly-not-apologetic empty state. Mirrors the design's two-CTA pattern:
    // primary purple button creates from scratch; secondary link opens Templates
    // (often the faster path for new users).
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 16.dp, start = 8.dp, end = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(BrandColors.PrimarySoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = BrandColors.Primary,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Nothing scheduled yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = BrandColors.TextHeading,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Create your first reminder, or start with a ready-made template.",
            style = MaterialTheme.typography.bodyMedium,
            color = BrandColors.TextBody,
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onCreate,
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandColors.Primary,
                contentColor = BrandColors.OnPrimary,
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(0.65f),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Create reminder", fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = onBrowseTemplates) {
            Text(
                "Browse templates",
                color = BrandColors.Primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// --- Per-type icon + colour mapping ---------------------------------------
// Tied to reminder *type* until the Category domain field lands. Each branch
// returns the icon and the (tileBg, tileFg) pair from the brand palette so
// adding a new reminder type lights up the compiler here.

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

private fun scheduleSummary(reminder: Reminder): String = when (reminder) {
    is Reminder.Interval -> "Every ${reminder.intervalMinutes} min · " +
        "${reminder.activeWindow.start}–${reminder.activeWindow.end}"
    is Reminder.OneTime  -> "Once · " +
        SimpleDateFormat("d MMM · h:mm a", Locale.getDefault())
            .format(Date(reminder.triggerAtEpochMillis))
    is Reminder.Daily    -> "Daily · ${reminder.timeOfDay}"
    is Reminder.Weekly   -> "Weekly · ${
        reminder.daysOfWeek.sortedBy { it.value }.joinToString { it.name.take(3) }
    } · ${reminder.timeOfDay}"
}

