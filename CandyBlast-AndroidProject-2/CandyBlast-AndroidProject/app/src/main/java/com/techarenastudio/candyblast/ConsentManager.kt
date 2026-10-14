package com.techarenastudio.candyblast

import android.app.Activity
import android.webkit.JavascriptInterface
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Google UMP consent (EU / UK / etc). Exposed to the game's JavaScript as "AndroidConsent".
 * The popup shows only to users whose region requires it.
 */
class ConsentManager(
    private val activity: Activity,
    /** Called on the UI thread whenever consent state may have changed. */
    private val onChanged: () -> Unit
) {
    companion object {
        // TESTING THE EU POPUP: run a DEBUG build, open Logcat, search "ConsentDebugSettings",
        // copy the hashed device id Google prints, and paste it here. Leave "" for normal use.
        // It only has an effect in debug builds.
        private const val TEST_DEVICE_HASH = ""
    }

    private val consentInfo: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(activity)
    private val adsInitialized = AtomicBoolean(false)

    /** Call once on every app launch. */
    fun start() {
        val params = ConsentRequestParameters.Builder().apply {
            if (BuildConfig.DEBUG && TEST_DEVICE_HASH.isNotEmpty()) {
                setConsentDebugSettings(
                    ConsentDebugSettings.Builder(activity)
                        .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                        .addTestDeviceHashedId(TEST_DEVICE_HASH)
                        .build()
                )
            }
        }.build()

        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ -> finish() }
            },
            { _ -> finish() }   // e.g. offline: a previously saved choice may still allow ads
        )

        // A choice saved in an earlier session lets ads start immediately.
        if (consentInfo.canRequestAds()) finish()
    }

    private fun finish() {
        if (consentInfo.canRequestAds() && adsInitialized.compareAndSet(false, true)) {
            MobileAds.initialize(activity) { }
        }
        onChanged()
    }

    // ---- Called from JavaScript (WebView background thread) ----

    @JavascriptInterface
    fun canRequestAds(): Boolean = consentInfo.canRequestAds()

    @JavascriptInterface
    fun isPrivacyOptionsRequired(): Boolean =
        consentInfo.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    @JavascriptInterface
    fun showPrivacyOptions() {
        activity.runOnUiThread {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> finish() }
        }
    }
}
