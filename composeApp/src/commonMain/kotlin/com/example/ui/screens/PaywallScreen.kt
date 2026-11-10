package com.example.ui.screens

import com.example.ui.components.readableWidth
import com.example.platform.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ScrapbookInk
import com.example.ui.components.ScrapbookInkSoft
import com.example.ui.components.ScrapbookPaper
import com.example.ui.data.SubscriptionManager
import com.example.ui.progress.StarGold
import com.example.ui.progress.StarIcon
import kotlinx.coroutines.launch

/** Plans for parents (opened from the Parent corner, so it sits behind the grown-ups check). */
@Composable
fun PaywallScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val state by SubscriptionManager.state.collectAsState()
    val products by SubscriptionManager.products.collectAsState()
    var selected by remember { mutableStateOf(PlanPeriod.Yearly) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val signedIn = remember { PlatformAuth.currentUser() != null }

    LaunchedEffect(Unit) {
        SubscriptionManager.loadProducts()
        SubscriptionManager.refresh()
    }

    fun price(p: PlanPeriod) = products.firstOrNull { it.period == p }?.price ?: p.listPrice

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScrapbookPaper)
            .safeDrawingPadding()
            .readableWidth(640.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White).clickable { onBack() }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ScrapbookInk)
            }
            Text("Premium", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
        }

        if (state.hasPremium && state.plan != null) {
            // Already subscribed
            Column(
                Modifier.fillMaxWidth().background(ScrapbookInk, RoundedCornerShape(26.dp)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StarIcon(24.dp, StarGold)
                    Text("You're on Premium", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
                Text("${state.plan!!.title} · ${price(state.plan!!)} ${state.plan!!.perText}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCFE3F5))
                if (state.renewsAt > 0) {
                    Text(
                        (if (state.autoRenew) "Renews on " else "Ends on ") + formatDate(state.renewsAt),
                        fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White
                    )
                }
            }
            PillButton("Manage or cancel in ${PlatformBilling.storeName()}", DashSun, ScrapbookInk) { uriHandler.openUri(PlatformBilling.manageUrl()) }
            Text("To switch plans, cancel this one in ${PlatformBilling.storeName()} and choose a new plan when it ends.", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft)
        } else if (state.bonusUntil > currentTimeMillis()) {
            Text("Premium is on as a gift until ${formatDate(state.bonusUntil)}.", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF168F84))
        }

        // What Premium includes
        Column(
            Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(24.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Everything in Kannada Kids", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
            listOf(
                "All 49 letters, ಕಾಗುಣಿತ and numbers",
                "Every level of every game",
                "All Fun Videos, quizzes and stories",
                "Writing videos and tracing in the Letter Box"
            ).forEach { line ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(24.dp).background(Color(0xFFD9F6F2), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF168F84), modifier = Modifier.size(16.dp))
                    }
                    Text(line, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ScrapbookInk)
                }
            }
        }

        if (!(state.hasPremium && state.plan != null)) {
            // Plan cards
            PlanPeriod.entries.reversed().forEach { p ->
                val on = p == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (on) Color(0xFFFFF6E0) else Color.White)
                        .border(3.dp, if (on) DashSun else Color.Transparent, RoundedCornerShape(22.dp))
                        .clickable { selected = p }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("plan_${p.name}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier.size(24.dp).border(3.dp, if (on) ScrapbookInk else Color(0xFFB8C7D4), CircleShape).padding(5.dp)
                            .background(if (on) ScrapbookInk else Color.Transparent, CircleShape)
                    )
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(p.title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                            if (p == PlanPeriod.Yearly) Tag("Best value", Color(0xFFFF6B6B), Color.White)
                            if (p.savingPercent > 0) Tag("Save ${p.savingPercent}%", Color(0xFFE8F7EE), Color(0xFF1E6B3A))
                        }
                        Text(
                            if (p.months == 1) "Billed every month" else "About ₹${p.rupees / p.months} a month · billed ${p.perText}",
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft
                        )
                    }
                    Text(price(p), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                }
            }

            if (!signedIn) {
                Text(
                    "Sign in with Google first (Parent corner → Sign out → Continue with Google) so the subscription stays with your account on every phone.",
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB23A3A)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(29.dp))
                    .background(if (busy || !signedIn || !PlatformBilling.isSupported()) Color(0xFFB8C7D4) else Color(0xFFFF6B6B))
                    .clickable(enabled = !busy && signedIn && PlatformBilling.isSupported()) {
                        busy = true
                        message = null
                        scope.launch {
                            message = SubscriptionManager.buy(context, selected)
                            busy = false
                        }
                    }
                    .testTag("subscribe_button"),
                contentAlignment = Alignment.Center
            ) {
                if (busy) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(26.dp))
                else Text("Subscribe · ${price(selected)} ${selected.perText}", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }

        message?.let {
            Text(it, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        Text(
            "Restore purchases",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = !busy) {
                    busy = true
                    scope.launch { message = SubscriptionManager.restore(); busy = false }
                }
                .padding(10.dp),
            fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk
        )
        Text(
            "Payment is taken by ${PlatformBilling.storeName()}. The plan renews automatically at the same price " +
                "until you cancel. Cancel any time in ${PlatformBilling.storeName()} at least 24 hours before the renewal date.",
            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft, textAlign = TextAlign.Center
        )
        // Terms of use and privacy policy, which App Store and Google Play ask for next to subscriptions
        Row(
            Modifier.align(Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Terms of use",
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { uriHandler.openUri(TERMS_OF_USE_URL) }
                    .padding(8.dp).testTag("paywall_terms"),
                fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk
            )
            Text("·", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInkSoft)
            Text(
                "Privacy policy",
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { uriHandler.openUri(PRIVACY_POLICY_URL) }
                    .padding(8.dp).testTag("paywall_privacy"),
                fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
internal fun Tag(text: String, bg: Color, fg: Color) {
    Text(
        text,
        modifier = Modifier.background(bg, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
        fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = fg
    )
}

@Composable
internal fun PillButton(text: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).background(bg).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(text, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = fg) }
}
