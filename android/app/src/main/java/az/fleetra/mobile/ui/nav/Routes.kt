package az.fleetra.mobile.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

data class TabItem(val route: String, val label: String, val icon: ImageVector)

object CustomerTabs {
    const val HOME = "customer_home"
    const val NEW_ORDER = "customer_new_order"
    const val MY_ORDERS = "customer_my_orders"
    const val TRACK = "customer_track"
    const val CHAT = "customer_chat_inbox"
    const val PROFILE = "customer_profile"

    val items = listOf(
        TabItem(HOME, "Ana səhifə", Icons.Default.Home),
        TabItem(NEW_ORDER, "Yeni sifariş", Icons.Default.Add),
        TabItem(MY_ORDERS, "Sifarişlərim", Icons.Default.List),
        TabItem(TRACK, "İzləmə", Icons.Default.LocationOn),
        TabItem(CHAT, "Söhbət", Icons.Default.Chat),
        TabItem(PROFILE, "Profil", Icons.Default.Person),
    )
}

object DriverTabs {
    const val ACTIVE_TRIP = "driver_active_trip"
    const val HISTORY = "driver_history"
    const val RATINGS = "driver_ratings"
    const val CHAT = "driver_chat_inbox"
    const val PROFILE = "driver_profile"

    val items = listOf(
        TabItem(ACTIVE_TRIP, "Aktiv reys", Icons.Default.Navigation),
        TabItem(HISTORY, "Tarixçə", Icons.Default.History),
        TabItem(RATINGS, "Reytinq", Icons.Default.Star),
        TabItem(CHAT, "Söhbət", Icons.Default.Chat),
        TabItem(PROFILE, "Profil", Icons.Default.Person),
    )
}
