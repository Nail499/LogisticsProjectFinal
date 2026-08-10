package az.fleetra.mobile.ui.common

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
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
import az.fleetra.mobile.config.ApiConfig
import az.fleetra.mobile.network.ApiClient
import az.fleetra.mobile.network.dto.ChatMessageRequest
import az.fleetra.mobile.network.dto.ChatMessageResponse
import az.fleetra.mobile.network.extractErrorMessage
import az.fleetra.mobile.ui.theme.FleetraBorder
import az.fleetra.mobile.ui.theme.FleetraDanger
import az.fleetra.mobile.ui.theme.FleetraInk
import az.fleetra.mobile.ui.theme.FleetraOrange
import az.fleetra.mobile.ui.theme.FleetraOrangeLight
import az.fleetra.mobile.ui.theme.FleetraTextMuted
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

// Rollar arası paylaşılan söhbət ekranı — bax ChatController
// (/api/chat/cargo/{cargoId}/messages). Backend-də STOMP send-handler
// YOXDUR (yalnız server->client push), ona görə canlı yeniləmə üçün sadə
// polling istifadə olunur (hər 4 saniyədə tam tarixçə yenidən yüklənir —
// backend pagination dəstəkləmir, bax tədqiqat qeydləri).
@Composable
fun ChatScreen(
    cargoId: Long,
    channel: String,
    title: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf<List<ChatMessageResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun reload(scrollToEnd: Boolean) {
        try {
            val res = ApiClient.chatApi.history(cargoId, channel)
            messages = res
            if (scrollToEnd && res.isNotEmpty()) {
                listState.animateScrollToItem(res.size - 1)
            }
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(cargoId, channel) {
        loading = true
        reload(scrollToEnd = true)
        loading = false
        while (true) {
            delay(4_000)
            reload(scrollToEnd = false)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val stream = context.contentResolver.openInputStream(uri)
                val bytes = stream?.readBytes()
                stream?.close()
                if (bytes != null) {
                    val body = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("image", "chat.jpg", body)
                    ApiClient.chatApi.sendImage(cargoId, channel, part)
                    reload(scrollToEnd = true)
                }
            } catch (e: Exception) {
                error = extractErrorMessage(e, "Şəkil göndərilmədi")
            }
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
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = FleetraInk)
        }

        if (loading) {
            LoadingView()
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 10.dp),
            ) {
                items(messages) { m -> MessageBubble(m) }
            }
        }

        if (error != null) {
            Text(error!!, color = FleetraDanger, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { imagePicker.launch("image/*") }) {
                Icon(Icons.Default.Image, contentDescription = "Şəkil göndər", tint = FleetraOrange)
            }
            OutlinedTextField(
                value = input,
                onValueChange = { if (it.length <= 2000) input = it },
                placeholder = { Text("Mesaj yazın...") },
                modifier = Modifier.weight(1f),
                maxLines = 4,
            )
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = {
                    val text = input.trim()
                    if (text.isBlank()) return@IconButton
                    sending = true
                    scope.launch {
                        try {
                            ApiClient.chatApi.sendMessage(cargoId, channel, ChatMessageRequest(text))
                            input = ""
                            reload(scrollToEnd = true)
                        } catch (e: Exception) {
                            error = extractErrorMessage(e, "Mesaj göndərilmədi")
                        } finally {
                            sending = false
                        }
                    }
                },
                enabled = !sending && input.isNotBlank(),
            ) {
                Icon(Icons.Default.Send, contentDescription = "Göndər", tint = FleetraOrange)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessageResponse) {
    val mine = message.mine
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (mine) FleetraOrange else FleetraOrangeLight)
                .widthIn(max = 260.dp)
                .padding(10.dp),
        ) {
            if (!mine) {
                Text(
                    message.senderName ?: "İstifadəçi",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (mine) Color.White else FleetraInk,
                )
                Spacer(Modifier.height(2.dp))
            }
            message.imageUrl?.let { url ->
                AsyncImage(
                    model = ApiConfig.BASE_URL.trimEnd('/') + url,
                    contentDescription = "Şəkil",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp)),
                )
                if (!message.message.isNullOrBlank()) Spacer(Modifier.height(6.dp))
            }
            message.message?.let {
                Text(it, color = if (mine) Color.White else FleetraInk, fontSize = 14.sp)
            }
            message.createdAt?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    it.take(16).replace("T", " "),
                    fontSize = 10.sp,
                    color = if (mine) Color.White.copy(alpha = 0.8f) else FleetraTextMuted,
                )
            }
        }
    }
}
