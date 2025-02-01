package com.nyagami.mpv

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.preference.PreferenceManager
import android.util.Log
import android.view.Surface
import androidx.core.content.ContextCompat

class MPV {
    companion object {
        init {
            val libs = arrayOf("mpv", "player")
            for (lib in libs) {
                System.loadLibrary(lib)
            }
        }

        @JvmStatic
        private external fun create(appContext: Context, logLvl: String)

        @JvmStatic
        private external fun init()

        @JvmStatic
        external fun destroy()

        @JvmStatic
        external fun attachSurface(surface: Surface)

        @JvmStatic
        external fun detachSurface()

        @JvmStatic
        external fun command(cmd: Array<String>)

        @JvmStatic
        external fun setOptionString(name: String, value: String): Int

        @JvmStatic
        external fun grabThumbnail(dimension: Int): Bitmap

        @JvmStatic
        external fun getPropertyInt(property: String): Int?

        @JvmStatic
        external fun setPropertyInt(property: String, value: Int)

        @JvmStatic
        external fun getPropertyDouble(property: String): Double?

        @JvmStatic
        external fun setPropertyDouble(property: String, value: Double)

        @JvmStatic
        external fun getPropertyBoolean(property: String): Boolean?

        @JvmStatic
        external fun setPropertyBoolean(property: String, value: Boolean)

        @JvmStatic
        external fun getPropertyString(property: String): String?

        @JvmStatic
        external fun setPropertyString(property: String, value: String)

        @JvmStatic
        external fun observeProperty(property: String, format: Int)

        private val observers: MutableList<EventObserver> = ArrayList()

        fun addObserver(o: EventObserver) {
            synchronized(observers) { observers.add(o) }
        }

        fun removeObserver(o: EventObserver) {
            synchronized(observers) { observers.remove(o) }
        }

        @JvmStatic
        private fun eventProperty(property: String, value: Long) {
            synchronized(observers) {
                for (o in observers) o.eventProperty(property, value)
            }
        }

        @JvmStatic
        private fun eventProperty(property: String, value: Boolean) {
            synchronized(observers) {
                for (o in observers) o.eventProperty(property, value)
            }
        }

        @JvmStatic
        private fun eventProperty(property: String, value: Double) {
            synchronized(observers) {
                for (o in observers) o.eventProperty(property, value)
            }
        }

        @JvmStatic
        private fun eventProperty(property: String, value: String) {
            synchronized(observers) {
                for (o in observers) o.eventProperty(property, value)
            }
        }

        @JvmStatic
        private fun eventProperty(property: String) {
            synchronized(observers) {
                for (o in observers) o.eventProperty(property)
            }
        }

        @JvmStatic
        private fun event(eventId: Int) {
            synchronized(observers) {
                for (o in observers) o.event(eventId)
            }
        }

        @JvmStatic
        private fun efEvent(err: String) {
            synchronized(observers) {
                for (o in observers) o.efEvent(err)
            }
        }

        private val log_observers: MutableList<LogObserver> = ArrayList()

        fun addLogObserver(o: LogObserver) {
            synchronized(log_observers) { log_observers.add(o) }
        }

        fun removeLogObserver(o: LogObserver) {
            synchronized(log_observers) { log_observers.remove(o) }
        }

        @JvmStatic
        private fun logMessage(prefix: String, level: Int, text: String) {
            synchronized(log_observers) {
                for (o in log_observers) o.logMessage(prefix, level, text)
            }
        }

        interface EventObserver {
            fun eventProperty(property: String)
            fun eventProperty(property: String, value: Long)
            fun eventProperty(property: String, value: Boolean)
            fun eventProperty(property: String, value: String)
            fun eventProperty(property: String, value: Double)
            fun event(eventId: Int)
            fun efEvent(err: String)
        }

        interface LogObserver {
            fun logMessage(prefix: String, level: Int, text: String)
        }

        object mpvFormat {
            const val MPV_FORMAT_NONE: Int = 0
            const val MPV_FORMAT_STRING: Int = 1
            const val MPV_FORMAT_OSD_STRING: Int = 2
            const val MPV_FORMAT_FLAG: Int = 3
            const val MPV_FORMAT_INT64: Int = 4
            const val MPV_FORMAT_DOUBLE: Int = 5
            const val MPV_FORMAT_NODE: Int = 6
            const val MPV_FORMAT_NODE_ARRAY: Int = 7
            const val MPV_FORMAT_NODE_MAP: Int = 8
            const val MPV_FORMAT_BYTE_ARRAY: Int = 9
        }

        object mpvEventId {
            const val MPV_EVENT_NONE: Int = 0
            const val MPV_EVENT_SHUTDOWN: Int = 1
            const val MPV_EVENT_LOG_MESSAGE: Int = 2
            const val MPV_EVENT_GET_PROPERTY_REPLY: Int = 3
            const val MPV_EVENT_SET_PROPERTY_REPLY: Int = 4
            const val MPV_EVENT_COMMAND_REPLY: Int = 5
            const val MPV_EVENT_START_FILE: Int = 6
            const val MPV_EVENT_END_FILE: Int = 7
            const val MPV_EVENT_FILE_LOADED: Int = 8

            @Deprecated("")
            const val MPV_EVENT_IDLE: Int = 11

            @Deprecated("")
            const val MPV_EVENT_TICK: Int = 14
            const val MPV_EVENT_CLIENT_MESSAGE: Int = 16
            const val MPV_EVENT_VIDEO_RECONFIG: Int = 17
            const val MPV_EVENT_AUDIO_RECONFIG: Int = 18
            const val MPV_EVENT_SEEK: Int = 20
            const val MPV_EVENT_PLAYBACK_RESTART: Int = 21
            const val MPV_EVENT_PROPERTY_CHANGE: Int = 22
            const val MPV_EVENT_QUEUE_OVERFLOW: Int = 24
            const val MPV_EVENT_HOOK: Int = 25
        }

        object mpvLogLevel {
            const val MPV_LOG_LEVEL_NONE: Int = 0
            const val MPV_LOG_LEVEL_FATAL: Int = 10
            const val MPV_LOG_LEVEL_ERROR: Int = 20
            const val MPV_LOG_LEVEL_WARN: Int = 30
            const val MPV_LOG_LEVEL_INFO: Int = 40
            const val MPV_LOG_LEVEL_V: Int = 50
            const val MPV_LOG_LEVEL_DEBUG: Int = 60
            const val MPV_LOG_LEVEL_TRACE: Int = 70
        }

        internal var voInUse: String = ""

        fun setVo(vo: String) {
            voInUse = vo
            setOptionString("vo", vo)
        }

        fun prepare(
            appContext: Context,
            configDir: String = appContext.filesDir.path,
            cacheDir: String = appContext.cacheDir.path,
            logLvl: String = "v",
            vo: String = "gpu",
            initOptions: ((vo: String) -> Unit)? = null,
            postInitOptions: (() -> Unit)? = null,
            observeProperties: (() -> Unit)? = null
        ) {
            setOptionString("sub-ass-force-margins", "yes")
            setOptionString("sub-use-margins", "yes")

            create(appContext, logLvl)

            setOptionString("config", "yes")
            setOptionString("config-dir", configDir)
            for (opt in arrayOf("gpu-shader-cache-dir", "icc-cache-dir")) setOptionString(
                opt,
                cacheDir
            )
            if (initOptions != null) {
                initOptions(vo)
            } else {
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)

                // apply phone-optimized defaults
                setOptionString("profile", "fast")

                // vo
                setVo(vo)

                // hwdec
                val hwdec = if (sharedPreferences.getBoolean("hardware_decoding", true))
                    "auto"
                else
                    "no"

                // vo: set display fps as reported by android
                val disp = ContextCompat.getDisplayOrDefault(appContext)
                val refreshRate = disp.mode.refreshRate

                Log.v("v", "Display ${disp.displayId} reports FPS of $refreshRate")
                setOptionString("display-fps-override", refreshRate.toString())

                val opts = arrayOf(
                    "default_audio_language" to "alang",
                    "default_subtitle_language" to "slang",

                    // vo-related
                    "video_scale" to "scale",
                    "video_scale_param1" to "scale-param1",
                    "video_scale_param2" to "scale-param2",

                    "video_downscale" to "dscale",
                    "video_downscale_param1" to "dscale-param1",
                    "video_downscale_param2" to "dscale-param2",

                    "video_tscale" to "tscale",
                    "video_tscale_param1" to "tscale-param1",
                    "video_tscale_param2" to "tscale-param2"
                )

                for ((preference_name, mpv_option) in opts) {
                    val preference = sharedPreferences.getString(preference_name, "")
                    if (!preference.isNullOrBlank())
                        setOptionString(mpv_option, preference)
                }

                val debandMode = sharedPreferences.getString("video_debanding", "")
                if (debandMode == "gradfun") {
                    // lower the default radius (16) to improve performance
                    setOptionString("vf", "gradfun=radius=12")
                } else if (debandMode == "gpu") {
                    setOptionString("deband", "yes")
                }

                val vidsync = sharedPreferences.getString(
                    "video_sync",
                    "audio"
                )
                setOptionString("video-sync", vidsync!!)

                if (sharedPreferences.getBoolean("video_interpolation", false))
                    setOptionString("interpolation", "yes")

                if (sharedPreferences.getBoolean("gpudebug", false))
                    setOptionString("gpu-debug", "yes")

                if (sharedPreferences.getBoolean("video_fastdecode", false)) {
                    setOptionString("vd-lavc-fast", "yes")
                    setOptionString("vd-lavc-skiploopfilter", "nonkey")
                }

                setOptionString("gpu-context", "android")
                setOptionString("opengl-es", "yes")
                setOptionString("hwdec", hwdec)
                setOptionString("hwdec-codecs", "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1")
                setOptionString("ao", "audiotrack,opensles")
                setOptionString("input-default-bindings", "yes")
                // Limit demuxer cache since the defaults are too high for mobile devices
                val cacheMegs = 64
                setOptionString("demuxer-max-bytes", "${cacheMegs * 1024 * 1024}")
                setOptionString("demuxer-max-back-bytes", "${cacheMegs * 1024 * 1024}")
                //
                val screenshotDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                screenshotDir.mkdirs()
                setOptionString("screenshot-directory", screenshotDir.path)
                // workaround for <https://github.com/mpv-player/mpv/issues/14651>
                setOptionString("vd-lavc-film-grain", "cpu")
            }

            init()

            if(postInitOptions != null){
                postInitOptions()
            }else{
                // we need to call write-watch-later manually
                setOptionString("save-position-on-quit", "no")
            }

            // would crash before the surface is attached
            setOptionString("force-window", "no")
            // need to idle at least once for playFile() logic to work
            setOptionString("idle", "once")
            
            if(observeProperties != null) {
                observeProperties()
            }else {
                val observerOptions = arrayOf(
                    "time-pos" to mpvFormat.MPV_FORMAT_INT64,
                    "duration/full" to mpvFormat.MPV_FORMAT_INT64,
                    "demuxer-cache-time" to mpvFormat.MPV_FORMAT_INT64,
                    "paused-for-cache" to mpvFormat.MPV_FORMAT_FLAG,
                    "seeking" to mpvFormat.MPV_FORMAT_FLAG,
                    "pause" to mpvFormat.MPV_FORMAT_FLAG,
                    "eof-reached" to mpvFormat.MPV_FORMAT_FLAG,
                    "paused-for-cache" to mpvFormat.MPV_FORMAT_FLAG,
                    "speed" to mpvFormat.MPV_FORMAT_STRING,
                    "track-list" to mpvFormat.MPV_FORMAT_NONE,
                    "video-params/aspect" to mpvFormat.MPV_FORMAT_DOUBLE,
                    "video-params/rotate" to mpvFormat.MPV_FORMAT_DOUBLE,
                    "playlist-pos" to mpvFormat.MPV_FORMAT_INT64,
                    "playlist-count" to mpvFormat.MPV_FORMAT_INT64,
                    "current-tracks/video/image" to mpvFormat.MPV_FORMAT_NONE,
                    "media-title" to mpvFormat.MPV_FORMAT_STRING,
                    "metadata" to mpvFormat.MPV_FORMAT_NONE,
                    "loop-playlist" to mpvFormat.MPV_FORMAT_NONE,
                    "loop-file" to mpvFormat.MPV_FORMAT_NONE,
                    "shuffle" to mpvFormat.MPV_FORMAT_FLAG,
                    "hwdec-current" to mpvFormat.MPV_FORMAT_NONE
                )

                for ((name, format) in observerOptions)
                    observeProperty(name, format)
            }
        }
    }
}