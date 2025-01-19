package app.simple.inure.ui.viewers

import android.content.pm.PackageInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import app.simple.inure.R
import app.simple.inure.adapters.viewers.AdapterFeatures
import app.simple.inure.constants.BundleConstants
import app.simple.inure.decorations.overscroll.CustomVerticalRecyclerView
import app.simple.inure.extensions.fragments.SearchBarScopedFragment
import app.simple.inure.factories.panels.PackageInfoFactory
import app.simple.inure.viewmodels.viewers.ApkDataViewModel

class Features : SearchBarScopedFragment() {

    private lateinit var recyclerView: CustomVerticalRecyclerView
    private lateinit var componentsViewModel: ApkDataViewModel
    private lateinit var packageInfoFactory: PackageInfoFactory

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_features, container, false)

        recyclerView = view.findViewById(R.id.features_recycler_view)
        packageInfoFactory = PackageInfoFactory(packageInfo)
        componentsViewModel = ViewModelProvider(this, packageInfoFactory)[ApkDataViewModel::class.java]

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        startPostponedEnterTransition()

        componentsViewModel.getFeatures().observe(viewLifecycleOwner) {
            recyclerView.adapter = AdapterFeatures(it)
            setCount(it.count())
        }

        componentsViewModel.getError().observe(viewLifecycleOwner) {
            showError(it)
        }

        componentsViewModel.notFound.observe(viewLifecycleOwner) {
            showWarning(R.string.no_features_found)
        }
    }

    companion object {
        fun newInstance(packageInfo: PackageInfo): Features {
            val args = Bundle()
            args.putParcelable(BundleConstants.packageInfo, packageInfo)
            val fragment = Features()
            fragment.arguments = args
            return fragment
        }

        const val TAG = "Features"
    }
}
