package com.phamhuu.photographer.presentation.common.ads

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.phamhuu.photographer.contants.AdMobConstants

/**
 * Banner Ad Component for Jetpack Compose
 * 
 * @param adUnitId Ad Unit ID from AdMob console
 * @param modifier Modifier for the ad container
 */
@Composable
fun BannerAd(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val adView = remember {
        AdView(context).apply {
            // Use LARGE_BANNER for bigger size (320x100)
            setAdSize(AdSize.LARGE_BANNER)
            this.adUnitId = AdMobConstants.BANNER_AD_UNIT_ID
            loadAd(AdRequest.Builder().build())
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            adView.destroy()
        }
    }


    Box(modifier = modifier) {
        AndroidView(
            factory = { adView },
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        )
    }
}

