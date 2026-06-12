package iad1tya.echo.music.viewmodels

import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iad1tya.echo.music.App
import iad1tya.echo.music.constants.AccountChannelHandleKey
import iad1tya.echo.music.constants.AccountEmailKey
import iad1tya.echo.music.constants.AccountNameKey
import iad1tya.echo.music.constants.DataSyncIdKey
import iad1tya.echo.music.constants.InnerTubeCookieKey
import iad1tya.echo.music.constants.VisitorDataKey
import iad1tya.echo.music.security.SecureAuthStore
import iad1tya.echo.music.utils.SyncUtils
import iad1tya.echo.music.utils.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AccountSettingsViewModel
    @Inject
    constructor(
        private val syncUtils: SyncUtils,
    ) : ViewModel() {
        fun logoutAndClearSyncedContent(
            context: Context,
            onCookieChange: (String) -> Unit,
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                syncUtils.clearAllSyncedContent()
                App.forgetAccount(context)

                withContext(Dispatchers.Main) {
                    onCookieChange("")
                }
            }
        }

        fun logoutKeepData(
            context: Context,
            onCookieChange: (String) -> Unit,
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                App.forgetAccount(context)

                withContext(Dispatchers.Main) {
                    onCookieChange("")
                }
            }
        }

        fun saveTokenAndRestart(
            context: Context,
            cookie: String,
            visitorData: String,
            dataSyncId: String,
            accountName: String,
            accountEmail: String,
            accountChannelHandle: String,
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                val normalizedDataSyncId = normalizeDataSyncId(dataSyncId)

                SecureAuthStore.put(
                    context,
                    SecureAuthStore.YOUTUBE_COOKIE,
                    cookie,
                )

                SecureAuthStore.put(
                    context,
                    SecureAuthStore.YOUTUBE_VISITOR_DATA,
                    visitorData,
                )

                SecureAuthStore.put(
                    context,
                    SecureAuthStore.YOUTUBE_DATA_SYNC_ID,
                    normalizedDataSyncId,
                )

                context.dataStore.edit { settings ->
                    settings[InnerTubeCookieKey] = SecureAuthStore.STORED_MARKER
                    settings[VisitorDataKey] = SecureAuthStore.STORED_MARKER
                    settings[DataSyncIdKey] = SecureAuthStore.STORED_MARKER

                    settings[AccountNameKey] = accountName
                    settings[AccountEmailKey] = accountEmail
                    settings[AccountChannelHandleKey] = accountChannelHandle
                }

                withContext(Dispatchers.Main) {
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    context.startActivity(intent)
                    Runtime.getRuntime().exit(0)
                }
            }
        }

        private fun normalizeDataSyncId(value: String): String =
            value.takeIf { !it.contains("||") }
                ?: value.takeIf { it.endsWith("||") }?.substringBefore("||")
                ?: value.substringAfter("||")
    }
