package `in`.deepak.remindme.ui.screens.templates

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.deepak.remindme.data.templates.ReminderTemplate
import `in`.deepak.remindme.data.templates.TemplateTag
import `in`.deepak.remindme.data.templates.TemplateTone
import `in`.deepak.remindme.ui.navigation.Destination
import `in`.deepak.remindme.ui.screens.common.AppBottomNavigation
import `in`.deepak.remindme.ui.theme.BrandColors
import kotlinx.coroutines.launch

/**
 * Templates tab — "one tap to add a ready-made reminder".
 *
 * Layout (top → bottom):
 *   1. Title + subtitle.
 *   2. Search box (live filter against template title).
 *   3. Filter chips: All + each [TemplateTag].
 *   4. 2-column grid of template tiles.
 *
 * Tapping a tile saves the template as a real reminder and shows a snackbar
 * confirmation. The user can switch to Home to see/edit it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    viewModel: TemplatesViewModel,
    onTabSelected: (Destination) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = BrandColors.PageBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AppBottomNavigation(
                currentRoute = Destination.Templates.route,
                onSelect = onTabSelected,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandColors.PageBackground)
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Templates",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = BrandColors.TextHeading,
            )
            Text(
                text = "One tap to add a ready-made reminder",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandColors.TextBody,
            )
            Spacer(Modifier.height(16.dp))

            SearchField(
                query = state.query,
                onQueryChange = viewModel::setQuery,
            )

            Spacer(Modifier.height(16.dp))
            TagChips(
                selected = state.selectedTag,
                onSelect = viewModel::selectTag,
            )

            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(state.filtered, key = { it.id }) { template ->
                    TemplateTile(template = template) {
                        viewModel.apply(template) { name ->
                            scope.launch {
                                snackbarHostState.showSnackbar("$name added — find it on Home.")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search templates") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BrandColors.Primary,
            unfocusedBorderColor = BrandColors.BorderSubtle,
            focusedContainerColor = BrandColors.SurfaceCard,
            unfocusedContainerColor = BrandColors.SurfaceCard,
            cursorColor = BrandColors.Primary,
        ),
    )
}

@Composable
private fun TagChips(
    selected: TemplateTag?,
    onSelect: (TemplateTag?) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip(label = "All", isSelected = selected == null, onClick = { onSelect(null) })
        TemplateTag.entries.forEach { tag ->
            Chip(
                label = tag.label,
                isSelected = selected == tag,
                onClick = { onSelect(tag) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Chip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) BrandColors.Primary else BrandColors.SurfaceCard
    val fg = if (isSelected) BrandColors.OnPrimary else BrandColors.TextBody
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bg,
        contentColor = fg,
        onClick = onClick,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontWeight = FontWeight.Medium,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateTile(template: ReminderTemplate, onClick: () -> Unit) {
    val (tileBg, tileFg) = template.tone.colors()
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandColors.SurfaceCard),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tileBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = template.icon,
                    contentDescription = null,
                    tint = tileFg,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = template.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = BrandColors.TextHeading,
            )
            Text(
                text = template.scheduleSummary,
                style = MaterialTheme.typography.bodySmall,
                color = BrandColors.TextBody,
            )
        }
    }
}

/** Maps the data-layer [TemplateTone] enum to actual `BrandColors.Tile` pairs. */
@Composable
private fun TemplateTone.colors(): Pair<Color, Color> = when (this) {
    TemplateTone.Lavender -> BrandColors.Tile.IntervalBg to BrandColors.Tile.IntervalFg
    TemplateTone.Peach    -> BrandColors.Tile.DailyBg    to BrandColors.Tile.DailyFg
    TemplateTone.Mint     -> BrandColors.Tile.WeeklyBg   to BrandColors.Tile.WeeklyFg
    TemplateTone.Pink     -> BrandColors.Tile.OnceBg     to BrandColors.Tile.OnceFg
    TemplateTone.Sky      -> BrandColors.Tile.SkyBg      to BrandColors.Tile.SkyFg
}
