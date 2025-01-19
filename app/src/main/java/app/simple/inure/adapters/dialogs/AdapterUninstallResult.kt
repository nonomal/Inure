package app.simple.inure.adapters.dialogs

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import app.simple.inure.R
import app.simple.inure.decorations.overscroll.VerticalListViewHolder
import app.simple.inure.decorations.typeface.TypeFaceTextView
import app.simple.inure.models.PackageStateResult
import app.simple.inure.util.TextViewUtils.setDrawableTint

class AdapterUninstallResult(private val data: ArrayList<PackageStateResult>) : RecyclerView.Adapter<AdapterUninstallResult.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.adapter_uninstall_result, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.name.text = data[position].name
        holder.packageName.text = data[position].packageName

        if (data[position].isSuccess) {
            holder.name
                .setCompoundDrawablesWithIntrinsicBounds(
                        null, null, ContextCompat.getDrawable(holder.context, R.drawable.ic_check_12dp), null)
            holder.name.setDrawableTint(Color.GREEN)
        } else {
            holder.name
                .setCompoundDrawablesWithIntrinsicBounds(
                        null, null, ContextCompat.getDrawable(holder.context, R.drawable.ic_close_12dp), null)
            holder.name.setDrawableTint(Color.RED)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class Holder(itemView: View) : VerticalListViewHolder(itemView) {
        val name: TypeFaceTextView = itemView.findViewById(R.id.app_name)
        val packageName: TypeFaceTextView = itemView.findViewById(R.id.package_name)
    }
}