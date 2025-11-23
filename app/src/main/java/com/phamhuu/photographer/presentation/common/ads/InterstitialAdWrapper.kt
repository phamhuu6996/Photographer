package com.phamhuu.photographer.presentation.common.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.phamhuu.photographer.contants.AdMobConstants

/**
 * Interstitial Ad Wrapper Component
 * Wraps content and manages Interstitial Ad lifecycle
 *
 * Usage:
 * ```
 * InterstitialAdWrapper(
 *     adUnitId = AdMobConstants.INTERSTITIAL_AD_UNIT_ID,
 *     showAd = shouldShowAd,
 *     onAdDismissed = { /* handle */ }
 * ) {
 *     // Your content here
 *     YourScreenContent()
 * }
 * ```
 *
 * @param adUnitId Ad Unit ID for interstitial ad
 * @param showAd Trigger to show ad (set to true when you want to show ad)
 * @param onAdDismissed Callback when ad is dismissed
 * @param onAdFailedToShow Callback when ad fails to show
 * @param content Content to wrap
 */
@Composable
fun InterstitialAdWrapper(
    showAd: Boolean = false,
    onAdDismissed: () -> Unit = {},
    onAdFailedToShow: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val interstitialAdManager = remember { InterstitialAdManager(context) }
    val adsId = AdMobConstants.INTERSTITIAL_AD_UNIT_ID

    // Load ad when composable is first created
    LaunchedEffect(adsId) {
        interstitialAdManager.loadAd(adsId)
    }

    // Show ad when trigger is true
    LaunchedEffect(showAd) {
        if (showAd && interstitialAdManager.isAdLoaded()) {
            interstitialAdManager.showAd(
                onAdDismissed = {
                    onAdDismissed()
                    // Reload ad for next time
                    interstitialAdManager.loadAd(adsId)
                },
                onAdFailedToShow = {
                    onAdFailedToShow()
                    // Reload ad for next time
                    interstitialAdManager.loadAd(adsId)
                }
            )
        }
    }

    // Render content
    content()
}

