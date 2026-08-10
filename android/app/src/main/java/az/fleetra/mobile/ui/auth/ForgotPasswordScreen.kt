package az.fleetra.mobile.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.fleetra.mobile.ui.theme.FleetraDanger
import az.fleetra.mobile.ui.theme.FleetraOrange
import az.fleetra.mobile.ui.theme.FleetraTextMuted
import kotlinx.coroutines.launch

// Şifrə bərpası — 1-ci addım: email daxil et, kod göndərilsin (bax
// AuthController#forgotPassword, cavab həmişə eyni ümumi mesajdır —
// email-in mövcud olub-olmadığını sızdırmır).
@Composable
fun ForgotPasswordScreen(
    authViewModel: AuthViewModel,
    onCodeSent: (email: String) -> Unit,
    onNavigateToLogin: () -> Unit = {},
) {
    var email by remember { mutableStateOf("") }
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
            Text("Şifrəni bərpa et", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Qeydiyyat email ünvanınızı daxil edin, bərpa kodu göndərəcəyik",
                color = FleetraTextMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
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
                        val trimmed = email.trim()
                        val result = authViewModel.forgotPassword(trimmed)
                        loading = false
                        result.onSuccess { onCodeSent(trimmed) }
                            .onFailure { error = it.message }
                    }
                },
                enabled = !loading && email.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = FleetraOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Kod göndər", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onNavigateToLogin) {
                Text("Girişə qayıt", color = FleetraTextMuted, fontSize = 13.sp)
            }
        }
    }
}
