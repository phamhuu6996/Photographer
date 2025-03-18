package com.phamhuu.photographer.presentation.common

import android.content.Context
import android.util.Size
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.LifecycleOwner
import com.phamhuu.photographer.R
import com.phamhuu.photographer.presentation.camera.CameraState
import com.phamhuu.photographer.presentation.camera.CameraViewModel
import com.phamhuu.photographer.presentation.utils.Permission

@Composable
fun ResolutionControl(
    viewModel: CameraViewModel,
    showSelectResolution: Boolean = false,
    resolution: Size ?= null,
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView
) {

    Column {

        Card(
            modifier = Modifier
                .padding(all = 20.dp)
                .clickable {
                    viewModel.changeShowSelectResolution(true)
                }
        ) {
            Text(
                text = (resolution ?: "Max").toString(),
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                color = Color.Black,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        DropdownMenu(
            expanded = showSelectResolution,
            properties = PopupProperties(focusable = false),
            onDismissRequest = {
                viewModel.changeShowSelectResolution(false)
            }
        ) {
            val resolutions = viewModel.getResolutionsWithCameraCurrent()
            resolutions?.forEach { resolution ->
                DropdownMenuItem(
                    text = { Text(text = resolution.toString()) },
                    onClick = {
                        viewModel.changeShowSelectResolution(false)
                        viewModel.setResolution(resolution)
                        viewModel.startCamera(context, lifecycleOwner, previewView, resolution)
                    }
                )

            }
        }
    }
}