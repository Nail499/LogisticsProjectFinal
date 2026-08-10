package az.fleetra.mobile.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import az.fleetra.mobile.ui.theme.FleetraOrange

// Müştəri/sürücü root ekranlarında paylaşılan üst panel — zəng ikonu +
// oxunmamış say nişanı. Bax CustomerRootScreen.kt / DriverRootScreen.kt
// Scaffold(topBar = { FleetraTopBar(...) }).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetraTopBar(unreadCount: Int, onBellClick: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text("Fleetra", fontWeight = FontWeight.ExtraBold, color = FleetraOrange)
        },
        actions = {
            IconButton(onClick = onBellClick) {
                BadgedBox(
                    badge = {
                        if (unreadCount > 0) {
                            Badge { Text(if (unreadCount > 99) "99+" else unreadCount.toString()) }
                        }
                    },
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = "Bildirişlər")
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
    )
}
