package app.simple.inure.ui.panels

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.ViewModelProvider
import app.simple.inure.R
import app.simple.inure.adapters.ui.AdapterBatteryOptimization
import app.simple.inure.constants.BottomMenuConstants
import app.simple.inure.decorations.overscroll.CustomVerticalRecyclerView
import app.simple.inure.dialogs.batteryoptimizations.BatteryOptimizationSort.Companion.showBatteryOptimizationSort
import app.simple.inure.dialogs.batteryoptimizations.BatteryOptimizationSwitch.Companion.showBatteryOptimizationSwitch
import app.simple.inure.extensions.fragments.ScopedFragment
import app.simple.inure.interfaces.adapters.AdapterCallbacks
import app.simple.inure.models.BatteryOptimizationModel
import app.simple.inure.popups.battery.PopupOptimizationSwitch
import app.simple.inure.preferences.BatteryOptimizationPreferences
import app.simple.inure.preferences.DevelopmentPreferences
import app.simple.inure.util.NullSafety.isNotNull
import app.simple.inure.viewmodels.panels.BatteryOptimizationViewModel

class BatteryOptimization : ScopedFragment() {

    private lateinit var recyclerView: CustomVerticalRecyclerView
    private lateinit var adapterBatteryOptimization: AdapterBatteryOptimization

    private lateinit var batteryOptimizationViewModel: BatteryOptimizationViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_battery_optimization, container, false)

        recyclerView = view.findViewById(R.id.battery_optimization_recycler_view)

        batteryOptimizationViewModel = ViewModelProvider(requireActivity())[BatteryOptimizationViewModel::class.java]

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        fullVersionCheck()

        //        if(batteryOptimizationViewModel.isBatteryOptimizationDataEmpty()) {
        //            kotlin.runCatching {
        //                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
        //                    withTimeout(5000) {
        //                        showLoader(manualOverride = true)
        //                        startPostponedEnterTransition()
        //                    }
        //                }
        //            }.onFailure {
        //                showWarning(it.message.toString())
        //            }
        //        }

        batteryOptimizationViewModel.getBatteryOptimizationData().observe(viewLifecycleOwner) { batteryOptimizationModelArrayList ->
            hideLoader()
            adapterBatteryOptimization = AdapterBatteryOptimization(batteryOptimizationModelArrayList)

            adapterBatteryOptimization.setOnItemClickListener(object : AdapterCallbacks {
                override fun onSearchPressed(view: View) {
                    openFragmentSlide(Search.newInstance(true), Search.TAG)
                }

                override fun onSettingsPressed(view: View) {
                    openFragmentSlide(Preferences.newInstance(), Preferences.TAG)
                }

                override fun onBatteryOptimizationClicked(view: View, batteryOptimizationModel: BatteryOptimizationModel, position: Int) {
                    if (DevelopmentPreferences.get(DevelopmentPreferences.ALTERNATIVE_BATTERY_OPTIMIZATION_SWITCH)) {
                        PopupOptimizationSwitch(view, batteryOptimizationModel).setOnOptimizeClicked {
                            batteryOptimizationViewModel.getBatteryOptimizationUpdate().observe(viewLifecycleOwner) {
                                if (it.isNotNull()) {
                                    adapterBatteryOptimization.updateItem(it.first, it.second)
                                    batteryOptimizationViewModel.clearBatteryOptimizationAppData()
                                }
                            }

                            batteryOptimizationViewModel.setBatteryOptimization(batteryOptimizationModel, position)
                        }
                    } else {
                        childFragmentManager.showBatteryOptimizationSwitch(batteryOptimizationModel).setBatteryOptimizationCallbacks { batteryOptimizationModel_ ->
                            batteryOptimizationViewModel.getBatteryOptimizationUpdate().observe(viewLifecycleOwner) {
                                if (it.isNotNull()) {
                                    adapterBatteryOptimization.updateItem(it.first, it.second)
                                    batteryOptimizationViewModel.clearBatteryOptimizationAppData()
                                }
                            }

                            batteryOptimizationViewModel.setBatteryOptimization(batteryOptimizationModel_, position)
                        }
                    }
                }
            })

            recyclerView.adapter = adapterBatteryOptimization

            bottomRightCornerMenu?.initBottomMenuWithRecyclerView(BottomMenuConstants.getAllAppsBottomMenuItems(), recyclerView) { id, _ ->
                when (id) {
                    R.drawable.ic_filter -> {
                        childFragmentManager.showBatteryOptimizationSort()
                    }
                    R.drawable.ic_search -> {
                        openFragmentSlide(Search.newInstance(true), Search.TAG)
                    }
                    R.drawable.ic_settings -> {
                        openFragmentSlide(Preferences.newInstance(), Preferences.TAG)
                    }
                    R.drawable.ic_refresh -> {
                        showLoader(manualOverride = true)
                        batteryOptimizationViewModel.refresh()
                    }
                }
            }

            (view.parent as? ViewGroup)?.doOnPreDraw {
                startPostponedEnterTransition()
            }
        }

        batteryOptimizationViewModel.getWarning().observe(viewLifecycleOwner) {
            showWarning(it)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            BatteryOptimizationPreferences.BATTERY_OPTIMIZATION_SORT_STYLE,
            BatteryOptimizationPreferences.BATTERY_OPTIMIZATION_IS_SORTING_REVERSED,
            BatteryOptimizationPreferences.BATTERY_OPTIMIZATION_CATEGORY,
            BatteryOptimizationPreferences.BATTERY_OPTIMIZATION_FILTER -> {
                batteryOptimizationViewModel.refresh()
            }
        }
    }

    companion object {
        fun newInstance(): BatteryOptimization {
            val args = Bundle()
            val fragment = BatteryOptimization()
            fragment.arguments = args
            return fragment
        }

        const val TAG = "BatteryOptimization"
    }
}
