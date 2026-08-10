package az.fleetra.mobile.ui.driver

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import az.fleetra.mobile.location.LocationTrackingService
import az.fleetra.mobile.network.ApiClient
import az.fleetra.mobile.network.dto.LiveTripResponse
import az.fleetra.mobile.network.dto.TripRejectRequest
import az.fleetra.mobile.network.dto.TripStatusUpdateRequest
import az.fleetra.mobile.network.extractErrorMessage
import az.fleetra.mobile.ui.common.LoadingView
import az.fleetra.mobile.ui.common.StatusBadge
import az.fleetra.mobile.ui.theme.FleetraBorder
import az.fleetra.mobile.ui.theme.FleetraInk
import az.fleetra.mobile.ui.theme.FleetraOrange
import az.fleetra.mobile.ui.theme.FleetraOrangeLight
import az.fleetra.mobile.ui.theme.FleetraTextMuted
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

private val NEXT_STATUS = mapOf("PLANNED" to "PICKED_UP", "PICKED_UP" to "IN_TRANSIT", "IN_TRANSIT" to "DELIVERED")
private val NEXT_LABEL = mapOf(
    "PLANNED" to "Götürüldü kimi qeyd et",
    "PICKED_UP" to "Yoldadır kimi qeyd et",
    "IN_TRANSIT" to "Çatdırıldı kimi qeyd et",
)

