package com.vylune.music.ui.screens

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.vylune.music.ui.screens.home.ModernHomeScreen
import com.vylune.music.viewmodels.HomeViewModel

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
