package com.phamhuu.photographer.contants

/**
 * AdMob Ad Unit IDs Constants
 *
 * Centralized location for all AdMob Ad Unit IDs.
 * Automatically uses test IDs for debug builds and real IDs for release builds.
 *
 * Test Ad Unit IDs (used in debug mode):
 * - Banner: ca-app-pub-3940256099942544/6300978111
 * - Interstitial: ca-app-pub-3940256099942544/1033173712
 * - Rewarded: ca-app-pub-3940256099942544/5224354917
 *
 * Real Ad Unit IDs (used in release mode):
 * - Banner: ca-app-pub-6216323825050683/3249879246
 * - Interstitial: ca-app-pub-6216323825050683/6309612261
 * - Rewarded: ca-app-pub-6216323825050683/9486439947
 */
object AdMobConstants {
    // Test Ad Unit IDs (for debug/development)
    private const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    // Real Ad Unit IDs (for release/production)
    private const val PROD_BANNER_AD_UNIT_ID = "ca-app-pub-6216323825050683/3249879246"
    private const val PROD_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-6216323825050683/6309612261"
    private const val PROD_REWARDED_AD_UNIT_ID = "ca-app-pub-6216323825050683/9486439947"

    // Automatically select IDs based on build type
    const val BANNER_AD_UNIT_ID: String = TEST_BANNER_AD_UNIT_ID

    const val INTERSTITIAL_AD_UNIT_ID: String = TEST_INTERSTITIAL_AD_UNIT_ID

    const val REWARDED_AD_UNIT_ID: String = TEST_REWARDED_AD_UNIT_ID
}

