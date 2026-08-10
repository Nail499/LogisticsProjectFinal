package az.fleetra.mobile.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.fleetra.mobile.network.dto.RegisterCustomerRequest
import az.fleetra.mobile.ui.theme.FleetraDanger
import az.fleetra.mobile.ui.theme.FleetraOrange
import az.fleetra.mobile.ui.theme.FleetraTextMuted
import kotlinx.coroutines.launch

// Müştəri özü-qeydiyyat — bax AuthController#registerCustomer. Uğurlu
// olduqda token qaytarılmır, istifadəçi VerifyEmailScreen-ə yönləndirilir.
@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onRegistered: (username: String) -> Unit,
    onNavigateToLogin: () -> Unit = {},
) {
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val canSubmit = fullName.isNotBlank() && phone.isNotBlank() && email.isNotBlank() &&
        username.isNotBlank() && password.isNotBlank() && !loading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
        ) {
            Text("Qeydiyyatdan keç", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text("Fleetra hesabı yaradın", color = FleetraTextMuted, fontSize = 14.sp)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Ad Soyad") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Telefon") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("Şirkət adı (istəyə bağlı)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("İstifadəçi adı") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Şifrə") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Ən azı 8 simvol, 1 böyük, 1 kiçik hərf və 1 rəqəm",
                color = FleetraTextMuted,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )

            if (error != null) {
                Spacer(Modifier.height(14.dp))
                Text(error!!, color = FleetraDanger, textAlign = TextAlign.Center, fontSize = 13.sp)
            }

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = {
                    error = null
                    loading = true
                    scope.launch {
                        val result = authViewModel.registerCustomer(
                            RegisterCustomerRequest(
                                fullName = fullName.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                companyName = companyName.trim().ifBlank { null },
                                username = username.trim(),
                                password = password,
                            ),
                        )
                        loading = false
                        result.onSuccess { onRegistered(it.username ?: username.trim()) }
                            .onFailure { error = it.message }
                    }
                },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = FleetraOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Qeydiyyatdan keç", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onNavigateToLogin) {
                Text("Artıq hesabınız var? Daxil olun", color = FleetraTextMuted, fontSize = 13.sp)
            }
        }
    }
}
