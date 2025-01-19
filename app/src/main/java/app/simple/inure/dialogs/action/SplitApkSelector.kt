package app.simple.inure.dialogs.action

import android.content.pm.PackageInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import app.simple.inure.R
import app.simple.inure.adapters.dialogs.AdapterSplitApkSelector
import app.simple.inure.adapters.dialogs.AdapterSplitApkSelector.Companion.OnSplitApkSelectorListener
import app.simple.inure.apk.utils.PackageUtils.safeApplicationInfo
import app.simple.inure.constants.BundleConstants
import app.simple.inure.decorations.overscroll.CustomVerticalRecyclerView
import app.simple.inure.decorations.ripple.DynamicRippleImageButton
import app.simple.inure.decorations.ripple.DynamicRippleTextView
import app.simple.inure.dialogs.action.Extract.Companion.launchExtract
import app.simple.inure.extensions.fragments.ScopedBottomSheetFragment

class SplitApkSelector : ScopedBottomSheetFragment() {

    private lateinit var recyclerView: CustomVerticalRecyclerView
    private lateinit var selectAll: DynamicRippleImageButton
    private lateinit var cancel: DynamicRippleTextView
    private lateinit var extract: DynamicRippleTextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.dialog_selector_split_apk, container, false)

        recyclerView = view.findViewById(R.id.split_apk_selector_rv)
        selectAll = view.findViewById(R.id.select_all)
        cancel = view.findViewById(R.id.cancel)
        extract = view.findViewById(R.id.extract)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val paths = mutableListOf<Pair<String, Boolean>>()
        val selectedPaths = mutableSetOf<String>()

        paths.add(Pair(packageInfo.safeApplicationInfo.publicSourceDir, true)) // base apk
        paths.addAll(packageInfo.safeApplicationInfo.splitSourceDirs!!.map { Pair(it, true) }) // split apks

        selectedPaths.addAll(paths.map { it.first })

        val adapterSplitApkSelector = AdapterSplitApkSelector(paths)

        adapterSplitApkSelector.setOnSplitApkSelectorListener(object : OnSplitApkSelectorListener {
            override fun onSplitApkSelected(path: String, isChecked: Boolean) {
                if (isChecked) {
                    selectedPaths.add(path)
                } else {
                    selectedPaths.remove(path)
                }
            }
        })

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapterSplitApkSelector

        selectAll.setOnClickListener {
            selectedPaths.clear()
            selectedPaths.addAll(paths.map { it.first })
            adapterSplitApkSelector.selectAll()
        }

        cancel.setOnClickListener {
            dismiss()
        }

        extract.setOnClickListener {
            parentFragmentManager.launchExtract(packageInfo, selectedPaths)
            dismiss()
        }
    }

    companion object {
        fun newInstance(packageInfo: PackageInfo): SplitApkSelector {
            val args = Bundle()
            args.putParcelable(BundleConstants.packageInfo, packageInfo)
            val fragment = SplitApkSelector()
            fragment.arguments = args
            return fragment
        }

        fun FragmentManager.showSplitApkSelector(packageInfo: PackageInfo): SplitApkSelector {
            val fragment = newInstance(packageInfo)
            fragment.show(this, "split_apk_selector")
            return fragment
        }
    }
}
