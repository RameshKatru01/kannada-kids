package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.auth.AuthState
import com.example.ui.auth.AuthViewModel
import com.example.ui.data.AppUser
import com.example.ui.data.UserManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdministrationScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onOpenSubscriptions: () -> Unit = {}
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val currentEmail = (authState as? AuthState.Authenticated)?.email ?: ""
    val isCurrentAdmin = UserManager.isAdmin(currentEmail)

    LaunchedEffect(Unit) {
        UserManager.init(context)
    }

    val users by UserManager.usersFlow.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var userToEdit by remember { mutableStateOf<AppUser?>(null) }
    var showAddUserDialog by remember { mutableStateOf(false) }

    // Filter users based on query and selected category
    val filteredUsers = remember(users, searchQuery, selectedFilter) {
        users.filter { user ->
            val matchesQuery = searchQuery.isBlank() ||
                    user.email.contains(searchQuery, ignoreCase = true) ||
                    user.displayName.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "Admins" -> user.isAdmin
                "Gold" -> user.isGold
                "Silver" -> user.isSilver
                "Free" -> !user.isGold && !user.isSilver && !user.isAdmin
                "Expiring" -> user.daysRemaining in 1..14 || user.isExpired
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    val totalUsers = users.size
    val adminCount = users.count { it.isAdmin }
    val goldSubscribers = users.count { it.isGold }
    val silverSubscribers = users.count { it.isSilver }
    val freeUsers = users.count { !it.isGold && !it.isSilver && !it.isAdmin }
    val expiringCount = users.count { it.daysRemaining in 1..14 || it.isExpired }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Administration Portal",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "User Directory & Subscription Plans",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = onOpenSubscriptions,
                        modifier = Modifier.testTag("admin_subscriptions_btn")
                    ) {
                        Text("Subscriptions", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = { showAddUserDialog = true },
                        modifier = Modifier.testTag("admin_add_user_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add User",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E3A8A))
            )
        },
        containerColor = Color(0xFFF1F5F9)
    ) { paddingValues ->
        if (!isCurrentAdmin) {
            // Access Restricted Screen for non-admins
            AccessRestrictedView(
                currentEmail = currentEmail,
                onSwitchToAdmin = {
                    authViewModel.signInWithGmailAddress(context, UserManager.PRIMARY_ADMIN_EMAIL)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            // Full Admin Portal
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag("admin_user_list_screen"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Admin Identity & Privileges Banner
                item {
                    AdminIdentityBanner(currentEmail = currentEmail)
                }

                // Metric KPI Summary Row
                item {
                    AdminMetricsRow(
                        total = totalUsers,
                        gold = goldSubscribers,
                        free = freeUsers,
                        expiring = expiringCount
                    )
                }

                // Search & Filter Controls
                item {
                    SearchAndFilterSection(
                        searchQuery = searchQuery,
                        onQueryChange = { searchQuery = it },
                        selectedFilter = selectedFilter,
                        onFilterSelected = { selectedFilter = it }
                    )
                }

                // Header with Result Count
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Registered Users (${filteredUsers.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Tap user to manage plan",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // User Cards
                if (filteredUsers.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = "No users found",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No users match your criteria",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = "Try clearing the search query or changing filter",
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                } else {
                    items(filteredUsers, key = { it.email }) { user ->
                        AdminUserCard(
                            user = user,
                            onEditClick = { userToEdit = user }
                        )
                    }
                }
            }
        }
    }

    // Dialog for Editing Plan and Expiry Date
    userToEdit?.let { user ->
        EditUserPlanDialog(
            user = user,
            onDismiss = { userToEdit = null },
            onSave = { newPlan, expiryDateStr, expiryTs, role ->
                UserManager.updateUserCustomExpiry(
                    context = context,
                    email = user.email,
                    newPlan = newPlan,
                    expiryDateStr = expiryDateStr,
                    expiryTimestamp = expiryTs,
                    role = role
                )
                Toast.makeText(context, "Updated plan for ${user.email}", Toast.LENGTH_SHORT).show()
                userToEdit = null
            }
        )
    }

    // Dialog for Adding a New User
    if (showAddUserDialog) {
        AddUserDialog(
            onDismiss = { showAddUserDialog = false },
            onAdd = { email, name, plan, months ->
                val success = UserManager.addNewUser(context, email, name, plan, months)
                if (success) {
                    Toast.makeText(context, "Added user $email", Toast.LENGTH_SHORT).show()
                    showAddUserDialog = false
                } else {
                    Toast.makeText(context, "User with this email already exists!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

/**
 * Banner displaying the active Admin credentials and assigned administrative status.
 */
@Composable
private fun AdminIdentityBanner(currentEmail: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_identity_banner"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin Icon",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Admin Session",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFF047857),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "VERIFIED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = currentEmail.ifBlank { UserManager.PRIMARY_ADMIN_EMAIL },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFF334155))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Assigned Administrator: ${UserManager.PRIMARY_ADMIN_EMAIL}",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Metrics row showing user count statistics.
 */
@Composable
private fun AdminMetricsRow(
    total: Int,
    gold: Int,
    free: Int,
    expiring: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricCard(
            label = "Total Users",
            value = total.toString(),
            color = Color(0xFF0284C7),
            icon = Icons.Default.People,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            label = "Gold Plan",
            value = gold.toString(),
            color = Color(0xFFD97706),
            icon = Icons.Default.Star,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            label = "Free Plan",
            value = free.toString(),
            color = Color(0xFF475569),
            icon = Icons.Default.PersonOutline,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            label = "Expiring",
            value = expiring.toString(),
            color = if (expiring > 0) Color(0xFFDC2626) else Color(0xFF10B981),
            icon = Icons.Default.AccessTime,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B),
                maxLines = 1
            )
        }
    }
}

/**
 * Search text field and filter pills.
 */
@Composable
private fun SearchAndFilterSection(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_search_field"),
            placeholder = { Text("Search by email or name...", fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF64748B)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color(0xFF64748B)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color(0xFF1E3A8A),
                unfocusedBorderColor = Color(0xFFCBD5E1)
            )
        )

        val filters = listOf("All", "Gold", "Free", "Expiring", "Admins")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters) { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterSelected(filter) },
                    label = {
                        Text(
                            text = filter,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF1E3A8A),
                        selectedLabelColor = Color.White,
                        containerColor = Color.White,
                        labelColor = Color(0xFF475569)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFF1E3A8A) else Color(0xFFCBD5E1)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

/**
 * Individual user card showing email, plan, and expiry date.
 */
@Composable
private fun AdminUserCard(
    user: AppUser,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_user_card_${user.email.replace("@", "_").replace(".", "_")}")
            .clickable { onEditClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Avatar initial
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            brush = Brush.linearGradient(
                                if (user.isAdmin) listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                                else if (user.isGold) listOf(Color(0xFF0284C7), Color(0xFF0369A1))
                                else listOf(Color(0xFF64748B), Color(0xFF475569))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (user.displayName.take(1).ifBlank { user.email.take(1) }).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.displayName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (user.isAdmin) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "👑 ADMIN",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = user.email,
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit User Plan",
                        tint = Color(0xFF0284C7)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            // Plan and Expiry Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Plan Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (user.isGold) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            if (user.isGold) Color(0xFFFDE68A) else Color(0xFFE2E8F0)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (user.isGold) Icons.Default.Star else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (user.isGold) Color(0xFFD97706) else Color(0xFF64748B),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = user.plan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (user.isGold) Color(0xFF92400E) else Color(0xFF334155)
                            )
                        }
                    }
                }

                // Expiry Date Badge
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Expiry Date",
                            tint = if (user.isExpired) Color(0xFFDC2626) else Color(0xFF64748B),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Expiry: ${user.expiryDate}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (user.isExpired) Color(0xFFDC2626) else Color(0xFF1E293B)
                        )
                    }

                    // Remaining days or status chip
                    val statusText = when {
                        user.expiryTimestamp == Long.MAX_VALUE -> "Permanent"
                        user.expiryTimestamp == 0L -> "Lifetime Free"
                        user.isExpired -> "Expired"
                        user.daysRemaining <= 7 -> "${user.daysRemaining} days left (Expiring soon)"
                        else -> "${user.daysRemaining} days remaining"
                    }

                    val statusColor = when {
                        user.isExpired -> Color(0xFFDC2626)
                        user.daysRemaining <= 7 && user.expiryTimestamp > 0 -> Color(0xFFD97706)
                        else -> Color(0xFF059669)
                    }

                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }
        }
    }
}

