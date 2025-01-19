package app.simple.inure.dialogs.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import app.simple.inure.R
import app.simple.inure.decorations.ripple.DynamicRippleTextView
import app.simple.inure.decorations.toggles.Switch
import app.simple.inure.extensions.fragments.ScopedBottomSheetFragment
import app.simple.inure.preferences.NotesPreferences

class NotesEditorMenu : ScopedBottomSheetFragment() {

    private lateinit var autoSave: Switch
    private lateinit var openSettings: DynamicRippleTextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_menu_notes_editor, container, false)

        autoSave = view.findViewById(R.id.auto_save)
        openSettings = view.findViewById(R.id.dialog_open_apps_settings)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        autoSave.isChecked = NotesPreferences.isAutoSave()

        autoSave.setOnSwitchCheckedChangeListener {
            NotesPreferences.setAutoSave(it)
        }

        openSettings.setOnClickListener {
            openSettings()
        }
    }

    companion object {
        fun newInstance(): NotesEditorMenu {
            val args = Bundle()
            val fragment = NotesEditorMenu()
            fragment.arguments = args
            return fragment
        }
    }
}