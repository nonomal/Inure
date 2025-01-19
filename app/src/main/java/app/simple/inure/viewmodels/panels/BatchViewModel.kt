package app.simple.inure.viewmodels.panels

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Build
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.simple.inure.apk.parsers.FOSSParser
import app.simple.inure.apk.utils.PackageUtils
import app.simple.inure.apk.utils.PackageUtils.getPackageSize
import app.simple.inure.apk.utils.PackageUtils.safeApplicationInfo
import app.simple.inure.constants.SortConstant
import app.simple.inure.database.instances.BatchDatabase
import app.simple.inure.database.instances.BatchProfileDatabase
import app.simple.inure.extensions.viewmodels.DataGeneratorViewModel
import app.simple.inure.models.BatchModel
import app.simple.inure.models.BatchPackageInfo
import app.simple.inure.preferences.BatchPreferences
import app.simple.inure.util.ArrayUtils.toArrayList
import app.simple.inure.util.FlagUtils
import app.simple.inure.util.NullSafety.isNotNull
import app.simple.inure.util.Sort.getSortedList
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Collections
import java.util.stream.Collectors

class BatchViewModel(application: Application) : DataGeneratorViewModel(application) {

    private var batchDatabase: BatchDatabase? = null
    private var batchProfileDatabase: BatchProfileDatabase? = null

    private var clearedSize = 0L

    private val batchData: MutableLiveData<ArrayList<BatchPackageInfo>> by lazy {
        MutableLiveData<ArrayList<BatchPackageInfo>>()
    }

    private val selectedApps: MutableLiveData<ArrayList<BatchPackageInfo>> by lazy {
        MutableLiveData<ArrayList<BatchPackageInfo>>().also {
            loadSelectedApps()
        }
    }

    private val clearedCacheSize: MutableLiveData<Long> by lazy {
        MutableLiveData<Long>()
    }

    fun getBatchData(): LiveData<ArrayList<BatchPackageInfo>> {
        return batchData
    }

    fun getSelectedApps(): LiveData<ArrayList<BatchPackageInfo>> {
        return selectedApps
    }

    fun getClearedCacheSize(): LiveData<Long> {
        return clearedCacheSize
    }

    fun shouldShowLoader(): Boolean {
        return batchData.value.isNullOrEmpty()
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadAppData() {
        viewModelScope.launch(Dispatchers.Default) {
            var apps = (getInstalledApps() + getUninstalledApps()).toArrayList()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                apps.removeIf { it.packageName == applicationContext().packageName }
            } else {
                apps = apps.filter { it.packageName != applicationContext().packageName } as ArrayList<PackageInfo>
            }

            when (BatchPreferences.getAppsCategory()) {
                SortConstant.SYSTEM -> {
                    apps = apps.stream().filter { p ->
                        p.safeApplicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                    }.collect(Collectors.toList()) as ArrayList<PackageInfo>
                }
                SortConstant.USER -> {
                    apps = apps.stream().filter { p ->
                        p.safeApplicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0
                    }.collect(Collectors.toList()) as ArrayList<PackageInfo>
                }
            }

            apps.getSortedList(BatchPreferences.getSortStyle(), BatchPreferences.isReverseSorting())

            val list = getBatchStateData(apps.clone() as ArrayList<PackageInfo>)
            var filtered = arrayListOf<BatchPackageInfo>()

            for (item in list) {
                if (FlagUtils.isFlagSet(BatchPreferences.getAppsFilter(), SortConstant.BATCH_SELECTED)) {
                    if (item.isSelected) {
                        if (!filtered.contains(item)) {
                            filtered.add(item)
                        }
                    }
                }

                if (FlagUtils.isFlagSet(BatchPreferences.getAppsFilter(), SortConstant.BATCH_NOT_SELECTED)) {
                    if (!item.isSelected) {
                        if (!filtered.contains(item)) {
                            filtered.add(item)
                        }
                    }
                }

                if (FlagUtils.isFlagSet(BatchPreferences.getAppsFilter(), SortConstant.BATCH_ENABLED)) {
                    if (item.packageInfo.safeApplicationInfo.enabled) {
                        if (!filtered.contains(item)) {
                            filtered.add(item)
                        }
                    }
                }

                if (FlagUtils.isFlagSet(BatchPreferences.getAppsFilter(), SortConstant.BATCH_DISABLED)) {
                    if (!item.packageInfo.safeApplicationInfo.enabled) {
                        if (!filtered.contains(item)) {
                            filtered.add(item)
                        }
                    }
                }

                if (FlagUtils.isFlagSet(BatchPreferences.getAppsFilter(), SortConstant.BATCH_UNINSTALLED)) {
                    if (item.packageInfo.safeApplicationInfo.flags and ApplicationInfo.FLAG_INSTALLED == 0) {
                        if (!filtered.contains(item)) {
                            filtered.add(item)
                        }
                    }
                }

                if (FlagUtils.isFlagSet(BatchPreferences.getAppsFilter(), SortConstant.BATCH_FOSS)) {
                    if (FOSSParser.isPackageFOSS(item.packageInfo)) {
                        if (!filtered.contains(item)) {
                            filtered.add(item)
                        }
                    }
                }

                if (FlagUtils.isFlagSet(BatchPreferences.getAppsFilter(), SortConstant.BATCH_APK)) {
                    if (item.packageInfo.safeApplicationInfo.splitSourceDirs.isNullOrEmpty()) {
                        if (!filtered.contains(item)) {
                            filtered.add(item)
                        }
                    }
                }

                if (FlagUtils.isFlagSet(BatchPreferences.getAppsFilter(), SortConstant.BATCH_SPLIT)) {
                    if (!item.packageInfo.safeApplicationInfo.splitSourceDirs.isNullOrEmpty()) {
                        if (!filtered.contains(item)) {
                            filtered.add(item)
                        }
                    }
                }
            }

            filtered = filtered.parallelStream().distinct().collect(Collectors.toList()) as ArrayList<BatchPackageInfo>

            batchData.postValue(filtered)
        }
    }

