package az.fleetra.mobile.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import az.fleetra.mobile.data.UserSession
import az.fleetra.mobile.ui.theme.FleetraDanger
import az.fleetra.mobile.ui.theme.FleetraOrange
import az.fleetra.mobile.ui.theme.FleetraTextMuted
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = viewModel(),
    onLoggedIn: (UserSession) -> Unit,
    onNavigateToRegister: () -> Unit = {},
    onNavigateToForgotPassword: () -> Unit = {},
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row {
                Text("Fleet", fontSize = 28.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold)
                Text("ra", fontSize = 28.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = FleetraOrange)
            }
            Spacer(Modifier.height(4.dp))
            Text("Hesabınıza daxil olun", color = FleetraTextMuted, fontSize = 14.sp)
            Spacer(Modifier.height(28.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("İstifadəçi adı") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Şifrə") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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
                        val result = authViewModel.login(username.trim(), password)
                        loading = false
                        result.onSuccess { onLoggedIn(it) }
                            .onFailure { error = it.message }
                    }
                },
                enabled = !loading && username.isNotBlank() && password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = FleetraOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Daxil ol", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onNavigateToForgotPassword) {
                Text("Şifrəni unutmusunuz?", color = FleetraTextMuted, fontSize = 13.sp)
            }

            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hesabınız yoxdur?", color = FleetraTextMuted, fontSize = 13.sp)
                TextButton(onClick = onNavigateToRegister) {
                    Text("Qeydiyyatdan keçin", color = FleetraOrange, fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }
        }
    }
}
