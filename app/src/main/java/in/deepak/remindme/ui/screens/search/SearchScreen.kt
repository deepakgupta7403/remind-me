package `in`.deepak.remindme.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.deepak.remindme.data.templates.ReminderTemplate
import `in`.deepak.remindme.data.templates.TemplateTone
import `in`.deepak.remindme.domain.model.Category
import `in`.deepak.remindme.domain.model.Reminder
import `in`.deepak.remindme.ui.state.ReminderType
import `in`.deepak.remindme.ui.theme.BrandColors
import kotlinx.coroutines.launch

/**
 * Full-screen search.
 *
 * Empty mode shows quick filters + recent searches + suggested smart filters
 * (Active now / This week). Results mode shows the live-filtered reminder list
 * plus any matching templates the user hasn't created yet — letting "I'm
 * looking for X but it doesn't exist" turn into one tap to add X.
 *
 * Query and filters are mirrored verbatim from
 * `sampledata/nextstep/search.png`. Matching characters in result titles get a
 * yellow span via [highlight] so the user can see *why* each row matched.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBack: () -> Unit,
    onResultTap: (Long) -> Unit,
    onToggle: (Long, Boolean) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Push the current query into Recent the first time we transition into
    // Results mode for that query. Cheap; commitQueryToHistory is idempotent
    // for duplicates.
    LaunchedEffect(state.mode, state.filters.query) {
        if (state.mode == SearchMode.Results) viewModel.commitQueryToHistory()
    }

    Scaffold(
        containerColor = BrandColors.PageBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandColors.PageBackground)
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            SearchHeader(
                query = state.filters.query,
                onQueryChange = viewModel::setQuery,
                onBack = onBack,
            )
            Spacer(Modifier.height(16.dp))

            when (state.mode) {
                SearchMode.Empty -> EmptyMode(
                    state = state,
                    onPickType = viewModel::setType,
                    onPickCategory = viewModel::setCategory,
                    onPickPaused = { viewModel.setStatus(StatusFilter.Paused) },
                    onPickTime = viewModel::setTime,
                    onTapRecent = viewModel::useRecent,
                    onClearRecent = viewModel::clearRecent,
                )
                SearchMode.Results -> ResultsMode(
                    state = state,
                    onSetType = viewModel::setType,
                    onSetStatus = viewModel::setStatus,
                    onSetTime = viewModel::setTime,
                    onResultTap = onResultTap,
                    onToggle = onToggle,
                    onApplyTemplate = { template ->
                        viewModel.applyTemplate(template) { name ->
                            scope.launch {
                                snackbarHostState.showSnackbar("$name added — find it on Home.")
                            }
                        }
                    },
                )
            }
        }
    }
}

// --- Header ---------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = BrandColors.TextHeading,
            )
        }
        Spacer(Modifier.size(4.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search reminders") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) ({
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = BrandColors.TextBody)
                }
            }) else null,
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandColors.Primary,
                unfocusedBorderColor = BrandColors.BorderSubtle,
                focusedContainerColor = BrandColors.SurfaceCard,
                unfocusedContainerColor = BrandColors.SurfaceCard,
                cursorColor = BrandColors.Primary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// --- Empty mode -----------------------------------------------------------

@Composable
private fun EmptyMode(
    state: SearchUiState,
    onPickType: (ReminderType?) -> Unit,
    onPickCategory: (Category?) -> Unit,
    onPickPaused: () -> Unit,
    onPickTime: (TimeFilter) -> Unit,
    onTapRecent: (String) -> Unit,
    onClearRecent: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SectionLabel("FILTER BY") }
        item {
            FilterRow {
                ChipRow {
                    QuickFilterChip(label = "Interval", icon = Icons.Filled.Refresh) {
                        onPickType(ReminderType.INTERVAL)
                    }
                    QuickFilterChip(label = "Daily", icon = Icons.Filled.WbSunny) {
                        onPickType(ReminderType.DAILY)
                    }
                    QuickFilterChip(label = "Weekly", icon = Icons.Filled.CalendarMonth) {
                        onPickType(ReminderType.WEEKLY)
                    }
                }
                Spacer(Modifier.height(8.dp))
                ChipRow {
                    QuickFilterChip(label = "Health", tone = BrandColors.Tile.IntervalFg) {
                        onPickCategory(Category.Health)
                    }
                    QuickFilterChip(label = "Work", tone = BrandColors.Tile.OnceFg) {
                        onPickCategory(Category.Work)
                    }
                    QuickFilterChip(label = "Paused", icon = Icons.Filled.PauseCircle) {
                        onPickPaused()
                    }
                }
            }
        }

        item { Spacer(Modifier.height(10.dp)) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel("RECENT SEARCHES", modifier = Modifier.weight(1f))
                if (state.recent.isNotEmpty()) {
                    TextButton(onClick = onClearRecent) {
                        Text("Clear", color = BrandColors.Primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        if (state.recent.isEmpty()) {
            item {
                Text(
                    text = "Your recent searches will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandColors.TextBody,
                )
            }
        } else {
            items(state.recent, key = { it }) { recent ->
                RecentRow(text = recent, onClick = { onTapRecent(recent) })
            }
        }

        item { Spacer(Modifier.height(10.dp)) }
        item { SectionLabel("SUGGESTED") }
        item {
            ChipRow {
                QuickFilterChip(label = "Active now", icon = Icons.Filled.Today) {
                    onPickTime(TimeFilter.ActiveNow)
                }
                QuickFilterChip(label = "This week", icon = Icons.Filled.DateRange) {
                    onPickTime(TimeFilter.ThisWeek)
                }
            }
        }
    }
}

@Composable
private fun RecentRow(text: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BrandColors.SurfaceCard),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.History,
                contentDescription = null,
                tint = BrandColors.TextBody,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = BrandColors.TextHeading,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Filled.CallMade,
                contentDescription = null,
                tint = BrandColors.TextBody,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// --- Results mode ---------------------------------------------------------

@Composable
private fun ResultsMode(
    state: SearchUiState,
    onSetType: (ReminderType?) -> Unit,
    onSetStatus: (StatusFilter) -> Unit,
    onSetTime: (TimeFilter) -> Unit,
    onResultTap: (Long) -> Unit,
    onToggle: (Long, Boolean) -> Unit,
    onApplyTemplate: (ReminderTemplate) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ChipRow {
                ResultFilterChip(
                    label = "All types",
                    selected = state.filters.type == null,
                    onClick = { onSetType(null) },
                )
                ResultFilterChip(
                    label = "Active only",
                    selected = state.filters.status == StatusFilter.Active,
                    onClick = {
                        onSetStatus(
                            if (state.filters.status == StatusFilter.Active) StatusFilter.All
                            else StatusFilter.Active
                        )
                    },
                )
                ResultFilterChip(
                    label = "All time",
                    selected = state.filters.time == TimeFilter.AllTime,
                    onClick = { onSetTime(TimeFilter.AllTime) },
                )
            }
        }
        item {
            Text(
                text = "${state.results.size} reminder${if (state.results.size == 1) "" else "s"} match",
                style = MaterialTheme.typography.labelLarge,
                color = BrandColors.TextBody,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (state.results.isEmpty()) {
            item { NoMatchesBlock(query = state.filters.query) }
        } else {
            items(state.results, key = { it.id }) { reminder ->
                ResultCard(
                    reminder = reminder,
                    query = state.filters.query,
                    onClick = { onResultTap(reminder.id) },
                    onToggle = { onToggle(reminder.id, it) },
                )
            }
        }

        if (state.templateMatches.isNotEmpty()) {
            item { Spacer(Modifier.height(8.dp)) }
            item { SectionLabel("FROM TEMPLATES") }
            items(state.templateMatches, key = { "tpl-${it.id}" }) { template ->
                TemplateMatchCard(
                    template = template,
                    onApply = { onApplyTemplate(template) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultCard(
    reminder: Reminder,
    query: String,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val (tileBg, tileFg) = tileColorsFor(reminder)
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
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
                Icon(iconFor(reminder), contentDescription = null, tint = tileFg, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = highlight(reminder.title, query),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandColors.TextHeading,
                )
                Text(
                    text = resultSubtitle(reminder),
                    style = MaterialTheme.typography.bodySmall,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateMatchCard(template: ReminderTemplate, onApply: () -> Unit) {
    val (tileBg, tileFg) = templateToneColors(template)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = BrandColors.BorderSubtle,
                shape = RoundedCornerShape(14.dp),
            ),
        onClick = onApply,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BrandColors.PageBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tileBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(template.icon, contentDescription = null, tint = tileFg, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandColors.TextHeading,
                )
                Text(
                    text = "Tap to add this template",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandColors.TextBody,
                )
            }
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = BrandColors.Primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun NoMatchesBlock(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(BrandColors.SurfaceDim),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = BrandColors.TextBody,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "No matches" + if (query.isBlank()) "" else " for \"$query\"",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = BrandColors.TextHeading,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Try a different word or check your filters.",
            style = MaterialTheme.typography.bodyMedium,
            color = BrandColors.TextBody,
            textAlign = TextAlign.Center,
        )
    }
}

// --- Chips + small components --------------------------------------------

@Composable
private fun FilterRow(content: @Composable () -> Unit) {
    Column { content() }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickFilterChip(
    label: String,
    icon: ImageVector? = null,
    tone: Color? = null,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = BrandColors.SurfaceCard,
        contentColor = tone ?: BrandColors.TextBody,
        onClick = onClick,
        modifier = Modifier.border(
            width = 1.dp,
            color = BrandColors.BorderSubtle,
            shape = RoundedCornerShape(20.dp),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
            }
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) BrandColors.Primary else BrandColors.SurfaceCard,
        contentColor = if (selected) BrandColors.OnPrimary else BrandColors.TextBody,
        onClick = onClick,
        modifier = if (selected) Modifier else Modifier.border(
            width = 1.dp,
            color = BrandColors.BorderSubtle,
            shape = RoundedCornerShape(20.dp),
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = BrandColors.TextBody,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = modifier,
    )
}

// --- Per-type icon + colour mirrors HomeScreen ---------------------------

private fun iconFor(reminder: Reminder): ImageVector = when (reminder) {
    is Reminder.Interval -> Icons.Filled.Refresh
    is Reminder.OneTime  -> Icons.Filled.DateRange
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
private fun templateToneColors(template: ReminderTemplate): Pair<Color, Color> = when (template.tone) {
    TemplateTone.Lavender -> BrandColors.Tile.IntervalBg to BrandColors.Tile.IntervalFg
    TemplateTone.Peach    -> BrandColors.Tile.DailyBg    to BrandColors.Tile.DailyFg
    TemplateTone.Mint     -> BrandColors.Tile.WeeklyBg   to BrandColors.Tile.WeeklyFg
    TemplateTone.Pink     -> BrandColors.Tile.OnceBg     to BrandColors.Tile.OnceFg
    TemplateTone.Sky      -> BrandColors.Tile.SkyBg      to BrandColors.Tile.SkyFg
}

private fun resultSubtitle(reminder: Reminder): String = when (reminder) {
    is Reminder.Interval -> "Every ${reminder.intervalMinutes} min · ${if (reminder.enabled) "active" else "paused"}"
    is Reminder.OneTime  -> "Once · ${if (reminder.enabled) "scheduled" else "paused"}"
    is Reminder.Daily    -> "Daily · ${reminder.timeOfDay}${if (reminder.enabled) "" else " · paused"}"
    is Reminder.Weekly   -> "Weekly · ${reminder.daysOfWeek.size} days${if (reminder.enabled) "" else " · paused"}"
}

// --- Match highlighting ---------------------------------------------------

private fun highlight(text: String, query: String): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    val q = query.trim()
    val idx = text.indexOf(q, ignoreCase = true)
    if (idx < 0) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text.substring(0, idx))
        withStyle(
            SpanStyle(
                background = Color(0xFFFFF59D), // soft yellow
                fontWeight = FontWeight.Bold,
            )
        ) { append(text.substring(idx, idx + q.length)) }
        append(text.substring(idx + q.length))
    }
}
