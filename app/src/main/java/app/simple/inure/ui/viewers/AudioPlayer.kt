package app.simple.inure.ui.viewers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.SeekBar
import androidx.core.app.SharedElementCallback
import androidx.core.view.get
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import app.simple.inure.R
import app.simple.inure.adapters.music.AlbumArtAdapter
import app.simple.inure.constants.BundleConstants
import app.simple.inure.constants.ServiceConstants
import app.simple.inure.decorations.corners.DynamicCornerTextView
import app.simple.inure.decorations.lrc.LrcHelper
import app.simple.inure.decorations.lrc.LrcView
import app.simple.inure.decorations.ripple.DynamicRippleImageButton
import app.simple.inure.decorations.theme.ThemeSeekBar
import app.simple.inure.decorations.typeface.TypeFaceTextView
import app.simple.inure.decorations.views.CustomProgressBar
import app.simple.inure.extensions.fragments.ScopedFragment
import app.simple.inure.models.AudioModel
import app.simple.inure.preferences.DevelopmentPreferences
import app.simple.inure.preferences.MusicPreferences
import app.simple.inure.services.AudioServicePager
import app.simple.inure.util.ActivityUtils.isAppInLockTaskMode
import app.simple.inure.util.AudioUtils.toBitrate
import app.simple.inure.util.CommonUtils.withInvertedBooleanScope
import app.simple.inure.util.ConditionUtils.invert
import app.simple.inure.util.IntentHelper
import app.simple.inure.util.NullSafety.isNotNull
import app.simple.inure.util.NumberUtils
import app.simple.inure.util.ParcelUtils.parcelable
import app.simple.inure.util.ViewUtils
import app.simple.inure.util.ViewUtils.gone
import app.simple.inure.viewmodels.panels.MusicViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

class AudioPlayer : ScopedFragment() {

    private lateinit var artPager: ViewPager2
    private lateinit var lrcView: LrcView
    private lateinit var replay: DynamicRippleImageButton
    private lateinit var playPause: DynamicRippleImageButton
    private lateinit var close: DynamicRippleImageButton
    private lateinit var next: DynamicRippleImageButton
    private lateinit var previous: DynamicRippleImageButton
    private lateinit var duration: TypeFaceTextView
    private lateinit var progress: TypeFaceTextView
    private lateinit var number: DynamicCornerTextView
    private lateinit var title: TypeFaceTextView
    private lateinit var artist: TypeFaceTextView
    private lateinit var album: TypeFaceTextView
    private lateinit var fileInfo: TypeFaceTextView
    private lateinit var seekBar: ThemeSeekBar
    private lateinit var loader: CustomProgressBar

    private var audioModels: ArrayList<AudioModel>? = null
    private var audioModel: AudioModel? = null
    private var audioServicePager: AudioServicePager? = null
    private var serviceConnection: ServiceConnection? = null
    private var audioBroadcastReceiver: BroadcastReceiver? = null
    private var viewPagerCallback: ViewPagerCallback? = null

    private val audioIntentFilter = IntentFilter()
    private var serviceBound = false
    private var wasSongPlaying = false
    private var isFinished = false
    private var shouldSwitch = MutableStateFlow(true)

    /**
     * [currentSeekPosition] will keep the current position of the playback
     * in the memory. This is necessary in cases where multiple instances of
     * the [app.simple.inure.activities.association.AudioPlayerActivity] is
     * started and the service lost the state of the previous playback so when
     * the instance is resumed let's say from task manager we can easily seek to
     * that position where we left right when onPrepared is called before running
     * our handler [progressRunnable].
     */
    private var currentSeekPosition = 0

    private val musicViewModel: MusicViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = if (DevelopmentPreferences.get(DevelopmentPreferences.USE_ALTERNATE_AUDIO_PLAYER_INTERFACE)) {
            inflater.inflate(R.layout.fragment_audio_player_pager_alternate, container, false)
        } else {
            inflater.inflate(R.layout.fragment_audio_player_pager, container, false)
        }

