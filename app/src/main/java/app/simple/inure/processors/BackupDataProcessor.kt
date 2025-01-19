package app.simple.inure.processors

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import app.simple.inure.activities.alias.TerminalAlias
import app.simple.inure.activities.association.ApkInstallerActivity
import app.simple.inure.activities.association.AppInformationActivity
import app.simple.inure.activities.association.AudioPlayerActivity
import app.simple.inure.activities.association.BashAssociation
import app.simple.inure.activities.association.ImageActivity
import app.simple.inure.activities.association.InformationActivity
import app.simple.inure.activities.association.ManifestAssociationActivity
import app.simple.inure.activities.association.TTFViewerActivity
import app.simple.inure.activities.association.TextViewerActivity
import app.simple.inure.constants.Warnings
import app.simple.inure.database.instances.BatchDatabase
import app.simple.inure.database.instances.BatchProfileDatabase
import app.simple.inure.database.instances.FOSSDatabase
import app.simple.inure.database.instances.NotesDatabase
import app.simple.inure.database.instances.QuickAppsDatabase
import app.simple.inure.database.instances.StackTraceDatabase
import app.simple.inure.database.instances.TagsDatabase
import app.simple.inure.database.instances.TerminalCommandDatabase
import app.simple.inure.preferences.MainPreferences
import app.simple.inure.preferences.SharedPreferences
import app.simple.inure.preferences.TrialPreferences
import app.simple.inure.util.AppUtils
import app.simple.inure.util.FileUtils.toFile
import net.lingala.zip4j.ZipFile
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

object BackupDataProcessor {

    private const val FILENAME = "inure_data_@date.inrbkp"
    private const val PREFERENCES = "prefs_bkp"

    private val errors: MutableList<String> = mutableListOf()

    private val components = arrayListOf<String>(
            AppInformationActivity::class.java.name,
            AudioPlayerActivity::class.java.name,
            BashAssociation::class.java.name,
            ImageActivity::class.java.name,
            InformationActivity::class.java.name,
            ApkInstallerActivity::class.java.name,
            ManifestAssociationActivity::class.java.name,
            TTFViewerActivity::class.java.name,
            TextViewerActivity::class.java.name,
            TerminalAlias::class.java.name
    )

    fun Context.exportAppData(): String {
        val paths = mutableListOf<File>()
        MainPreferences.addLegacyPreferences()

        saveSharedPreferencesToFile(this).toFile().let {
            if (it.exists()) {
                paths.add(it)
            }
        }

        BatchDatabase.getBatchDataPath(this).toFile().let {
            if (it.exists()) {
                paths.add(it)
            }
        }

        NotesDatabase.getNotesDataPath(this).toFile().let {
            if (it.exists()) {
                paths.add(it)
            }
        }

        QuickAppsDatabase.getQuickAppsDataPath(this).toFile().let {
            if (it.exists()) {
                paths.add(it)
            }
        }

        StackTraceDatabase.getStackTraceDataPath(this).toFile().let {
            if (it.exists()) {
                paths.add(it)
            }
        }

        TerminalCommandDatabase.getTerminalCommandDataPath(this).toFile().let {
            if (it.exists()) {
                paths.add(it)
            }
        }

        TagsDatabase.getTagDataPath(this).toFile().let {
            if (it.exists()) {
                paths.add(it)
            }
        }

        FOSSDatabase.getFOSSDataPath(this).toFile().let {
            if (it.exists()) {
                paths.add(it)
            }
        }

        exportComponentState().let {
            if (it.exists()) {
                paths.add(it)
            }
        }

        BatchDatabase.getInstance(this)?.close()
        NotesDatabase.getInstance(this)?.close()
        QuickAppsDatabase.getInstance(this)?.close()
        StackTraceDatabase.getInstance(this)?.close()
        TerminalCommandDatabase.getInstance(this)?.close()
        TagsDatabase.getInstance(this)?.close()
        FOSSDatabase.getInstance(this)?.close()
        BatchProfileDatabase.getInstance(this)?.close()

        if (File(filesDir.path + "/backup").exists().not()) {
            File(filesDir.path + "/backup").mkdir()
        } else {
            File(filesDir.path + "/backup").listFiles()?.forEach {
                if (it.name.endsWith(".inrbkp")) {
                    it.delete()
                }
            }
        }

        val zipPath = filesDir.path + "/backup/" +
                "/${FILENAME.replace("@date", System.currentTimeMillis().toString())}"

        ZipFile(zipPath).use {
            it.addFiles(paths)
        }

        // Reopen databases
        SharedPreferences.init(this)
        BatchDatabase.getInstance(this)
        NotesDatabase.getInstance(this)
        QuickAppsDatabase.getInstance(this)
        StackTraceDatabase.getInstance(this)
        TerminalCommandDatabase.getInstance(this)
        TagsDatabase.getInstance(this)
        FOSSDatabase.getInstance(this)
        BatchProfileDatabase.getInstance(this)

        return zipPath
    }