/**
 * Dialog to modify a user's subscription plan, duration, and expiry date.
 */
@Composable
private fun EditUserPlanDialog(
    user: AppUser,
    onDismiss: () -> Unit,
    onSave: (newPlan: String, expiryDateStr: String, expiryTs: Long, role: String) -> Unit
) {
    var selectedPlan by remember { mutableStateOf(user.plan) }
    var selectedRole by remember { mutableStateOf(user.role) }
    var customMonths by remember { mutableStateOf("12") }

    // Current calculated expiry preview based on selection
    val calculatedExpiry = remember(selectedPlan, customMonths) {
        if (selectedPlan.contains("Lifetime", ignoreCase = true)) {
            Pair("Permanent (No Expiry)", Long.MAX_VALUE)
        } else if (selectedPlan.contains("Free", ignoreCase = true)) {
            Pair("Unlimited Free", 0L)
        } else {
            val months = customMonths.toIntOrNull() ?: 12
            val cal = addToDate(currentTimeMillis(), months = months)
            Pair(formatDate(cal), cal)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Manage User Subscription",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = user.email,
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Select Plan:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )

                val plans = listOf(
                    "Gold (Annual)" to "1 Year full access",
                    "Gold (Monthly)" to "1 Month full access",
                    "Gold (Lifetime Admin)" to "Permanent unlimited",
                    "Free Explorer" to "Basic features only"
                )

                plans.forEach { (planName, subtitle) ->
                    val isSelected = selectedPlan == planName
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedPlan = planName },
                        color = if (isSelected) Color(0xFFE0F2FE) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) Color(0xFF0284C7) else Color(0xFFE2E8F0)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedPlan = planName },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0284C7))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = planName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = subtitle,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                // If Gold, allow quick duration extensions
                if (selectedPlan.contains("Gold") && !selectedPlan.contains("Lifetime")) {
                    Text(
                        text = "Set Expiry Duration:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val durationOptions = listOf("1" to "+1 Mo", "3" to "+3 Mo", "6" to "+6 Mo", "12" to "+1 Yr")
                        durationOptions.forEach { (months, label) ->
                            val isSel = customMonths == months
                            OutlinedButton(
                                onClick = { customMonths = months },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSel) Color(0xFF0284C7) else Color.White,
                                    contentColor = if (isSel) Color.White else Color(0xFF334155)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Expiry Date Summary Box
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Projected Expiry Date:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = calculatedExpiry.first,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF78350F)
                        )
                    }
                }

                // Role selector
                if (!user.email.equals(UserManager.PRIMARY_ADMIN_EMAIL, ignoreCase = true)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Grant Admin Rights:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155)
                        )
                        Switch(
                            checked = selectedRole.equals("Admin", ignoreCase = true),
                            onCheckedChange = { checked ->
                                selectedRole = if (checked) "Admin" else "User"
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(selectedPlan, calculatedExpiry.first, calculatedExpiry.second, selectedRole)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF64748B))
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

