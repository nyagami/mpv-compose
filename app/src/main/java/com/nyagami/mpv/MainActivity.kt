package com.nyagami.mpv

import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.nyagami.mpv.ui.theme.MPVComposeTheme
import com.nyagami.mpv.MPV.mpvFormat.*

class MainActivity : ComponentActivity(), MPV.EventObserver, MPV.LogObserver {
    private val videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    private var voInUse: String = "gpu"
    fun setVo(vo: String) {
        voInUse = vo
        MPV.setOptionString("vo", vo)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        MPV.setOptionString("sub-ass-force-margins", "yes")
        MPV.setOptionString("sub-use-margins", "yes")
        MPV.create(this, "MPVCompose")
        MPV.setOptionString("config", "yes")
        MPV.setOptionString("config-dir", filesDir.path)
        for (opt in arrayOf("gpu-shader-cache-dir", "icc-cache-dir")) MPV.setOptionString(
            opt,
            cacheDir.path
        )

        // init options
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // apply phone-optimized defaults
        MPV.setOptionString("profile", "fast")
        val hwdec = if (sharedPreferences.getBoolean("hardware_decoding", true))
            "auto"
        else
            "no"

        // vo: set display fps as reported by android
        val disp = ContextCompat.getDisplayOrDefault(this)
        val refreshRate = disp.mode.refreshRate

        Log.v("v", "Display ${disp.displayId} reports FPS of $refreshRate")
        MPV.setOptionString("display-fps-override", refreshRate.toString())
        data class Property(val preference_name: String, val mpv_option: String)

        val opts = arrayOf(
            Property("default_audio_language", "alang"),
            Property("default_subtitle_language", "slang"),

            // vo-related
            Property("video_scale", "scale"),
            Property("video_scale_param1", "scale-param1"),
            Property("video_scale_param2", "scale-param2"),

            Property("video_downscale", "dscale"),
            Property("video_downscale_param1", "dscale-param1"),
            Property("video_downscale_param2", "dscale-param2"),

            Property("video_tscale", "tscale"),
            Property("video_tscale_param1", "tscale-param1"),
            Property("video_tscale_param2", "tscale-param2")
        )

        for ((preference_name, mpv_option) in opts) {
            val preference = sharedPreferences.getString(preference_name, "")
            if (!preference.isNullOrBlank())
                MPV.setOptionString(mpv_option, preference)
        }

        val debandMode = sharedPreferences.getString("video_debanding", "")
        if (debandMode == "gradfun") {
            // lower the default radius (16) to improve performance
            MPV.setOptionString("vf", "gradfun=radius=12")
        } else if (debandMode == "gpu") {
            MPV.setOptionString("deband", "yes")
        }

        val vidsync = sharedPreferences.getString(
            "video_sync",
            "audio"
        )
        MPV.setOptionString("video-sync", vidsync!!)

        if (sharedPreferences.getBoolean("video_interpolation", false))
            MPV.setOptionString("interpolation", "yes")

        if (sharedPreferences.getBoolean("gpudebug", false))
            MPV.setOptionString("gpu-debug", "yes")

        if (sharedPreferences.getBoolean("video_fastdecode", false)) {
            MPV.setOptionString("vd-lavc-fast", "yes")
            MPV.setOptionString("vd-lavc-skiploopfilter", "nonkey")
        }

        MPV.setOptionString("gpu-context", "android")
        MPV.setOptionString("opengl-es", "yes")
        MPV.setOptionString("hwdec", hwdec)
        MPV.setOptionString("hwdec-codecs", "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1")
        MPV.setOptionString("ao", "audiotrack,opensles")
        MPV.setOptionString("input-default-bindings", "yes")
        // Limit demuxer cache since the defaults are too high for mobile devices
        val cacheMegs = 64
        MPV.setOptionString("demuxer-max-bytes", "${cacheMegs * 1024 * 1024}")
        MPV.setOptionString("demuxer-max-back-bytes", "${cacheMegs * 1024 * 1024}")
        //
        val screenshotDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        screenshotDir.mkdirs()
        MPV.setOptionString("screenshot-directory", screenshotDir.path)
        // workaround for <https://github.com/mpv-player/mpv/issues/14651>
        MPV.setOptionString("vd-lavc-film-grain", "cpu")

        MPV.init()

        // post init options
        // we need to call write-watch-later manually
        MPV.setOptionString("save-position-on-quit", "no")

        MPV.setOptionString("force-window", "no")
        // need to idle at least once for playFile() logic to work
        MPV.setOptionString("idle", "once")

        // observeProperties
        data class ObserveProperty(val name: String, val format: Int = MPV_FORMAT_NONE)
        val p = arrayOf(
            ObserveProperty("time-pos", MPV_FORMAT_INT64),
            ObserveProperty("duration/full", MPV_FORMAT_INT64),
            ObserveProperty("demuxer-cache-time", MPV_FORMAT_INT64),
            ObserveProperty("paused-for-cache", MPV_FORMAT_FLAG),
            ObserveProperty("seeking", MPV_FORMAT_FLAG),
            ObserveProperty("pause", MPV_FORMAT_FLAG),
            ObserveProperty("eof-reached", MPV_FORMAT_FLAG),
            ObserveProperty("paused-for-cache", MPV_FORMAT_FLAG),
            ObserveProperty("speed", MPV_FORMAT_STRING),
            ObserveProperty("track-list"),
            ObserveProperty("video-params/aspect", MPV_FORMAT_DOUBLE),
            ObserveProperty("video-params/rotate", MPV_FORMAT_DOUBLE),
            ObserveProperty("playlist-pos", MPV_FORMAT_INT64),
            ObserveProperty("playlist-count", MPV_FORMAT_INT64),
            ObserveProperty("current-tracks/video/image"),
            ObserveProperty("media-title", MPV_FORMAT_STRING),
            ObserveProperty("metadata"),
            ObserveProperty("loop-playlist"),
            ObserveProperty("loop-file"),
            ObserveProperty("shuffle", MPV_FORMAT_FLAG),
            ObserveProperty("hwdec-current")
        )

        for ((name, format) in p)
            MPV.observeProperty(name, format)

        MPV.setPropertyString("vo", voInUse)

        MPV.addObserver(this)
        MPV.addLogObserver(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LaunchedEffect(Unit) {
                MPV.command(arrayOf("loadfile", videoUrl))
            }
            MPVComposeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)){
                        MPVView(modifier = Modifier.fillMaxWidth().height(200.dp), voInUse)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        MPV.destroy()
        super.onDestroy()
    }

    override fun eventProperty(p0: String) {
        TODO("Not yet implemented")
    }

    override fun eventProperty(p0: String, p1: Long) {
        TODO("Not yet implemented")
    }

    override fun eventProperty(p0: String, p1: Boolean) {
        TODO("Not yet implemented")
    }

    override fun eventProperty(p0: String, p1: String) {
        TODO("Not yet implemented")
    }

    override fun eventProperty(p0: String, p1: Double) {
        TODO("Not yet implemented")
    }

    override fun event(p0: Int) {
        TODO("Not yet implemented")
    }

    override fun efEvent(p0: String?) {
        TODO("Not yet implemented")
    }

    override fun logMessage(p0: String, p1: Int, p2: String) {
        TODO("Not yet implemented")
    }
}