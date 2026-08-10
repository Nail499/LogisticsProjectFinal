package az.fleetra.mobile.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.fleetra.mobile.network.ApiClient
import az.fleetra.mobile.network.dto.LiveTripResponse
import az.fleetra.mobile.ui.common.LoadingView
import az.fleetra.mobile.ui.common.StatusBadge
import az.fleetra.mobile.ui.theme.FleetraBorder
import az.fleetra.mobile.ui.theme.FleetraInk
import az.fleetra.mobile.ui.theme.FleetraTextMuted

@Composable
fun TripHistoryScreen() {
    var trips by remember { mutableStateOf<List<LiveTripResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            trips = ApiClient.driverApi.tripHistory()
        } catch (_: Exception) {
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
        Text("Tamamlanmış reyslər", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Spacer(Modifier.height(16.dp))

        if (trips.isEmpty()) {
            Text("Tarixçə boşdur", color = FleetraTextMuted)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(trips) { trip ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(1.dp, FleetraBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Reys #${trip.tripId}", fontWeight = FontWeight.Bold, color = FleetraInk)
                            StatusBadge(trip.status)
                        }
                        trip.destinationAddress?.let {
                            Spacer(Modifier.height(6.dp))
                            Text("Çatdırılma: $it", color = FleetraTextMuted, fontSize = 12.sp)
                        }
                        trip.deliveredAt?.let {
                            Spacer(Modifier.height(2.dp))
                            Text("Çatdırılıb: $it", color = FleetraTextMuted, fontSize = 12.sp)
                        }
                        trip.estimatedDistanceKm?.let {
                            Spacer(Modifier.height(2.dp))
                            Text("Məsafə: $it km", color = FleetraTextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
