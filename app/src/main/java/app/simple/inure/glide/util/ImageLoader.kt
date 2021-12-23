package app.simple.inure.glide.util

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ProviderInfo
import android.content.pm.ServiceInfo
import android.widget.ImageView
import app.simple.inure.decorations.views.ZoomImageView
import app.simple.inure.glide.activities.ActivityIconModel
import app.simple.inure.glide.graphics.AppGraphicsModel
import app.simple.inure.glide.icon.AppIcon
import app.simple.inure.glide.modules.GlideApp
import app.simple.inure.glide.providers.ProviderIconModel
import app.simple.inure.glide.services.ServiceIconModel
import com.bumptech.glide.Glide
import org.jetbrains.annotations.NotNull

object ImageLoader {
    /**
     * Loads app icon asynchronously
     *
     * @param packageName is package id of the app whose icon needs to be loaded
     */
    fun ImageView.loadAppIcon(packageName: String) {
        GlideApp.with(this)
            .asBitmap()
            .load(AppIcon(this.context, packageName))
            .into(this)
    }

    /**
     * Loads app graphics asynchronously
     *
     * @param path - of the apk file or ApplicationInfo.sourceDir
     * @param filePath - path of the raster file inside the zip/apk file
     */
    fun ImageView.loadGraphics(@NotNull path: String, filePath: String) {
        GlideApp.with(this)
            .asBitmap()
            .load(AppGraphicsModel(path, filePath))
            .into(this)
    }

    fun ZoomImageView.loadGraphics(context: Context, @NotNull path: String, filePath: String) {
        Glide.with(context)
            .asBitmap()
            .centerInside()
            .dontAnimate()
            .load(AppGraphicsModel(path, filePath))
            .into(this)
    }

    fun ImageView.loadIconFromActivityInfo(activityInfo: ActivityInfo) {
        GlideApp.with(this)
            .asBitmap()
            .load(ActivityIconModel(activityInfo, this.context))
            .into(this)
    }

    fun ImageView.loadIconFromServiceInfo(serviceInfo: ServiceInfo) {
        GlideApp.with(this.context)
            .asBitmap()
            .load(ServiceIconModel(serviceInfo, this.context))
            .into(this)
    }

    fun ImageView.loadIconFromProviderInfo(providerInfo: ProviderInfo) {
        GlideApp.with(this)
            .asBitmap()
            .load(ProviderIconModel(providerInfo, this.context))
            .into(this)
    }
}
