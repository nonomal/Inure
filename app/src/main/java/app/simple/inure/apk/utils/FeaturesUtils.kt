package app.simple.inure.apk.utils

import android.content.Context
import android.content.pm.ConfigurationInfo
import android.content.pm.FeatureInfo
import android.opengl.GLES10
import android.opengl.GLES11
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLES31
import android.opengl.GLES32
import android.util.Log

object FeaturesUtils {
    private const val TAG = "FeaturesUtils"

    private val featureNameMap = mapOf(
            "android.hardware.audio.low_latency" to "Low Latency Audio",
            "android.hardware.bluetooth" to "Bluetooth",
            "android.hardware.bluetooth_le" to "Bluetooth LE",
            "android.hardware.camera" to "Camera",
            "android.hardware.camera.any" to "Any Camera",
            "android.hardware.camera.autofocus" to "Camera Autofocus",
            "android.hardware.camera.capability.manual_post_processing" to "Manual Post Processing",
            "android.hardware.camera.capability.manual_sensor" to "Manual Sensor",
            "android.hardware.camera.capability.raw" to "Raw Camera",
            "android.hardware.camera.external" to "External Camera",
            "android.hardware.camera.flash" to "Camera Flash",
            "android.hardware.camera.front" to "Front Camera",
            "android.hardware.faketouch" to "Faketouch",
            "android.hardware.location" to "Location",
            "android.hardware.location.gps" to "GPS",
            "android.hardware.location.network" to "Network Location",
            "android.hardware.microphone" to "Microphone",
            "android.hardware.nfc" to "NFC",
            "android.hardware.sensor.accelerometer" to "Accelerometer",
            "android.hardware.sensor.barometer" to "Barometer",
            "android.hardware.sensor.compass" to "Compass",
            "android.hardware.sensor.gyroscope" to "Gyroscope",
            "android.hardware.sensor.heartrate.ecg" to "ECG Heart Rate Sensor",
            "android.hardware.sensor.heart_rate" to "Heart Rate Sensor",
            "android.hardware.sensor.light" to "Light Sensor",
            "android.hardware.sensor.proximity" to "Proximity Sensor",
            "android.hardware.telephony" to "Telephony",
            "android.hardware.telephony.cdma" to "CDMA Telephony",
            "android.hardware.telephony.gsm" to "GSM Telephony",
            "android.hardware.touchscreen" to "Touchscreen",
            "android.hardware.touchscreen.multitouch" to "Multitouch",
            "android.hardware.touchscreen.multitouch.distinct" to "Distinct Multitouch",
            "android.hardware.touchscreen.multitouch.jazzhand" to "Jazzhand Multitouch",
            "android.hardware.usb.accessory" to "USB Accessory",
            "android.hardware.usb.host" to "USB Host",
            "android.hardware.wifi" to "WiFi",
            "android.hardware.wifi.direct" to "WiFi Direct",
            "android.software.app_widgets" to "App Widgets",
            "android.software.autofill" to "Autofill",
            "android.software.backup" to "Backup",
            "android.software.device_admin" to "Device Admin",
            "android.software.home_screen" to "Home Screen",
            "android.software.input_methods" to "Input Methods",
            "android.software.leanback" to "Leanback",
            "android.software.leanback_only" to "Leanback Only",
            "android.software.live_wallpaper" to "Live Wallpaper",
            "android.software.managed_users" to "Managed Users",
            "android.software.picture_in_picture" to "Picture in Picture",
            "android.software.print" to "Print",
            "android.software.securely_removes_users" to "Securely Removes Users",
            "android.software.sip" to "SIP",
            "android.software.sip.voip" to "SIP VOIP",
            "android.software.vr.high_performance" to "VR High Performance",
            "android.software.vr.mode" to "VR Mode",
            "android.software.voice_recognizers" to "Voice Recognizers",
            "android.software.webview" to "WebView",
            "com.android.future.usb.accessory" to "Future USB Accessory",
            "com.android.nfc_extras" to "NFC Extras",
            "com.nxp.mifare" to "Mifare",
            "android.hardware.screen.landscape" to "Landscape Screen",
            "android.hardware.screen.portrait" to "Portrait Screen"
    )

    private fun getProperName(featureCode: String?): String? {
        return featureNameMap[featureCode] ?: featureCode
    }

    fun FeatureInfo.getProperName(): String? {
        return getProperName(this.name)
    }

    fun isOpenGLVersionSupported(context: Context, version: Int): Boolean {
        val packageManager = context.packageManager
        val configurationInfo: FeatureInfo? = packageManager.systemAvailableFeatures
            .firstOrNull { it.reqGlEsVersion != ConfigurationInfo.GL_ES_VERSION_UNDEFINED }

        return if (configurationInfo != null) {
            val supportedVersion = configurationInfo.reqGlEsVersion
            Log.d("", "Supported OpenGL ES version: check $supportedVersion for $version")

            /**
             * OpenGL ES 1.0: 0x00010000 (65536 in decimal)
             * OpenGL ES 1.1: 0x00010001 (65537 in decimal)
             * OpenGL ES 2.0: 0x00020000 (131072 in decimal)
             * OpenGL ES 3.0: 0x00030000 (196608 in decimal)
             * OpenGL ES 3.1: 0x00030001 (196609 in decimal)
             * OpenGL ES 3.2: 0x00030002 (196610 in decimal)
             */
            when (version) {
                65536 -> supportedVersion >= GLES10.GL_VERSION
                65537 -> supportedVersion >= GLES11.GL_VERSION
                131072 -> supportedVersion >= GLES20.GL_VERSION
                196608 -> supportedVersion >= GLES30.GL_VERSION
                196609 -> supportedVersion >= GLES31.GL_VERSION
                196610 -> supportedVersion >= GLES32.GL_VERSION
                else -> false
            }
        } else {
            false
        }
    }

    /**
     * Check if the device supports a specific feature
     * @param context of the given environment
     * @param feature is the feature to be checked
     * @return [Boolean] true if the feature is supported
     */
    fun isFeatureSupported(context: Context, feature: String): Boolean {
        val packageManager = context.packageManager
        return packageManager.hasSystemFeature(feature)
    }

    data class Feature(val name: String, val id: String, val description: String)
}
