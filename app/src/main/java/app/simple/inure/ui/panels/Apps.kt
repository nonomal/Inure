package app.simple.inure.ui.panels

import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.ViewModelProvider
import app.simple.inure.R
import app.simple.inure.adapters.ui.AdapterApps
import app.simple.inure.constants.BottomMenuConstants
import app.simple.inure.constants.BundleConstants
import app.simple.inure.decorations.overscroll.CustomVerticalRecyclerView
import app.simple.inure.dialogs.app.AppMenu.Companion.showAppMenu
import app.simple.inure.dialogs.apps.AppsMenu.Companion.newAppsMenuInstance
import app.simple.inure.dialogs.apps.AppsSort.Companion.showAppsSortDialog
import app.simple.inure.dialogs.miscellaneous.GenerateAppData.Companion.showGeneratedDataTypeSelector
import app.simple.inure.extensions.fragments.ScopedFragment
import app.simple.inure.interfaces.adapters.AdapterCallbacks
import app.simple.inure.preferences.AppsPreferences
import app.simple.inure.ui.viewers.HTML
import app.simple.inure.ui.viewers.JSON
import app.simple.inure.ui.viewers.Markdown
import app.simple.inure.ui.viewers.XML
import app.simple.inure.util.NullSafety.isNotNull
import app.simple.inure.util.StringUtils.endsWithAny
import app.simple.inure.viewmodels.panels.AppsViewModel

class Apps : ScopedFragment() {

    private lateinit var appsListRecyclerView: CustomVerticalRecyclerView
    private lateinit var adapter: AdapterApps
    private lateinit var appsViewModel: AppsViewModel

    private val generatedDataTextExtension = arrayOf(".xml", ".txt", ".csv")
    private val markdownExtensions = arrayOf(".md", ".markdown")
    private val jsonExtension = ".json"
    private val htmlExtension = ".html"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_all_apps, container, false)

        appsListRecyclerView = view.findViewById(R.id.all_apps_recycler_view)
        appsViewModel = ViewModelProvider(requireActivity())[AppsViewModel::class.java]

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()

        if (appsViewModel.shouldShowLoader()) {
            showLoader(manualOverride = true)
        }

        appsViewModel.getAppData().observe(viewLifecycleOwner) { it ->
            postponeEnterTransition()
            hideLoader()

            adapter = AdapterApps(it)
            appsListRecyclerView.adapter = adapter

            (view.parent as? ViewGroup)?.doOnPreDraw {
                startPostponedEnterTransition()
            }

            adapter.setOnItemClickListener(object : AdapterCallbacks {
                override fun onAppClicked(packageInfo: PackageInfo, icon: ImageView) {
                    openFragmentArc(AppInfo.newInstance(packageInfo), icon, AppInfo.TAG)
                }

                override fun onAppLongPressed(packageInfo: PackageInfo, icon: ImageView) {
                    childFragmentManager.showAppMenu(packageInfo)
                }
            })

            bottomRightCornerMenu?.initBottomMenuWithRecyclerView(BottomMenuConstants.getAllAppsBottomMenuItems(), appsListRecyclerView) { id, _ ->
                when (id) {
                    R.drawable.ic_filter -> {
                        childFragmentManager.showAppsSortDialog()
                    }

                    R.drawable.ic_settings -> {
                        childFragmentManager.newAppsMenuInstance().setOnGenerateListClicked {
                            if ((appsViewModel.getAppData().value ?: it).isNotEmpty()) {
                                childFragmentManager.showGeneratedDataTypeSelector().onGenerateData {
                                    showLoader(manualOverride = true)
                                    kotlin.runCatching {
                                        appsViewModel.generateAppsData(appsViewModel.getAppData().value ?: it)

                                    }.onFailure {
                                        showWarning(it.message ?: "Failed to generate data", goBack = false)
                                    }
                                }
                            } else {
                                showWarning("ERR: empty list", goBack = false)
                            }
                        }
                    }

                    R.drawable.ic_search -> {
                        openFragmentSlide(Search.newInstance(true), Search.TAG)
                    }

                    R.drawable.ic_refresh -> {
                        showLoader(manualOverride = true)
                        appsViewModel.refreshPackageData()
                    }
                }
            }
        }

        appsViewModel.getGeneratedDataPath().observe(viewLifecycleOwner) { path ->
            if (path.isNotNull()) {
                hideLoader()

                /**
                 * We are using empty [PackageInfo] object because we don't want to show
                 * any app info in the toolbar, it's a file viewer for data generated by
                 * the app itself.
                 *
                 * isRaw = true because we are using the viewer for special purpose
                 * of viewing the generated data
                 */
                when {
                    path.endsWithAny(*generatedDataTextExtension) -> {
                        openFragmentSlide(XML.newInstance(PackageInfo(), false /* Not needed? */, path, true), XML.TAG)
                    }

                    path.endsWith(htmlExtension) -> {
                        openFragmentSlide(HTML.newInstance(PackageInfo(), path, true), HTML.TAG)
                    }

                    path.endsWith((jsonExtension)) -> {
                        openFragmentSlide(JSON.newInstance(PackageInfo(), path, true), JSON.TAG)
                    }

                    path.endsWithAny(*markdownExtensions) -> {
                        openFragmentSlide(Markdown.newInstance(PackageInfo(), path, true), Markdown.TAG)
                    }
                }

                appsViewModel.clearGeneratedAppsDataLiveData()
            } else {
                hideLoader()
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            AppsPreferences.SORT_STYLE,
            AppsPreferences.IS_SORTING_REVERSED,
            AppsPreferences.APPS_CATEGORY,
            AppsPreferences.APPS_FILTER,
            AppsPreferences.COMBINE_FILTER,
            AppsPreferences.FILTER_STYLE,
            AppsPreferences.APPS_TYPE -> {
                appsViewModel.loadAppData()
            }
        }
    }

    companion object {
        fun newInstance(loading: Boolean = false): Apps {
            val args = Bundle()
            val fragment = Apps()
            args.putBoolean(BundleConstants.loading, loading)
            fragment.arguments = args
            return fragment
        }

        const val TAG = "Apps"
    }
}
