package az.fleetra.mobile.ui.customer

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import az.fleetra.mobile.data.UserSession
import az.fleetra.mobile.network.ApiClient
import az.fleetra.mobile.ui.auth.AuthViewModel
import az.fleetra.mobile.ui.common.ChatInboxScreen
import az.fleetra.mobile.ui.common.ChatScreen
import az.fleetra.mobile.ui.common.FleetraTopBar
import az.fleetra.mobile.ui.common.NotificationListScreen
import az.fleetra.mobile.ui.nav.CustomerTabs
import az.fleetra.mobile.ui.profile.ProfileScreen
import az.fleetra.mobile.ui.theme.FleetraOrange
import kotlinx.coroutines.delay
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun CustomerRootScreen(session: UserSession, authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    var unreadCount by remember { mutableStateOf(0) }

    // Sadə polling — bu tətbiqdə hələ WorkManager/push-based canlı yeniləmə
    // yoxdur (bax Faza 2: Firebase push). 30 saniyədə bir sayı yeniləyir.
    LaunchedEffect(Unit) {
        while (true) {
            try {
                unreadCount = ApiClient.notificationApi.unreadCount().count.toInt()
            } catch (_: Exception) {
            }
            delay(30_000)
        }
    }

    Scaffold(
        topBar = {
            FleetraTopBar(
                unreadCount = unreadCount,
                onBellClick = { navController.navigate("notifications") },
            )
        },
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                CustomerTabs.items.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = FleetraOrange, selectedTextColor = FleetraOrange),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = CustomerTabs.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(CustomerTabs.HOME) {
                CustomerHomeScreen(
                    username = session.username,
                    onNavigate = { route -> navController.navigate(route) },
                )
            }
            composable(CustomerTabs.NEW_ORDER) { NewOrderScreen() }
            composable(CustomerTabs.MY_ORDERS) {
                MyOrdersScreen(onOrderClick = { order -> navController.navigate("customer_order_detail/${order.id}") })
            }
            composable(CustomerTabs.TRACK) { TrackOrderScreen() }
            composable(CustomerTabs.CHAT) {
                ChatInboxScreen(
                    role = "CUSTOMER",
                    onBack = { navController.popBackStack() },
                    onOpenChat = { cargoId, channel, title ->
                        val encodedTitle = URLEncoder.encode(title, "UTF-8")
                        navController.navigate("customer_chat/$cargoId/$channel/$encodedTitle")
                    },
                )
            }
            composable(CustomerTabs.PROFILE) { ProfileScreen(role = session.role, onLogout = { authViewModel.logout() }) }
            composable(
                "customer_chat/{cargoId}/{channel}/{title}",
                arguments = listOf(navArgument("cargoId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val cargoId = backStackEntry.arguments?.getLong("cargoId") ?: 0L
                val channel = backStackEntry.arguments?.getString("channel").orEmpty()
                val encodedTitle = backStackEntry.arguments?.getString("title").orEmpty()
                val title = URLDecoder.decode(encodedTitle, "UTF-8")
                ChatScreen(cargoId = cargoId, channel = channel, title = title, onBack = { navController.popBackStack() })
            }
            composable(
                "customer_order_detail/{cargoId}",
                arguments = listOf(navArgument("cargoId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val cargoId = backStackEntry.arguments?.getLong("cargoId") ?: 0L
                OrderDetailScreen(cargoId = cargoId, onBack = { navController.popBackStack() })
            }
            composable("notifications") {
                NotificationListScreen(
                    onBack = { navController.popBackStack() },
                    onNotificationClick = { n ->
                        // Backend link sahəsi veb marşrutu formatındadır (məs.
                        // "/customer/orders") — mobil ekvivalent varsa ora
                        // keçirik, yoxdursa (dispetçer/admin marşrutları) sadəcə
                        // geri qayıdırıq.
                        val targetTab = when (n.link) {
                            "/customer" -> CustomerTabs.HOME
                            "/customer/orders" -> CustomerTabs.MY_ORDERS
                            else -> null
                        }
                        if (targetTab != null) {
                            navController.navigate(targetTab) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        } else {
                            navController.popBackStack()
                        }
                    },
                )
            }
        }
    }
}