        artPager = view.findViewById(R.id.album_art_mime)
        lrcView = view.findViewById(R.id.lrc_view)
        replay = view.findViewById(R.id.mime_repeat_button)
        playPause = view.findViewById(R.id.mime_play_button)
        close = view.findViewById(R.id.mime_close_button)
        next = view.findViewById(R.id.mime_next_button)
        previous = view.findViewById(R.id.mime_previous_button)
        duration = view.findViewById(R.id.current_duration_mime)
        progress = view.findViewById(R.id.current_time_mime)
        fileInfo = view.findViewById(R.id.mime_info)
        number = view.findViewById(R.id.number)
        title = view.findViewById(R.id.mime_title)
        artist = view.findViewById(R.id.mime_artist)
        album = view.findViewById(R.id.mime_album)
        seekBar = view.findViewById(R.id.seekbar_mime)
        loader = view.findViewById(R.id.loader)

        audioModel = requireArguments().parcelable(BundleConstants.audioModel)

        audioIntentFilter.addAction(ServiceConstants.actionPreparedPager)
        audioIntentFilter.addAction(ServiceConstants.actionQuitMusicServicePager)
        audioIntentFilter.addAction(ServiceConstants.actionMetaDataPager)
        audioIntentFilter.addAction(ServiceConstants.actionPausePager)
        audioIntentFilter.addAction(ServiceConstants.actionPlayPager)
        audioIntentFilter.addAction(ServiceConstants.actionBufferingPager)
        audioIntentFilter.addAction(ServiceConstants.actionNextPager)
        audioIntentFilter.addAction(ServiceConstants.actionPreviousPager)
        audioIntentFilter.addAction(ServiceConstants.actionMediaErrorPager)

        if (DevelopmentPreferences.get(DevelopmentPreferences.USE_ALTERNATE_AUDIO_PLAYER_INTERFACE)) {
            artPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        } else {
            artPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        }

        viewPagerCallback = ViewPagerCallback()

