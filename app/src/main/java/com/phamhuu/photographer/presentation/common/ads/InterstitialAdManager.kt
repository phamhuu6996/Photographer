package com.phamhuu.photographer.presentation.common.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Manager for Interstitial Ads
 * Handles loading and showing interstitial ads
 * 
 * Based on official Google AdMob documentation:
 * https://developers.google.com/admob/android/interstitial
 */
class InterstitialAdManager(private val context: Context) {
    private var interstitialAd: InterstitialAd? = null
    private val TAG = "InterstitialAdManager"
    
    /**
     * Load an interstitial ad
     * @param adUnitId Ad Unit ID from AdMob console
     */
    fun loadAd(adUnitId: String) {
        // Don't load if already loading or loaded
        if (interstitialAd != null) {
            return
        }
        
        val adRequest = AdRequest.Builder().build()
        
        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Ad was loaded.")
                    interstitialAd = ad
                }
                
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.d(TAG, loadAdError.message)
                    interstitialAd = null
                }
            }
        )
    }
    
    /**
     * Show interstitial ad if loaded
     * @param onAdDismissed Callback when ad is dismissed
     * @param onAdFailedToShow Callback when ad fails to show
     * @return true if ad was shown, false if no ad available
     */
    fun showAd(
        onAdDismissed: () -> Unit = {},
        onAdFailedToShow: () -> Unit = {}
    ): Boolean {
        val ad = interstitialAd ?: return false
        
        val activity = context as? Activity ?: return false
        
        // Update callbacks to include user callbacks
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed fullscreen content.")
                interstitialAd = null
                onAdDismissed()
            }
            
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Ad failed to show fullscreen content: ${adError.message}")
                interstitialAd = null
                onAdFailedToShow()
            }
            
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed fullscreen content.")
            }
            
            override fun onAdClicked() {
                Log.d(TAG, "Ad was clicked.")
            }
        }
        
        ad.show(activity)
        return true
    }
    
    /**
     * Check if ad is loaded and ready to show
     */
    fun isAdLoaded(): Boolean {
        return interstitialAd != null
    }
}

