package az.fleetra.mobile.ui.customer

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import az.fleetra.mobile.network.ApiClient
import az.fleetra.mobile.network.dto.CargoRequest
import az.fleetra.mobile.network.dto.CargoType
import az.fleetra.mobile.network.dto.UrgencyLevel
import az.fleetra.mobile.network.extractErrorMessage
import az.fleetra.mobile.ui.theme.FleetraOrange
import az.fleetra.mobile.ui.theme.FleetraTextMuted
import kotlinx.coroutines.launch

private val CARGO_TYPE_LABELS = mapOf(
    CargoType.GENERAL to "Ümumi",
    CargoType.FRAGILE to "Kövrək",
    CargoType.REFRIGERATED to "Soyuducu",
    CargoType.HAZARDOUS to "Təhlükəli",
)
private val URGENCY_LABELS = mapOf(
    UrgencyLevel.STANDARD to "Standart",
    UrgencyLevel.EXPRESS to "Sürətli",
)

@Composable
fun NewOrderScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var description by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var volume by remember { mutableStateOf("") }
    var pickupAddress by remember { mutableStateOf("") }
    var destinationAddress by remember { mutableStateOf("") }
    var cargoType by remember { mutableStateOf(CargoType.GENERAL) }
    var urgency by remember { mutableStateOf(UrgencyLevel.STANDARD) }
    var coords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var locating by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Yeni sifariş", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = description, onValueChange = { description = it },
            label = { Text("Yükün təsviri") }, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = weight, onValueChange = { weight = it },
                label = { Text("Çəki (kg)") }, modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = volume, onValueChange = { volume = it },
                label = { Text("Həcm (m³)") }, modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = pickupAddress, onValueChange = { pickupAddress = it },
            label = { Text("Götürülmə ünvanı") }, modifier = Modifier.fillMaxWidth(),
        )

        TextButton(onClick = {
            val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!fineGranted) {
                message = "Məkan icazəsi verilməyib"
                isError = true
                return@TextButton
            }
            locating = true
            fusedClient.lastLocation
                .addOnSuccessListener { loc ->
                    locating = false
                    if (loc != null) {
                        coords = loc.latitude to loc.longitude
                    } else {
                        message = "Məkan alınmadı"
                        isError = true
                    }
                }
                .addOnFailureListener {
                    locating = false
                    message = "Məkan alınmadı"
                    isError = true
                }
        }) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = FleetraOrange)
            Spacer(Modifier.width(6.dp))
            Text(
                if (locating) "Yüklənir..." else if (coords != null) "Məkan təyin edildi ✓" else "Cari məkanımdan istifadə et",
                color = FleetraOrange, fontSize = 13.sp,
            )
        }

        OutlinedTextField(
            value = destinationAddress, onValueChange = { destinationAddress = it },
            label = { Text("Çatdırılma ünvanı") }, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))

        Text("Yük növü", color = FleetraTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        FlowChipRow(
            options = CargoType.entries.toList(),
            labels = CARGO_TYPE_LABELS,
            selected = cargoType,
            onSelect = { cargoType = it },
        )
        Spacer(Modifier.height(14.dp))

        Text("Prioritet", color = FleetraTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        FlowChipRow(
            options = UrgencyLevel.entries.toList(),
            labels = URGENCY_LABELS,
            selected = urgency,
            onSelect = { urgency = it },
        )

        message?.let {
            Spacer(Modifier.height(14.dp))
            Text(it, color = if (isError) MaterialTheme.colorScheme.error else az.fleetra.mobile.ui.theme.FleetraSuccess, fontSize = 13.sp)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (description.isBlank() || pickupAddress.isBlank() || destinationAddress.isBlank()) {
                    message = "Təsvir, götürülmə və çatdırılma ünvanlarını doldurun"
                    isError = true
                    return@Button
                }
                loading = true
                message = null
                scope.launch {
                    try {
                        ApiClient.customerApi.createCargo(
                            CargoRequest(
                                description = description,
                                weight = weight.toDoubleOrNull(),
                                volume = volume.toDoubleOrNull(),
                                pickupAddress = pickupAddress,
                                pickupLatitude = coords?.first,
                                pickupLongitude = coords?.second,
                                destinationAddress = destinationAddress,
                                cargoType = cargoType,
                                urgency = urgency,
                            )
                        )
                        message = "Sifarişiniz göndərildi"
                        isError = false
                        description = ""; weight = ""; volume = ""
                        pickupAddress = ""; destinationAddress = ""; coords = null
                    } catch (e: Exception) {
                        message = extractErrorMessage(e, "Sifariş göndərilmədi")
                        isError = true
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading,
            colors = ButtonDefaults.buttonColors(containerColor = FleetraOrange),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        ) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
            else Text("Sifarişi göndər", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun <T> FlowChipRow(options: List<T>, labels: Map<T, String>, selected: T, onSelect: (T) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(labels[option] ?: option.toString()) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FleetraOrange, selectedLabelColor = androidx.compose.ui.graphics.Color.White),
            )
        }
    }
}
