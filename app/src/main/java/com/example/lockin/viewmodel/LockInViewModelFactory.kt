package com.example.lockin.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
class LockInViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LockInViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LockInViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}