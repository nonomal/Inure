package app.simple.inure.adapters.installer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import app.simple.inure.R
import app.simple.inure.decorations.overscroll.VerticalListViewHolder
import app.simple.inure.decorations.typeface.TypeFaceTextView
import app.simple.inure.decorations.views.BulletTextView
import app.simple.inure.models.Triple

class AdapterInstallerChanges(private val list: ArrayList<Triple<String, String, String>>) : RecyclerView.Adapter<AdapterInstallerChanges.Holder>() {

    inner class Holder(itemView: View) : VerticalListViewHolder(itemView) {
        val title: TypeFaceTextView = itemView.findViewById(R.id.title)
        val added: BulletTextView = itemView.findViewById(R.id.added)
        val removed: BulletTextView = itemView.findViewById(R.id.removed)

        init {
            ContextCompat.getDrawable(itemView.context, R.drawable.ic_check_12dp)?.let { added.setBulletDrawable(it) }
            ContextCompat.getDrawable(itemView.context, R.drawable.ic_close_12dp)?.let { removed.setBulletDrawable(it) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.adapter_installer_changes, parent, false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.title.text = list[position].first()
        holder.added.text = list[position].second()
        holder.removed.text = list[position].third()
    }
}