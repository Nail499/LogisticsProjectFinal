package az.fleetra.mobile.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.fleetra.mobile.network.ApiClient
import az.fleetra.mobile.network.dto.Cargo
import az.fleetra.mobile.ui.common.LoadingView
import az.fleetra.mobile.ui.common.StatusBadge
import az.fleetra.mobile.ui.theme.FleetraBorder
import az.fleetra.mobile.ui.theme.FleetraInk
import az.fleetra.mobile.ui.theme.FleetraTextMuted

@Composable
fun MyOrdersScreen(onOrderClick: (Cargo) -> Unit = {}) {
    var orders by remember { mutableStateOf<List<Cargo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            orders = ApiClient.customerApi.myOrders()
        } catch (_: Exception) {
            // swallow — empty list + no crash is an acceptable degrade here
        } finally {
            loading = false
        }
    }

    if (loading) {
        LoadingView()
        return
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(20.dp)) {
        Text("Sifarişlərim", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Spacer(Modifier.height(16.dp))

        if (orders.isEmpty()) {
            Text("Hələ sifariş yoxdur", color = FleetraTextMuted)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(orders) { order -> OrderCard(order, onClick = { onOrderClick(order) }) }
            }
        }
    }
}

@Composable
private fun OrderCard(order: Cargo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(androidx.compose.ui.graphics.Color.White)
            .border(1.dp, FleetraBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("#${order.trackingNumber ?: order.id}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FleetraInk)
            StatusBadge(order.status)
        }
        Spacer(Modifier.height(6.dp))
        Text(order.description ?: "", color = FleetraInk, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "${order.pickupAddress ?: "—"} → ${order.destinationAddress ?: "—"}",
            color = FleetraTextMuted, fontSize = 12.sp,
        )
    }
}