    private fun loadSelectedApps() {
        viewModelScope.launch(Dispatchers.IO) {
            selectedApps.postValue(getSelectedBatchStateData(getInstalledApps()))
        }
    }

    private fun getBatchStateData(apps: ArrayList<PackageInfo>): ArrayList<BatchPackageInfo> {
        batchDatabase = BatchDatabase.getInstance(context)

        val list = arrayListOf<BatchPackageInfo>()

        for (app in apps) {
            list.add(BatchPackageInfo(app, false, -1))
        }

        for (batch in batchDatabase!!.batchDao()!!.getBatch()) {
            for (item in list) {
                if (batch.packageName == item.packageInfo.packageName) {
                    with(batch.isSelected) {
                        item.isSelected = this
                        item.dateSelected = if (this) batch.dateSelected else -1
                    }

                    break
                }
            }
        }

        if (BatchPreferences.isSelectionOnTop()) {
            list.sortByDescending {
                it.isSelected
            }
        }

        return list
    }

    private fun getSelectedBatchStateData(apps: ArrayList<PackageInfo>): ArrayList<BatchPackageInfo> {
        batchDatabase = BatchDatabase.getInstance(context)

        var list = arrayListOf<BatchPackageInfo>()

        for (app in apps) {
            list.add(BatchPackageInfo(app, false, -1))
        }

        for (batch in batchDatabase!!.batchDao()!!.getSelectedApps()) {
            for (item in list) {
                if (batch.packageName == item.packageInfo.packageName) {
                    with(batch.isSelected) {
                        item.isSelected = this
                        if (this) {
                            item.dateSelected = batch.dateSelected
                        } else {
                            item.dateSelected = -1
                        }
                    }
                    break
                }
            }
        }

        list = list.stream().filter { p -> p.isSelected }.collect(Collectors.toList()) as ArrayList<BatchPackageInfo>

        for (i in list.indices) {
            list[i].packageInfo.safeApplicationInfo.name = PackageUtils.getApplicationName(
                    applicationContext(), list[i].packageInfo.safeApplicationInfo)
        }

        if (BatchPreferences.isSelectionOnTop()) {
            list.sortByDescending {
                it.isSelected
            }
        }

        return list
    }

