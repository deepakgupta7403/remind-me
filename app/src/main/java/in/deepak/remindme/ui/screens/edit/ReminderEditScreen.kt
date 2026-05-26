package `in`.deepak.remindme.ui.screens.edit

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import `in`.deepak.remindme.domain.model.Category
import `in`.deepak.remindme.domain.model.TimeOfDay
import `in`.deepak.remindme.ui.state.ReminderForm
import `in`.deepak.remindme.ui.state.ReminderType
import `in`.deepak.remindme.ui.theme.BrandColors
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Two-step Add/Edit Reminder flow.
 *
 *   Step 1 (name + type + category) → Continue → Step 2 (per-type config).
 *   Back from Step 2 returns to Step 1; back from Step 1 closes the screen.
 *
 * Step state lives in the screen (UI concern, not domain), so navigation
 * survives recomposition via [rememberSaveable] but not process death — that's
 * fine, the form contents are preserved by [ReminderEditViewModel] and the
 * user re-lands on Step 1.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditScreen(
    viewModel: ReminderEditViewModel,
    onDone: () -> Unit,
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableStateOf(EditStep.NameAndType) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(saveState) {
        if (saveState is SaveState.Saved) onDone()
    }

    Scaffold(
        containerColor = BrandColors.PageBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            EditTopBar(
                title = when (step) {
                    EditStep.NameAndType -> if (form.id == 0L) "New reminder" else "Edit reminder"
                    EditStep.Configure   -> form.title.ifBlank { "Reminder" }
                },
                onBack = {
                    if (step == EditStep.Configure) step = EditStep.NameAndType else onDone()
                },
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
            ProgressSegments(step = step)
            Spacer(Modifier.height(24.dp))

            when (step) {
                EditStep.NameAndType -> Step1(
                    form = form,
                    onUpdate = viewModel::update,
                    onContinue = { step = EditStep.Configure },
                    onNewCategoryTap = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Custom categories are coming soon.")
                        }
                    },
                )
                EditStep.Configure -> Step2(
                    form = form,
                    onUpdate = viewModel::update,
                    onSave = viewModel::save,
                    isSaving = saveState is SaveState.Saving,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private enum class EditStep { NameAndType, Configure }

// --- Shared chrome --------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = BrandColors.TextHeading,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = BrandColors.TextHeading,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BrandColors.PageBackground,
        ),
    )
}

@Composable
private fun ProgressSegments(step: EditStep) {
    // Two pill segments side by side; left always filled, right filled on step 2.
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ProgressPill(active = true, modifier = Modifier.weight(1f))
        ProgressPill(active = step == EditStep.Configure, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ProgressPill(active: Boolean, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { if (active) 1f else 0f },
        modifier = modifier
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp)),
        color = BrandColors.Primary,
        trackColor = BrandColors.SurfaceDim,
        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
    )
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

// --- STEP 1 — Name + Type + Category --------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Step1(
    form: ReminderForm,
    onUpdate: ((ReminderForm) -> ReminderForm) -> Unit,
    onContinue: () -> Unit,
    onNewCategoryTap: () -> Unit,
) {
    SectionLabel("REMINDER NAME")
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = form.title,
        onValueChange = { v -> onUpdate { it.copy(title = v) } },
        placeholder = { Text("e.g. Drink water") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BrandColors.Primary,
            unfocusedBorderColor = BrandColors.BorderSubtle,
            focusedContainerColor = BrandColors.SurfaceCard,
            unfocusedContainerColor = BrandColors.SurfaceCard,
            cursorColor = BrandColors.Primary,
        ),
    )

    Spacer(Modifier.height(20.dp))
    SectionLabel("REMINDER TYPE")
    Spacer(Modifier.height(10.dp))
    TypeGrid(
        selected = form.type,
        onSelect = { newType -> onUpdate { it.copy(type = newType) } },
    )

    Spacer(Modifier.height(20.dp))
    SectionLabel("CATEGORY")
    Spacer(Modifier.height(10.dp))
    CategoryChips(
        selected = form.category,
        onSelect = { newCat -> onUpdate { it.copy(category = newCat) } },
        onNewTap = onNewCategoryTap,
    )

    Spacer(Modifier.height(28.dp))
    PrimaryCta(
        text = "Continue  →",
        enabled = form.title.isNotBlank(),
        onClick = onContinue,
    )
}

