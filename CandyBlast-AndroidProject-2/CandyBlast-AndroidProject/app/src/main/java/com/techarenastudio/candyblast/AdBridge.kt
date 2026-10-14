package com.techarenastudio.candyblast

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.FrameLayout
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import org.json.JSONArray
import org.json.JSONObject

/**
 * Native side of the game's ad system (AdMob only).
 *  - AndroidRewardedAd.showRewardedHintAd(requestId)  -> rewarded ad for a hint
 *  - AndroidBanner.showBanner(unitId) / hideBanner()  -> home screen banner
 * Ad unit IDs come from the HTML's AdConfig (test IDs in debug builds, real IDs in release).
 * Results go back to the page through HintAccessManager.handleNativeEvent(...).
 */
class AdBridge(
    private val activity: Activity,
    private val webView: WebView,
    private val bannerHost: FrameLayout,
    private val consent: ConsentManager
) {
    private val main = Handler(Looper.getMainLooper())

    private var rewarded: RewardedAd? = null
    private var loading = false
    private var pendingId: String? = null
    private var pendingTimeout: Runnable? = null

    private var banner: AdView? = null
    private var bannerUnit: String? = null

    val rewardedInterface = RewardedInterface()
    val bannerInterface = BannerInterface()

    inner class RewardedInterface {
        @JavascriptInterface fun showRewardedHintAd(requestId: String) { main.post { requestRewarded(requestId) } }
        @JavascriptInterface fun showRewardedAd(requestId: String) { main.post { requestRewarded(requestId) } }
    }

    inner class BannerInterface {
        @JavascriptInterface fun showBanner(unitId: String) { main.post { showBannerNow(unitId) } }
        @JavascriptInterface fun hideBanner() { main.post { hideBannerNow() } }
    }

    /** Called after consent state changes: preload a rewarded ad if allowed. */
    fun onConsentChanged() {
        if (consent.canRequestAds()) loadRewarded()
        webView.evaluateJavascript(
            "window.CandyBlastConsent && window.CandyBlastConsent.update()", null
        )
    }

    // ---------------- Rewarded ----------------

    private fun sendEvent(event: String, id: String, reason: String? = null) {
        val js = "window.HintAccessManager && window.HintAccessManager.handleNativeEvent(" +
            JSONObject.quote(event) + "," + JSONObject.quote(id) +
            (if (reason != null) "," + JSONObject.quote(reason) else "") + ")"
        webView.post { webView.evaluateJavascript(js, null) }
    }

    private fun requestRewarded(id: String) {
        if (!consent.canRequestAds()) { sendEvent("REWARDED_AD_FAILED", id, "consent"); return }
        if (pendingId != null) { sendEvent("REWARDED_AD_FAILED", id, "busy"); return }
        val ad = rewarded
        if (ad != null) { present(ad, id); return }
        // Not preloaded yet: load now and show when ready (max 10 seconds).
        pendingId = id
        val timeout = Runnable {
            val p = pendingId
            if (p != null) { pendingId = null; sendEvent("REWARDED_AD_FAILED", p, "timeout") }
        }
        pendingTimeout = timeout
        main.postDelayed(timeout, 10_000)
        loadRewarded()
    }

    private fun clearPending() {
        pendingTimeout?.let { main.removeCallbacks(it) }
        pendingTimeout = null
        pendingId = null
    }

    private fun readConfig(cb: (rewardedUnit: String?, bannerUnit: String?) -> Unit) {
        webView.evaluateJavascript("JSON.stringify(window.AdConfig.getPublicConfig())") { raw ->
            try {
                val text = JSONArray("[$raw]").getString(0)
                val o = JSONObject(text)
                fun s(k: String) = if (o.isNull(k)) null else o.getString(k).takeIf { it.isNotEmpty() }
                cb(s("rewardedHintAdUnitId"), s("homeScreenAdUnitId"))
            } catch (e: Exception) {
                cb(null, null)
            }
        }
    }

    private fun loadRewarded() {
        if (loading || rewarded != null) return
        if (!consent.canRequestAds()) return
        loading = true
        readConfig { unit, _ ->
            if (unit == null) {
                loading = false
                pendingId?.let { sendEvent("REWARDED_AD_FAILED", it, "no-unit"); clearPending() }
                return@readConfig
            }
            RewardedAd.load(activity, unit, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    loading = false
                    rewarded = ad
                    pendingId?.let { id -> clearPending(); rewarded = null; present(ad, id) }
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                    rewarded = null
                    pendingId?.let { id -> clearPending(); sendEvent("REWARDED_AD_FAILED", id, "load:" + error.code) }
                }
            })
        }
    }

    private fun present(ad: RewardedAd, id: String) {
        rewarded = null
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                sendEvent(if (earned) "REWARDED_AD_COMPLETED" else "REWARDED_AD_CLOSED", id)
                loadRewarded()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                sendEvent("REWARDED_AD_FAILED", id, "show:" + error.code)
                loadRewarded()
            }
        }
        ad.show(activity) { earned = true }
    }

    // ---------------- Banner ----------------

    private fun showBannerNow(unitId: String) {
        if (!consent.canRequestAds()) return
        if (banner == null || bannerUnit != unitId) {
            banner?.destroy()
            bannerHost.removeAllViews()
            val dm = activity.resources.displayMetrics
            val widthDp = (dm.widthPixels / dm.density).toInt()
            val view = AdView(activity)
            view.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp))
            view.adUnitId = unitId
            bannerHost.addView(view)
            view.loadAd(AdRequest.Builder().build())
            banner = view
            bannerUnit = unitId
        }
        banner?.resume()
        bannerHost.visibility = View.VISIBLE
    }

    private fun hideBannerNow() {
        bannerHost.visibility = View.GONE
        banner?.pause()
    }

    // ---------------- Lifecycle ----------------

    fun onPause() { banner?.pause() }
    fun onResume() { if (bannerHost.visibility == View.VISIBLE) banner?.resume() }
    fun onDestroy() {
        clearPending()
        banner?.destroy()
        banner = null
    }
}
