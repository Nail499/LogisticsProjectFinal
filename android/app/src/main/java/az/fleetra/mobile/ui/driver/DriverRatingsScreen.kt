package az.fleetra.mobile.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import az.fleetra.mobile.network.dto.RatingDetailResponse
import az.fleetra.mobile.network.dto.RatingSummary
import az.fleetra.mobile.ui.common.LoadingView
import az.fleetra.mobile.ui.theme.FleetraBorder
import az.fleetra.mobile.ui.theme.FleetraInk
import az.fleetra.mobile.ui.theme.FleetraOrange
import az.fleetra.mobile.ui.theme.FleetraOrangeLight
import az.fleetra.mobile.ui.theme.FleetraTextMuted

// Sürücünün öz reytinqləri — bax DriverController#myRatings /
// #myRatingSummary (ratings/summary).
@Composable
fun DriverRatingsScreen() {
    var summary by remember { mutableStateOf<RatingSummary?>(null) }
    var ratings by remember { mutableStateOf<List<RatingDetailResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            summary = ApiClient.driverApi.myRatingSummary()
            ratings = ApiClient.driverApi.myRatings()
        } catch (_: Exception) {
            // swallow — boş görünüş acceptable degrade
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
        Text("Reytinqim", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Spacer(Modifier.height(16.dp))

        summary?.let { s ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(FleetraOrangeLight)
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (s.count > 0) String.format("%.1f", s.average) else "—",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 30.sp,
                        color = FleetraInk,
                    )
                    Spacer(Modifier.width(10.dp))
                    StaticStarRow(average = s.average)
                }
                Spacer(Modifier.height(4.dp))
                Text("${s.count} qiymətləndirmə", color = FleetraTextMuted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(16.dp))
        }

        if (ratings.isEmpty()) {
            Text("Hələ qiymətləndirmə yoxdur", color = FleetraTextMuted)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(ratings) { rating -> RatingCard(rating) }
            }
        }
    }
}

@Composable
private fun RatingCard(rating: RatingDetailResponse) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, FleetraBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            StaticStarRow(average = (rating.stars ?: 0).toDouble())
            rating.createdAt?.let { Text(it, color = FleetraTextMuted, fontSize = 11.sp) }
        }
        rating.customerName?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = FleetraInk, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        rating.routeInfo?.let {
            Spacer(Modifier.height(2.dp))
            Text(it, color = FleetraTextMuted, fontSize = 12.sp)
        }
        if (!rating.comment.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text("\"${rating.comment}\"", color = FleetraTextMuted, fontSize = 13.sp, fontStyle = FontStyle.Italic)
        }
    }
}

@Composable
private fun StaticStarRow(average: Double) {
    Row {
        for (i in 1..5) {
            val filled = i <= Math.round(average)
            Icon(
                imageVector = if (filled) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = FleetraOrange,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
