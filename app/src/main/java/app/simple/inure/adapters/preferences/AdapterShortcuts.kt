package app.simple.inure.adapters.preferences

import android.content.ComponentName
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.recyclerview.widget.RecyclerView
import app.simple.inure.R
import app.simple.inure.activities.app.MainActivity
import app.simple.inure.decorations.overscroll.VerticalListViewHolder
import app.simple.inure.decorations.ripple.DynamicRippleLinearLayout
import app.simple.inure.decorations.theme.ThemeIcon
import app.simple.inure.decorations.toggles.CheckBox
import app.simple.inure.decorations.typeface.TypeFaceTextView
import app.simple.inure.models.Shortcut
import app.simple.inure.preferences.AppearancePreferences
import app.simple.inure.util.ColorUtils.toHexColor
import app.simple.inure.util.ConditionUtils.isZero
import app.simple.inure.util.HtmlHelper
import app.simple.inure.util.RecyclerViewUtils

class AdapterShortcuts(private val list: List<Shortcut>,
                       private val shortcuts: MutableList<ShortcutInfoCompat>,
                       private val onShortcutLongPressed: (ShortcutInfoCompat, View) -> Unit) :
        RecyclerView.Adapter<VerticalListViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VerticalListViewHolder {
        return when (viewType) {
            RecyclerViewUtils.TYPE_ITEM -> {
                Holder(LayoutInflater.from(parent.context).inflate(R.layout.adapter_shortcuts, parent, false))
            }
            RecyclerViewUtils.TYPE_HEADER -> {
                Header(LayoutInflater.from(parent.context).inflate(R.layout.adapter_header_typeface, parent, false))
            }
            else -> {
                throw RuntimeException("there is no type that matches the type $viewType + make sure your using types correctly")
            }
        }
    }

    override fun onBindViewHolder(holder: VerticalListViewHolder, position_: Int) {
        val position = position_.minus(1)

        if (holder is Holder) {
            holder.icon.setImageResource(list[position].icon)
            holder.name.setText(list[position].name)

            holder.id.text = HtmlHelper.fromHtml(buildString {
                append("id: ")
                append("<font color=\"${AppearancePreferences.getAccentColor().toHexColor()}\">")
                append(list[position].id)
                append("</font>")
            })

            holder.action.text = HtmlHelper.fromHtml(buildString {
                append("action: ")
                append("<font color=\"${AppearancePreferences.getAccentColor().toHexColor()}\">")
                append(list[position].action)
                append("</font>")
            })

            for (shortcut in shortcuts) {
                if (list[position].id == shortcut.id) {
                    holder.checkBox.isChecked = shortcut.isEnabled
                    break
                } else {
                    holder.checkBox.isChecked = false
                }
            }

            holder.checkBox.setOnCheckedChangeListener {
                val intent = Intent(holder.context, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                intent.action = list[position].action

                val shortcut = ShortcutInfoCompat.Builder(holder.context, list[position].id)
                    .setShortLabel(holder.getString(list[position].name))
                    .setActivity(ComponentName(holder.context, MainActivity::class.java))
                    .setIcon(IconCompat.createWithResource(holder.context, list[position].icon))
                    .setIntent(intent)
                    .build()

                if (it) {
                    ShortcutManagerCompat.pushDynamicShortcut(holder.context, shortcut)
                } else {
                    ShortcutManagerCompat.removeDynamicShortcuts(holder.context, arrayListOf(shortcut.id))
                }
            }

            holder.container.setOnClickListener {
                holder.checkBox.callOnClick()
            }

            holder.container.setOnLongClickListener {
                val intent = Intent(holder.context, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                intent.action = list[position].action

                val shortcut = ShortcutInfoCompat.Builder(holder.context, list[position].id)
                    .setShortLabel(holder.getString(list[position].name))
                    .setActivity(ComponentName(holder.context, MainActivity::class.java))
                    .setIcon(IconCompat.createWithResource(holder.context, list[position].icon))
                    .setIntent(intent)
                    .build()

                onShortcutLongPressed(shortcut, it)
                true
            }

        } else if (holder is Header) {
            holder.total.text = holder.itemView.context.getString(R.string.total, list.size)
            holder.title.setText(R.string.shortcuts)
        }
    }

    override fun getItemCount(): Int {
        return list.size.plus(1)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position.isZero()) {
            RecyclerViewUtils.TYPE_HEADER
        } else RecyclerViewUtils.TYPE_ITEM
    }

    inner class Holder(itemView: View) : VerticalListViewHolder(itemView) {
        val icon: ThemeIcon = itemView.findViewById(R.id.shortcut_icon)
        val name: TypeFaceTextView = itemView.findViewById(R.id.shortcut_name)
        val id: TypeFaceTextView = itemView.findViewById(R.id.shortcut_id)
        val action: TypeFaceTextView = itemView.findViewById(R.id.shortcut_action)
        val checkBox: CheckBox = itemView.findViewById(R.id.shortcut_checkbox)
        val container: DynamicRippleLinearLayout = itemView.findViewById(R.id.shortcut_container)
    }

    inner class Header(itemView: View) : VerticalListViewHolder(itemView) {
        val title: TypeFaceTextView = itemView.findViewById(R.id.adapter_header_title)
        val total: TypeFaceTextView = itemView.findViewById(R.id.adapter_type_face_total)
    }
}
