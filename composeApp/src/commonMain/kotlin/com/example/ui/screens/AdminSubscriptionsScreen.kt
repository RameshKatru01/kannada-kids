package com.example.ui.screens

import com.example.ui.components.readableWidth
import com.example.platform.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.data.SubscriptionManager
import com.example.ui.data.SubscriptionManager.Subscriber
import com.example.ui.data.str
import kotlinx.coroutines.launch

private val AdminBg = Color(0xFFEEF4FA)
private val Ink = Color(0xFF0B3B66)
private val Soft = Color(0xFF3A5F82)
private val Green = Color(0xFF168F84)
private val Orange = Color(0xFFB55F00)
private val Red = Color(0xFFB23A3A)

private val planColors = mapOf(
    PlanPeriod.Monthly to (Color(0xFFDCEEFF) to Color(0xFF1F4E8C)),
    PlanPeriod.HalfYearly to (Color(0xFFE6E4FF) to Color(0xFF3D35A8)),
    PlanPeriod.Yearly to (Color(0xFFFFF0C2) to Color(0xFF8A5A00))
)
private val avatarTints = listOf(0xFFD9F6F2, 0xFFFFE6CC, 0xFFE6E4FF, 0xFFFFE1E1, 0xFFDCEEFF, 0xFFFBDDEB).map { Color(it) }

private fun Subscriber.statusLine(): Pair<String, Color> = when (effectiveStatus) {
    "active" -> "Renews ${formatDate(renewsAt, "d MMM yyyy")}" to Green
    "autorenew_off" -> "Auto-renew off · ends ${formatDate(renewsAt, "d MMM")}" to Orange
    "overdue" -> "Renewal overdue since ${formatDate(renewsAt, "d MMM")}" to Red
    "free_time" -> "Free time until ${formatDate(bonusUntil, "d MMM yyyy")}" to Green
    "expired" -> "Expired ${if (renewsAt > 0) formatDate(renewsAt, "d MMM") else ""}".trim() to Red
    else -> "No subscription" to Soft
}

@Composable
private fun AdminHeader(title: String, subtitle: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White).clickable { onBack() }, contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("ADMIN", modifier = Modifier.background(Ink, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 2.dp), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text(subtitle, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Soft)
        }
        trailing()
    }
}

