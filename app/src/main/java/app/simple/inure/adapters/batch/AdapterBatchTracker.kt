package app.simple.inure.adapters.batch

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.simple.inure.R
import app.simple.inure.adapters.viewers.AdapterTrackers
import app.simple.inure.decorations.condensed.CondensedDynamicRippleConstraintLayout
import app.simple.inure.decorations.overscroll.VerticalListViewHolder
import app.simple.inure.decorations.toggles.CheckBox
import app.simple.inure.decorations.typeface.TypeFaceTextView
import app.simple.inure.decorations.views.AppIconImageView
import app.simple.inure.glide.util.ImageLoader.loadIconFromActivityInfo
import app.simple.inure.glide.util.ImageLoader.loadIconFromProviderInfo
import app.simple.inure.glide.util.ImageLoader.loadIconFromServiceInfo
import app.simple.inure.models.Tracker
import app.simple.inure.preferences.ConfigurationPreferences
import app.simple.inure.util.AdapterUtils
import app.simple.inure.util.ConditionUtils.invert
import app.simple.inure.util.StringUtils.appendFlag
import app.simple.inure.util.ViewUtils.gone
import app.simple.inure.util.ViewUtils.visible

class AdapterBatchTracker(private val trackers: ArrayList<Tracker>, private val batchTrackerCallbacks: BatchTrackerCallbacks) : RecyclerView.Adapter<AdapterBatchTracker.Holder>() {

    private val isRooted = ConfigurationPreferences.isUsingRoot()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context)
                          .inflate(R.layout.adapter_selector_batch_tracker, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        when {
            trackers[position].isActivity -> {
                holder.icon.loadIconFromActivityInfo(trackers[position].activityInfo)
            }
            trackers[position].isService -> {
                holder.icon.loadIconFromServiceInfo(trackers[position].serviceInfo)
            }
            trackers[position].isReceiver -> {
                holder.icon.loadIconFromActivityInfo(trackers[position].receiverInfo)
            }
            trackers[position].isProvider -> {
                holder.icon.loadIconFromProviderInfo(trackers[position].providerInfo)
            }
        }

        holder.name.text = trackers[position].componentName.substringAfterLast(".")
        holder.packageID.text = trackers[position].componentName
        holder.trackerName.text = trackers[position].name
        holder.details.text = buildString {
            when {
                trackers[position].isActivity -> {
                    appendFlag(holder.itemView.context.getString(R.string.activity))
                }
                trackers[position].isService -> {
                    appendFlag(holder.itemView.context.getString(R.string.service))
                }
                trackers[position].isReceiver -> {
                    appendFlag(holder.itemView.context.getString(R.string.receiver))
                }
                trackers[position].isProvider -> {
                    appendFlag(holder.itemView.context.getString(R.string.provider))
                }
            }

            trackers[position].categories.forEach {
                appendFlag(it)
            }

            trackers[position].isETIP.let {
                if (it) {
                    appendFlag(holder.itemView.context.getString(R.string.suspected_tracker))
                }
            }
        }

        AdapterUtils.searchHighlighter(holder.packageID, trackers[position].codeSignature)
        holder.checkBox.isChecked = trackers[position].isBlocked.invert()

        if (trackers[position].isETIP) {
            holder.container.setWarningBackground(AdapterTrackers.ETIP_HIGHLIGHT)
        }

        if (isRooted) {
            holder.checkBox.visible(false)
        } else {
            holder.checkBox.gone(false)
        }

        if (isRooted) {
            holder.container.setOnClickListener {
                trackers[position].isBlocked = !trackers[position].isBlocked
                holder.checkBox.toggle()
            }
        } else {
            holder.container.setOnClickListener {
                batchTrackerCallbacks.onBatchLongPressed(trackers[position])
            }
        }

        holder.container.setOnLongClickListener {
            batchTrackerCallbacks.onBatchLongPressed(trackers[position])
            true
        }
    }

    override fun getItemCount(): Int {
        return trackers.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun selectAll() {
        trackers.forEach {
            it.isBlocked = false // contextually unblock all
        }

        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun unselectAll() {
        trackers.forEach {
            it.isBlocked = true // contextually block all
        }

        notifyDataSetChanged()
    }

    fun isAllSelected(): Boolean {
        trackers.forEach {
            if (it.isBlocked) {
                return false
            }
        }

        return true
    }

    fun getSelectedPackages(): java.util.ArrayList<Tracker> {
        val selectedPackages = java.util.ArrayList<Tracker>()

        trackers.forEach {
            if (!it.isBlocked) {
                selectedPackages.add(it)
            }
        }

        return selectedPackages
    }

    inner class Holder(itemView: View) : VerticalListViewHolder(itemView) {
        val container: CondensedDynamicRippleConstraintLayout = itemView.findViewById(R.id.container)
        val name: TypeFaceTextView = itemView.findViewById(R.id.name)
        val packageID: TypeFaceTextView = itemView.findViewById(R.id.package_id)
        val trackerName: TypeFaceTextView = itemView.findViewById(R.id.tracker_name)
        val details: TypeFaceTextView = itemView.findViewById(R.id.details)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkbox)
        val icon: AppIconImageView = itemView.findViewById(R.id.icon)
    }

    companion object {
        interface BatchTrackerCallbacks {
            fun onBatchLongPressed(tracker: Tracker)
        }
    }
}
