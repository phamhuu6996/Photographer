package com.phamhuu.photographer.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import com.phamhuu.photographer.R

@Composable
private fun colorSchemeFromResources() =
    lightColorScheme(
        primary = colorResource(R.color.md_theme_light_primary),
        onPrimary = colorResource(R.color.md_theme_light_onPrimary),
        primaryContainer = colorResource(R.color.md_theme_light_primaryContainer),
        onPrimaryContainer = colorResource(R.color.md_theme_light_onPrimaryContainer),
        secondary = colorResource(R.color.md_theme_light_secondary),
        onSecondary = colorResource(R.color.md_theme_light_onSecondary),
        secondaryContainer = colorResource(R.color.md_theme_light_secondaryContainer),
        onSecondaryContainer = colorResource(R.color.md_theme_light_onSecondaryContainer),
        tertiary = colorResource(R.color.md_theme_light_tertiary),
        onTertiary = colorResource(R.color.md_theme_light_onTertiary),
        tertiaryContainer = colorResource(R.color.md_theme_light_tertiaryContainer),
        onTertiaryContainer = colorResource(R.color.md_theme_light_onTertiaryContainer),
        error = colorResource(R.color.md_theme_light_error),
        onError = colorResource(R.color.md_theme_light_onError),
        errorContainer = colorResource(R.color.md_theme_light_errorContainer),
        onErrorContainer = colorResource(R.color.md_theme_light_onErrorContainer),
        background = colorResource(R.color.md_theme_light_background),
        onBackground = colorResource(R.color.md_theme_light_onBackground),
        surface = colorResource(R.color.md_theme_light_surface),
        onSurface = colorResource(R.color.md_theme_light_onSurface),
        surfaceVariant = colorResource(R.color.md_theme_light_surfaceVariant),
        onSurfaceVariant = colorResource(R.color.md_theme_light_onSurfaceVariant),
        outline = colorResource(R.color.md_theme_light_outline),
        outlineVariant = colorResource(R.color.md_theme_light_outlineVariant)
    )

@Composable
fun PhotographerTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = colorSchemeFromResources()

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

