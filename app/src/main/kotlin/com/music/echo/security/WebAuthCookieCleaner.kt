package iad1tya.echo.music.security

import android.webkit.CookieManager
import android.webkit.WebStorage

object WebAuthCookieCleaner {
    fun clearWebViewAuthState() {
        runCatching {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }

        runCatching {
            WebStorage.getInstance().deleteAllData()
        }
    }
}