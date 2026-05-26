package `in`.deepak.remindme.ui.screens.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import `in`.deepak.remindme.ui.theme.BrandColors
import kotlinx.coroutines.launch

/**
 * First-launch onboarding flow.
 *
 * Four pages, in this order, mirroring the design:
 *   1. Welcome — value prop + primary CTA, optional Sign-in stub.
 *   2. Name — single field so the home greeting can address the user by name.
 *      Optional; an empty/skipped name just yields "Good morning".
 *   3. Feature tour — the four reminder types as an at-a-glance tile grid.
 *   4. Notifications permission — explains *why* before triggering the system
 *      dialog. "Maybe later" still completes onboarding so the user isn't
 *      trapped; the existing PermissionBanner will nag them on the list screen.
 *
 * The pager owns paging state; each page is a stateless composable that gets
 * its action callbacks injected. Adding a new page = new composable + a
 * `when` branch + bumping pageCount — no other plumbing.
 */
@Composable
fun OnboardingScreen(
    onCompleted: (userName: String) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var enteredName by rememberSaveable { mutableStateOf("") }

    val notificationsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        // Outcome doesn't gate completion — see class doc.
        onCompleted(enteredName)
    }

    fun advance() {
        if (pagerState.currentPage < pagerState.pageCount - 1) {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
        }
    }

    fun jumpToLast() {
        scope.launch { pagerState.animateScrollToPage(pagerState.pageCount - 1) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (page) {
                    0 -> WelcomePage(
                        onGetStarted = ::advance,
                        onSignIn = {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Sign in is coming soon — RemindMe is local-only for now."
                                )
                            }
                        },
                    )
                    1 -> NamePage(
                        name = enteredName,
                        onNameChange = { enteredName = it },
                        onContinue = ::advance,
                    )
                    2 -> FeatureTourPage(onNext = ::advance)
                    3 -> NotificationsPage(
                        onAllow = { notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        onMaybeLater = { onCompleted(enteredName) },
                    )
                }
            }

            // Skip is only meaningful while the user still has pages ahead.
            AnimatedVisibility(
                visible = pagerState.currentPage < pagerState.pageCount - 1,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            ) {
                TextButton(onClick = ::jumpToLast) {
                    Text(
                        "Skip",
                        color = BrandColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }

}

// --- Pages ---------------------------------------------------------------

@Composable
private fun WelcomePage(
    onGetStarted: () -> Unit,
    onSignIn: () -> Unit,
) {
    PageScaffold {
        Spacer(Modifier.weight(1f))
        BigIconTile(
            icon = Icons.Filled.Notifications,
            backgroundColor = BrandColors.Primary,
            iconTint = BrandColors.OnPrimary,
        )
        Spacer(Modifier.height(28.dp))
        Text(
            text = "Remind me",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = BrandColors.TextHeading,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Never miss what matters.\nReminders that fit your life.",
            style = MaterialTheme.typography.bodyLarge,
            color = BrandColors.TextBody,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        PrimaryCta(text = "Get started", onClick = onGetStarted)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Already have an account? ",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandColors.TextBody,
            )
            TextButton(
                onClick = onSignIn,
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    "Sign in",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun NamePage(
    name: String,
    onNameChange: (String) -> Unit,
    onContinue: () -> Unit,
) {
    PageScaffold {
        Spacer(Modifier.weight(1f))
        BigIconTile(
            icon = Icons.Filled.Notifications,
            backgroundColor = BrandColors.PrimarySoft,
            iconTint = BrandColors.Primary,
        )
        Spacer(Modifier.height(28.dp))
        Text(
            text = "What should we call you?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = BrandColors.TextHeading,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "We'll use your name in greetings. You can change it later in settings.",
            style = MaterialTheme.typography.bodyLarge,
            color = BrandColors.TextBody,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            placeholder = { Text("Your name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandColors.Primary,
                cursorColor = BrandColors.Primary,
            ),
        )
        Spacer(Modifier.weight(1f))
        PrimaryCta(text = "Continue", onClick = onContinue)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FeatureTourPage(onNext: () -> Unit) {
    PageScaffold {
        Spacer(Modifier.weight(1f))
        FeatureGrid()
        Spacer(Modifier.height(28.dp))
        Text(
            text = "Four ways to remind",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = BrandColors.TextHeading,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Interval, once, daily, or weekly — match the schedule to the habit.",
            style = MaterialTheme.typography.bodyLarge,
            color = BrandColors.TextBody,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        PageIndicator(total = 4, current = 2)
        Spacer(Modifier.height(20.dp))
        PrimaryCta(text = "Next  →", onClick = onNext)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun NotificationsPage(
    onAllow: () -> Unit,
    onMaybeLater: () -> Unit,
) {
    PageScaffold {
        Spacer(Modifier.weight(1f))
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(BrandColors.PrimarySoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = BrandColors.Primary,
                    modifier = Modifier.size(64.dp),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 18.dp, end = 18.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(BrandColors.Danger),
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = "Stay on track",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = BrandColors.TextHeading,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Allow notifications so we can nudge you at the right moment. " +
                "You can change this anytime in settings.",
            style = MaterialTheme.typography.bodyLarge,
            color = BrandColors.TextBody,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        PrimaryCta(text = "Allow notifications", onClick = onAllow)
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onMaybeLater) {
            Text(
                "Maybe later",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandColors.TextBody,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

// --- Reusable bits -------------------------------------------------------

@Composable
private fun PageScaffold(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

@Composable
private fun BigIconTile(
    icon: ImageVector,
    backgroundColor: Color,
    iconTint: Color,
) {
    Box(
        modifier = Modifier
            .size(112.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(56.dp),
        )
    }
}

@Composable
private fun FeatureGrid() {
    // Two rows of two tiles. Hard-coded because there are exactly four reminder
    // types — abstracting this into a generic grid would obscure the intent.
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FeatureTile(Icons.Filled.Refresh,       BrandColors.Tile.IntervalBg, BrandColors.Tile.IntervalFg)
            FeatureTile(Icons.Filled.DateRange,     BrandColors.Tile.OnceBg,     BrandColors.Tile.OnceFg)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FeatureTile(Icons.Filled.WbSunny,       BrandColors.Tile.DailyBg,    BrandColors.Tile.DailyFg)
            FeatureTile(Icons.Filled.CalendarMonth, BrandColors.Tile.WeeklyBg,   BrandColors.Tile.WeeklyFg)
        }
    }
}

@Composable
private fun FeatureTile(icon: ImageVector, bg: Color, tint: Color) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(34.dp))
    }
}

@Composable
private fun PageIndicator(total: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(total) { i ->
            val active = i == current
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(if (active) 24.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) BrandColors.Primary
                        else BrandColors.DotInactive,
                    ),
            )
        }
    }
}

@Composable
private fun PrimaryCta(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandColors.Primary,
            contentColor = BrandColors.OnPrimary,
        ),
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}
