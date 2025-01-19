package app.simple.inure.glide.icon

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import app.simple.inure.apk.utils.PackageUtils.safeApplicationInfo
import app.simple.inure.glide.util.GlideUtils.getGeneratedAppIconBitmap
import app.simple.inure.util.BitmapHelper.toBitmap
import app.simple.inure.util.BitmapHelper.toGrayscale
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher

class AppIconFetcher internal constructor(private val appIcon: AppIcon) : DataFetcher<Bitmap> {

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in Bitmap>) {
        try {
            kotlin.runCatching {
                // try getting the properly colored launcher icons
                val launcher = appIcon.context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                val activityList = launcher.getActivityList(appIcon.packageName, android.os.Process.myUserHandle())[0]
                if (appIcon.enabled) {
                    callback.onDataReady(activityList.getIcon(0).toBitmap())
                } else {
                    callback.onDataReady(activityList.getIcon(0).toBitmap().toGrayscale())
                }
            }.onFailure {
                // Loading proper icon failed, try loading default icon
                kotlin.runCatching {
                    if (appIcon.enabled) {
                        callback.onDataReady(
                                appIcon.context.packageManager
                                    .getApplicationIcon(appIcon.packageName)
                                    .toBitmap())
                    } else {
                        callback.onDataReady(
                                appIcon.context.packageManager
                                    .getApplicationIcon(appIcon.packageName)
                                    .toBitmap().toGrayscale())
                    }
                }.onFailure {
                    // Loading default icon failed, load icon from APK
                    val p0 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        appIcon.context.packageManager
                            .getPackageArchiveInfo(appIcon.file!!.absolutePath,
                                                   PackageManager.PackageInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
                    } else {
                        @Suppress("DEPRECATION")
                        appIcon.context.packageManager
                            .getPackageArchiveInfo(appIcon.file!!.absolutePath,
                                                   PackageManager.GET_META_DATA)
                    }
                    p0!!.safeApplicationInfo.sourceDir = appIcon.file.absolutePath
                    p0.safeApplicationInfo.publicSourceDir = appIcon.file.absolutePath
                    val b = appIcon.context.packageManager.getApplicationIcon(p0.safeApplicationInfo)
                    callback.onDataReady(b.toBitmap().toGrayscale())
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            callback.onDataReady(appIcon.context.getGeneratedAppIconBitmap())
        } catch (e: NullPointerException) {
            callback.onDataReady(appIcon.context.getGeneratedAppIconBitmap())
        } catch (e: Exception) {
            callback.onDataReady(appIcon.context.getGeneratedAppIconBitmap())
        }
    }

    override fun cleanup() {
        /* no-op */
    }

    override fun cancel() {
        /* no-op */
    }

    override fun getDataClass(): Class<Bitmap> {
        return Bitmap::class.java
    }

    override fun getDataSource(): DataSource {
        return DataSource.LOCAL
    }

    companion object {
        private const val TAG = "AppIconFetcher"
    }
}
