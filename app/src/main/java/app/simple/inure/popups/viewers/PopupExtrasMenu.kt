package app.simple.inure.popups.viewers

import android.view.LayoutInflater
import android.view.View
import app.simple.inure.R
import app.simple.inure.constants.Misc
import app.simple.inure.decorations.toggles.CheckBox
import app.simple.inure.extensions.popup.BasePopupWindow
import app.simple.inure.extensions.popup.PopupLinearLayout
import app.simple.inure.extensions.popup.PopupMenuCallback
import app.simple.inure.preferences.ExtrasPreferences

class PopupExtrasMenu(view: View) : BasePopupWindow() {

    private var popupMenuCallback: PopupMenuCallback? = null
    private var highlightCheckBox: CheckBox

    init {
        val contentView = LayoutInflater.from(view.context).inflate(R.layout.popup_extras_options, PopupLinearLayout(view.context))
        init(contentView, view, Misc.xOffset, Misc.yOffset)

        highlightCheckBox = contentView.findViewById(R.id.extras_highlight_checkbox)

        highlightCheckBox.isChecked = ExtrasPreferences.isExtensionsHighlighted()

        highlightCheckBox.setOnCheckedChangeListener { isChecked ->
            ExtrasPreferences.setHighlightExtensions(isChecked)
        }
    }

    override fun dismiss() {
        super.dismiss()
        popupMenuCallback?.onDismiss()
    }

    fun setOnMenuItemClickListener(popupMenuCallback: PopupMenuCallback) {
        this.popupMenuCallback = popupMenuCallback
    }
}
