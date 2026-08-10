package az.fleetra.mobile.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import az.fleetra.mobile.config.ApiConfig
import az.fleetra.mobile.network.ApiClient
import az.fleetra.mobile.network.dto.CredentialsUpdateRequest
import az.fleetra.mobile.network.dto.ProfileResponse
import az.fleetra.mobile.network.dto.ProfileUpdateRequest
import az.fleetra.mobile.network.extractErrorMessage
import az.fleetra.mobile.ui.common.LoadingView
import az.fleetra.mobile.ui.theme.FleetraDanger
import az.fleetra.mobile.ui.theme.FleetraInk
import az.fleetra.mobile.ui.theme.FleetraOrange
import az.fleetra.mobile.ui.theme.FleetraOrangeLight
import az.fleetra.mobile.ui.theme.FleetraSuccess
import az.fleetra.mobile.ui.theme.FleetraTextMuted
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@Composable
fun ProfileScreen(role: String, onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hasExtraFields = role == "CUSTOMER" || role == "DRIVER"

    var profile by remember { mutableStateOf<ProfileResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var uploading by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var savingCreds by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<Pair<Boolean, String>?>(null) } // (isError, text)

    var fullName by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var nationality by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    var newUsername by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }

    fun loadProfile() {
        scope.launch {
            try {
                val res = ApiClient.profileApi.me()
                profile = res
                fullName = res.fullName ?: ""
                dateOfBirth = res.dateOfBirth ?: ""
                nationality = res.nationality ?: ""
                location = res.location ?: ""
                newUsername = res.username
            } catch (_: Exception) {
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadProfile() }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        uploading = true
        scope.launch {
            try {
                val stream = context.contentResolver.openInputStream(uri)
                val bytes = stream?.readBytes()
                stream?.close()
                if (bytes != null) {
                    val body = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("photo", "profile.jpg", body)
                    profile = ApiClient.profileApi.uploadPhoto(part)
                    notice = false to "Şəkil yükləndi"
                }
            } catch (e: Exception) {
                notice = true to extractErrorMessage(e, "Şəkil yüklənmədi")
            } finally {
                uploading = false
            }
        }
    }

    if (loading) {
        LoadingView()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Profil ayarları", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text("Şəxsi məlumatlarınızı və giriş detallarınızı buradan tənzimləyin", color = FleetraTextMuted, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))

        notice?.let { (isError, text) ->
            Text(text, color = if (isError) FleetraDanger else FleetraSuccess, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
        }

        if (hasExtraFields) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 18.dp)) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(FleetraOrangeLight)
                        .border(2.dp, FleetraOrange, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    val photoUrl = profile?.photoUrl
                    if (photoUrl != null) {
                        AsyncImage(
                            model = ApiConfig.BASE_URL.trimEnd('/') + photoUrl,
                            contentDescription = "Profil şəkli",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = FleetraOrange)
                    }
                }
                Spacer(Modifier.width(14.dp))
                TextButton(onClick = { photoPicker.launch("image/*") }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = FleetraOrange)
                    Spacer(Modifier.width(6.dp))
                    Text(if (uploading) "Yüklənir..." else "Şəkil dəyiş", color = FleetraOrange)
                }
            }
        }

        Text("Şəxsi məlumatlar", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FleetraInk)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Ad Soyad") }, modifier = Modifier.fillMaxWidth())

        if (hasExtraFields) {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = dateOfBirth, onValueChange = { dateOfBirth = it },
                label = { Text("Doğum tarixi (YYYY-AA-GÜN)") }, placeholder = { Text("1995-04-12") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = nationality, onValueChange = { nationality = it }, label = { Text("Millət") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Yaşadığı yer") }, modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                saving = true
                scope.launch {
                    try {
                        profile = ApiClient.profileApi.update(
                            ProfileUpdateRequest(
                                fullName = fullName,
                                dateOfBirth = dateOfBirth.ifBlank { null },
                                nationality = nationality.ifBlank { null },
                                location = location.ifBlank { null },
                            )
                        )
                        notice = false to "Profil yeniləndi"
                    } catch (e: Exception) {
                        notice = true to extractErrorMessage(e, "Profil yenilənmədi")
                    } finally {
                        saving = false
                    }
                }
            },
            enabled = !saving,
            colors = ButtonDefaults.buttonColors(containerColor = FleetraOrange),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (saving) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("Yadda saxla", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(28.dp))
        Text("Giriş məlumatları", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FleetraInk)
        Spacer(Modifier.height(4.dp))
        Text("$role hesabı — ${profile?.username}", color = FleetraTextMuted, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = newUsername, onValueChange = { newUsername = it }, label = { Text("İstifadəçi adı") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = newPassword, onValueChange = { newPassword = it },
            label = { Text("Yeni şifrə (boş saxlasanız dəyişməz)") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = currentPassword, onValueChange = { currentPassword = it },
            label = { Text("Cari şifrə (təsdiq üçün)") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (currentPassword.isBlank()) {
                    notice = true to "Cari şifrənizi daxil edin"
                    return@Button
                }
                savingCreds = true
                val usernameChanged = newUsername.isNotBlank() && newUsername != profile?.username
                scope.launch {
                    try {
                        ApiClient.profileApi.updateCredentials(
                            CredentialsUpdateRequest(
                                currentPassword = currentPassword,
                                newUsername = newUsername.ifBlank { null },
                                newPassword = newPassword.ifBlank { null },
                            )
                        )
                        currentPassword = ""
                        newPassword = ""
                        if (usernameChanged) {
                            notice = false to "İstifadəçi adı dəyişdi, yenidən daxil olun"
                            onLogout()
                        } else {
                            notice = false to "Giriş məlumatları yeniləndi"
                        }
                    } catch (e: Exception) {
                        notice = true to extractErrorMessage(e, "Şifrə yenilənmədi — cari şifrəni yoxlayın")
                    } finally {
                        savingCreds = false
                    }
                }
            },
            enabled = !savingCreds,
            colors = ButtonDefaults.buttonColors(containerColor = FleetraOrange),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (savingCreds) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("Giriş məlumatlarını yenilə", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = onLogout,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = FleetraDanger),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Çıxış")
        }
        Spacer(Modifier.height(24.dp))
    }
}
