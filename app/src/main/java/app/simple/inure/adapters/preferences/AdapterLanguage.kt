package app.simple.inure.adapters.preferences

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.simple.inure.R
import app.simple.inure.decorations.overscroll.VerticalListViewHolder
import app.simple.inure.decorations.ripple.DynamicRippleLinearLayout
import app.simple.inure.decorations.ripple.DynamicRippleTextView
import app.simple.inure.decorations.theme.ThemeIcon
import app.simple.inure.decorations.typeface.TypeFaceTextView
import app.simple.inure.models.Locales
import app.simple.inure.preferences.ConfigurationPreferences
import app.simple.inure.util.ConditionUtils.isZero
import app.simple.inure.util.LocaleUtils
import app.simple.inure.util.RecyclerViewUtils
import app.simple.inure.util.ViewUtils.invisible
import app.simple.inure.util.ViewUtils.visible

class AdapterLanguage : RecyclerView.Adapter<VerticalListViewHolder>() {

    private var languageCallback: LanguageCallback? = null

    private var list: MutableList<Locales> = LocaleUtils.localeList.also { it ->
        it.subList(1, it.size).sortBy {
            it.language
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VerticalListViewHolder {
        return when (viewType) {
            RecyclerViewUtils.TYPE_ITEM -> {
                Holder(LayoutInflater.from(parent.context).inflate(R.layout.adapter_locale, parent, false))
            }
            RecyclerViewUtils.TYPE_HEADER -> {
                Header(LayoutInflater.from(parent.context).inflate(R.layout.adapter_header_language, parent, false))
            }
            else -> {
                throw RuntimeException("there is no type that matches the type $viewType + make sure your using types correctly")
            }
        }
    }

    override fun onBindViewHolder(holder: VerticalListViewHolder, positionRaw: Int) {
        val position = positionRaw - 1

        if (holder is Holder) {
            holder.container.isClickable = list[position].localeCode != ConfigurationPreferences.getAppLanguage()
            holder.container.isEnabled = list[position].localeCode != ConfigurationPreferences.getAppLanguage()

            holder.language.text = if (position == 0) holder.itemView.context.getString(R.string.auto) else list[position].language
            holder.code.text = list[position].localeCode

            holder.container.setOnClickListener {
                ConfigurationPreferences.setAppLanguage(list[position].localeCode)
            }

            if (ConfigurationPreferences.getAppLanguage() == list[position].localeCode) {
                holder.icon.visible(false)
            } else {
                holder.icon.invisible(false)
            }
        } else if (holder is Header) {
            holder.total.text = holder.itemView.context.getString(R.string.total, list.size)
            holder.title.setText(R.string.language)

            holder.clickToParticipate.setOnClickListener {
                languageCallback?.onParticipateClicked()
            }

            holder.credits.setOnClickListener {
                languageCallback?.onCreditsClicked()
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size.plus(1)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position.isZero()) {
            RecyclerViewUtils.TYPE_HEADER
        } else {
            RecyclerViewUtils.TYPE_ITEM
        }
    }

    inner class Header(itemView: View) : VerticalListViewHolder(itemView) {
        val total: TypeFaceTextView = itemView.findViewById(R.id.adapter_type_face_total)
        val title: TypeFaceTextView = itemView.findViewById(R.id.adapter_header_title)
        val clickToParticipate: DynamicRippleTextView = itemView.findViewById(R.id.click_to_participate)
        val credits: DynamicRippleTextView = itemView.findViewById(R.id.credits)
    }

    inner class Holder(itemView: View) : VerticalListViewHolder(itemView) {
        val container: DynamicRippleLinearLayout = itemView.findViewById(R.id.adapter_locale_container)
        val language: TypeFaceTextView = itemView.findViewById(R.id.adapter_locale_name)
        val code: TypeFaceTextView = itemView.findViewById(R.id.adapter_locale_code)
        val icon: ThemeIcon = itemView.findViewById(R.id.adapter_locale_check_icon)
    }

    fun setLanguageCallback(callback: LanguageCallback) {
        languageCallback = callback
    }

    companion object {
        interface LanguageCallback {
            fun onParticipateClicked()
            fun onCreditsClicked()
        }
    }
}
