package echo.music.iad1tya.ui.screens

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import echo.music.iad1tya.ui.screens.home.ModernHomeScreen
import echo.music.iad1tya.viewmodels.HomeViewModel

@Composable
fun HomeScreen(
  navController: NavController,
  snackbarHostState: SnackbarHostState,
  viewModel: HomeViewModel = hiltViewModel(),
) {
  ModernHomeScreen(
    navController = navController,
    snackbarHostState = snackbarHostState,
    viewModel = viewModel,
  )
}