    fun Context.importAppData(dataPath: String, function: (MutableList<String>) -> Unit) {
        errors.clear()

        val paths = mutableListOf(
                (filesDir.path + "/backup/" + PREFERENCES).toFile(),
                BatchDatabase.getBatchDataPath(this).toFile(),
                NotesDatabase.getNotesDataPath(this).toFile(),
                QuickAppsDatabase.getQuickAppsDataPath(this).toFile(),
                StackTraceDatabase.getStackTraceDataPath(this).toFile(),
                TerminalCommandDatabase.getTerminalCommandDataPath(this).toFile(),
                TagsDatabase.getTagDataPath(this).toFile(),
                FOSSDatabase.getFOSSDataPath(this).toFile(),
                BatchProfileDatabase.getBatchProfileDataPath(this).toFile(),
                (filesDir.path + "/backup/component").toFile()
        )

        BatchDatabase.getInstance(this)?.close()
        NotesDatabase.getInstance(this)?.close()
        QuickAppsDatabase.getInstance(this)?.close()
        StackTraceDatabase.getInstance(this)?.close()
        TerminalCommandDatabase.getInstance(this)?.close()
        TagsDatabase.getInstance(this)?.close()
        FOSSDatabase.getInstance(this)?.close()
        BatchProfileDatabase.getInstance(this)?.close()

        for (path in paths) {
            if (path.exists()) {
                path.delete()
            }
        }

        val zipFile = ZipFile(dataPath)

        if (zipFile.isValidZipFile.not()) {
            errors.add("Invalid backup file")
            function(errors)
            return
        }

        for (file in zipFile.fileHeaders) {
            try {
                for (path in paths) {
                    if (file.fileName.endsWith(path.name)) {
                        if (path.name.equals(PREFERENCES)) {
                            zipFile.extractFile(file.fileName, path.parent)
                            loadSharedPreferencesFromFile(path)
                        } else if (path.name.equals("component")) {
                            zipFile.extractFile(file.fileName, path.parent)
                            loadComponentState(path)
                        } else {
                            zipFile.extractFile(file.fileName, path.parent)
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Log.d("AppDataLoader", "Error extracting file: ${file.fileName}")
                errors.add("Error extracting file: ${file.fileName}")
            }
        }

        zipFile.close()

        SharedPreferences.init(this)
        BatchDatabase.getInstance(this)
        NotesDatabase.getInstance(this)
        QuickAppsDatabase.getInstance(this)
        StackTraceDatabase.getInstance(this)
        TerminalCommandDatabase.getInstance(this)
        TagsDatabase.getInstance(this)
        FOSSDatabase.getInstance(this)
        BatchProfileDatabase.getInstance(this)
        TrialPreferences.migrateLegacy()

        function(errors)
    }

    private fun saveSharedPreferencesToFile(context: Context): String {
        var output: ObjectOutputStream? = null
        val path = (context.filesDir.path + "/backup/" + PREFERENCES).toFile()

        if (path.exists()) {
            path.delete()
        } else {
            path.parentFile?.mkdirs()
            path.createNewFile()
        }

        try {
            output = ObjectOutputStream(FileOutputStream(path))
            val pref = SharedPreferences.getSharedPreferences()
            output.writeObject(pref.all)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                if (output != null) {
                    output.flush()
                    output.close()
                }
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        }

        return path.absolutePath
    }

    @SuppressLint("ApplySharedPref")
    private fun loadSharedPreferencesFromFile(src: File): Boolean {
        var result = false
        var input: ObjectInputStream? = null

        try {
            input = ObjectInputStream(FileInputStream(src))
            val prefEdit: android.content.SharedPreferences.Editor = SharedPreferences.getSharedPreferences().edit()
            prefEdit.clear()
            @Suppress("UNCHECKED_CAST")
            val entries = input.readObject() as Map<String, *>

            for ((key, value) in entries) {
                when (value) {
                    is Boolean -> prefEdit.putBoolean(key, value)
                    is Float -> prefEdit.putFloat(key, value)
                    is Int -> prefEdit.putInt(key, value)
                    is Long -> prefEdit.putLong(key, value)
                    is String -> prefEdit.putString(key, value)
                    is java.util.HashSet<*> -> {
                        try {
                            @Suppress("UNCHECKED_CAST")
                            prefEdit.putStringSet(key, value as HashSet<String>)
                        } catch (_: ClassCastException) {
                            // Swallow it, for now
                        }
                    }
                    else -> throw IllegalArgumentException(
                            Warnings.UNSUPPORTED_TYPE_VALUE
                                .replace("$", value?.javaClass?.name ?: "null"))
                }
            }

            prefEdit.commit()
            result = true

        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } finally {
            try {
                input?.close()
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        }

        return result
    }

    private fun Context.exportComponentState(): File {
        val string = buildString {
            for (component in components) {
                append(component)
                append(" ")
                append(packageManager.getComponentEnabledSetting(
                        ComponentName(this@exportComponentState, Class.forName(component))))
                append("\n")
            }
        }

        val path = (filesDir.path + "/backup/component").toFile()

        if (path.exists()) {
            path.delete()
        } else {
            path.parentFile?.mkdirs()
            path.createNewFile()
        }

        path.writeText(string.trim())

        return path
    }

    private fun Context.loadComponentState(path: File) {
        // val path = (filesDir.path + "/backup/component").toFile()

        if (path.exists()) {
            val lines = path.readLines()
            val componentState = mutableMapOf<String, Int>()

            for (line in lines) {
                try {
                    val split = line.split(" ")
                    // componentState[split[0]] = split[1].toInt()

                    when {
                        AppUtils.isPlayFlavor() -> {
                            if (split[0].contains("app.simple.inure.play")) {
                                componentState[split[0]] = split[1].toInt()
                            } else {
                                componentState[split[0]
                                    .replace("app.simple.inure", "app.simple.inure.play")] = split[1].toInt()
                            }
                        }
                        AppUtils.isPlayFlavor().not() -> {
                            if (split[0].contains("app.simple.inure.play")) {
                                componentState[split[0]
                                    .replace("app.simple.inure.play", "app.simple.inure")] = split[1].toInt()
                            } else {
                                componentState[split[0]] = split[1].toInt()
                            }
                        }
                    }
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                    errors.add("Error loading component state: $line")
                }
            }

            for (component in components) {
                try {
                    packageManager.setComponentEnabledSetting(
                            ComponentName(this, Class.forName(component)),
                            componentState[component]
                                ?: PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP)
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                    errors.add("Error setting component state: $component")
                }
            }
        }
    }
}
