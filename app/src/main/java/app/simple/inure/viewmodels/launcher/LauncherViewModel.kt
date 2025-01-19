package app.simple.inure.viewmodels.launcher

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.simple.inure.apk.utils.APKCertificateUtils
import app.simple.inure.apk.utils.PackageUtils.getPackageInfo
import app.simple.inure.apk.utils.PackageUtils.isPackageInstalled
import app.simple.inure.constants.Warnings
import app.simple.inure.extensions.viewmodels.WrappedViewModel
import app.simple.inure.preferences.TrialPreferences
import app.simple.inure.util.AppUtils
import app.simple.inure.util.AppUtils.isNewerUnlocker
import app.simple.inure.util.ConditionUtils.invert
import app.simple.inure.util.FileUtils.toFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.cert.X509Certificate

class LauncherViewModel(application: Application) : WrappedViewModel(application) {

    @Suppress("PrivatePropertyName")
    private val SHA1 = arrayListOf(
            "85D8B419713565CC7A4E14AAE269F2EC14F37013",
            "8592E01F4AC28B2CA1E423573773D71A1EC82849"
    )

    private val shouldVerify: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    fun getShouldVerify(): LiveData<Boolean> {
        return shouldVerify
    }

    private fun verifyCertificate() {
        viewModelScope.launch(Dispatchers.Default) {
            kotlin.runCatching {
                val packageInfo = packageManager.getPackageInfo(AppUtils.UNLOCKER_PACKAGE_NAME)
                val file = packageInfo?.applicationInfo?.sourceDir?.toFile()
                val certificates: Array<X509Certificate> = APKCertificateUtils(file, packageInfo!!.packageName, applicationContext()).x509Certificates
                val fingerPrint = computeFingerPrint(certificates[0].encoded)

                if (packageInfo.isNewerUnlocker()) {
                    if (SHA1.contains(fingerPrint)) { // Signature is valid
                        if (TrialPreferences.getLastVerificationDate() == -1L || TrialPreferences.getLastVerificationDate() < packageInfo.firstInstallTime) {
                            shouldVerify.postValue(true)
                        } else {
                            shouldVerify.postValue(false)
                        }
                    } else {
                        postWarning(Warnings.getUnableToVerifyUnlockerWarning())
                    }
                } else {
                    shouldVerify.postValue(SHA1.contains(fingerPrint))
                }
            }.getOrElse {
                postWarning(Warnings.getUnableToVerifyUnlockerWarning())
            }
        }
    }

    private fun computeFingerPrint(certRaw: ByteArray?): String {
        var strResult = ""
        val messageDigest: MessageDigest
        try {
            messageDigest = MessageDigest.getInstance("SHA1")
            certRaw?.let { messageDigest.update(it) }
            var strAppend: String

            for (b in messageDigest.digest()) {
                strAppend = (b.toInt() and 0xff).toString(16)
                if (strAppend.length == 1) strResult += "0"
                strResult += strAppend
            }

            strResult = strResult.uppercase()
        } catch (ex: NoSuchAlgorithmException) {
            ex.printStackTrace()
        }

        return strResult
    }

    fun initCheck() {
        viewModelScope.launch(Dispatchers.Default) {
            if (TrialPreferences.isFullVersion().invert()) {
                if (packageManager.isPackageInstalled(AppUtils.UNLOCKER_PACKAGE_NAME)) {
                    if (TrialPreferences.isUnlockerVerificationRequired()) {
                        verifyCertificate()
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "LauncherViewModel"
    }
}
