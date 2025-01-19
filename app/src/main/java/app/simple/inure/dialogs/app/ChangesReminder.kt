package app.simple.inure.dialogs.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import app.simple.inure.BuildConfig
import app.simple.inure.R
import app.simple.inure.decorations.ripple.DynamicRippleTextView
import app.simple.inure.extensions.fragments.ScopedBottomSheetFragment
import app.simple.inure.preferences.MainPreferences
import app.simple.inure.ui.panels.WebPage

class ChangesReminder : ScopedBottomSheetFragment() {

    private lateinit var sure: DynamicRippleTextView
    private lateinit var cancel: DynamicRippleTextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_reminder_changelogs, container, false)

        sure = view.findViewById(R.id.sure)
        cancel = view.findViewById(R.id.cancel)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MainPreferences.setChangeLogReminder(BuildConfig.VERSION_CODE)

        sure.setOnClickListener {
            openFragmentSlide(WebPage.newInstance(getString(R.string.change_logs)), WebPage.TAG)
            dismiss()
        }

        cancel.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        fun newInstance(): ChangesReminder {
            val args = Bundle()
            val fragment = ChangesReminder()
            fragment.arguments = args
            return fragment
        }

        fun FragmentManager.showChangesReminder(): ChangesReminder {
            val dialog = newInstance()
            dialog.show(this, TAG)
            return dialog
        }

        const val TAG = "changes_reminder"
    }
}