@Composable
private fun PlanTag(plan: PlanPeriod?) {
    val (bg, fg) = planColors[plan] ?: (Color(0xFFE6ECF2) to Soft)
    Text(plan?.title ?: "Free", modifier = Modifier.background(bg, RoundedCornerShape(10.dp)).padding(horizontal = 9.dp, vertical = 3.dp), color = fg, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
}

@Composable
private fun Avatar(name: String, index: Int, size: Int = 40) {
    Box(Modifier.size(size.dp).background(avatarTints[index % avatarTints.size], CircleShape), contentAlignment = Alignment.Center) {
        Text(name.take(1).uppercase(), fontSize = (size * 0.42f).sp, fontWeight = FontWeight.ExtraBold, color = Ink)
    }
}

/** Loads the subscriber list (admin only). */
@Composable
private fun rememberSubscribers(): Triple<List<Subscriber>?, String?, () -> Unit> {
    var list by remember { mutableStateOf<List<Subscriber>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(reload) {
        error = null
        try {
            if (!SubscriptionManager.isCloudAdmin()) {
                error = "This account isn't set up as an admin in the online database yet."
                list = emptyList()
            } else {
                list = SubscriptionManager.allSubscribers()
            }
        } catch (e: Exception) {
            error = e.message ?: "Couldn't load subscriptions."
            list = emptyList()
        }
    }
    val doReload: () -> Unit = {
        list = null
        reload += 1
    }
    return Triple(list, error, doReload)
}

// ---------------------------------------------------------------------------------------------
// A · Subscriptions
// ---------------------------------------------------------------------------------------------

@Composable
fun AdminSubscriptionsScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onRenewals: () -> Unit, onAdmins: () -> Unit = {}) {
    val (list, error, reload) = rememberSubscribers()
    var filter by remember { mutableStateOf("All") }
    var query by remember { mutableStateOf("") }
    val now = currentTimeMillis()

    val all = list.orEmpty()
    val paying = all.filter { it.effectiveStatus == "active" || it.effectiveStatus == "autorenew_off" }
    val monthlyRevenue = paying.sumOf { (it.plan?.rupees ?: 0).toDouble() / (it.plan?.months ?: 1) }.toInt()
    val renewSoon = all.count { it.effectiveStatus == "active" && it.renewsAt in now..(now + 7 * 86_400_000L) }
    val filters = listOf("All", "Monthly", "Half-yearly", "Yearly", "Renewing", "Auto-renew off", "Overdue", "Free time", "Expired")
    val shown = all.filter { s ->
        when (filter) {
            "Monthly" -> s.plan == PlanPeriod.Monthly && s in paying
            "Half-yearly" -> s.plan == PlanPeriod.HalfYearly && s in paying
            "Yearly" -> s.plan == PlanPeriod.Yearly && s in paying
            "Renewing" -> s.effectiveStatus == "active"
            "Auto-renew off" -> s.effectiveStatus == "autorenew_off"
            "Overdue" -> s.effectiveStatus == "overdue"
            "Free time" -> s.bonusUntil > now
            "Expired" -> s.effectiveStatus == "expired"
            else -> true
        }
    }.filter { query.isBlank() || it.name.contains(query, true) || it.email.contains(query, true) }

    Column(Modifier.fillMaxSize().background(AdminBg).safeDrawingPadding().readableWidth(760.dp).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AdminHeader("Subscriptions", "${all.size} families · plans renew automatically", onBack) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White).clickable { reload() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = Ink)
            }
        }
        if (list == null) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Ink) }
            return@Column
        }
        if (error != null) {
            Text(error, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Red, modifier = Modifier.background(Color.White, RoundedCornerShape(16.dp)).padding(14.dp))
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard("${paying.size}", "paying families", Color(0xFF2EC4B6), Modifier.weight(1f))
                        StatCard("₹${monthlyRevenue}", "a month (average)", Color(0xFF6C63FF), Modifier.weight(1f))
                        StatCard("$renewSoon", "renew in 7 days", Color(0xFF4DA8FF), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard("${all.count { it.effectiveStatus == "autorenew_off" }}", "auto-renew off", Color(0xFFFF9F1C), Modifier.weight(1f))
                        StatCard("${all.count { it.effectiveStatus == "overdue" }}", "renewal overdue", Color(0xFFFF6B6B), Modifier.weight(1f))
                        StatCard("${all.count { it.effectiveStatus == "expired" }}", "expired", Color(0xFF9FB3C8), Modifier.weight(1f))
                    }
                    PlanSplit(paying)
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Text(
                            "Renewals & plans →",
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onRenewals() }.padding(vertical = 6.dp),
                            fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Ink
                        )
                        Text(
                            "Admins →",
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onAdmins() }.padding(vertical = 6.dp).testTag("open_admin_roles"),
                            fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Ink
                        )
                    }
                    // Search
                    Row(
                        Modifier.fillMaxWidth().height(44.dp).background(Color.White, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Soft, modifier = Modifier.size(20.dp))
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) Text("Search name or email", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Soft.copy(alpha = 0.7f))
                            BasicTextField(query, { query = it }, singleLine = true, textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink), modifier = Modifier.fillMaxWidth().testTag("admin_search"))
                        }
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        filters.forEach { f ->
                            val on = f == filter
                            Text(
                                if (f == "All") "All ${all.size}" else f,
                                modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (on) Ink else Color.White).clickable { filter = f }.padding(horizontal = 12.dp, vertical = 7.dp),
                                color = if (on) Color.White else Ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
            if (shown.isEmpty()) {
                item { Text("No families here yet.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Soft, modifier = Modifier.padding(8.dp)) }
            }
            items(shown, key = { it.uid }) { s ->
                val (line, color) = s.statusLine()
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).clickable { onOpen(s.uid) }.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Avatar(s.name, all.indexOf(s))
                    Column(Modifier.weight(1f)) {
                        Text(s.name, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(line, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    PlanTag(if (s in paying) s.plan else null)
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, color: Color, modifier: Modifier) {
    Row(modifier.height(IntrinsicSize.Min).clip(RoundedCornerShape(16.dp)).background(Color.White)) {
        Box(Modifier.width(5.dp).fillMaxHeight().background(color))
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(value, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Ink, lineHeight = 24.sp)
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Soft, lineHeight = 13.sp)
        }
    }
}

@Composable
private fun PlanSplit(paying: List<Subscriber>) {
    val counts = PlanPeriod.entries.associateWith { p -> paying.count { it.plan == p } }
    val total = counts.values.sum()
    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFE3EEF8))) {
            if (total > 0) PlanPeriod.entries.forEach { p ->
                val n = counts[p] ?: 0
                if (n > 0) Box(Modifier.weight(n.toFloat()).fillMaxHeight().background(planColors[p]!!.second.copy(alpha = 0.75f)))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PlanPeriod.entries.forEach { p ->
                Text("${p.title} ${counts[p] ?: 0}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = planColors[p]!!.second)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// B · One subscriber
// ---------------------------------------------------------------------------------------------

@Composable
fun AdminSubscriberScreen(uid: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var sub by remember { mutableStateOf<Subscriber?>(null) }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uid) {
        sub = try { SubscriptionManager.subscriber(uid) } catch (e: Exception) { message = e.message; null }
        loading = false
    }

    fun gift(days: Int, note: String) {
        scope.launch {
            message = null
            try {
                sub = SubscriptionManager.giveFreeTime(uid, days, note)
                message = if (days > 0) "Free time added." else "Free time removed."
            } catch (e: Exception) {
                message = e.message ?: "Couldn't save."
            }
        }
    }

    Column(
        Modifier.fillMaxSize().background(AdminBg).safeDrawingPadding().readableWidth(760.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AdminHeader("Subscriber", "Plan, free time and payments", onBack)
        val s = sub
        if (loading) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Ink) }
            return@Column
        }
        if (s == null) {
            Text(message ?: "Not found.", color = Red, fontWeight = FontWeight.Bold)
            return@Column
        }
        val (line, color) = s.statusLine()
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(s.name, 0, 52)
            Column(Modifier.weight(1f)) {
                Text(s.name, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text(s.email + if (s.joinedAt > 0) " · since ${formatDate(s.joinedAt, "d MMM yyyy")}" else "", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Soft)
            }
        }

        AdminCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        s.plan?.let { "${it.title} · ${it.listPrice}" } ?: "No paid plan",
                        fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Ink
                    )
                    Text(line, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = color)
                    if (s.platform.isNotBlank()) {
                        Text("Paid through ${if (s.platform == "ios") "App Store" else "Google Play"}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Soft)
                    }
                }
                PlanTag(s.plan)
            }
            Text(
                if (s.plan == null) "" else if (s.autoRenew) "Auto-renew is on." else "Auto-renew is off: access continues until the end date.",
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Soft
            )
            Text(
                "Plan changes, auto-renew and refunds are handled by the family in ${if (s.platform == "ios") "App Store" else "Google Play"}, or by you in the store's console.",
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Soft
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallButton("Play Console", Modifier.weight(1f)) { uriHandler.openUri("https://play.google.com/console") }
                SmallButton("App Store Connect", Modifier.weight(1f)) { uriHandler.openUri("https://appstoreconnect.apple.com") }
            }
        }

        AdminCard {
            Text("Give free time", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            if (s.bonusUntil > currentTimeMillis()) {
                Text("Free Premium until ${formatDate(s.bonusUntil, "d MMM yyyy")}" + if (s.bonusNote.isNotBlank()) " (${s.bonusNote})" else "", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Green)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallButton("+ 7 days", Modifier.weight(1f)) { gift(7, "7 days gift") }
                SmallButton("+ 1 month", Modifier.weight(1f)) { gift(30, "1 month gift") }
                SmallButton("+ 3 months", Modifier.weight(1f)) { gift(91, "3 months gift") }
            }
            if (s.bonusUntil > currentTimeMillis()) {
                Text("Remove free time", modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { gift(0, "") }.padding(vertical = 4.dp), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Red)
            }
        }

        AdminCard {
            Text("Payments", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            if (s.payments.isEmpty()) Text("No payments yet.", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Soft)
            s.payments.reversed().forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        val date = (p["date"] as? Long) ?: 0L
                        Text("${if (date > 0) formatDate(date, "d MMM yyyy") else "–"} · ${p.str("amount") ?: ""}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                        Text("${PlanPeriod.byKey(p.str("plan"))?.title ?: ""} · ${p.str("orderId") ?: ""}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Soft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("Paid", modifier = Modifier.background(Color(0xFFE8F7EE), RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E6B3A))
                }
            }
        }
        message?.let { Text(it, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Ink) }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun AdminCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

@Composable
private fun SmallButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFFEAF6FF)).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Ink, maxLines = 1)
    }
}

