package app.simple.inure.viewmodels.viewers

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.simple.inure.R
import app.simple.inure.apk.utils.PackageUtils.isPackageInstalled
import app.simple.inure.apk.utils.PackageUtils.safeApplicationInfo
import app.simple.inure.apk.utils.PermissionUtils.getPermissionInfo
import app.simple.inure.extensions.viewmodels.WrappedViewModel
import app.simple.inure.models.PermissionInfo
import app.simple.inure.preferences.SearchPreferences
import app.simple.inure.util.StringUtils.capitalizeFirstLetter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class PermissionsViewModel(application: Application, val packageInfo: PackageInfo) : WrappedViewModel(application) {

    private val permissions: MutableLiveData<MutableList<PermissionInfo>> by lazy {
        MutableLiveData<MutableList<PermissionInfo>>().also {
            if (SearchPreferences.isSearchKeywordModeEnabled()) {
                Log.d("PermissionsViewModel", "Loading permission data with keyword: ${SearchPreferences.getLastSearchKeyword()}")
                loadPermissionData(SearchPreferences.getLastSearchKeyword())
            } else {
                loadPermissionData("")
            }
        }
    }

    fun getPermissions(): LiveData<MutableList<PermissionInfo>> {
        return permissions
    }

    fun loadPermissionData(keyword: String) {
        viewModelScope.launch(Dispatchers.Default) {
            kotlin.runCatching {
                val context = context
                val appPackageInfo = if (packageManager.isPackageInstalled(packageInfo.packageName)) {
                    packageManager.getPackageInfo(packageInfo.packageName, PackageManager.GET_PERMISSIONS)!!
                } else {
                    packageManager.getPackageArchiveInfo(packageInfo.safeApplicationInfo.sourceDir, PackageManager.GET_PERMISSIONS)!!
                }

                val permissions = arrayListOf<PermissionInfo>()

                for (count in appPackageInfo.requestedPermissions?.indices!!) {
                    val permissionInfo = PermissionInfo()

                    kotlin.runCatching {
                        permissionInfo.permissionInfo = appPackageInfo.requestedPermissions!![count].getPermissionInfo(context)
                        permissionInfo.label = permissionInfo.permissionInfo!!.loadLabel(context.packageManager).toString().capitalizeFirstLetter()

                        if (isKeywordMatched(keyword, appPackageInfo.requestedPermissions!![count], permissionInfo.label)) {
                            if (appPackageInfo.requestedPermissionsFlags!![count] and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0) {
                                permissionInfo.isGranted = 1
                            } else {
                                permissionInfo.isGranted = 0
                            }
                            permissionInfo.name = appPackageInfo.requestedPermissions!![count]
                            permissions.add(permissionInfo)
                        }
                    }.onFailure {
                        permissionInfo.permissionInfo = null
                        permissionInfo.label = appPackageInfo.requestedPermissions!![count]

                        if (isKeywordMatched(keyword, appPackageInfo.requestedPermissions!![count])) {
                            if (appPackageInfo.requestedPermissionsFlags!![count] and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0) {
                                permissionInfo.isGranted = 1
                            } else {
                                permissionInfo.isGranted = 0
                            }
                            permissionInfo.name = appPackageInfo.requestedPermissions!![count]
                            permissions.add(permissionInfo)
                        }
                    }
                }

                appPackageInfo.permissions

                /*
                val requestedPermissions = appPackageInfo.requestedPermissions.toMutableList()

                try {
                    ApkFile(packageInfo.safeApplicationInfo.sourceDir).use { apkFile ->
                        try {
                            apkFile.apkMeta.permissions.forEach { permission ->
                                if (permission.name !in requestedPermissions) {
                                    val permissionInfo = PermissionInfo()

                                    permissionInfo.permissionInfo = permission.name.getPermissionInfo(context)
                                    permissionInfo.label = kotlin.runCatching {
                                        permissionInfo.permissionInfo!!.loadLabel(context.packageManager).toString().capitalizeFirstLetter()
                                    }.getOrElse {
                                        permission.name
                                    }

                                    if (isKeywordMatched(keyword, permission.name, permissionInfo.label)) {
                                        permissionInfo.isGranted = 2
                                        permissionInfo.name = permission.name
                                        permissions.add(permissionInfo)
                                    }
                                }
                            }
                        } catch (e: ParserException) {
                            e.printStackTrace()
                        }
                    }
                } catch (e: java.lang.ClassCastException) {
                    e.printStackTrace()
                }
                */

                this@PermissionsViewModel.permissions.postValue(permissions.apply {
                    sortBy {
                        it.name.lowercase(Locale.getDefault())
                    }
                })
            }.getOrElse {
                if (it is java.lang.NullPointerException) {
                    postWarning(getString(R.string.this_app_doesnt_require_any_permissions))
                } else {
                    postError(it)
                }
            }
        }
    }

    private fun isKeywordMatched(keyword: String, name: String, loadLabel: String): Boolean {
        return name.lowercase().contains(keyword.lowercase()) || loadLabel.lowercase().contains(keyword.lowercase())
    }

    private fun isKeywordMatched(keyword: String, name: String): Boolean {
        return name.lowercase().contains(keyword.lowercase())
    }
}
