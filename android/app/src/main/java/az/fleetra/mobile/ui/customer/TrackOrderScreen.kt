package az.fleetra.mobile.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.fleetra.mobile.network.ApiClient
import az.fleetra.mobile.network.dto.PublicTrackingResponse
import az.fleetra.mobile.ui.common.StatusBadge
import az.fleetra.mobile.ui.theme.FleetraBorder
import az.fleetra.mobile.ui.theme.FleetraDanger
import az.fleetra.mobile.ui.theme.FleetraInk
import az.fleetra.mobile.ui.theme.FleetraOrange
import az.fleetra.mobile.ui.theme.FleetraTextMuted
import kotlinx.coroutines.launch

@Composable
fun TrackOrderScreen() {
    var trackingNumber by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<PublicTrackingResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(20.dp)) {
        Text("Yükü izlə", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(
                value = trackingNumber,
                onValueChange = { trackingNumber = it },
                label = { Text("TRK1234567890") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = {
                    if (trackingNumber.isBlank()) return@Button
                    loading = true; error = null; result = null
                    scope.launch {
                        try {
                            result = ApiClient.trackingApi.track(trackingNumber.trim())
                        } catch (_: Exception) {
                            error = "Bu nömrə ilə heç bir yük tapılmadı"
                        } finally {
                            loading = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FleetraOrange),
            ) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Axtar")
            }
        }

        error?.let {
            Spacer(Modifier.height(14.dp))
            Text(it, color = FleetraDanger, fontSize = 13.sp)
        }

        result?.let { r ->
            Spacer(Modifier.height(20.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, FleetraBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text("#${r.trackingNumber}", fontWeight = FontWeight.Bold, color = FleetraInk)
                    StatusBadge(r.status)
                }
                Spacer(Modifier.height(10.dp))
                r.driverName?.let { InfoRow("Sürücü", it) }
                r.vehiclePlate?.let { InfoRow("Nəqliyyat", it) }
                r.destinationAddress?.let { InfoRow("Çatdırılma ünvanı", it) }
                r.lastUpdatedAt?.let { InfoRow("Son yenilənmə", it) }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Text("$label: $value", color = FleetraTextMuted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
}
