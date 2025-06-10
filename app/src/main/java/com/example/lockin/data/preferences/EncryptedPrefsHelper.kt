package com.example.lockin.data.preferences

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

object EncryptedPrefsHelper {
    private const val PREFS_NAME = "lockin_prefs"

    fun getEncryptedPrefs(context: Context): EncryptedSharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            context.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }
}