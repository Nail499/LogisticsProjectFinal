package az.fleetra.mobile.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.fleetra.mobile.ui.theme.*

private data class StatusStyle(val bg: Color, val fg: Color, val label: String)

private val STATUS_STYLES = mapOf(
    "PENDING" to StatusStyle(FleetraOrangeLight, FleetraOrange, "Gözləmədə"),
    "ASSIGNED" to StatusStyle(FleetraOrangeLight, FleetraOrange, "Təyin edilib"),
    "PLANNED" to StatusStyle(FleetraOrangeLight, FleetraOrange, "Planlaşdırılıb"),
    "PICKED_UP" to StatusStyle(FleetraOrangeLight, FleetraOrange, "Götürülüb"),
    "IN_TRANSIT" to StatusStyle(Color(0xFFEFF6FF), Color(0xFF2563EB), "Yoldadır"),
    "DELIVERED" to StatusStyle(FleetraSuccessBg, FleetraSuccess, "Çatdırılıb"),
)

@Composable
fun StatusBadge(status: String?) {
    val style = STATUS_STYLES[status] ?: StatusStyle(FleetraBorder, FleetraTextMuted, status ?: "—")
    Text(
        text = style.label,
        color = style.fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(style.bg, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
