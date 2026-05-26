package `in`.deepak.remindme.ui.screens.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import `in`.deepak.remindme.ui.navigation.Destination
import `in`.deepak.remindme.ui.theme.BrandColors

/**
 * Bottom nav shared by the four top-level destinations (Home / Stats /
 * Templates / Settings). Lives in `common` because every tab screen renders
 * it inside its own Scaffold — there's no single parent to host it.
 *
 * Selection state is purely a function of [currentRoute] so the same widget
 * works regardless of which tab includes it.
 */
@Composable
fun AppBottomNavigation(
    currentRoute: String?,
    onSelect: (Destination) -> Unit,
) {
    NavigationBar(
        containerColor = BrandColors.SurfaceCard,
    ) {
        Item(currentRoute, Destination.Home,      Icons.Filled.Home,        "Home",      onSelect)
        Item(currentRoute, Destination.Stats,     Icons.Filled.InsertChart, "Stats",     onSelect)
        Item(currentRoute, Destination.Templates, Icons.Filled.ViewAgenda,  "Templates", onSelect)
        Item(currentRoute, Destination.Settings,  Icons.Filled.Settings,    "Settings",  onSelect)
    }
}

@Composable
private fun RowScope.Item(
    currentRoute: String?,
    destination: Destination,
    icon: ImageVector,
    label: String,
    onSelect: (Destination) -> Unit,
) {
    val selected = currentRoute == destination.route
    NavigationBarItem(
        selected = selected,
        onClick = { if (!selected) onSelect(destination) },
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = BrandColors.Primary,
            selectedTextColor = BrandColors.Primary,
            unselectedIconColor = BrandColors.TextBody,
            unselectedTextColor = BrandColors.TextBody,
            indicatorColor = BrandColors.PrimarySoft,
        ),
    )
}
