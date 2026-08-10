package az.fleetra.mobile.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.fleetra.mobile.network.ApiClient
import az.fleetra.mobile.network.dto.Cargo
import az.fleetra.mobile.network.dto.PublicTrackingResponse
import az.fleetra.mobile.network.dto.RatingRequest
import az.fleetra.mobile.network.dto.RatingResponse
import az.fleetra.mobile.network.extractErrorMessage
import az.fleetra.mobile.ui.common.LoadingView
import az.fleetra.mobile.ui.common.StatusBadge
import az.fleetra.mobile.ui.theme.FleetraBorder
import az.fleetra.mobile.ui.theme.FleetraDanger
import az.fleetra.mobile.ui.theme.FleetraDangerBg
import az.fleetra.mobile.ui.theme.FleetraInk
import az.fleetra.mobile.ui.theme.FleetraOrange
import az.fleetra.mobile.ui.theme.FleetraSuccess
import az.fleetra.mobile.ui.theme.FleetraSuccessBg
import az.fleetra.mobile.ui.theme.FleetraTextMuted
import kotlinx.coroutines.launch

// Müştəri üçün sifariş/reys detalı — GET /api/customer/cargo (siyahıdan
// cargoId ilə tapılır, qiymət/ödəniş statusu üçün) + izləmə nömrəsi
// varsa GET /api/tracking/{trackingNumber} (sürücü/marşrut/ETA üçün) ilə
// zənginləşdirilir. Backend-də ayrıca "cargo by id" endpoint yoxdur, ona
// görə siyahı yenidən yüklənir və içindən tapılır.
@Composable
fun OrderDetailScreen(cargoId: Long, onBack: () -> Unit) {
    var cargo by remember { mutableStateOf<Cargo?>(null) }
    var tracking by remember { mutableStateOf<PublicTrackingResponse?>(null) }
    var rating by remember { mutableStateOf<RatingResponse?>(null) }
    var ratingLoaded by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(cargoId) {
        loading = true
        errorMessage = null
        try {
            val found = ApiClient.customerApi.myOrders().firstOrNull { it.id == cargoId }
            cargo = found
            if (found == null) {
                errorMessage = "Sifariş tapılmadı"
            } else {
                val trackingNumber = found.trackingNumber
                if (!trackingNumber.isNullOrBlank()) {
                    try {
                        tracking = ApiClient.trackingApi.track(trackingNumber)
                    } catch (_: Exception) {
                        // İzləmə məlumatı hələ mövcud olmaya bilər (məs. reys
                        // hələ təyin edilməyib) — bu, əsas sifariş
                        // məlumatının göstərilməsinə mane olmamalıdır.
                    }
                }
                val tripId = found.trip?.id
                if (found.status == "DELIVERED" && tripId != null) {
                    try {
                        rating = ApiClient.customerApi.getRating(tripId)
                    } catch (_: Exception) {
                        // Reytinq yüklənə bilməsə də əsas sifariş məlumatı
                        // göstərilməlidir.
                    } finally {
                        ratingLoaded = true
                    }
                }
            }
        } catch (e: Exception) {
            errorMessage = "Sifariş yüklənə bilmədi"
        } finally {
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
            }
            Text("Sifariş detalı", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = FleetraInk)
        }

        if (loading) {
            LoadingView()
            return@Column
        }

        val currentCargo = cargo
        if (currentCargo == null) {
            Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                Text(errorMessage ?: "Sifariş tapılmadı", color = FleetraTextMuted)
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("#${currentCargo.trackingNumber ?: currentCargo.id}", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = FleetraInk)
                StatusBadge(currentCargo.status)
            }

            SectionCard(title = "Yük məlumatı") {
                DetailRow("Təsvir", currentCargo.description ?: "—")
                DetailRow("Çəki", currentCargo.weight?.let { "$it kq" } ?: "—")
                DetailRow("Həcm", currentCargo.volume?.let { "$it m³" } ?: "—")
                DetailRow("Növ", currentCargo.cargoType ?: "—")
                DetailRow("Təcililik", currentCargo.urgency ?: "—")
            }

            SectionCard(title = "Marşrut") {
                DetailRow("Götürmə", currentCargo.pickupAddress ?: "—")
                DetailRow("Çatdırılma", currentCargo.destinationAddress ?: "—")
                tracking?.estimatedEtaMinutes?.let { DetailRow("Təxmini vaxt", "$it dəqiqə") }
            }

            val driverName = tracking?.driverName
            if (!driverName.isNullOrBlank()) {
                SectionCard(title = "Sürücü") {
                    DetailRow("Ad", driverName)
                    tracking?.driverPhone?.let { DetailRow("Telefon", it) }
                    tracking?.vehiclePlate?.let { DetailRow("Nəqliyyat", it) }
                }
            }

            SectionCard(title = "Ödəniş") {
                DetailRow("Qiymət", currentCargo.price?.let { "$it AZN" } ?: "Hələ təyin edilməyib")
                val paid = currentCargo.paid == true
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (paid) FleetraSuccessBg else FleetraDangerBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            if (paid) "Ödənilib" else "Ödəniş gözlənilir",
                            color = if (paid) FleetraSuccess else FleetraDanger,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            tracking?.proofOfDeliveryUrl?.let {
                SectionCard(title = "Çatdırılma sübutu") {
                    Text("Çatdırılma şəkli mövcuddur", color = FleetraTextMuted, fontSize = 13.sp)
                }
            }

            val tripId = currentCargo.trip?.id
            if (currentCargo.status == "DELIVERED" && tripId != null && ratingLoaded) {
                SectionCard(title = "Reytinq") {
                    RatingSection(
                        tripId = tripId,
                        existingRating = rating,
                        onSubmitted = { rating = it },
                    )
                }
            }

            if (currentCargo.status == "CANCELLED" && !currentCargo.cancelReason.isNullOrBlank()) {
                SectionCard(title = "Ləğv səbəbi") {
                    Text(currentCargo.cancelReason ?: "", color = FleetraTextMuted, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, FleetraBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FleetraOrange)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = FleetraTextMuted, fontSize = 13.sp)
        Text(value, color = FleetraInk, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// Çatdırılmış reys üçün qiymətləndirmə — bax
// CustomerRatingController (/api/customer/trips/{tripId}/rating). Artıq
// qiymətləndirilibsə salt-oxu görünüş, əks halda ulduz+şərh forması.
@Composable
private fun RatingSection(
    tripId: Long,
    existingRating: RatingResponse?,
    onSubmitted: (RatingResponse) -> Unit,
) {
    if (existingRating != null) {
        Column {
            StarRow(stars = existingRating.stars ?: 0, onSelect = null)
            if (!existingRating.comment.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text("\"${existingRating.comment}\"", color = FleetraTextMuted, fontSize = 13.sp, fontStyle = FontStyle.Italic)
            }
        }
        return
    }

    var selectedStars by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column {
        Text("Bu reysi qiymətləndirin", color = FleetraInk, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        StarRow(stars = selectedStars, onSelect = { selectedStars = it })
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = comment,
            onValueChange = { if (it.length <= 500) comment = it },
            placeholder = { Text("Şərh (istəyə bağlı)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
        )

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = FleetraDanger, fontSize = 12.sp)
        }

        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                error = null
                submitting = true
                scope.launch {
                    try {
                        val res = ApiClient.customerApi.submitRating(
                            tripId,
                            RatingRequest(stars = selectedStars, comment = comment.trim().ifBlank { null }),
                        )
                        onSubmitted(res)
                    } catch (e: Exception) {
                        error = extractErrorMessage(e, "Reytinq göndərilmədi")
                    } finally {
                        submitting = false
                    }
                }
            },
            enabled = !submitting && selectedStars in 1..5,
            colors = ButtonDefaults.buttonColors(containerColor = FleetraOrange),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (submitting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Göndər", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StarRow(stars: Int, onSelect: ((Int) -> Unit)?) {
    Row {
        for (i in 1..5) {
            val filled = i <= stars
            val iconModifier = if (onSelect != null) {
                Modifier
                    .padding(end = 2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onSelect(i) }
            } else {
                Modifier.padding(end = 2.dp)
            }
            Icon(
                imageVector = if (filled) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = FleetraOrange,
                modifier = iconModifier.then(Modifier.size(28.dp)),
            )
        }
    }
}