@Composable
private fun TypeGrid(selected: ReminderType, onSelect: (ReminderType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TypeTile(
                icon = Icons.Filled.Refresh,
                title = "Interval",
                subtitle = "Every X minutes",
                tileBg = BrandColors.Tile.IntervalBg,
                tileFg = BrandColors.Tile.IntervalFg,
                selected = selected == ReminderType.INTERVAL,
                onClick = { onSelect(ReminderType.INTERVAL) },
                modifier = Modifier.weight(1f),
            )
            TypeTile(
                icon = Icons.Filled.DateRange,
                title = "Once",
                subtitle = "Specific date",
                tileBg = BrandColors.Tile.OnceBg,
                tileFg = BrandColors.Tile.OnceFg,
                selected = selected == ReminderType.ONE_TIME,
                onClick = { onSelect(ReminderType.ONE_TIME) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TypeTile(
                icon = Icons.Filled.WbSunny,
                title = "Daily",
                subtitle = "Every day at time",
                tileBg = BrandColors.Tile.DailyBg,
                tileFg = BrandColors.Tile.DailyFg,
                selected = selected == ReminderType.DAILY,
                onClick = { onSelect(ReminderType.DAILY) },
                modifier = Modifier.weight(1f),
            )
            TypeTile(
                icon = Icons.Filled.CalendarMonth,
                title = "Weekly",
                subtitle = "Pick weekdays",
                tileBg = BrandColors.Tile.WeeklyBg,
                tileFg = BrandColors.Tile.WeeklyFg,
                selected = selected == ReminderType.WEEKLY,
                onClick = { onSelect(ReminderType.WEEKLY) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tileBg: Color,
    tileFg: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .then(
                if (selected) Modifier.border(
                    width = 2.dp,
                    color = BrandColors.Primary,
                    shape = RoundedCornerShape(16.dp),
                ) else Modifier
            ),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) BrandColors.PrimarySoft.copy(alpha = 0.35f) else BrandColors.SurfaceCard,
        ),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(tileBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = tileFg, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandColors.TextHeading,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandColors.TextBody,
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(BrandColors.Primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = BrandColors.OnPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChips(
    selected: Category,
    onSelect: (Category) -> Unit,
    onNewTap: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallChip("Health",   selected == Category.Health,   onClick = { onSelect(Category.Health) })
        SmallChip("Work",     selected == Category.Work,     onClick = { onSelect(Category.Work) })
        SmallChip("Personal", selected == Category.Personal, onClick = { onSelect(Category.Personal) })
        SmallChip("+ New",    isSelected = false,            onClick = onNewTap, outlined = true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmallChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    outlined: Boolean = false,
) {
    val bg = when {
        isSelected -> BrandColors.PrimarySoft
        outlined   -> Color.Transparent
        else       -> BrandColors.SurfaceCard
    }
    val fg = if (isSelected) BrandColors.Primary else BrandColors.TextBody
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bg,
        contentColor = fg,
        onClick = onClick,
        modifier = if (outlined) Modifier.border(
            width = 1.dp,
            color = BrandColors.BorderSubtle,
            shape = RoundedCornerShape(20.dp),
        ) else Modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

// --- STEP 2 — Per-type configuration --------------------------------------

@Composable
private fun Step2(
    form: ReminderForm,
    onUpdate: ((ReminderForm) -> ReminderForm) -> Unit,
    onSave: () -> Unit,
    isSaving: Boolean,
) {
    TypeBadge(type = form.type)
    Spacer(Modifier.height(20.dp))

    when (form.type) {
        ReminderType.INTERVAL -> IntervalConfig(form = form, onUpdate = onUpdate)
        ReminderType.ONE_TIME -> OnceConfig(form = form, onUpdate = onUpdate)
        ReminderType.DAILY    -> DailyConfig(form = form, onUpdate = onUpdate)
        ReminderType.WEEKLY   -> WeeklyConfig(form = form, onUpdate = onUpdate)
    }

    Spacer(Modifier.height(28.dp))
    PrimaryCta(
        text = "Save reminder",
        enabled = form.isValid && !isSaving,
        onClick = onSave,
    )
}

@Composable
private fun TypeBadge(type: ReminderType) {
    val (label, icon, bg, fg) = badgeStyleFor(type)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bg,
        contentColor = fg,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private data class BadgeStyle(val label: String, val icon: ImageVector, val bg: Color, val fg: Color)

@Composable
private fun badgeStyleFor(type: ReminderType): BadgeStyle = when (type) {
    ReminderType.INTERVAL -> BadgeStyle("Interval reminder",  Icons.Filled.Refresh,       BrandColors.Tile.IntervalBg, BrandColors.Tile.IntervalFg)
    ReminderType.ONE_TIME -> BadgeStyle("Once-only reminder", Icons.Filled.DateRange,     BrandColors.Tile.OnceBg,     BrandColors.Tile.OnceFg)
    ReminderType.DAILY    -> BadgeStyle("Daily reminder",     Icons.Filled.WbSunny,       BrandColors.Tile.DailyBg,    BrandColors.Tile.DailyFg)
    ReminderType.WEEKLY   -> BadgeStyle("Weekly reminder",    Icons.Filled.CalendarMonth, BrandColors.Tile.WeeklyBg,   BrandColors.Tile.WeeklyFg)
}

// --- Interval config ------------------------------------------------------

@Composable
private fun IntervalConfig(
    form: ReminderForm,
    onUpdate: ((ReminderForm) -> ReminderForm) -> Unit,
) {
    SectionLabel("REMIND ME EVERY")
    Spacer(Modifier.height(10.dp))
    IntervalStepper(
        value = form.intervalMinutes,
        onChange = { v -> onUpdate { it.copy(intervalMinutes = v.coerceIn(1, 1440)) } },
    )
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(15, 30, 60, 120).forEach { mins ->
            QuickChip(
                label = if (mins < 60) "${mins}m" else "${mins / 60}h",
                isSelected = form.intervalMinutes == mins,
                onClick = { onUpdate { it.copy(intervalMinutes = mins) } },
            )
        }
    }

    Spacer(Modifier.height(20.dp))
    SectionLabel("ACTIVE TIME RANGE")
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TimePill(
            label = "From",
            value = form.activeWindowStart,
            onChange = { v -> onUpdate { it.copy(activeWindowStart = v) } },
            modifier = Modifier.weight(1f),
        )
        TimePill(
            label = "To",
            value = form.activeWindowEnd,
            onChange = { v -> onUpdate { it.copy(activeWindowEnd = v) } },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun IntervalStepper(value: Int, onChange: (Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandColors.SurfaceCard),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StepperButton(
                icon = Icons.Filled.Remove,
                onClick = { onChange((value - 5).coerceAtLeast(1)) },
                filled = false,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$value",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = BrandColors.Primary,
                )
                Text(
                    text = "minutes",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandColors.TextBody,
                )
            }
            StepperButton(
                icon = Icons.Filled.Add,
                onClick = { onChange((value + 5).coerceAtMost(1440)) },
                filled = true,
            )
        }
    }
}

@Composable
private fun StepperButton(icon: ImageVector, onClick: () -> Unit, filled: Boolean) {
    FilledIconButton(
        onClick = onClick,
        shape = CircleShape,
        modifier = Modifier.size(44.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (filled) BrandColors.Primary else BrandColors.SurfaceDim,
            contentColor = if (filled) BrandColors.OnPrimary else BrandColors.TextBody,
        ),
    ) { Icon(icon, contentDescription = null) }
}

// --- Once config ----------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnceConfig(
    form: ReminderForm,
    onUpdate: ((ReminderForm) -> ReminderForm) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val initialMillis = form.triggerAtEpochMillis ?: LocalDateTime.now(zone).plusHours(1)
        .atZone(zone).toInstant().toEpochMilli()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    val current = Instant.ofEpochMilli(initialMillis).atZone(zone).toLocalDateTime()

    SectionLabel("DATE")
    Spacer(Modifier.height(10.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandColors.SurfaceCard),
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = false,
            title = null,
            headline = null,
        )
    }

    // Push date selection back into the form whenever the picker emits a new
    // millis value; the time-of-day defaults to 9 am if the user hasn't set it.
    LaunchedEffect(datePickerState.selectedDateMillis) {
        val dateMillis = datePickerState.selectedDateMillis ?: return@LaunchedEffect
        val date = Instant.ofEpochMilli(dateMillis).atZone(zone).toLocalDate()
        val time = form.triggerAtEpochMillis
            ?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalTime() }
            ?: LocalTime.of(9, 0)
        val merged = LocalDateTime.of(date, time).atZone(zone).toInstant().toEpochMilli()
        onUpdate { it.copy(triggerAtEpochMillis = merged) }
    }

    Spacer(Modifier.height(20.dp))
    SectionLabel("TIME")
    Spacer(Modifier.height(10.dp))
    BigTimeRow(
        timeOfDay = TimeOfDay.of(current.hour, current.minute),
        onChange = { tod ->
            val date = (datePickerState.selectedDateMillis ?: initialMillis)
                .let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            val merged = LocalDateTime.of(date, LocalTime.of(tod.hour, tod.minute))
                .atZone(zone).toInstant().toEpochMilli()
            onUpdate { it.copy(triggerAtEpochMillis = merged) }
        },
    )
}

// --- Daily config ---------------------------------------------------------

@Composable
private fun DailyConfig(
    form: ReminderForm,
    onUpdate: ((ReminderForm) -> ReminderForm) -> Unit,
) {
    SectionLabel("REMIND EVERY DAY AT")
    Spacer(Modifier.height(10.dp))
    BigTimeRow(
        timeOfDay = form.timeOfDay,
        onChange = { v -> onUpdate { it.copy(timeOfDay = v) } },
    )

    Spacer(Modifier.height(20.dp))
    SectionLabel("ALSO REMIND ME")
    Spacer(Modifier.height(10.dp))
    ToggleRow(
        label = "5 minutes before",
        checked = form.minutesBefore == 5,
        onCheckedChange = { on -> onUpdate { it.copy(minutesBefore = if (on) 5 else 0) } },
    )
    Spacer(Modifier.height(8.dp))
    ToggleRow(
        label = "Skip on weekends",
        checked = form.skipWeekends,
        onCheckedChange = { on -> onUpdate { it.copy(skipWeekends = on) } },
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BrandColors.SurfaceCard),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = BrandColors.TextHeading,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
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

// --- Weekly config --------------------------------------------------------

@Composable
private fun WeeklyConfig(
    form: ReminderForm,
    onUpdate: ((ReminderForm) -> ReminderForm) -> Unit,
) {
    SectionLabel("SELECT DAYS")
    Spacer(Modifier.height(10.dp))
    DayChipsRow(
        selected = form.daysOfWeek,
        onToggle = { day ->
            onUpdate {
                val next = if (day in it.daysOfWeek) it.daysOfWeek - day else it.daysOfWeek + day
                it.copy(daysOfWeek = next.ifEmpty { it.daysOfWeek })
            }
        },
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = "${form.daysOfWeek.size} day${if (form.daysOfWeek.size == 1) "" else "s"} selected",
        style = MaterialTheme.typography.bodySmall,
        color = BrandColors.TextBody,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallChip("Weekdays", isSelected = form.daysOfWeek == WEEKDAYS, onClick = {
            onUpdate { it.copy(daysOfWeek = WEEKDAYS) }
        })
        SmallChip("Weekends", isSelected = form.daysOfWeek == WEEKENDS, onClick = {
            onUpdate { it.copy(daysOfWeek = WEEKENDS) }
        })
        SmallChip("Daily", isSelected = form.daysOfWeek == ALL_DAYS, onClick = {
            onUpdate { it.copy(daysOfWeek = ALL_DAYS) }
        })
    }

    Spacer(Modifier.height(20.dp))
    SectionLabel("TIME")
    Spacer(Modifier.height(10.dp))
    BigTimeRow(
        timeOfDay = form.timeOfDay,
        onChange = { v -> onUpdate { it.copy(timeOfDay = v) } },
    )
}

private val WEEKDAYS: Set<DayOfWeek> = setOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
)
private val WEEKENDS: Set<DayOfWeek> = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
private val ALL_DAYS: Set<DayOfWeek> = DayOfWeek.entries.toSet()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayChipsRow(selected: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit) {
    // Mon → Sun left to right; single-letter label, circle chip.
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        DayOfWeek.entries.forEach { day ->
            val isSel = day in selected
            Surface(
                shape = CircleShape,
                color = if (isSel) BrandColors.Primary else BrandColors.SurfaceCard,
                contentColor = if (isSel) BrandColors.OnPrimary else BrandColors.TextHeading,
                onClick = { onToggle(day) },
                modifier = Modifier
                    .size(40.dp)
                    .then(
                        if (isSel) Modifier else Modifier.border(
                            width = 1.dp,
                            color = BrandColors.BorderSubtle,
                            shape = CircleShape,
                        )
                    ),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = day.name.take(1),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// --- Shared pickers / chips / CTAs ----------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePill(
    label: String,
    value: TimeOfDay,
    onChange: (TimeOfDay) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BrandColors.SurfaceCard),
        onClick = { showDialog = true },
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = BrandColors.TextBody,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = format12h(value),
                style = MaterialTheme.typography.titleMedium,
                color = BrandColors.TextHeading,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
    if (showDialog) {
        TimeOfDayPickerDialog(
            initial = value,
            onConfirm = { onChange(it); showDialog = false },
            onDismiss = { showDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BigTimeRow(timeOfDay: TimeOfDay, onChange: (TimeOfDay) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val isPm = timeOfDay.hour >= 12
    val hour12 = (timeOfDay.hour % 12).let { if (it == 0) 12 else it }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandColors.SurfaceCard),
        onClick = { showDialog = true },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "%02d".format(hour12),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = BrandColors.Primary,
            )
            Text(
                text = " : ",
                style = MaterialTheme.typography.displayMedium,
                color = BrandColors.TextHeading,
            )
            Text(
                text = "%02d".format(timeOfDay.minute),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = BrandColors.TextHeading,
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = if (isPm) "PM" else "AM",
                style = MaterialTheme.typography.titleSmall,
                color = BrandColors.TextBody,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
    if (showDialog) {
        TimeOfDayPickerDialog(
            initial = timeOfDay,
            onConfirm = { onChange(it); showDialog = false },
            onDismiss = { showDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeOfDayPickerDialog(
    initial: TimeOfDay,
    onConfirm: (TimeOfDay) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = false,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(TimeOfDay.of(state.hour, state.minute)) }) {
                Text("OK", color = BrandColors.Primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = BrandColors.TextBody)
            }
        },
        text = { TimePicker(state = state) },
        containerColor = BrandColors.SurfaceCard,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) BrandColors.Primary else BrandColors.SurfaceCard,
        contentColor = if (isSelected) BrandColors.OnPrimary else BrandColors.TextBody,
        onClick = onClick,
        modifier = if (isSelected) Modifier else Modifier.border(
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
private fun PrimaryCta(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandColors.Primary,
            contentColor = BrandColors.OnPrimary,
            disabledContainerColor = BrandColors.Primary.copy(alpha = 0.4f),
            disabledContentColor = BrandColors.OnPrimary,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun format12h(t: TimeOfDay): String {
    val h12 = (t.hour % 12).let { if (it == 0) 12 else it }
    val am  = t.hour < 12
    return "%d:%02d %s".format(h12, t.minute, if (am) "am" else "pm")
}
