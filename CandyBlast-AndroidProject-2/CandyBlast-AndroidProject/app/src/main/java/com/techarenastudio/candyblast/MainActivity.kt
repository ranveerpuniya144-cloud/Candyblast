package com.techarenastudio.candyblast

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewAssetLoader
import java.io.ByteArrayInputStream

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var consent: ConsentManager
    private lateinit var ads: AdBridge

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        val bg = Color.parseColor("#1A0F33")
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        webView = WebView(this).apply {
            setBackgroundColor(bg)
            overScrollMode = View.OVER_SCROLL_NEVER
            isLongClickable = false
            setOnLongClickListener { true }
        }
        val bannerHost = FrameLayout(this).apply { visibility = View.GONE }
        root.addView(webView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(bannerHost, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        setContentView(root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val b = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(b.left, b.top, b.right, b.bottom)
            insets
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true            // game progress is saved in localStorage
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = false
        }
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        // Order matters: ads need consent, consent calls back into ads.
        lateinit var adsRef: AdBridge
        consent = ConsentManager(this) { runOnUiThread { adsRef.onConsentChanged() } }
        ads = AdBridge(this, webView, bannerHost, consent)
        adsRef = ads

        webView.addJavascriptInterface(consent, "AndroidConsent")
        webView.addJavascriptInterface(ads.rewardedInterface, "AndroidRewardedAd")
        webView.addJavascriptInterface(ads.bannerInterface, "AndroidBanner")

        val loader = WebViewAssetLoader.Builder()
            .setDomain("appassets.androidplatform.net")
            .addPathHandler("/app/", GameHandler(this))
            .build()

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView, request: WebResourceRequest
            ): WebResourceResponse? = loader.shouldInterceptRequest(request.url)

            // Keep the player inside the game: ignore any outside navigation.
            override fun shouldOverrideUrlLoading(
                view: WebView, request: WebResourceRequest
            ): Boolean = request.url.host != "appassets.androidplatform.net"
        }

        webView.loadUrl("https://appassets.androidplatform.net/app/index.html")
        consent.start()
    }

    override fun onPause() { super.onPause(); webView.onPause(); ads.onPause() }
    override fun onResume() { super.onResume(); webView.onResume(); ads.onResume() }
    override fun onDestroy() {
        ads.onDestroy()
        (webView.parent as? android.view.ViewGroup)?.removeView(webView)
        webView.destroy()
        super.onDestroy()
    }
}

/**
 * Serves assets/index.html. In DEBUG builds it switches the game's ad environment to 'test'
 * (Google sample ads), so you can never tap your own real ads while developing.
 * RELEASE builds serve the file unchanged ('production' ads).
 */
private class GameHandler(private val ctx: Context) : WebViewAssetLoader.PathHandler {
    override fun handle(path: String): WebResourceResponse? {
        if (path != "index.html") return null
        var html = ctx.assets.open("index.html").bufferedReader(Charsets.UTF_8).use { it.readText() }
        if (BuildConfig.DEBUG) {
            html = html.replace(Regex("environment:\\s*'production',"), "environment: 'test',")
        }
        return WebResourceResponse("text/html", "UTF-8", ByteArrayInputStream(html.toByteArray(Charsets.UTF_8)))
    }
}
