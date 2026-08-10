package az.fleetra.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import az.fleetra.mobile.ui.auth.AuthViewModel
import az.fleetra.mobile.ui.auth.ForgotPasswordScreen
import az.fleetra.mobile.ui.auth.LoginScreen
import az.fleetra.mobile.ui.auth.RegisterScreen
import az.fleetra.mobile.ui.auth.ResetPasswordScreen
import az.fleetra.mobile.ui.auth.VerifyEmailScreen
import az.fleetra.mobile.ui.common.LoadingView
import az.fleetra.mobile.ui.common.UnsupportedRoleScreen
import az.fleetra.mobile.ui.customer.CustomerRootScreen
import az.fleetra.mobile.ui.driver.DriverRootScreen
import az.fleetra.mobile.ui.theme.FleetraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FleetraTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FleetraApp()
                }
            }
        }
    }
}

@Composable
fun FleetraApp() {
    val authViewModel: AuthViewModel = viewModel()
    val loading by authViewModel.loading.collectAsState()
    val session by authViewModel.session.collectAsState()

    when {
        loading -> LoadingView()
        // session == null olduğu müddətcə bütün giriş/qeydiyyat/bərpa
        // axını bu daxili NavHost-da idarə olunur. session dəyişən kimi
        // (login və ya verify-email uğurlu olduqda) yuxarıdakı `when`
        // yenidən qiymətləndirilir və birbaşa CustomerRootScreen/
        // DriverRootScreen-ə keçir — bu NavHost-un öz vəziyyəti önəmsizləşir.
        session == null -> AuthNavHost(authViewModel)
        session!!.role == "CUSTOMER" -> CustomerRootScreen(session = session!!, authViewModel = authViewModel)
        session!!.role == "DRIVER" -> DriverRootScreen(session = session!!, authViewModel = authViewModel)
        else -> UnsupportedRoleScreen(role = session!!.role, onLogout = { authViewModel.logout() })
    }
}

@Composable
private fun AuthNavHost(authViewModel: AuthViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                onLoggedIn = {},
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateToForgotPassword = { navController.navigate("forgot_password") },
            )
        }
        composable("register") {
            RegisterScreen(
                authViewModel = authViewModel,
                onRegistered = { username -> navController.navigate("verify_email/$username") },
                onNavigateToLogin = { navController.popBackStack("login", inclusive = false) },
            )
        }
        composable("verify_email/{username}") { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username").orEmpty()
            VerifyEmailScreen(authViewModel = authViewModel, username = username)
        }
        composable("forgot_password") {
            ForgotPasswordScreen(
                authViewModel = authViewModel,
                onCodeSent = { email -> navController.navigate("reset_password/$email") },
                onNavigateToLogin = { navController.popBackStack("login", inclusive = false) },
            )
        }
        composable("reset_password/{email}") { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email").orEmpty()
            ResetPasswordScreen(
                authViewModel = authViewModel,
                email = email,
                onResetSuccess = {
                    navController.navigate("login") { popUpTo("login") { inclusive = true } }
                },
            )
        }
    }
}
