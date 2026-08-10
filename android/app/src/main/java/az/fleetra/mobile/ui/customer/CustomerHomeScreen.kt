package az.fleetra.mobile.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.fleetra.mobile.ui.nav.CustomerTabs
import az.fleetra.mobile.ui.theme.FleetraInk
import az.fleetra.mobile.ui.theme.FleetraOrange
import az.fleetra.mobile.ui.theme.FleetraOrangeLight
import az.fleetra.mobile.ui.theme.FleetraTextMuted

@Composable
fun CustomerHomeScreen(username: String, onNavigate: (String) -> Unit) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(20.dp)) {
        Text("Xoş gəldin,", color = FleetraTextMuted, fontSize = 14.sp)
        Text(username, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = FleetraInk)
        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(FleetraOrange)
                .clickable { onNavigate(CustomerTabs.NEW_ORDER) }
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Yeni sifariş yarat", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Yükünüzü daşımaq üçün sorğu göndərin", color = Color(0xFFFFE8CC), fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(14.dp))

        QuickActionCard(
            icon = Icons.Default.List,
            title = "Sifarişlərim",
            onClick = { onNavigate(CustomerTabs.MY_ORDERS) },
        )
        Spacer(Modifier.height(14.dp))
        QuickActionCard(
            icon = Icons.Default.LocationOn,
            title = "Yükü izlə",
            onClick = { onNavigate(CustomerTabs.TRACK) },
        )
    }
}

@Composable
private fun QuickActionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FleetraOrangeLight)
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = FleetraOrange)
        Spacer(Modifier.width(14.dp))
        Text(title, color = FleetraInk, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}
