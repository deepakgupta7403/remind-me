package `in`.deepak.remindme.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.deepak.remindme.ui.theme.BrandColors
import kotlinx.coroutines.launch

/**
 * Settings → Profile editor.
 *
 * Layout mirrors the design comp (`sampledata/nextstep/name addition.png`):
 *   1. Top bar — back + "Profile" + Save.
 *   2. Avatar (first letter of the name) tinted by the chosen accent.
 *   3. Your name — editable field.
 *   4. Avatar color — six-swatch accent picker.
 *   5. Greeting — "Show greeting on home" toggle + tap-to-cycle style.
 *   6. Preview — exactly how Home will render with the current settings.
 *
 * Edits live in the ViewModel until Save; backing out discards them.
 */
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = BrandColors.PageBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = BrandColors.TextHeading,
                    )
                }
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = BrandColors.TextHeading,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    viewModel.save()
                    scope.launch { snackbarHostState.showSnackbar("Profile saved.") }
                    onBack()
                }) {
                    Text(
                        "Save",
                        color = BrandColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            Avatar(
                initial = state.avatarInitial,
                color = AvatarPalette.colorAt(state.avatarColorIndex),
                onCameraClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Photo upload is coming soon — your initial works for now.")
                    }
                },
            )

            Spacer(Modifier.height(24.dp))
            FieldLabel("YOUR NAME")
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                placeholder = { Text("Your name") },
                singleLine = true,
                trailingIcon = {
                    Icon(Icons.Filled.Edit, contentDescription = null, tint = BrandColors.TextBody)
                },
                shape = RoundedCornerShape(12.dp),
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
            FieldLabel("AVATAR COLOR")
            ColorPicker(
                selectedIndex = state.avatarColorIndex,
                onSelect = viewModel::setAvatarColor,
            )

            Spacer(Modifier.height(20.dp))
            FieldLabel("GREETING")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BrandColors.SurfaceCard),
            ) {
                Column {
                    GreetingRow(
                        title = "Show greeting on home",
                        trailing = {
                            Switch(
                                checked = state.showGreeting,
                                onCheckedChange = viewModel::setShowGreeting,
                                colors = primarySwitchColors(),
                            )
                        },
                        onClick = { viewModel.setShowGreeting(!state.showGreeting) },
                    )
                    RowDivider()
                    GreetingRow(
                        title = "Greeting style",
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = state.greetingStyle.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BrandColors.TextBody,
                                )
                                Spacer(Modifier.size(4.dp))
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = BrandColors.TextBody,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                        onClick = viewModel::cycleGreetingStyle,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            FieldLabel("PREVIEW")
            PreviewCard(greeting = state.previewGreeting)
            Spacer(Modifier.height(24.dp))
        }
    }
}

// --- Pieces ---------------------------------------------------------------

@Composable
private fun Avatar(initial: String, color: Color, onCameraClick: () -> Unit) {
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(BrandColors.SurfaceCard)
                .border(1.dp, BrandColors.BorderSubtle, CircleShape)
                .clickable(onClick = onCameraClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.PhotoCamera,
                contentDescription = "Change photo",
                tint = BrandColors.TextBody,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ColorPicker(selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AvatarPalette.colors.forEachIndexed { index, color ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (selected) Modifier.border(3.dp, BrandColors.TextHeading, CircleShape)
                        else Modifier
                    )
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewCard(greeting: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandColors.SurfaceCard),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (greeting.isNotEmpty()) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandColors.TextBody,
                )
            }
            Text(
                text = "5 reminders today",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BrandColors.TextHeading,
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = BrandColors.TextBody,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun GreetingRow(
    title: String,
    trailing: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = BrandColors.TextHeading,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(BrandColors.BorderSubtle),
    )
}

@Composable
private fun primarySwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = BrandColors.Primary,
    uncheckedThumbColor = BrandColors.SurfaceCard,
    uncheckedTrackColor = BrandColors.SurfaceDim,
    uncheckedBorderColor = BrandColors.BorderSubtle,
)
