package app.simple.inure.viewmodels.viewers

import android.app.Application
import android.content.pm.PackageInfo
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.simple.inure.R
import app.simple.inure.apk.utils.PackageUtils.safeApplicationInfo
import app.simple.inure.extensions.viewmodels.RootServiceViewModel
import app.simple.inure.util.NullSafety.isNotNull
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class SharedPreferencesViewModel(private val packageInfo: PackageInfo, application: Application) : RootServiceViewModel(application) {

    init {
        initRootProc()
    }

    private val path = packageInfo.safeApplicationInfo.dataDir + "/shared_prefs/"

    private val sharedPrefsFiles: MutableLiveData<ArrayList<String>> by lazy {
        MutableLiveData<ArrayList<String>>()
    }

    private val deleted: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }

    fun getSharedPrefs(): LiveData<ArrayList<String>> {
        return sharedPrefsFiles
    }

    fun getDeleted(): LiveData<Int> {
        return deleted
    }

    private fun loadSharedPrefsFiles(fileSystemManager: FileSystemManager) {
        viewModelScope.launch(Dispatchers.IO) {
            withTimeout(10000) {
                kotlin.runCatching {
                    with(fileSystemManager.getFile(path)) {
                        kotlin.runCatching {
                            val list = this.list()

                            if (list.isNullOrEmpty()) {
                                postWarning(getString(R.string.no_preferences_found))
                            } else {
                                try {
                                    sharedPrefsFiles.postValue(list.toList() as ArrayList<String>)
                                } catch (e: ClassCastException) {
                                    /**
                                     *  If the directory only contains one file, the list will be a singleton list
                                     *  and the cast will fail. This is a workaround for that.
                                     */
                                    val list1 = arrayListOf<String>()
                                    list.forEach {
                                        list1.add(it)
                                    }
                                    sharedPrefsFiles.postValue(list1)
                                }
                            }
                        }.getOrElse {
                            postError(it)
                            sharedPrefsFiles.postValue(arrayListOf())
                        }
                    }
                }.getOrElse {
                    postError(it)
                }
            }
        }
    }

    fun deletePreferences(path: String, requestCode: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            kotlin.runCatching {
                // Force close the app first
                Shell.cmd("am force-stop ${packageInfo.packageName}").exec()

                with(getFileSystemManager()?.getFile(getSharedPrefsPath() + path)) {
                    if (this?.delete() == true) {
                        deleted.postValue(requestCode)
                    } else {
                        deleted.postValue(-1)
                        postWarning("Failed to delete file: ${getSharedPrefsPath() + path}")
                    }
                }
            }.getOrElse {
                it.printStackTrace()
                postWarning(it.stackTraceToString())
            }
        }
    }

    fun resetDeleted() {
        deleted.value = -1
    }

    fun getSharedPrefsPath(): String {
        return path
    }

    override fun runRootProcess(fileSystemManager: FileSystemManager?) {
        if (fileSystemManager.isNotNull()) {
            fileSystemManager?.let {
                loadSharedPrefsFiles(it)
            }
        } else {
            postWarning("ERR: Could not acquire file system manager with root access")
        }
    }
}
