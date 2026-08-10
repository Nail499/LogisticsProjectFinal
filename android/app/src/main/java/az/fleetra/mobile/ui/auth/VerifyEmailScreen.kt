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
import az.fleetra.mobile.ui.theme.FleetraSuccess
import az.fleetra.mobile.ui.theme.FleetraTextMuted
import kotlinx.coroutines.launch

// RegisterScreen-dən sonra göstərilir — email-ə göndərilən 6 rəqəmli kodu
// təsdiqlədikdə avtomatik giriş edilir (bax AuthController#verifyEmail).
@Composable
fun VerifyEmailScreen(
    authViewModel: AuthViewModel,
    username: String,
) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var resending by remember { mutableStateOf(false) }
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
            Text("Email təsdiqi", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "$username üçün email ünvanınıza göndərilən kodu daxil edin",
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

            if (error != null) {
                Spacer(Modifier.height(14.dp))
                Text(error!!, color = FleetraDanger, textAlign = TextAlign.Center, fontSize = 13.sp)
            }
            if (info != null) {
                Spacer(Modifier.height(14.dp))
                Text(info!!, color = FleetraSuccess, textAlign = TextAlign.Center, fontSize = 13.sp)
            }

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = {
                    error = null
                    info = null
                    loading = true
                    scope.launch {
                        val result = authViewModel.verifyEmail(username, code.trim())
                        loading = false
                        // Uğurlu olduqda session dəyişir, FleetraApp avtomatik
                        // CustomerRootScreen-ə keçir — burada əlavə naviqasiya
                        // etməyə ehtiyac yoxdur.
                        result.onFailure { error = it.message }
                    }
                },
                enabled = !loading && code.trim().length == 6,
                colors = ButtonDefaults.buttonColors(containerColor = FleetraOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Təsdiqlə", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))
            TextButton(
                onClick = {
                    error = null
                    info = null
                    resending = true
                    scope.launch {
                        val result = authViewModel.resendVerificationCode(username)
                        resending = false
                        result.onSuccess { info = it }.onFailure { error = it.message }
                    }
                },
                enabled = !resending,
            ) {
                Text(if (resending) "Göndərilir..." else "Kodu yenidən göndər", color = FleetraTextMuted, fontSize = 13.sp)
            }
        }
    }
}