@Composable
fun ActiveTripScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var trips by remember { mutableStateOf<List<LiveTripResponse>>(emptyList()) }
    var pendingTrips by remember { mutableStateOf<List<LiveTripResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var trackingTripId by remember { mutableStateOf<Long?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var pendingPhotoTripId by remember { mutableStateOf<Long?>(null) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var respondingTripId by remember { mutableStateOf<Long?>(null) }
    var rejectDialogTripId by remember { mutableStateOf<Long?>(null) }

    fun reload() {
        scope.launch {
            try {
                trips = ApiClient.driverApi.currentTripsLive()
                pendingTrips = ApiClient.driverApi.pendingAcceptanceTrips()
            } catch (_: Exception) {
            } finally {
                loading = false
            }
        }
    }

    fun acceptTrip(tripId: Long) {
        respondingTripId = tripId
        scope.launch {
            try {
                ApiClient.driverApi.acceptTrip(tripId)
                snackbarMessage = "Reys qəbul edildi"
                reload()
            } catch (e: Exception) {
                snackbarMessage = extractErrorMessage(e, "Reys qəbul edilmədi")
            } finally {
                respondingTripId = null
            }
        }
    }

    fun rejectTrip(tripId: Long, reason: String?) {
        respondingTripId = tripId
        scope.launch {
            try {
                ApiClient.driverApi.rejectTrip(tripId, TripRejectRequest(reason?.ifBlank { null }))
                snackbarMessage = "Reys imtina edildi"
                reload()
            } catch (e: Exception) {
                snackbarMessage = extractErrorMessage(e, "Reys imtina edilmədi")
            } finally {
                respondingTripId = null
            }
        }
    }

    LaunchedEffect(Unit) { reload() }

    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val fineGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (fineGranted) {
            trackingTripId?.let { LocationTrackingService.start(context, it) }
        } else {
            trackingTripId = null
            snackbarMessage = "GPS izləmə üçün məkan icazəsi lazımdır"
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val tripId = pendingPhotoTripId
        val uri = pendingPhotoUri
        if (success && tripId != null && uri != null) {
            scope.launch {
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    val bytes = stream?.readBytes()
                    stream?.close()
                    if (bytes != null) {
                        val body = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                        val part = MultipartBody.Part.createFormData("photo", "proof.jpg", body)
                        ApiClient.driverApi.uploadProof(tripId, part)
                        snackbarMessage = "Şəkil yükləndi"
                    }
                } catch (e: Exception) {
                    snackbarMessage = extractErrorMessage(e, "Şəkil yüklənmədi")
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            pendingPhotoTripId?.let { tripId ->
                val photoFile = File(context.cacheDir.resolve("camera_photos").apply { mkdirs() }, "proof_${tripId}_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                pendingPhotoUri = uri
                cameraLauncher.launch(uri)
            }
        } else {
            snackbarMessage = "Kameraya giriş icazəsi lazımdır"
        }
    }

    if (loading) {
        LoadingView()
        return
    }

    rejectDialogTripId?.let { tripId ->
        RejectReasonDialog(
            onDismiss = { rejectDialogTripId = null },
            onConfirm = { reason ->
                rejectDialogTripId = null
                rejectTrip(tripId, reason)
            },
        )
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(20.dp)) {
        Text("Aktiv reyslər", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Spacer(Modifier.height(16.dp))

        snackbarMessage?.let {
            Text(it, color = FleetraOrange, fontSize = 13.sp, modifier = Modifier.padding(bottom = 10.dp))
        }

        if (trips.isEmpty() && pendingTrips.isEmpty()) {
            Text("Hazırda aktiv reys yoxdur", color = FleetraTextMuted)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (pendingTrips.isNotEmpty()) {
                    item {
                        Text(
                            "Yeni reys təklifləri",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = FleetraInk,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                    items(pendingTrips) { trip ->
                        PendingTripCard(
                            trip = trip,
                            responding = respondingTripId == trip.tripId,
                            onAccept = { acceptTrip(trip.tripId) },
                            onReject = { rejectDialogTripId = trip.tripId },
                        )
                    }
                    if (trips.isNotEmpty()) {
                        item {
                            Text(
                                "Davam edən reyslər",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = FleetraInk,
                                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                            )
                        }
                    }
                }
                items(trips) { trip ->
                    TripCard(
                        trip = trip,
                        isTracking = trackingTripId == trip.tripId,
                        onToggleTracking = { enable ->
                            if (enable) {
                                trackingTripId = trip.tripId
                                val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                }
                                locationPermissionLauncher.launch(permissions.toTypedArray())
                            } else {
                                trackingTripId = null
                                LocationTrackingService.stop(context)
                            }
                        },
                        onOpenMaps = {
                            if (trip.destinationLatitude != null && trip.destinationLongitude != null) {
                                val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${trip.destinationLatitude},${trip.destinationLongitude}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            } else {
                                snackbarMessage = "Bu reys üçün koordinat təyin edilməyib"
                            }
                        },
                        onTakePhoto = {
                            pendingPhotoTripId = trip.tripId
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onAdvance = { nextStatus ->
                            scope.launch {
                                try {
                                    ApiClient.driverApi.updateStatus(trip.tripId, TripStatusUpdateRequest(nextStatus))
                                    if (nextStatus == "DELIVERED" && trackingTripId == trip.tripId) {
                                        trackingTripId = null
                                        LocationTrackingService.stop(context)
                                    }
                                    reload()
                                } catch (e: Exception) {
                                    snackbarMessage = extractErrorMessage(e, "Status yenilənmədi")
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TripCard(
    trip: LiveTripResponse,
    isTracking: Boolean,
    onToggleTracking: (Boolean) -> Unit,
    onOpenMaps: () -> Unit,
    onTakePhoto: () -> Unit,
    onAdvance: (String) -> Unit,
) {
    val nextStatus = NEXT_STATUS[trip.status]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, FleetraBorder, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Reys #${trip.tripId}", fontWeight = FontWeight.Bold, color = FleetraInk)
            StatusBadge(trip.status)
        }
        trip.destinationAddress?.let {
            Spacer(Modifier.height(6.dp))
            Text("Çatdırılma: $it", color = FleetraInk, fontSize = 14.sp)
        }
        trip.vehiclePlate?.let {
            Spacer(Modifier.height(2.dp))
            Text("Nəqliyyat: $it", color = FleetraTextMuted, fontSize = 12.sp)
        }

        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onOpenMaps) {
            Icon(Icons.Default.Navigation, contentDescription = null, tint = FleetraOrange)
            Spacer(Modifier.width(6.dp))
            Text("Xəritədə göstər", color = FleetraOrange, fontSize = 13.sp)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Canlı GPS izləmə", color = FleetraInk, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Switch(
                checked = isTracking,
                onCheckedChange = onToggleTracking,
                colors = SwitchDefaults.colors(checkedTrackColor = FleetraOrange),
            )
        }

        Button(
            onClick = onTakePhoto,
            colors = ButtonDefaults.buttonColors(containerColor = FleetraOrangeLight, contentColor = FleetraInk),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Çatdırılma şəkli çək")
        }

        if (nextStatus != null) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onAdvance(nextStatus) },
                colors = ButtonDefaults.buttonColors(containerColor = FleetraOrange),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(NEXT_LABEL[trip.status] ?: "Növbəti status", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Sürücüyə göndərilib hələ qəbul/imtina edilməmiş reys kartı — bax
// DriverController#pendingAcceptanceTrips / #acceptTrip / #rejectTrip.
// TripCard-dan ayrıdır: GPS izləmə/foto/status-irəli funksiyaları yoxdur,
// yalnız Qəbul et / İmtina et var.
@Composable
private fun PendingTripCard(
    trip: LiveTripResponse,
    responding: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, FleetraOrange, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Reys #${trip.tripId}", fontWeight = FontWeight.Bold, color = FleetraInk)
            StatusBadge(trip.status)
        }
        trip.destinationAddress?.let {
            Spacer(Modifier.height(6.dp))
            Text("Çatdırılma: $it", color = FleetraInk, fontSize = 14.sp)
        }
        trip.routeInfo?.let {
            Spacer(Modifier.height(2.dp))
            Text(it, color = FleetraTextMuted, fontSize = 12.sp)
        }
        if (trip.estimatedDistanceKm != null || trip.estimatedCost != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                listOfNotNull(
                    trip.estimatedDistanceKm?.let { "${it} km" },
                    trip.estimatedCost?.let { "${it} AZN" },
                ).joinToString(" · "),
                color = FleetraTextMuted,
                fontSize = 12.sp,
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onReject,
                enabled = !responding,
                modifier = Modifier.weight(1f),
            ) {
                Text("İmtina et")
            }
            Button(
                onClick = onAccept,
                enabled = !responding,
                colors = ButtonDefaults.buttonColors(containerColor = FleetraOrange),
                modifier = Modifier.weight(1f),
            ) {
                Text("Qəbul et", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Reysi imtina edərkən sürücüdən (könüllü) səbəb soruşan dialoq — bax
// DriverController#rejectTrip (reason optional).
@Composable
private fun RejectReasonDialog(
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reysi imtina et") },
        text = {
            Column {
                Text("İmtina səbəbini yaza bilərsiniz (məcburi deyil):", color = FleetraTextMuted, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Səbəb (istəyə bağlı)") },
                    singleLine = false,
                    maxLines = 3,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason.ifBlank { null }) }) {
                Text("İmtina et", color = FleetraOrange, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Ləğv et")
            }
        },
    )
}