        return view
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(names: List<String>, sharedElements: MutableMap<String, View>) {
                // Locate the ViewHolder for the clicked position.
                val selectedViewHolder = (artPager[0] as RecyclerView)
                    .findViewHolderForAdapterPosition(requireArguments().getInt(BundleConstants.position, 0))
                if (selectedViewHolder is AlbumArtAdapter.Holder) {
                    // Map the first shared element name to the child ImageView.
                    sharedElements[names[0]] = selectedViewHolder.itemView.findViewById(R.id.album_art)
                }
            }
        })

        postponeEnterTransition()

        if (requireArguments().getBoolean(BundleConstants.fromSearch)) {
            musicViewModel.getSearched().observe(viewLifecycleOwner) {
                audioModels = it
                artPager.adapter = AlbumArtAdapter(audioModels!!)
                artPager.setCurrentItem(requireArguments().getInt(BundleConstants.position, 0), false)

                (artPager.adapter as AlbumArtAdapter).onAlbumArtClicked = { _, _ ->
                    audioServicePager?.changePlayerState()
                }

                /**
                 * This will break the transition for some reason, start the animation without
                 * any callback
                 */
                //                (view.parent as? ViewGroup)?.doOnPreDraw {
                //                    startPostponedEnterTransition()
                //                }

                /**
                 * Like this, it works fine here
                 */
                startPostponedEnterTransition()

                setMetaData(artPager.currentItem)

                viewPagerCallback?.let { it1 -> artPager.registerOnPageChangeCallback(it1) }

                lifecycleScope.launch { // OnStart, but on steroids!!!
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        startService()
                    }
                }
            }
        } else {
            musicViewModel.getSongs().observe(viewLifecycleOwner) {
                audioModels = it
                artPager.adapter = AlbumArtAdapter(audioModels!!)
                artPager.setCurrentItem(requireArguments().getInt(BundleConstants.position, 0), false)

                (artPager.adapter as AlbumArtAdapter).onAlbumArtClicked = { _, _ ->
                    audioServicePager?.changePlayerState()
                }

                /**
                 * This will break the transition for some reason, start the animation without
                 * any callback
                 */
                //                (view.parent as? ViewGroup)?.doOnPreDraw {
                //                    startPostponedEnterTransition()
                //                }

                /**
                 * Like this, it works fine here
                 */
                startPostponedEnterTransition()
                setMetaData(artPager.currentItem)

                viewPagerCallback?.let { it1 -> artPager.registerOnPageChangeCallback(it1) }

                lifecycleScope.launch { // OnStart, but on steroids!!!
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        startService()
                    }
                }
            }
        }

        replayButtonStatus(animate = false)
        playPause.isEnabled = false

        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                serviceBound = true
                audioServicePager = (service as AudioServicePager.AudioBinder).getService()
                audioServicePager?.setAudioPlayerProps(audioModels!!, artPager.currentItem)
                audioServicePager?.setCurrentPosition(artPager.currentItem)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                serviceBound = false
            }
        }

        audioBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    ServiceConstants.actionPreparedPager -> {
                        audioServicePager?.seek(currentSeekPosition)
                    }
                    ServiceConstants.actionMetaDataPager -> {
                        try {
                            handler.removeCallbacks(progressRunnable)
                            duration.text = NumberUtils.getFormattedTime(audioServicePager?.getDuration()?.toLong()!!)
                            handler.post(progressRunnable)
                            loader.gone(animate = true)
                            playPause.isEnabled = true
                            wasSongPlaying = true
                            buttonStatus(audioServicePager?.isPlaying()!!, animate = false)
                        } catch (e: IllegalStateException) {
                            e.printStackTrace()
                            showError(e.stackTraceToString())
                        }
                    }
                    ServiceConstants.actionQuitMusicServicePager -> {
                        finish()
                    }
                    ServiceConstants.actionPlayPager -> {
                        buttonStatus(true)
                    }
                    ServiceConstants.actionPausePager -> {
                        buttonStatus(false)
                    }
                    ServiceConstants.actionNextPager -> {
                        withInvertedBooleanScope(shouldSwitch) {
                            currentSeekPosition = 0
                            viewPagerCallback?.let { artPager.unregisterOnPageChangeCallback(it) }
                            if (artPager.currentItem < audioModels!!.size - 1) {
                                artPager.setCurrentItem(artPager.currentItem + 1, true)
                            } else {
                                artPager.setCurrentItem(0, true)
                            }

                            setMetaData(artPager.currentItem)
                            artPager.registerOnPageChangeCallback(viewPagerCallback!!)
                        }
                    }
                    ServiceConstants.actionPreviousPager -> {
                        currentSeekPosition = 0
                        viewPagerCallback?.let { artPager.unregisterOnPageChangeCallback(it) }
                        if (artPager.currentItem > 0) {
                            artPager.setCurrentItem(artPager.currentItem - 1, true)
                        } else {
                            artPager.setCurrentItem(audioModels!!.size - 1, true)
                        }

                        setMetaData(artPager.currentItem)
                        artPager.registerOnPageChangeCallback(viewPagerCallback!!)
                    }
                    ServiceConstants.actionBufferingPager -> {
                        seekBar.updateSecondaryProgress(intent.extras?.getInt(IntentHelper.INT_EXTRA)!!)
                    }
                    ServiceConstants.actionMediaErrorPager -> {
                        showError(intent.extras?.getString("stringExtra", "unknown_media_playback_error")!!, goBack = false)
                    }
                }
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    this@AudioPlayer.progress.text = NumberUtils.getFormattedTime(progress.toLong())
                    lrcView.updateTimeWithoutScroll(progress.toLong())
                    currentSeekPosition = progress
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                if (seekBar.max != audioServicePager?.getDuration()!!) {
                    seekBar.max = audioServicePager?.getDuration()!!
                }

                this@AudioPlayer.seekBar.clearAnimation()
                handler.removeCallbacks(progressRunnable)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                audioServicePager?.seek(seekBar.progress)
                handler.post(progressRunnable)
            }
        })

        replay.setOnClickListener {
            MusicPreferences.setMusicRepeat(!MusicPreferences.getMusicRepeat())
        }

        playPause.setOnClickListener {
            audioServicePager?.changePlayerState()!!
        }

        close.setOnClickListener {
            if (requireContext().isAppInLockTaskMode().invert()) {
                handler.removeCallbacks(progressRunnable)
                stopService()
            } else {
                showWarning(getString(R.string.lock_task_warning), false)
            }
        }

        next.setOnClickListener {
            if (artPager.currentItem < audioModels!!.size - 1) {
                artPager.setCurrentItem(artPager.currentItem + 1, true)
            } else {
                artPager.setCurrentItem(0, true)
            }
        }

        previous.setOnClickListener {
            if (artPager.currentItem > 0) {
                artPager.setCurrentItem(artPager.currentItem - 1, true)
            } else {
                artPager.setCurrentItem(audioModels!!.size - 1, true)
            }
        }

        lrcView.setOnPlayIndicatorLineListener { time, _ ->
            audioServicePager?.seek(time.toInt())
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (audioModel.isNotNull()) {
                    setLrc()
                    Log.d(TAG, "Lrc refreshed")
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setMetaData(position: Int) {
        if (requireArguments().getInt(BundleConstants.position) < position) {
            title.setTextWithSlideAnimation(audioModels!![position].title, 250L, ViewUtils.LEFT, 0L)
            artist.setTextWithSlideAnimation(audioModels!![position].artists, 250L, ViewUtils.LEFT, 50L)
            album.setTextWithSlideAnimation(audioModels!![position].album, 250L, ViewUtils.LEFT, 100L)
            fileInfo.setTextWithSlideAnimation(getString(
                    R.string.audio_file_info,
                    "." + audioModels!![position].path?.substringAfterLast("."),
                    audioModels!![position].bitrate.toBitrate(),
                    audioModels!![position].mimeType), 250L, ViewUtils.LEFT, 150L)
        } else {
            title.setTextWithSlideAnimation(audioModels!![position].title, 250L, ViewUtils.RIGHT, 0L)
            artist.setTextWithSlideAnimation(audioModels!![position].artists, 250L, ViewUtils.RIGHT, 50L)
            album.setTextWithSlideAnimation(audioModels!![position].album, 250L, ViewUtils.RIGHT, 100L)
            fileInfo.setTextWithSlideAnimation(getString(
                    R.string.audio_file_info,
                    "." + audioModels!![position].path?.substringAfterLast("."),
                    audioModels!![position].bitrate.toBitrate(),
                    audioModels!![position].mimeType), 250L, ViewUtils.RIGHT, 150L)
        }

        setLrc()
        number.text = "${(position + 1)}/${audioModels!!.size}"
        requireArguments().putInt(BundleConstants.position, position)
    }

    private fun buttonStatus(isPlaying: Boolean, animate: Boolean = true) {
        if (isPlaying) {
            playPause.setIcon(R.drawable.ic_pause, animate)
        } else {
            playPause.setIcon(R.drawable.ic_play, animate)
        }
    }

    private fun setLrc() {
        lifecycleScope.launch {
            with(File(audioModels!![artPager.currentItem].path.replaceAfterLast(".", "lrc"))) {
                if (exists()) {
                    lrcView.setLrcData(LrcHelper.parseLrcFromFile(this))
                    delay(1000)
                    lrcView.animate()
                        .alpha(1F)
                        .setInterpolator(AccelerateInterpolator())
                        .setDuration(resources.getInteger(R.integer.animation_duration).toLong())
                        .withStartAction {
                            lrcView.visibility = View.VISIBLE
                        }
                        .start()
                } else {
                    delay(1000)
                    lrcView.animate()
                        .alpha(0F)
                        .setInterpolator(AccelerateInterpolator())
                        .setDuration(resources.getInteger(R.integer.animation_duration).toLong())
                        .withEndAction {
                            lrcView.visibility = View.INVISIBLE
                        }
                        .start()
                }
            }
        }
    }

    private fun replayButtonStatus(animate: Boolean = true) {
        if (MusicPreferences.getMusicRepeat()) {
            if (animate) {
                replay.animate().alpha(1.0F).setDuration(resources.getInteger(R.integer.animation_duration).toLong()).start()
            } else {
                replay.alpha = 1.0F
            }
        } else {
            if (animate) {
                replay.animate().alpha(0.3F).setDuration(resources.getInteger(R.integer.animation_duration).toLong()).start()
            } else {
                replay.alpha = 0.3F
            }
        }
    }

    private fun stopService() {
        serviceBound = false
        requireContext().unbindService(serviceConnection!!)
        requireContext().stopService(Intent(requireContext(), AudioServicePager::class.java))
        finish()
    }

    private fun finish() {
        isFinished = true
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (requireActivity().supportFragmentManager.backStackEntryCount > 0) {
                    popBackStack()
                } else {
                    requireActivity().finish()
                }
            }
        }
    }

    private val progressRunnable: Runnable = object : Runnable {
        override fun run() {
            currentSeekPosition = audioServicePager?.getProgress()!!
            seekBar.updateProgress(currentSeekPosition, audioServicePager?.getDuration()!!)
            lrcView.updateTime(currentSeekPosition.toLong())
            progress.text = NumberUtils.getFormattedTime(currentSeekPosition.toLong())

            if (lrcView.isPaused) {
                lrcView.resume()
            }

            handler.postDelayed(this, 1000L)
        }
    }

    private fun startService() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(audioBroadcastReceiver!!) // Just to be safe
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(audioBroadcastReceiver!!, audioIntentFilter)
        val intent = Intent(requireActivity(), AudioServicePager::class.java)
        requireContext().startService(intent)
        serviceConnection?.let { requireContext().bindService(intent, it, Context.BIND_AUTO_CREATE) }
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(progressRunnable)
        if (serviceBound) {
            try {
                serviceConnection?.let { requireContext().unbindService(it) }
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            MusicPreferences.MUSIC_REPEAT -> {
                replayButtonStatus(animate = true)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(audioBroadcastReceiver!!)
    }

    inner class ViewPagerCallback : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            if (position != MusicPreferences.getMusicPosition()) {
                handler.removeCallbacks(progressRunnable)
                currentSeekPosition = 0
                MusicPreferences.setMusicPosition(position)
                audioServicePager?.setCurrentPosition(position)
                setMetaData(position)
                Log.d(TAG, "Page selected: $position")
            }
        }

        override fun onPageScrollStateChanged(state: Int) {
            super.onPageScrollStateChanged(state)
            if (state == ViewPager2.SCROLL_STATE_IDLE) {
                if (artPager.currentItem != MusicPreferences.getMusicPosition()) {
                    handler.removeCallbacks(progressRunnable)
                    currentSeekPosition = 0
                    MusicPreferences.setMusicPosition(artPager.currentItem)
                    audioServicePager?.setCurrentPosition(artPager.currentItem)
                    setMetaData(artPager.currentItem)
                    Log.d(TAG, "Page scrolled: ${artPager.currentItem}")
                }
            }
        }
    }

    companion object {
        fun newInstance(position: Int, fromSearch: Boolean = false): AudioPlayer {
            val args = Bundle()
            MusicPreferences.setFromSearch(fromSearch)
            args.putBoolean(BundleConstants.fromSearch, fromSearch)
            args.putInt(BundleConstants.position, position)
            val fragment = AudioPlayer()
            fragment.arguments = args
            return fragment
        }

        const val TAG = "AudioPlayerPager"
    }
}
