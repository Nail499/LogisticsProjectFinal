package az.fleetra.mobile.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.fleetra.mobile.network.ApiClient
import az.fleetra.mobile.network.dto.Notification
import az.fleetra.mobile.ui.theme.FleetraBorder
import az.fleetra.mobile.ui.theme.FleetraInk
import az.fleetra.mobile.ui.theme.FleetraOrange
import az.fleetra.mobile.ui.theme.FleetraOrangeLight
import az.fleetra.mobile.ui.theme.FleetraTextMuted
import kotlinx.coroutines.launch

// Rollar arası paylaşılan bildiriş siyahısı — bax NotificationController
// (/api/notifications). Toxunduqda bildiriş oxunmuş kimi qeyd olunur və
// çağıran ekran (Customer/DriverRootScreen) link sahəsinə görə daxili
// naviqasiya edir (onNotificationClick).
@Composable
fun NotificationListScreen(
    onBack: () -> Unit,
    onNotificationClick: (Notification) -> Unit,
) {
    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var markingAll by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        try {
            notifications = ApiClient.notificationApi.list()
        } catch (_: Exception) {
            // swallow — boş siyahı acceptable degrade
        }
    }

    LaunchedEffect(Unit) {
        loading = true
        reload()
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                }
                Text("Bildirişlər", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = FleetraInk)
            }
            TextButton(
                onClick = {
                    markingAll = true
                    scope.launch {
                        try {
                            ApiClient.notificationApi.markAllAsRead()
                            reload()
                        } catch (_: Exception) {
                        } finally {
                            markingAll = false
                        }
                    }
                },
                enabled = !markingAll && notifications.any { it.read != true },
            ) {
                Text("Hamısını oxu", color = FleetraOrange, fontSize = 13.sp)
            }
        }

        if (loading) {
            LoadingView()
            return@Column
        }

        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                Text("Bildiriş yoxdur", color = FleetraTextMuted)
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 10.dp),
        ) {
            items(notifications) { n ->
                NotificationCard(
                    notification = n,
                    onClick = {
                        if (n.read != true) {
                            scope.launch {
                                try {
                                    ApiClient.notificationApi.markAsRead(n.id)
                                    reload()
                                } catch (_: Exception) {
                                }
                            }
                        }
                        onNotificationClick(n)
                    },
                )
            }
        }
    }
}

@Composable
private fun NotificationCard(notification: Notification, onClick: () -> Unit) {
    val unread = notification.read != true
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (unread) FleetraOrangeLight else Color.White)
            .border(1.dp, FleetraBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                notification.title ?: "Bildiriş",
                fontWeight = if (unread) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp,
                color = FleetraInk,
            )
            notification.createdAt?.let { Text(it, color = FleetraTextMuted, fontSize = 11.sp) }
        }
        notification.message?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, color = FleetraTextMuted, fontSize = 13.sp)
        }
    }
}
