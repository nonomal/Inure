package app.simple.inure.viewmodels.dialogs

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.simple.inure.R
import app.simple.inure.apk.utils.PackageUtils.safeApplicationInfo
import app.simple.inure.constants.Warnings
import app.simple.inure.extensions.viewmodels.RootShizukuViewModel
import app.simple.inure.helpers.ShizukuServiceHelper
import app.simple.inure.models.BatchPackageInfo
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BatchUninstallerViewModel(application: Application, val list: ArrayList<BatchPackageInfo>) : RootShizukuViewModel(application) {

    private val data: MutableLiveData<String> by lazy {
        MutableLiveData<String>().also {
            initializeCoreFramework()
        }
    }

    fun getData(): LiveData<String> {
        return data
    }

    override fun onShellCreated(shell: Shell?) {
        viewModelScope.launch(Dispatchers.IO) {
            buildString {
                for (app in list) {
                    runCatching {
                        Shell.cmd(app.packageInfo.getUninstallCommand()).exec().let {
                            if (it.isSuccess) {
                                append(getString(R.string.uninstalled) + " -> ${app.packageInfo.packageName}\n")
                            } else {
                                append(getString(R.string.failed) + " -> ${app.packageInfo.packageName}\n")
                            }
                        }
                    }.onFailure {
                        append(getString(R.string.failed) + " -> ${app.packageInfo.packageName} : ${it.stackTraceToString()}\n")
                    }
                }

                data.postValue(this.toString().trim())
            }
        }
    }

    private fun PackageInfo.getUninstallCommand(): String {
        return if (safeApplicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
            "pm uninstall -k --user current $packageName"
        } else {
            "pm uninstall $packageName"
        }
    }

    override fun onShellDenied() {
        warning.postValue(Warnings.getNoRootConnectionWarning())
    }

    override fun onShizukuCreated(shizukuServiceHelper: ShizukuServiceHelper) {
        viewModelScope.launch(Dispatchers.IO) {
            buildString {
                for (app in list) {
                    runCatching {
                        shizukuServiceHelper.service!!.simpleExecute(app.packageInfo.getUninstallCommand()).let {
                            if (it.isSuccess) {
                                append(getString(R.string.uninstalled) + " -> ${app.packageInfo.packageName}\n")
                            } else {
                                append(getString(R.string.failed) + " -> ${app.packageInfo.packageName}\n")
                            }
                        }
                    }.onFailure {
                        append(getString(R.string.failed) + " -> ${app.packageInfo.packageName} : ${it.stackTraceToString()}\n")
                    }
                }

                data.postValue(this.toString().trim())
            }
        }
    }
}
