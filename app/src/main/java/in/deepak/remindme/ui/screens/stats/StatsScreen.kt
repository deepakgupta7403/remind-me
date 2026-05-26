package `in`.deepak.remindme.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.deepak.remindme.ui.navigation.Destination
import `in`.deepak.remindme.ui.screens.common.AppBottomNavigation
import `in`.deepak.remindme.ui.theme.BrandColors

/**
 * Stats tab — empty state for now.
 *
 * The real version (streak/completion/heatmap/top reminders) needs completion
 * tracking, which doesn't exist yet. This screen matches the "Stats — empty"
 * design from `sampledata/nextstep/empty_state.png`: greyed-out metric cards
 * that preview what's coming, plus a "X of 7 days tracked" progress hint.
 *
 * Once the activity-tracking pipeline lands we'll branch on a real UiState
 * (Loading / Empty / Loaded) — for now there's only Empty.
 */
@Composable
fun StatsScreen(onTabSelected: (Destination) -> Unit) {
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
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Your stats",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = BrandColors.TextHeading,
            )
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MutedMetricCard(
                    icon = Icons.Filled.LocalFireDepartment,
                    label = "STREAK",
                    value = "—",
                    valueSuffix = "days",
                    modifier = Modifier.weight(1f),
                )
                MutedMetricCard(
                    icon = Icons.Filled.CheckCircle,
                    label = "DONE",
                    value = "—",
                    valueSuffix = "%",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.weight(1f))
            EmptyChartBlock()
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun MutedMetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    valueSuffix: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandColors.SurfaceDim),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BrandColors.TextBody,
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
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = BrandColors.TextBody.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = valueSuffix,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandColors.TextBody,
                )
            }
        }
    }
}

@Composable
private fun EmptyChartBlock() {
    Column(
        modifier = Modifier.fillMaxWidth(),
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
                imageVector = Icons.Filled.ShowChart,
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
            text = "Use the app for a few days. Your streaks and completion rate will start showing up here.",
            style = MaterialTheme.typography.bodyMedium,
            color = BrandColors.TextBody,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(20.dp))
        LinearProgressIndicator(
            progress = { 2f / 7f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = BrandColors.Primary,
            trackColor = BrandColors.SurfaceDim,
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "2 of 7 days tracked",
            style = MaterialTheme.typography.bodySmall,
            color = BrandColors.TextBody,
        )
    }
}
