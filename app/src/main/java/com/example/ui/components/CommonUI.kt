package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserEntity
import com.example.data.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    currentUser: UserEntity?,
    currentRole: UserRole,
    onRoleSelected: (UserRole) -> Unit,
    onLogoutClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRoleMenu by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        CenterAlignedTopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EventSync",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                    )
                }
            },
            actions = {
                // Role Switcher Dropdown Button
                Box {
                    FilterChip(
                        selected = true,
                        onClick = { showRoleMenu = true },
                        label = {
                            Text(
                                text = currentRole.name.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        trailingIcon = {
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Switch Role")
                        },
                        modifier = Modifier.testTag("role_switcher_chip")
                    )

                    DropdownMenu(
                        expanded = showRoleMenu,
                        onDismissRequest = { showRoleMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("⚡ Super Admin Mode") },
                            onClick = {
                                onRoleSelected(UserRole.SUPER_ADMIN)
                                showRoleMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("🏫 School Admin Mode") },
                            onClick = {
                                onRoleSelected(UserRole.ADMIN)
                                showRoleMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.School, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("👩‍🏫 Teacher Mode") },
                            onClick = {
                                onRoleSelected(UserRole.TEACHER)
                                showRoleMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("🌐 Public View (No Login)") },
                            onClick = {
                                onRoleSelected(UserRole.PUBLIC)
                                showRoleMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Public, contentDescription = null) }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Role Quick Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Role:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 6.dp)
                )

                UserRole.entries.forEach { role ->
                    val isSelected = currentRole == role
                    AssistChip(
                        onClick = { onRoleSelected(role) },
                        label = {
                            Text(
                                text = role.name.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("quick_role_${role.name.lowercase()}")
                    )
                }

                if (currentUser != null) {
                    Spacer(modifier = Modifier.weight(1f))
                    if (currentRole == UserRole.ADMIN || currentRole == UserRole.TEACHER) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentUser.schoolName.ifBlank { "J D International School" },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = "👤 ${currentUser.name}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

@Composable
fun AppBottomNavigation(
    currentRole: UserRole,
    currentScreen: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = when (currentRole) {
        UserRole.SUPER_ADMIN -> listOf(
            BottomNavItem("super_dashboard", "Dashboard", Icons.Outlined.Dashboard, Icons.Default.Dashboard),
            BottomNavItem("super_performance_types", "Perf & Access", Icons.Outlined.Category, Icons.Default.Category),
            BottomNavItem("super_direct_chat", "1:1 Chat", Icons.Outlined.Forum, Icons.Default.Forum)
        )
        UserRole.ADMIN -> listOf(
            BottomNavItem("admin_events", "Events", Icons.Outlined.Event, Icons.Default.Event),
            BottomNavItem("admin_teachers", "Teachers", Icons.Outlined.Group, Icons.Default.Group),
            BottomNavItem("admin_students", "Classes", Icons.Outlined.Class, Icons.Default.Class),
            BottomNavItem("admin_chat", "Chat", Icons.Outlined.Chat, Icons.Default.Chat)
        )
        UserRole.TEACHER -> listOf(
            BottomNavItem("teacher_dashboard", "My Events", Icons.Outlined.EventNote, Icons.Default.EventNote),
            BottomNavItem("teacher_performances", "Performances", Icons.Outlined.TheaterComedy, Icons.Default.TheaterComedy),
            BottomNavItem("teacher_chat", "Chat", Icons.Outlined.Chat, Icons.Default.Chat)
        )
        UserRole.PUBLIC -> listOf(
            BottomNavItem("public_overview", "Overview", Icons.Outlined.Event, Icons.Default.Event),
            BottomNavItem("public_history", "History", Icons.Outlined.History, Icons.Default.History),
            BottomNavItem("public_qr", "QR & Link", Icons.Outlined.QrCode, Icons.Default.QrCode)
        )
    }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("app_bottom_nav")
    ) {
        items.forEach { item ->
            val isSelected = currentScreen == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(text = item.label) },
                modifier = Modifier.testTag("nav_item_${item.route}")
            )
        }
    }
}
