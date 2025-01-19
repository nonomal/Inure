package app.simple.inure.apk.utils

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import app.simple.inure.R
import app.simple.inure.util.ArrayUtils.toArrayList
import java.io.BufferedReader
import java.io.InputStreamReader

object PermissionUtils {

    private const val PROTECTION_FLAG_OEM = 0x4000
    private const val PROTECTION_FLAG_VENDOR_PRIVILEGED = 0x8000
    private const val PROTECTION_FLAG_SYSTEM_TEXT_CLASSIFIER = 0x10000
    private const val PROTECTION_FLAG_WELLBEING = 0x20000
    private const val PROTECTION_FLAG_DOCUMENTER = 0x40000
    private const val PROTECTION_FLAG_CONFIGURATOR = 0x80000
    private const val PROTECTION_FLAG_INCIDENT_REPORT_APPROVER = 0x100000
    private const val PROTECTION_FLAG_APP_PREDICTOR = 0x200000
    private const val PROTECTION_FLAG_RETAIL_DEMO = 0x1000000

    private val exceptionPermissions = arrayOf(
            "android.permission.WRITE_SECURE_SETTINGS",
            "android.permission.DUMP",
            "android.permission.READ_LOGS",
            "android.permission.READ_FRAME_BUFFER",
            "android.permission.PACKAGE_USAGE_STATS",
            "android.permission.INTERACT_ACROSS_USERS",
    )

    fun String.getPermissionInfo(context: Context): PermissionInfo? {
        try {
            return context.packageManager.getPermissionInfo(this, PackageManager.GET_META_DATA)
        } catch (_: PackageManager.NameNotFoundException) {
            // e.printStackTrace()
        }
        return null
    }

    @Suppress("DEPRECATION")
    fun isDangerous(permissionInfo: PermissionInfo): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            when (permissionInfo.protection) {
                PermissionInfo.PROTECTION_DANGEROUS -> true
                PermissionInfo.PROTECTION_SIGNATURE -> false
                PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM -> false
                else -> false
            }
        } else {
            when (permissionInfo.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE) {
                PermissionInfo.PROTECTION_DANGEROUS -> true
                PermissionInfo.PROTECTION_SIGNATURE -> false
                PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM -> false
                else -> false
            }
        }
    }

    fun getPermissionMap(): HashMap<String, String> {
        val bufferedReader = BufferedReader(
                InputStreamReader(PermissionUtils::class.java.getResourceAsStream("/permissions.txt")))
        val permissionList = bufferedReader.readLines()
        val map = HashMap<String, String>()

        permissionList.forEach {
            val shortName = it.substring(it.lastIndexOf(".") + 1)
            map[shortName] = it
        }

        return map
    }

    fun getAndroidPermissionList(): List<String> {
        val bufferedReader = BufferedReader(
                InputStreamReader(PermissionUtils::class.java.getResourceAsStream("/permissions.txt")))
        return bufferedReader.readLines().toArrayList().sorted()
    }

    fun PermissionInfo.isException(): Boolean {
        return name in exceptionPermissions
    }

    @Suppress("deprecation")
    fun protectionToString(level: Int, flags: Int, context: Context): String {

        var protection = "????"

        when (level and PermissionInfo.PROTECTION_MASK_BASE) {
            PermissionInfo.PROTECTION_DANGEROUS -> protection = context.getString(R.string.dangerous)
            PermissionInfo.PROTECTION_NORMAL -> protection = context.getString(R.string.normal)
            PermissionInfo.PROTECTION_SIGNATURE -> protection = context.getString(R.string.signature)
            PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM -> protection = context.getString(R.string.signature_or_system)
        }

        if (flags and PermissionInfo.PROTECTION_FLAG_PRIVILEGED != 0) {
            protection += " | " + context.getString(R.string.privileged)
        }
        if (flags and PermissionInfo.PROTECTION_FLAG_DEVELOPMENT != 0) {
            protection += " | " + context.getString(R.string.development)
        }
        if (flags and PermissionInfo.PROTECTION_FLAG_APPOP != 0) {
            protection += " | AppOp"
        }
        if (flags and PermissionInfo.PROTECTION_FLAG_PRE23 != 0) {
            protection += " | Pre23"
        }
        if (flags and PermissionInfo.PROTECTION_FLAG_INSTALLER != 0) {
            protection += " | " + context.getString(R.string.installer)
        }
        if (flags and PermissionInfo.PROTECTION_FLAG_VERIFIER != 0) {
            protection += " | " + context.getString(R.string.verifier)
        }
        if (flags and PermissionInfo.PROTECTION_FLAG_PREINSTALLED != 0) {
            protection += " | " + context.getString(R.string.pre_installed)
        }
        if (flags and PermissionInfo.PROTECTION_FLAG_SETUP != 0) {
            protection += " | " + context.getString(R.string.setup)
        }
        if (flags and PermissionInfo.PROTECTION_FLAG_INSTANT != 0) {
            protection += " | " + context.getString(R.string.instant)
        }
        if (flags and PermissionInfo.PROTECTION_FLAG_RUNTIME_ONLY != 0) {
            protection += " | " + context.getString(R.string.runtime)
        }
        if (flags and PROTECTION_FLAG_OEM != 0) {
            protection += " | " + context.getString(R.string.oem)
        }
        if (flags and PROTECTION_FLAG_VENDOR_PRIVILEGED != 0) {
            protection += " | " + context.getString(R.string.vendor_privileged)
        }
        if (flags and PROTECTION_FLAG_SYSTEM_TEXT_CLASSIFIER != 0) {
            protection += " | " + context.getString(R.string.text_classifier)
        }
        if (flags and PROTECTION_FLAG_WELLBEING != 0) {
            protection += " | " + context.getString(R.string.wellbeing)
        }
        if (flags and PROTECTION_FLAG_DOCUMENTER != 0) {
            protection += " | " + context.getString(R.string.documenter)
        }
        if (flags and PROTECTION_FLAG_CONFIGURATOR != 0) {
            protection += " | " + context.getString(R.string.configurator)
        }
        if (flags and PROTECTION_FLAG_INCIDENT_REPORT_APPROVER != 0) {
            protection += " | " + context.getString(R.string.incident_report_approver)
        }
        if (flags and PROTECTION_FLAG_APP_PREDICTOR != 0) {
            protection += " | " + context.getString(R.string.app_predictor)
        }
        if (flags and PROTECTION_FLAG_RETAIL_DEMO != 0) {
            protection += " | " + context.getString(R.string.retail_demo)
        }
        return protection
    }

    fun Context.getPermissionDescription(name: String?): String {
        kotlin.runCatching {
            val desc = name!!.getPermissionInfo(this)!!.loadDescription(packageManager)

            return if (desc.isNullOrEmpty()) {
                getString(R.string.desc_not_available)
            } else {
                desc.toString()
            }
        }.getOrElse {
            return getString(R.string.desc_not_available)
        }
    }
}