/**
 * Dialog to add a new user to the system.
 */
@Composable
private fun AddUserDialog(
    onDismiss: () -> Unit,
    onAdd: (email: String, name: String, plan: String, months: Int) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var plan by remember { mutableStateOf("Gold (Annual)") }
    var months by remember { mutableStateOf(12) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add New User",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Gmail / Email Address") },
                    placeholder = { Text("user@gmail.com") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Student / Parent Name") },
                    placeholder = { Text("e.g. Ramesh") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Subscription Plan:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val plans = listOf("Gold (Annual)" to 12, "Gold (Monthly)" to 1, "Free Explorer" to 0)
                    plans.forEach { (p, m) ->
                        val isSel = plan == p
                        OutlinedButton(
                            onClick = {
                                plan = p
                                months = m
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSel) Color(0xFF1E3A8A) else Color.White,
                                contentColor = if (isSel) Color.White else Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                        ) {
                            Text(p.substringBefore(" "), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (email.isNotBlank()) {
                        val fullEmail = if (!email.contains("@")) "$email@gmail.com" else email
                        onAdd(fullEmail, name, plan, months)
                    }
                },
                enabled = email.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Add User", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF64748B))
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

/**
 * Access restricted fallback when a non-admin reaches this screen.
 */
@Composable
private fun AccessRestrictedView(
    currentEmail: String,
    onSwitchToAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Color(0xFFFFEBEE), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Access Restricted",
                tint = Color(0xFFD32F2F),
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Administration Portal Restricted",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "This tab is visible and accessible only for administrators.\nAssigned Admin: ${UserManager.PRIMARY_ADMIN_EMAIL}",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = Color(0xFFF1F5F9),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Currently logged in as: ${currentEmail.ifBlank { "Guest / Explorer" }}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF475569),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSwitchToAdmin,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign In as Assigned Admin", fontWeight = FontWeight.Bold)
        }
    }
}
