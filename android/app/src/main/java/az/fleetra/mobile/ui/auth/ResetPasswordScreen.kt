package az.fleetra.mobile.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
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
import az.fleetra.mobile.ui.theme.FleetraDanger
import az.fleetra.mobile.ui.theme.FleetraOrange
import az.fleetra.mobile.ui.theme.FleetraTextMuted
import kotlinx.coroutines.launch

// Şifrə bərpası — 2-ci addım: kod + yeni şifrə (bax
// AuthController#resetPassword). Uğurlu olduqda token qaytarılmır,
// istifadəçi Login ekranına göndərilib yeni şifrə ilə daxil olmalıdır.
@Composable
fun ResetPasswordScreen(
    authViewModel: AuthViewModel,
    email: String,
    onResetSuccess: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Yeni şifrə təyin et", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "$email ünvanına göndərilən kodu və yeni şifrənizi daxil edin",
                color = FleetraTextMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = code,
                onValueChange = { if (it.length <= 6) code = it },
                label = { Text("Təsdiq kodu") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("Yeni şifrə") },
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
                        val result = authViewModel.resetPassword(email, code.trim(), newPassword)
                        loading = false
                        result.onSuccess { onResetSuccess() }
                            .onFailure { error = it.message }
                    }
                },
                enabled = !loading && code.trim().length == 6 && newPassword.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = FleetraOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Şifrəni yenilə", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
