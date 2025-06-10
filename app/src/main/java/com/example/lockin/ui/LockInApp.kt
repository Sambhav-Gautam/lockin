package com.example.lockin.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lockin.viewmodel.LockInViewModel
import com.example.lockin.viewmodel.LockInViewModelFactory

@Composable
fun LockInApp(viewModel: LockInViewModel = viewModel(
    factory = LockInViewModelFactory(androidx.compose.ui.platform.LocalContext.current.applicationContext)
)) {
    MainScreen(viewModel)
}