// ---------------------------------------------------------------------------------------------
// C · Renewals & plans
// ---------------------------------------------------------------------------------------------

@Composable
fun AdminRenewalsScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val (list, error, _) = rememberSubscribers()
    val uriHandler = LocalUriHandler.current
    val products by SubscriptionManager.products.collectAsState()
    LaunchedEffect(Unit) { SubscriptionManager.loadProducts() }
    val now = currentTimeMillis()
    val all = list.orEmpty()
    val renewing = all.filter { it.effectiveStatus == "active" && it.renewsAt in now..(now + 7 * 86_400_000L) }.sortedBy { it.renewsAt }
    val problems = all.filter { it.effectiveStatus == "overdue" || it.effectiveStatus == "autorenew_off" }

    Column(
        Modifier.fillMaxSize().background(AdminBg).safeDrawingPadding().readableWidth(760.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AdminHeader("Renewals & plans", "Money coming in, and who needs a nudge", onBack)
        if (list == null) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Ink) }
            return@Column
        }
        error?.let { Text(it, color = Red, fontWeight = FontWeight.Bold, fontSize = 14.sp) }

        Row(verticalAlignment = Alignment.Bottom) {
            Text("Renewing this week", modifier = Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            Text("₹${renewing.sumOf { it.plan?.rupees ?: 0 }} expected", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Green)
        }
        AdminCard {
            if (renewing.isEmpty()) Text("Nobody renews in the next 7 days.", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Soft)
            renewing.forEachIndexed { i, s ->
                Row(Modifier.fillMaxWidth().clickable { onOpen(s.uid) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Avatar(s.name, i)
                    Column(Modifier.weight(1f)) {
                        Text(s.name, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                        Text("${s.plan?.title} · renews ${formatDate(s.renewsAt, "d MMM")}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Soft)
                    }
                    Text(s.plan?.listPrice ?: "", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                }
            }
        }

        Text("Need a nudge", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        AdminCard {
            if (problems.isEmpty()) Text("Everyone's renewals look fine.", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Soft)
            problems.forEachIndexed { i, s ->
                val (line, color) = s.statusLine()
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Avatar(s.name, i + 2)
                    Column(Modifier.weight(1f).clickable { onOpen(s.uid) }) {
                        Text(s.name, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                        Text(line, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (s.email.isNotBlank()) {
                        Text(
                            "Remind",
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable {
                                val subject = Uri.encode("Kannada Kids Premium")
                                val body = Uri.encode("Hello ${s.name},\n\nThis is a friendly reminder about your Kannada Kids Premium plan. You can check it in ${if (s.platform == "ios") "App Store" else "Google Play"} → Subscriptions.\n\nThank you!")
                                uriHandler.openUri("mailto:${s.email}?subject=$subject&body=$body")
                            }.padding(6.dp),
                            fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Ink
                        )
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.Bottom) {
            Text("Plans & prices", modifier = Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            Text("auto-renewing", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Soft)
        }
        AdminCard {
            PlanPeriod.entries.forEach { p ->
                val live = products.firstOrNull { it.period == p }?.price
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PlanTag(p)
                    Column(Modifier.weight(1f)) {
                        Text(p.perText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Soft)
                        if (p.savingPercent > 0) Text("saves ${p.savingPercent}%", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Green)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(live ?: p.listPrice, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                        Text(if (live != null) "live in store" else "not in store yet", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (live != null) Green else Orange)
                    }
                }
            }
            Text("Prices are changed in the Google Play Console and App Store Connect.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Soft)
        }
        Spacer(Modifier.height(12.dp))
    }
}

// ---------------------------------------------------------------------------------------------
// D · Admins (the owner gives and removes admin rights)
// ---------------------------------------------------------------------------------------------

@Composable
fun AdminRolesScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var admins by remember { mutableStateOf<List<com.example.ui.data.AdminRoles.AdminEntry>?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var newEmail by remember { mutableStateOf("") }
    var confirmRemove by remember { mutableStateOf<String?>(null) }
    val myRole by com.example.ui.data.AdminRoles.myRole.collectAsState()
    val isOwner = myRole == "owner"

    suspend fun reload() {
        admins = try {
            com.example.ui.data.AdminRoles.load()
            com.example.ui.data.AdminRoles.list()
        } catch (e: Exception) {
            message = e.message ?: "Couldn't load admins."
            emptyList()
        }
    }
    LaunchedEffect(Unit) { reload() }

    Column(
        Modifier.fillMaxSize().background(AdminBg).safeDrawingPadding().readableWidth(760.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AdminHeader("Admins", if (isOwner) "You're the owner: you can add and remove admins" else "Only the owner can change this list", onBack)

        if (isOwner) {
            AdminCard {
                Text("Give admin rights", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text("They'll see subscriptions and can give free time after signing in with this Gmail.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Soft)
                Row(
                    Modifier.fillMaxWidth().height(46.dp).background(AdminBg, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f)) {
                        if (newEmail.isEmpty()) Text("name@gmail.com", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Soft.copy(alpha = 0.6f))
                        BasicTextField(newEmail, { newEmail = it.trim() }, singleLine = true, textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink), modifier = Modifier.fillMaxWidth().testTag("new_admin_email"))
                    }
                }
                Box(
                    Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(23.dp)).background(Color(0xFF2EC4B6)).clickable {
                        scope.launch {
                            message = try {
                                com.example.ui.data.AdminRoles.grant(newEmail)
                                val added = newEmail
                                newEmail = ""
                                reload()
                                "$added is now an admin."
                            } catch (e: Exception) {
                                e.message ?: "Couldn't add admin."
                            }
                        }
                    }.testTag("add_admin_button"),
                    contentAlignment = Alignment.Center
                ) { Text("Make admin", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Ink) }
            }
        }

        message?.let { Text(it, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Ink) }

        AdminCard {
            Text("People with admin rights", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            val list = admins
            if (list == null) {
                CircularProgressIndicator(color = Ink, modifier = Modifier.size(28.dp))
            } else if (list.isEmpty()) {
                Text("No admins found.", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Soft)
            } else {
                list.forEachIndexed { i, a ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Avatar(a.email, i)
                        Column(Modifier.weight(1f)) {
                            Text(a.email, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                (if (a.role == "owner") "Owner" else "Admin") + if (a.addedAt > 0 && a.role != "owner") " · added ${formatDate(a.addedAt, "d MMM yyyy")}" else "",
                                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (a.role == "owner") Green else Soft
                            )
                        }
                        if (isOwner && a.role != "owner") {
                            Text(
                                "Remove",
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { confirmRemove = a.email }.padding(6.dp),
                                fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Red
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    confirmRemove?.let { email ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text("Remove admin rights?", fontWeight = FontWeight.Bold) },
            text = { Text("$email will no longer see subscriptions.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    confirmRemove = null
                    scope.launch {
                        message = try {
                            com.example.ui.data.AdminRoles.revoke(email)
                            reload()
                            "$email is no longer an admin."
                        } catch (e: Exception) {
                            e.message ?: "Couldn't remove admin."
                        }
                    }
                }) { Text("Remove") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmRemove = null }) { Text("Cancel") } }
        )
    }
}
