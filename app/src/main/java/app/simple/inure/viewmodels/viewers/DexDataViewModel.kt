package app.simple.inure.viewmodels.viewers

import android.app.Application
import android.content.pm.PackageInfo
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.simple.inure.extensions.viewmodels.WrappedViewModel
import app.simple.inure.models.DexClass
import app.simple.inure.util.TrackerUtils
import dalvik.system.DexFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class DexDataViewModel(application: Application, private val packageInfo: PackageInfo) : WrappedViewModel(application) {

    private val classes = ArrayList<String>()
    private val backup = ArrayList<DexClass>()

    private val dexData: MutableLiveData<ArrayList<DexClass>> by lazy {
        MutableLiveData<ArrayList<DexClass>>().also {
            loadDexData()
        }
    }

    fun getDexClasses(): MutableLiveData<ArrayList<DexClass>> {
        return dexData
    }

    private fun loadDexData() {
        viewModelScope.launch(Dispatchers.Default) {
            kotlin.runCatching {
                val classes: ArrayList<String> = getClassesOfPackage(packageInfo.packageName)
                val trackerSignatures = TrackerUtils.getTrackerSignatures()
                val trackerSignaturesPattern = trackerSignatures.joinToString("|") {
                    Regex.escape(it.lowercase())
                }.toRegex()

                val deferredDexClasses = classes.map { className ->
                    async {
                        val dexClass = DexClass(className)
                        val lowerCaseClassName = className.lowercase()

                        dexClass.trackerSignature = trackerSignaturesPattern.find(lowerCaseClassName)?.value
                        dexClass.isTracker = dexClass.trackerSignature != null

                        dexClass
                    }
                }

                val dexClasses = deferredDexClasses.awaitAll()
                backup.addAll(dexClasses)
                dexData.postValue(ArrayList(dexClasses))
            }.getOrElse {
                postError(it)
            }
        }
    }

    @Suppress("DEPRECATION") // Why is Android so hard to work with? :(
    private fun getClassesOfPackage(packageName: String): ArrayList<String> {
        val appContext = applicationContext().createPackageContext(packageName, 0)
        val packageCodePath: String = appContext.packageCodePath
        val dexFile = DexFile(packageCodePath)
        val enumeration = dexFile.entries()

        while (enumeration.hasMoreElements()) {
            val className = enumeration.nextElement()
            classes.add(className)
        }

        return classes
    }

    fun filterClasses(query: String) {
        viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                if (query.isNotEmpty()) {
                    if (TRACKER.contains(query.lowercase())) {
                        dexData.postValue(ArrayList(backup.filter {
                            it.isTracker
                        }))
                    } else {
                        dexData.postValue(ArrayList(backup.filter {
                            it.className.lowercase().contains(query.lowercase(), true)
                        }))
                    }
                } else {
                    dexData.postValue(backup)
                }
            }
        }
    }

    companion object {
        private val TRACKER = arrayOf("tracker", "trackers")
    }
}
