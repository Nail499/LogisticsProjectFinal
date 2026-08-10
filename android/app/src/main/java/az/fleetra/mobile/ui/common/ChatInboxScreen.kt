package az.fleetra.mobile.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.fleetra.mobile.network.ApiClient
import az.fleetra.mobile.network.dto.ChatCargoSummary
import az.fleetra.mobile.ui.theme.FleetraBorder
import az.fleetra.mobile.ui.theme.FleetraInk
import az.fleetra.mobile.ui.theme.FleetraOrange
import az.fleetra.mobile.ui.theme.FleetraTextMuted

// Söhbət "inbox"u — bax ChatCargoController#cargoList (/api/chat/cargo-list),
// hər sifariş üçün bir yazı (kanal deyil). Rola görə hansı kanal
// düymələrinin göstəriləcəyi fərqlidir (bax ChatService#requireAccess):
// CUSTOMER -> [Sürücü (varsa), Dispetçer]; DRIVER -> [Müştəri, Dispetçer/Daxili].
@Composable
fun ChatInboxScreen(
    role: String,
    onBack: () -> Unit,
    onOpenChat: (cargoId: Long, channel: String, title: String) -> Unit,
) {
    var cargos by remember { mutableStateOf<List<ChatCargoSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            cargos = ApiClient.chatApi.cargoList()
        } catch (_: Exception) {
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
            Text("Söhbətlər", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = FleetraInk)
        }

        if (loading) {
            LoadingView()
            return@Column
        }

        if (cargos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                Text("Söhbət üçün sifariş yoxdur", color = FleetraTextMuted)
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 10.dp),
        ) {
            items(cargos) { c ->
                ChatCargoCard(cargo = c, role = role, onOpenChat = onOpenChat)
            }
        }
    }
}

@Composable
private fun ChatCargoCard(
    cargo: ChatCargoSummary,
    role: String,
    onOpenChat: (cargoId: Long, channel: String, title: String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, FleetraBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text("#${cargo.trackingNumber ?: cargo.cargoId}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FleetraInk)
        cargo.description?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, color = FleetraTextMuted, fontSize = 13.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (role == "CUSTOMER") {
                if (cargo.hasDriver) {
                    OutlinedButton(onClick = {
                        onOpenChat(cargo.cargoId, "CUSTOMER_DRIVER", cargo.driverName ?: "Sürücü")
                    }) {
                        Text("Sürücü", fontSize = 12.sp)
                    }
                }
                Button(
                    onClick = { onOpenChat(cargo.cargoId, "CUSTOMER_DISPATCHER", "Dispetçer") },
                    colors = ButtonDefaults.buttonColors(containerColor = FleetraOrange),
                ) {
                    Text("Dispetçer", fontSize = 12.sp)
                }
            } else if (role == "DRIVER") {
                OutlinedButton(onClick = {
                    onOpenChat(cargo.cargoId, "CUSTOMER_DRIVER", cargo.customerName ?: "Müştəri")
                }) {
                    Text("Müştəri", fontSize = 12.sp)
                }
                Button(
                    onClick = { onOpenChat(cargo.cargoId, "INTERNAL", "Dispetçer") },
                    colors = ButtonDefaults.buttonColors(containerColor = FleetraOrange),
                ) {
                    Text("Dispetçer", fontSize = 12.sp)
                }
            }
        }
    }
}
