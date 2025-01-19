package app.simple.inure.viewmodels.installer

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.simple.inure.apk.utils.PermissionUtils.getPermissionInfo
import app.simple.inure.extensions.viewmodels.WrappedViewModel
import app.simple.inure.models.PermissionInfo
import app.simple.inure.util.StringUtils.capitalizeFirstLetter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class InstallerPermissionViewModel(application: Application, val file: File?) : WrappedViewModel(application) {

    private val permissionsInfo: MutableLiveData<Pair<ArrayList<PermissionInfo>, PackageInfo>> by lazy {
        MutableLiveData<Pair<ArrayList<PermissionInfo>, PackageInfo>>().also {
            loadPermissionData()
        }
    }

    fun getPermissionsInfo(): LiveData<Pair<ArrayList<PermissionInfo>, PackageInfo>> {
        return permissionsInfo
    }

    fun loadPermissionData() {
        viewModelScope.launch(Dispatchers.Default) {
            kotlin.runCatching {
                val context = context

                val appPackageInfo = packageManager.getPackageArchiveInfo(file!!.absolutePath, PackageManager.GET_PERMISSIONS)!!
                val permissions = arrayListOf<PermissionInfo>()

                if (appPackageInfo.requestedPermissions != null) {
                    for (count in appPackageInfo.requestedPermissions!!.indices) {
                        val permissionInfo = PermissionInfo()

                        kotlin.runCatching {
                            permissionInfo.permissionInfo = appPackageInfo.requestedPermissions!![count].getPermissionInfo(context)
                            permissionInfo.label = permissionInfo.permissionInfo!!.loadLabel(context.packageManager).toString().capitalizeFirstLetter()
                            if (appPackageInfo.requestedPermissionsFlags!![count] and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0) {
                                permissionInfo.isGranted = 1
                            } else {
                                permissionInfo.isGranted = 0
                            }
                            permissionInfo.name = appPackageInfo.requestedPermissions!![count]
                            permissions.add(permissionInfo)
                        }.onFailure {
                            permissionInfo.permissionInfo = null
                            permissionInfo.label = appPackageInfo.requestedPermissions!![count]
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

                /*
                val requestedPermissions = appPackageInfo.requestedPermissions.toMutableList()

                apkFile.apkMeta.permissions.forEach { permission ->
                    if (permission.name !in requestedPermissions) {
                        val permissionInfo = PermissionInfo()

                        permissionInfo.permissionInfo = permission.name.getPermissionInfo(context)
                        permissionInfo.label = kotlin.runCatching {
                            permissionInfo.permissionInfo!!.loadLabel(context.packageManager).toString().capitalizeFirstLetter()
                        }.getOrElse {
                            permission.name
                        }

                        permissionInfo.isGranted = 2
                        permissionInfo.name = permission.name
                        permissions.add(permissionInfo)
                    }
                }

                apkFile.close()
                */

                permissionsInfo.postValue(Pair(permissions.apply {
                    sortBy {
                        it.name.lowercase(Locale.getDefault())
                    }
                }, appPackageInfo))
            }.getOrElse {
                postError(it)
            }
        }
    }
}
