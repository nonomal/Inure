package app.simple.inure.dialogs.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import app.simple.inure.R
import app.simple.inure.apk.parsers.FOSSParser
import app.simple.inure.apk.utils.PackageUtils.launchThisPackage
import app.simple.inure.apk.utils.PackageUtils.safeApplicationInfo
import app.simple.inure.constants.BundleConstants
import app.simple.inure.database.instances.TagsDatabase
import app.simple.inure.decorations.ripple.DynamicRippleTextView
import app.simple.inure.decorations.typeface.TypeFaceTextView
import app.simple.inure.decorations.views.AppIconImageView
import app.simple.inure.dialogs.action.Send.Companion.showSend
import app.simple.inure.dialogs.foss.MarkFoss.Companion.showMarkFossDialog
import app.simple.inure.extensions.fragments.ScopedDialogFragment
import app.simple.inure.factories.panels.PackageInfoFactory
import app.simple.inure.glide.util.ImageLoader.loadAppIcon
import app.simple.inure.preferences.BehaviourPreferences
import app.simple.inure.preferences.DevelopmentPreferences
import app.simple.inure.preferences.SearchPreferences
import app.simple.inure.ui.editor.NotesEditor
import app.simple.inure.ui.panels.Components
import app.simple.inure.ui.viewers.Activities
import app.simple.inure.ui.viewers.Information
import app.simple.inure.ui.viewers.Permissions
import app.simple.inure.ui.viewers.Providers
import app.simple.inure.ui.viewers.Receivers
import app.simple.inure.ui.viewers.Resources
import app.simple.inure.ui.viewers.Services
import app.simple.inure.ui.viewers.Trackers
import app.simple.inure.ui.viewers.UsageStatisticsGraph
import app.simple.inure.ui.viewers.XML
import app.simple.inure.ui.viewers.XMLWebView
import app.simple.inure.util.AdapterUtils.setAppVisualStates
import app.simple.inure.util.ConditionUtils.invert
import app.simple.inure.util.ConditionUtils.isNotZero
import app.simple.inure.util.FileUtils.toFileOrNull
import app.simple.inure.util.InfoStripUtils.getAppInfo
import app.simple.inure.util.StatusBarHeight
import app.simple.inure.util.ViewUtils
import app.simple.inure.util.ViewUtils.gone
import app.simple.inure.util.ViewUtils.visibility
import app.simple.inure.viewmodels.panels.AppInfoViewModel
import app.simple.inure.viewmodels.panels.HomeViewModel
import app.simple.inure.viewmodels.panels.QuickAppsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppMenu : ScopedDialogFragment() {

    private lateinit var icon: AppIconImageView
    private lateinit var name: TypeFaceTextView
    private lateinit var packageName: TypeFaceTextView
    private lateinit var details: TypeFaceTextView

    private lateinit var copyPackageName: DynamicRippleTextView
    private lateinit var launch: DynamicRippleTextView
    private lateinit var appInformation: DynamicRippleTextView
    private lateinit var send: DynamicRippleTextView
    private lateinit var usageStatistics: DynamicRippleTextView
    private lateinit var components: DynamicRippleTextView
    private lateinit var deepSearchKeyword: TypeFaceTextView
    private lateinit var permissions: DynamicRippleTextView
    private lateinit var activities: DynamicRippleTextView
    private lateinit var services: DynamicRippleTextView
    private lateinit var receivers: DynamicRippleTextView
    private lateinit var providers: DynamicRippleTextView
    private lateinit var trackers: DynamicRippleTextView
    private lateinit var resources: DynamicRippleTextView
    private lateinit var manifest: DynamicRippleTextView
    private lateinit var notes: DynamicRippleTextView
    private lateinit var toQuickApp: DynamicRippleTextView
    private lateinit var markAsFOSS: DynamicRippleTextView

    private lateinit var quickAppsViewModel: QuickAppsViewModel
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var appInfoViewModel: AppInfoViewModel
    private var isAlreadyInQuickApp = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_apps_menu, container, false)

        icon = view.findViewById(R.id.fragment_app_info_icon)
        name = view.findViewById(R.id.fragment_app_name)
        packageName = view.findViewById(R.id.fragment_app_package_id)
        details = view.findViewById(R.id.fragment_app_details)

        copyPackageName = view.findViewById(R.id.copy_package_name)
        launch = view.findViewById(R.id.launch)
        appInformation = view.findViewById(R.id.app_information)
        send = view.findViewById(R.id.send)
        usageStatistics = view.findViewById(R.id.usage_stats)
        components = view.findViewById(R.id.components)
        deepSearchKeyword = view.findViewById(R.id.deep_search_keyword)
        permissions = view.findViewById(R.id.permissions)
        activities = view.findViewById(R.id.activities)
        services = view.findViewById(R.id.services)
        receivers = view.findViewById(R.id.receivers)
        providers = view.findViewById(R.id.providers)
        trackers = view.findViewById(R.id.trackers)
        resources = view.findViewById(R.id.resources)
        manifest = view.findViewById(R.id.manifest)
        notes = view.findViewById(R.id.notes)
        toQuickApp = view.findViewById(R.id.to_quick_app)
        markAsFOSS = view.findViewById(R.id.to_foss)

        val packageInfoFactory = PackageInfoFactory(packageInfo)
        quickAppsViewModel = ViewModelProvider(requireActivity())[QuickAppsViewModel::class.java]
        homeViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        appInfoViewModel = ViewModelProvider(this, packageInfoFactory)[AppInfoViewModel::class.java]

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val window = dialog!!.window ?: return
        val displayMetrics = DisplayMetrics()

        @Suppress("deprecation")
        window.windowManager.defaultDisplay.getMetrics(displayMetrics)

        if (BehaviourPreferences.isDimmingOn()) {
            dialog?.window?.setDimAmount(ViewUtils.getDimValue(requireContext()))
        } else {
            dialog?.window?.setDimAmount(0f)
        }

        window.attributes.gravity = Gravity.CENTER

        if (StatusBarHeight.isLandscape(requireContext())) {
            window.attributes.width = (displayMetrics.widthPixels * 1f / 100f * 60f).toInt()
            window.attributes.height = (displayMetrics.heightPixels * 1F / 100F * 90F).toInt()
        } else {
            window.attributes.width = (displayMetrics.widthPixels * 1f / 100f * 85f).toInt()
            window.attributes.height = (displayMetrics.heightPixels * 1F / 100F * 60F).toInt()
        }

        with(requireArguments().getString(BundleConstants.keywords).isNullOrEmpty().invert()) {
            SearchPreferences.setSearchKeywordMode(requireArguments().getString(BundleConstants.keywords).isNullOrEmpty().invert())
            if (this) {
                deepSearchKeyword.text = requireArguments().getString(BundleConstants.keywords)
            } else {
                deepSearchKeyword.gone()
            }
        }

        SearchPreferences.setSearchKeywordMode(requireArguments().getString(BundleConstants.keywords).isNullOrEmpty().invert())
        icon.loadAppIcon(packageInfo.packageName, packageInfo.safeApplicationInfo.enabled, packageInfo.safeApplicationInfo.sourceDir.toFileOrNull())
        name.text = packageInfo.safeApplicationInfo.name
        name.setAppVisualStates(packageInfo)
        markAsFOSS.visibility(FOSSParser.isEmbeddedFOSS(packageInfo).invert())

        markAsFOSS.text = buildString {
            if (FOSSParser.isPackageFOSS(packageInfo)) {
                append(getString(R.string.mark_as_non_foss))
            } else {
                append(getString(R.string.mark_as_foss))
            }
        }

        packageName.text = packageInfo.packageName

        copyPackageName.setOnClickListener {
            val clipBoard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("Package Name", packageInfo.packageName)
            clipBoard.setPrimaryClip(clipData)
            Toast.makeText(requireContext(), R.string.copied, Toast.LENGTH_SHORT).show()
            dismiss()
        }

        launch.setOnClickListener {
            kotlin.runCatching {
                packageInfo.launchThisPackage(requireContext())
            }.onFailure {
                it.printStackTrace()
                showError(it.stackTraceToString())
            }
        }

        appInformation.setOnClickListener {
            openFragmentSlide(Information.newInstance(packageInfo), Information.TAG)
        }

        send.setOnClickListener {
            parentFragmentManager.showSend(packageInfo).also {
                dismiss()
            }
        }

        usageStatistics.setOnClickListener {
            openFragmentSlide(UsageStatisticsGraph.newInstance(packageInfo), UsageStatisticsGraph.TAG)
        }

        components.setOnClickListener {
            openFragmentSlide(Components.newInstance(packageInfo), Components.TAG)
        }

        permissions.setOnClickListener {
            openFragmentSlide(Permissions.newInstance(
                    packageInfo, requireArguments().getString(BundleConstants.keywords)), Permissions.TAG)
        }

        activities.setOnClickListener {
            openFragmentSlide(Activities.newInstance(
                    packageInfo, requireArguments().getString(BundleConstants.keywords)), Activities.TAG)
        }

        services.setOnClickListener {
            openFragmentSlide(Services.newInstance(
                    packageInfo, requireArguments().getString(BundleConstants.keywords)), Activities.TAG)
        }

        receivers.setOnClickListener {
            openFragmentSlide(Receivers.newInstance(
                    packageInfo, requireArguments().getString(BundleConstants.keywords)), Receivers.TAG)
        }

        providers.setOnClickListener {
            openFragmentSlide(Providers.newInstance(
                    packageInfo, requireArguments().getString(BundleConstants.keywords)), Providers.TAG)
        }

        trackers.setOnClickListener {
            openFragmentSlide(Trackers.newInstance(packageInfo), Trackers.TAG)
        }

        resources.setOnClickListener {
            openFragmentSlide(Resources.newInstance(
                    packageInfo, requireArguments().getString(BundleConstants.keywords)), Resources.TAG)
        }

        manifest.setOnClickListener {
            if (DevelopmentPreferences.get(DevelopmentPreferences.IS_WEBVIEW_XML_VIEWER)) {
                openFragmentSlide(XMLWebView.newInstance(
                        packageInfo, "AndroidManifest.xml"), XMLWebView.TAG)
            } else {
                openFragmentSlide(XML.newInstance(
                        packageInfo, true, "AndroidManifest.xml"), XML.TAG)
            }
        }

        notes.setOnClickListener {
            openFragmentSlide(NotesEditor.newInstance(packageInfo), NotesEditor.TAG)
        }

        quickAppsViewModel.getSimpleQuickAppList().observe(viewLifecycleOwner) {
            if (it.size.isNotZero()) {
                for (i in it) {
                    if (i.packageName == packageInfo.packageName) {
                        toQuickApp.setText(R.string.remove_from_home_screen)
                        isAlreadyInQuickApp = true
                        break
                    } else {
                        isAlreadyInQuickApp = false
                    }
                }
            } else {
                isAlreadyInQuickApp = false
            }

            if (!isAlreadyInQuickApp) {
                toQuickApp.setText(R.string.pin_to_home_panel)
                isAlreadyInQuickApp = false
            }
        }

        toQuickApp.setOnClickListener {
            if (isAlreadyInQuickApp) {
                quickAppsViewModel.removeQuickApp(packageInfo.packageName)
            } else {
                quickAppsViewModel.addQuickApp(packageInfo.packageName)
            }
        }

        markAsFOSS.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                if (FOSSParser.isPackageFOSS(packageInfo)) {
                    FOSSParser.removePackage(packageInfo.packageName, requireContext())
                    withContext(Dispatchers.Main) {
                        setOpenSourceState(true)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        if (FOSSParser.isEmbeddedFOSS(packageInfo)) {
                            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                                FOSSParser.addPackage(packageInfo.packageName, FOSSParser.getPackageLicense(packageInfo), requireContext())
                                withContext(Dispatchers.Main) {
                                    setOpenSourceState(false)
                                }
                            }
                        } else {
                            childFragmentManager.showMarkFossDialog().onMarkFossSaved = { license ->
                                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                                    FOSSParser.addPackage(packageInfo.packageName, license, requireContext())
                                    TagsDatabase.getInstance(requireContext())?.let {
                                        it.getTagDao()?.getTag(getString(R.string.foss))?.let { tag ->
                                            tag.packages = tag.packages.plus("," + packageInfo.packageName)
                                            it.getTagDao()?.updateTag(tag)
                                        }
                                    }

                                    withContext(Dispatchers.Main) {
                                        setOpenSourceState(false)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        appInfoViewModel.getTrackers().observe(viewLifecycleOwner) {
            val details = requireContext().getAppInfo(packageInfo)

            if (details.isEmpty()) {
                details.append(getString(R.string.trackers_count, it))
            } else {
                details.append(" | ")
                details.append(getString(R.string.trackers_count, it))
            }

            this.details.alpha = 0F
            this.details.text = details
            this.details.animate().alpha(1F).start()
        }
    }

    private fun setOpenSourceState(isFOSS: Boolean) {
        markAsFOSS.text = buildString {
            if (isFOSS) {
                append(getString(R.string.mark_as_non_foss))
            } else {
                append(getString(R.string.mark_as_foss))
            }
        }

        name.setAppVisualStates(packageInfo)
        homeViewModel.refreshFOSSApps()
    }

    override fun onDestroy() {
        super.onDestroy()
        onDismissListener?.invoke()
        SearchPreferences.setSearchKeywordMode(false)
    }

    companion object {
        fun newInstance(packageInfo: PackageInfo, keywords: String? = null): AppMenu {
            val args = Bundle()
            args.putParcelable(BundleConstants.packageInfo, packageInfo)
            args.putString(BundleConstants.keywords, keywords)
            val fragment = AppMenu()
            fragment.arguments = args
            return fragment
        }

        fun FragmentManager.showAppMenu(packageInfo: PackageInfo, keywords: String? = null): AppMenu {
            val dialog = newInstance(packageInfo, keywords)
            dialog.show(this, TAG)
            return dialog
        }

        const val TAG = "apps_menu"
    }
}
