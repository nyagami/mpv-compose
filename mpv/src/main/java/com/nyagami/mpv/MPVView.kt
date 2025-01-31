package com.nyagami.mpv

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun MPVView(modifier: Modifier = Modifier, voInUse: String = "gpu") {
    AndroidView(
        factory = { ctx ->
            SurfaceView(ctx).apply {
                z = -10f
                setZOrderOnTop(true)
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        MPV.attachSurface(holder.surface)
                        MPV.setOptionString("force-window", "yes")
                        MPV.setPropertyString("vo", voInUse)
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) {
                        MPV.setPropertyString(
                            "android-surface-size",
                            "${width}x$height"
                        )
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        MPV.setPropertyString("vo", "null")
                        MPV.setOptionString("force-window", "no")
                        MPV.detachSurface()
                    }
                })
            }
        },
        modifier = modifier
    )
}