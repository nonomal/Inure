package app.simple.inure.ui.viewers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import app.simple.inure.R
import app.simple.inure.constants.BundleConstants
import app.simple.inure.decorations.fastscroll.FastScrollerBuilder
import app.simple.inure.decorations.padding.PaddingAwareNestedScrollView
import app.simple.inure.decorations.ripple.DynamicRippleImageButton
import app.simple.inure.decorations.typeface.TypeFaceEditText
import app.simple.inure.decorations.typeface.TypeFaceTextView
import app.simple.inure.decorations.views.CustomProgressBar
import app.simple.inure.dialogs.app.Sure.Companion.newSureInstance
import app.simple.inure.dialogs.menus.CodeViewerMenu
import app.simple.inure.dialogs.miscellaneous.LargeString.Companion.showLargeStringDialog
import app.simple.inure.extensions.fragments.FinderScopedFragment
import app.simple.inure.factories.panels.SharedPrefsViewerViewModelFactory
import app.simple.inure.interfaces.fragments.SureCallbacks
import app.simple.inure.popups.viewers.PopupSharedPrefsViewer
import app.simple.inure.preferences.FormattingPreferences
import app.simple.inure.util.ViewUtils.gone
import app.simple.inure.util.ViewUtils.visible
import app.simple.inure.viewmodels.viewers.SharedPreferencesViewerViewModel
import java.io.IOException

class SharedPrefsCode : FinderScopedFragment() {

    private lateinit var text: TypeFaceEditText
    private lateinit var icon: ImageView
    private lateinit var name: TypeFaceTextView
    private lateinit var progress: CustomProgressBar
    private lateinit var options: DynamicRippleImageButton
    private lateinit var settings: DynamicRippleImageButton
    private lateinit var scrollView: PaddingAwareNestedScrollView
    private lateinit var search: DynamicRippleImageButton

    private lateinit var sharedPreferencesViewerViewModel: SharedPreferencesViewerViewModel
    private lateinit var sharedPrefsViewerViewModelFactory: SharedPrefsViewerViewModelFactory

    private val requestCode = 555

    private val exportManifest = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
        if (uri == null) {
            // Back button pressed.
            return@registerForActivityResult
        }
        try {
            requireContext().contentResolver.openOutputStream(uri).use { outputStream ->
                if (outputStream == null) throw IOException()
                outputStream.write(text.text.toString().toByteArray())
                outputStream.flush()
                Toast.makeText(requireContext(), R.string.saved_successfully, Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_xml_viewer, container, false)

        text = view.findViewById(R.id.text_viewer)
        name = view.findViewById(R.id.xml_name)
        icon = view.findViewById(R.id.xml_viewer_header_icon)
        progress = view.findViewById(R.id.xml_loader)
        options = view.findViewById(R.id.xml_viewer_options)
        settings = view.findViewById(R.id.xml_viewer_settings)
        scrollView = view.findViewById(R.id.xml_nested_scroll_view)
        search = view.findViewById(R.id.search)

        name.text = requireArguments().getString(BundleConstants.pathToXml)!!

        sharedPrefsViewerViewModelFactory = SharedPrefsViewerViewModelFactory(requireArguments().getString(BundleConstants.pathToXml)!!, packageInfo)
        sharedPreferencesViewerViewModel = ViewModelProvider(this, sharedPrefsViewerViewModelFactory)[SharedPreferencesViewerViewModel::class.java]

        FastScrollerBuilder(scrollView).setupAesthetics().build()

        return view
    }

    override fun getScrollView(): PaddingAwareNestedScrollView {
        return scrollView
    }

    override fun getEditText(): TypeFaceEditText {
        return text
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (requireArguments().getBoolean(BundleConstants.isManifest)) {
            icon.setImageResource(R.drawable.ic_android)
        } else {
            icon.setImageResource(R.drawable.ic_file_xml)
        }

        startPostponedEnterTransition()

        sharedPreferencesViewerViewModel.getSpanned().observe(viewLifecycleOwner) {
            if (it.length > FormattingPreferences.getLargeStringLimit()) {
                childFragmentManager.showLargeStringDialog(it.length) {
                    postDelayed {
                        text.setText(it)
                    }
                }
            } else {
                text.setText(it)
            }

            progress.gone()
            options.visible(true)
            settings.visible(true)
        }

        sharedPreferencesViewerViewModel.getError().observe(viewLifecycleOwner) {
            progress.gone()
            showError(it)
        }

        options.setOnClickListener {
            PopupSharedPrefsViewer(it).setOnPopupClickedListener(object : PopupSharedPrefsViewer.PopupSharedPrefsCallbacks {
                override fun onPopupItemClicked(source: String) {
                    when (source) {
                        getString(R.string.copy) -> {
                            val clipboard: ClipboardManager? = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                            val clip = ClipData.newPlainText("SharedPref", text.text.toString())
                            clipboard?.setPrimaryClip(clip)
                        }
                        getString(R.string.save) -> {
                            childFragmentManager.newSureInstance().setOnSureCallbackListener(object : SureCallbacks {
                                override fun onSure() {
                                    progress.visible(true)
                                    sharedPreferencesViewerViewModel.writePreferencesTextToFile(text.text.toString(), requestCode)
                                }
                            })
                        }
                        getString(R.string.export) -> {
                            val name = with(name.text.toString()) {
                                substring(lastIndexOf("/") + 1, length)
                            }

                            val fileName: String = packageInfo.packageName + "_" + name
                            exportManifest.launch(fileName)
                        }
                    }
                }
            })
        }

        sharedPreferencesViewerViewModel.getLoaderCode().observe(viewLifecycleOwner) {
            if (it == requestCode) {
                Toast.makeText(requireContext(), R.string.saved_successfully, Toast.LENGTH_SHORT).show()
                progress.gone()
            } else {
                progress.gone()
            }
        }

        sharedPreferencesViewerViewModel.getWarning().observe(viewLifecycleOwner) {
            showWarning(it, goBack = false)
        }

        settings.setOnClickListener {
            CodeViewerMenu.newInstance()
                .show(childFragmentManager, "code_viewer_menu")
        }

        search.setOnClickListener {
            changeSearchState()
        }
    }

    companion object {
        fun newInstance(path: String, packageInfo: PackageInfo): SharedPrefsCode {
            val args = Bundle()
            args.putString(BundleConstants.pathToXml, path)
            args.putParcelable(BundleConstants.packageInfo, packageInfo)
            val fragment = SharedPrefsCode()
            fragment.arguments = args
            return fragment
        }

        const val TAG = "SharedPrefsCode"
    }
}
