package app.simple.inure.viewmodels.viewers

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.simple.inure.R
import app.simple.inure.apk.utils.MetaUtils
import app.simple.inure.apk.utils.PackageUtils.isPackageInstalled
import app.simple.inure.extensions.viewmodels.WrappedViewModel
import app.simple.inure.models.ServiceInfoModel
import app.simple.inure.preferences.SearchPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ServicesViewModel(application: Application, private val packageInfo: PackageInfo) : WrappedViewModel(application) {

    private val services: MutableLiveData<MutableList<ServiceInfoModel>> by lazy {
        MutableLiveData<MutableList<ServiceInfoModel>>().also {
            if (SearchPreferences.isSearchKeywordModeEnabled()) {
                getServicesData(SearchPreferences.getLastSearchKeyword())
            } else {
                getServicesData("")
            }
        }
    }

    fun getServices(): LiveData<MutableList<ServiceInfoModel>> {
        return services
    }

    fun getServicesData(keyword: String) {
        viewModelScope.launch(Dispatchers.Default) {
            kotlin.runCatching {
                val list = arrayListOf<ServiceInfoModel>()
                val signatures: Array<String> = context.resources.getStringArray(R.array.trackers)
                val isInstalled = packageManager.isPackageInstalled(packageInfo.packageName)

                for (info in getPackageInfo(isInstalled).services) {
                    val serviceInfoModel = ServiceInfoModel()

                    serviceInfoModel.serviceInfo = info
                    serviceInfoModel.name = info.name
                    serviceInfoModel.isExported = info.exported
                    serviceInfoModel.flags = info.flags
                    serviceInfoModel.foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) info.foregroundServiceType else -3
                    serviceInfoModel.permissions = info.permission ?: application.getString(R.string.no_permissions_required)

                    for (signature in signatures) {
                        if (serviceInfoModel.serviceInfo.name!!.contains(signature)) {
                            serviceInfoModel.trackerId = signature
                            break
                        }
                    }

                    with(StringBuilder()) {
                        append(" | ")
                        append(MetaUtils.getForegroundServiceType(serviceInfoModel.foregroundType, application))
                        append(" | ")
                        append(MetaUtils.getServiceFlags(info.flags, application))

                        serviceInfoModel.status = this.toString()
                    }

                    if (serviceInfoModel.name.lowercase().contains(keyword.lowercase())) {
                        list.add(serviceInfoModel)
                    }
                }

                list.sortBy {
                    it.name.substring(it.name.lastIndexOf(".") + 1)
                }

                services.postValue(list)
            }.getOrElse {
                if (it is NullPointerException) {
                    notFound.postValue(4)
                } else {
                    postError(it)
                }
            }
        }
    }

    private fun getPackageInfo(isInstalled: Boolean): PackageInfo {
        return if (isInstalled) {
            packageManager.getPackageInfo(packageInfo.packageName, PackageManager.GET_SERVICES)!!
        } else {
            packageManager.getPackageArchiveInfo(packageInfo.applicationInfo.sourceDir, PackageManager.GET_SERVICES)!!
        }
    }
}