    fun updateBatchItem(batchPackageInfo: BatchPackageInfo, update: Boolean = false, updateSelected: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            batchDatabase = BatchDatabase.getInstance(context)
            batchDatabase?.batchDao()
                ?.insertBatch(BatchModel(batchPackageInfo.packageInfo.packageName,
                                         batchPackageInfo.isSelected,
                                         System.currentTimeMillis()))

            if (update) {
                loadAppData()
            }

            if (updateSelected) {
                loadSelectedApps()
            }
        }
    }

    fun clearSelectedAppsCache(currentAppsList: java.util.ArrayList<BatchPackageInfo>) {
        viewModelScope.launch(Dispatchers.IO) {
            clearedSize = 0L

            runCatching {
                for (batchPackageInfo in currentAppsList) {
                    val size = with(batchPackageInfo.packageInfo.getPackageSize(applicationContext())) {
                        cacheSize + dataSize + codeSize
                    }

                    runCatching {
                        with(getCommand(batchPackageInfo.packageInfo)) {
                            if (this.isNotNull() || this.contains("null")) {
                                Shell.cmd(this).exec().let {
                                    val sizeNow = with(batchPackageInfo.packageInfo.getPackageSize(applicationContext())) {
                                        cacheSize + dataSize + codeSize
                                    }

                                    clearedSize += size - sizeNow
                                }
                            }
                        }
                    }.onFailure {
                        it.printStackTrace()
                    }.onSuccess {
                        Log.d("ClearCacheViewModel", "clearSelectedAppsCache: Success")
                    }
                }
            }.onSuccess {
                clearedCacheSize.postValue(clearedSize)
            }.onFailure {
                it.printStackTrace()
                postWarning(it.message ?: "ERR: ${it.stackTraceToString()}")
            }
        }
    }

    private fun getCommand(packageInfo: PackageInfo): String {
        //        return "rm -r -v /data/data/${packageInfo.packageName}/cache " +
        //                "& rm -r -v /data/data/${packageInfo.packageName}/app_cache " +
        //                "& rm -r -v /data/data/${packageInfo.packageName}/app_texture " +
        //                "& rm -r -v /data/data/${packageInfo.packageName}/app_webview " +
        //                "& rm -r -v /data/data/${packageInfo.packageName}/code_cache" +
        //                "& rm -r -v /data/data/${packageInfo.packageName}/files"

        val packageContext: Context = applicationContext().createPackageContext(packageInfo.packageName, 0)
        val directories: MutableList<File?> = java.util.ArrayList()
        val command = StringBuilder("rm -rf")

        directories.add(packageContext.cacheDir)
        Collections.addAll(directories, *packageContext.externalCacheDirs)

        for (directory in directories) {
            Log.d("ClearCacheViewModel", "getCommand: ${directory?.absolutePath}")
            if (directory?.absolutePath.isNotNull()) {
                command.append(" \"" + directory?.absolutePath.toString() + "\"")
            }
        }

        return command.toString()
    }

    override fun onAppUninstalled(packageName: String?) {
        super.onAppUninstalled(packageName)
        loadAppData()
    }

    fun refresh() {
        loadAppData()
    }

    override fun onAppsLoaded(apps: ArrayList<PackageInfo>) {
        loadAppData()
    }

    override fun onCleared() {
        super.onCleared()
        batchDatabase?.close()
    }

    fun selectAllBatchItems() {
        viewModelScope.launch(Dispatchers.IO) {
            batchDatabase = BatchDatabase.getInstance(context)
            for (batchData in batchData.value!!) {
                if (!batchData.isSelected) {
                    batchDatabase?.batchDao()
                        ?.insertBatch(BatchModel(batchData.packageInfo.packageName,
                                                 true,
                                                 System.currentTimeMillis()))
                }
            }

            loadSelectedApps()
            loadAppData()
        }
    }

    fun deselectAllBatchItems() {
        viewModelScope.launch(Dispatchers.IO) {
            batchDatabase = BatchDatabase.getInstance(context)
            for (batchData in batchData.value!!) {
                if (batchData.isSelected) {
                    batchDatabase?.batchDao()
                        ?.insertBatch(BatchModel(batchData.packageInfo.packageName,
                                                 false,
                                                 System.currentTimeMillis()))
                }
            }

            loadSelectedApps()
            loadAppData()
        }
    }

    fun loadBatchProfile(id: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            if (id == -1) {
                return@launch
            }

            batchDatabase = BatchDatabase.getInstance(context)
            batchProfileDatabase = BatchProfileDatabase.getInstance(context)
            batchDatabase?.batchDao()?.nukeTable()

            val batchProfile = batchProfileDatabase?.batchProfileDao()?.getBatchProfile(id)

            batchProfile?.packageNames?.split(",")?.forEach { selectionData ->
                val selection = selectionData.lastIndexOf("_").let {
                    arrayOf(selectionData.substring(0, it), selectionData.substring(it + 1))
                }

                BatchModel(selection[0], true, selection[1].toLong()).let {
                    batchDatabase?.batchDao()?.insertBatch(it)
                }
            }

            refreshPackageData()
        }
    }
}
