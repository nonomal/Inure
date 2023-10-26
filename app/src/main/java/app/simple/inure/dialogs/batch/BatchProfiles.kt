package app.simple.inure.dialogs.batch

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import app.simple.inure.R
import app.simple.inure.adapters.dialogs.AdapterBatchProfiles
import app.simple.inure.decorations.overscroll.CustomVerticalRecyclerView
import app.simple.inure.extensions.fragments.ScopedBottomSheetFragment
import app.simple.inure.preferences.BatchPreferences
import app.simple.inure.viewmodels.dialogs.BatchProfilesViewModel

class BatchProfiles : ScopedBottomSheetFragment() {

    private lateinit var recyclerView: CustomVerticalRecyclerView
    private lateinit var batchProfilesViewModel: BatchProfilesViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_batch_profiles, container, false)

        recyclerView = view.findViewById(R.id.recycler_view)

        batchProfilesViewModel = ViewModelProvider(this)[BatchProfilesViewModel::class.java]

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        batchProfilesViewModel.getProfiles().observe(viewLifecycleOwner) {
            recyclerView.adapter = AdapterBatchProfiles(it)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        super.onSharedPreferenceChanged(sharedPreferences, key)
        when (key) {
            BatchPreferences.lastSelectedProfile -> {
                dismiss()
            }
        }
    }

    companion object {
        fun newInstance(): BatchProfiles {
            val args = Bundle()
            val fragment = BatchProfiles()
            fragment.arguments = args
            return fragment
        }

        fun FragmentManager.showBatchProfiles(): BatchProfiles {
            val dialog = newInstance()
            dialog.show(this, "batch_profiles")
            return dialog
        }
    }
}