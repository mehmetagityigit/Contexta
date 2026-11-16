package com.example.feature.overlay

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.core.common.LocaleHelper
import com.example.core.common.VoiceCaptureController
import com.example.core.common.VoiceCaptureState
import com.example.core.common.VoiceErrorType
import com.example.core.data.AppContainer
import com.example.core.model.ContextSourceType
import com.example.core.model.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Real Android system overlay service using WindowManager + TYPE_APPLICATION_OVERLAY.
 * Supports:
 * - Single bubble & single expanded quick-capture card
 * - Dragging + automatic horizontal edge snapping + persistent position
 * - Screen bounds clamping across portrait/landscape rotations
 * - Scale & transparency driven by DataStore UserSettings
 * - Controlled shutdown if SYSTEM_ALERT_WINDOW is revoked
 * - Preserves unsaved text on failure
 * - Safe fallback to in-app voice capture when microphone from overlay is restricted
 */
class FloatingCaptureService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private var settingsJob: Job? = null
    private var voiceJob: Job? = null

    private var bubbleView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null

    private var panelView: View? = null
    private var panelParams: WindowManager.LayoutParams? = null

    private var currentSettings = UserSettings()
    private var voiceController: VoiceCaptureController? = null

    // Preserve user input across failed save or accidental collapse
    private var draftExpression: String = ""
    private var draftContext: String = ""
    private var draftSource: String = ""

    private var expressionInput: EditText? = null
    private var contextInput: EditText? = null
    private var sourceInput: EditText? = null
    private var feedbackText: TextView? = null
    private var saveButton: Button? = null
    private var micButton: ImageButton? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopOverlayCleanly()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                if (!Settings.canDrawOverlays(this)) {
                    stopOverlayCleanly()
                    return START_NOT_STICKY
                }
                startForegroundWithNotification()
                AppContainer.setOverlayRunning(true)
                ensureBubbleAttached()
                observeSettings()
            }
        }
        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!Settings.canDrawOverlays(this)) {
            stopOverlayCleanly()
            return
        }
        clampAndSnapBubble()
        clampPanelToBounds()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        AppContainer.setOverlayRunning(false)
        settingsJob?.cancel()
        voiceJob?.cancel()
        voiceController?.release()
        voiceController = null
        removePanel()
        removeBubble()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startForegroundWithNotification() {
        val stopIntent = Intent(this, FloatingCaptureService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            101,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            102,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.overlay_notif_title))
            .setContentText(getString(R.string.overlay_notif_text))
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(0, getString(R.string.overlay_notif_stop), stopPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.overlay_channel_desc)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun observeSettings() {
        if (settingsJob != null) return
        val settingsRepo = AppContainer.getSettingsRepository(this)
        settingsJob = serviceScope.launch {
            settingsRepo.settingsFlow.collectLatest { settings ->
                currentSettings = settings
                applyVisualSettings()
            }
        }
    }

    private fun overlayWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun ensureBubbleAttached() {
        if (bubbleView != null) return
        val settingsRepo = AppContainer.getSettingsRepository(this)
        val (savedX, savedY) = settingsRepo.getOverlayPosition()

        val bubbleSizePx = dpToPx((52 * currentSettings.overlayScale).toInt().coerceIn(40, 84))
        val container = FrameLayout(this).apply {
            contentDescription = getString(R.string.overlay_bubble_cd)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#3949AB"))
                setStroke(dpToPx(2), Color.parseColor("#FAF9F6"))
            }
            elevation = dpToPx(6).toFloat()
            alpha = currentSettings.overlayAlpha
        }

        val label = TextView(this).apply {
            text = "C+"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            gravity = Gravity.CENTER
        }
        container.addView(
            label,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val params = WindowManager.LayoutParams(
            bubbleSizePx,
            bubbleSizePx,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX
            y = savedY
        }

        attachDragAndTapListener(container, params)
        bubbleView = container
        bubbleParams = params
        runCatching {
            windowManager.addView(container, params)
            clampAndSnapBubble()
        }.onFailure {
            stopOverlayCleanly()
        }
    }

    private fun attachDragAndTapListener(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var touchStartX = 0f
        var touchStartY = 0f
        var moved = false
        val touchSlop = dpToPx(8)

        view.setOnTouchListener { _, event ->
            if (!Settings.canDrawOverlays(this)) {
                stopOverlayCleanly()
                return@setOnTouchListener true
            }
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchStartX).toInt()
                    val dy = (event.rawY - touchStartY).toInt()
                    if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                        moved = true
                    }
                    if (moved) {
                        val (maxW, maxH) = screenDimensions()
                        params.x = (initialX + dx).coerceIn(0, (maxW - params.width).coerceAtLeast(0))
                        params.y = (initialY + dy).coerceIn(dpToPx(24), (maxH - params.height - dpToPx(24)).coerceAtLeast(0))
                        runCatching { windowManager.updateViewLayout(view, params) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        togglePanel()
                    } else {
                        clampAndSnapBubble()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun clampAndSnapBubble() {
        val view = bubbleView ?: return
        val params = bubbleParams ?: return
        val (maxW, maxH) = screenDimensions()
        val margin = dpToPx(12)
        val snapLeft = params.x + params.width / 2 < maxW / 2
        params.x = if (snapLeft) margin else (maxW - params.width - margin).coerceAtLeast(0)
        params.y = params.y.coerceIn(dpToPx(32), (maxH - params.height - dpToPx(48)).coerceAtLeast(dpToPx(32)))
        runCatching { windowManager.updateViewLayout(view, params) }
        AppContainer.getSettingsRepository(this).saveOverlayPosition(params.x, params.y)
    }

    private fun applyVisualSettings() {
        val view = bubbleView ?: return
        val params = bubbleParams ?: return
        val sizePx = dpToPx((52 * currentSettings.overlayScale).toInt().coerceIn(40, 84))
        params.width = sizePx
        params.height = sizePx
        view.alpha = currentSettings.overlayAlpha
        runCatching { windowManager.updateViewLayout(view, params) }
        clampAndSnapBubble()
    }

    private fun togglePanel() {
        if (panelView == null) {
            showPanel()
        } else {
            saveDraftFromFields()
            removePanel()
        }
    }

    private fun showPanel() {
        if (!Settings.canDrawOverlays(this)) {
            stopOverlayCleanly()
            return
        }
        if (panelView != null) return

        val (maxW, _) = screenDimensions()
        val panelWidth = (maxW - dpToPx(32)).coerceAtMost(dpToPx(360))

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(18).toFloat()
                setColor(Color.parseColor("#FAF9F6"))
                setStroke(dpToPx(1), Color.parseColor("#DCE0EC"))
            }
            elevation = dpToPx(10).toFloat()
        }

        // Header row
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = getString(R.string.overlay_panel_title)
            setTextColor(Color.parseColor("#161D2E"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val closeBtn = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundColor(Color.TRANSPARENT)
            contentDescription = getString(R.string.action_close)
            minimumWidth = dpToPx(48)
            minimumHeight = dpToPx(48)
            setOnClickListener {
                saveDraftFromFields()
                removePanel()
            }
        }
        header.addView(title)
        header.addView(closeBtn)
        card.addView(header)

        // Word + Mic row
        val exprRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val exprEdit = EditText(this).apply {
            hint = getString(R.string.overlay_hint_word)
            setText(draftExpression)
            setTextColor(Color.parseColor("#161D2E"))
            setHintTextColor(Color.parseColor("#6D768A"))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = EditorInfo.IME_ACTION_NEXT
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        expressionInput = exprEdit

        val mic = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_btn_speak_now)
            setBackgroundColor(Color.parseColor("#E7EAFA"))
            contentDescription = getString(R.string.voice_mic_cd)
            minimumWidth = dpToPx(48)
            minimumHeight = dpToPx(48)
            setOnClickListener { handleOverlayVoiceTap() }
        }
        micButton = mic
        exprRow.addView(exprEdit)
        exprRow.addView(mic)
        card.addView(exprRow)

        val ctxEdit = EditText(this).apply {
            hint = getString(R.string.overlay_hint_context)
            setText(draftContext)
            setTextColor(Color.parseColor("#161D2E"))
            setHintTextColor(Color.parseColor("#6D768A"))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            maxLines = 3
        }
        contextInput = ctxEdit
        card.addView(ctxEdit)

        val srcEdit = EditText(this).apply {
            hint = getString(R.string.overlay_hint_source)
            setText(draftSource)
            setTextColor(Color.parseColor("#161D2E"))
            setHintTextColor(Color.parseColor("#6D768A"))
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_DONE
        }
        sourceInput = srcEdit
        card.addView(srcEdit)

        val fbText = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.parseColor("#2E7D6B"))
            visibility = View.GONE
            setPadding(0, dpToPx(6), 0, dpToPx(4))
        }
        feedbackText = fbText
        card.addView(fbText)

        // Action row
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dpToPx(8), 0, 0)
        }
        val cancelBtn = Button(this).apply {
            text = getString(R.string.action_cancel)
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.parseColor("#545E75"))
            minimumHeight = dpToPx(48)
            setOnClickListener {
                clearDraftFields()
                removePanel()
            }
        }
        val saveBtn = Button(this).apply {
            text = getString(R.string.action_save)
            setBackgroundColor(Color.parseColor("#3949AB"))
            setTextColor(Color.WHITE)
            minimumHeight = dpToPx(48)
            setOnClickListener { handleSaveFromOverlay() }
        }
        saveButton = saveBtn
        actions.addView(cancelBtn)
        actions.addView(saveBtn)
        card.addView(actions)

        val params = WindowManager.LayoutParams(
            panelWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dpToPx(64)
            @Suppress("DEPRECATION")
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        panelView = card
        panelParams = params
        runCatching {
            windowManager.addView(card, params)
            exprEdit.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(exprEdit, InputMethodManager.SHOW_IMPLICIT)
        }.onFailure {
            removePanel()
        }
    }

    private fun handleOverlayVoiceTap() {
        val hasAudioPerm = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasAudioPerm) {
            openInAppVoiceCaptureFallback(getString(R.string.overlay_voice_redirect_permission))
            return
        }

        val controller = voiceController ?: VoiceCaptureController(this).also { voiceController = it }
        if (!controller.isAvailable()) {
            showFeedback(getString(R.string.voice_error_unavailable), isError = true)
            return
        }

        if (voiceJob == null) {
            voiceJob = serviceScope.launch {
                controller.state.collectLatest { state ->
                    when (state) {
                        is VoiceCaptureState.Listening -> showFeedback(getString(R.string.voice_listening), isError = false)
                        is VoiceCaptureState.Partial -> expressionInput?.setText(state.text)
                        is VoiceCaptureState.Recognized -> {
                            expressionInput?.setText(state.text)
                            expressionInput?.setSelection(state.text.length)
                            draftExpression = state.text
                            showFeedback(getString(R.string.voice_recognized_edit), isError = false)
                        }
                        is VoiceCaptureState.Error -> {
                            if (state.errorType == VoiceErrorType.PERMISSION_DENIED ||
                                state.errorType == VoiceErrorType.SERVICE_UNAVAILABLE
                            ) {
                                openInAppVoiceCaptureFallback(getString(R.string.overlay_voice_redirect_restricted))
                            } else {
                                showFeedback(getString(R.string.voice_error_try_again), isError = true)
                            }
                        }
                        VoiceCaptureState.Idle -> Unit
                    }
                }
            }
        }

        if (controller.state.value is VoiceCaptureState.Listening) {
            controller.stopListening()
        } else {
            runCatching { controller.startListening() }.onFailure {
                openInAppVoiceCaptureFallback(getString(R.string.overlay_voice_redirect_restricted))
            }
        }
    }

    private fun openInAppVoiceCaptureFallback(notice: String) {
        saveDraftFromFields()
        showFeedback(notice, isError = false)
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_OPEN_VOICE_CAPTURE, true)
            putExtra(MainActivity.EXTRA_DRAFT_EXPRESSION, draftExpression)
        }
        startActivity(intent)
    }

    private fun handleSaveFromOverlay() {
        val word = expressionInput?.text?.toString()?.trim().orEmpty()
        val ctx = contextInput?.text?.toString()?.trim().orEmpty()
        val src = sourceInput?.text?.toString()?.trim().orEmpty()

        draftExpression = word
        draftContext = ctx
        draftSource = src

        if (word.isEmpty()) {
            showFeedback(getString(R.string.overlay_error_empty_word), isError = true)
            return
        }

        val repo = AppContainer.resolveActiveUserRepository(this)
        if (repo == null) {
            showFeedback(getString(R.string.overlay_error_sign_in_needed), isError = true)
            return
        }

        saveButton?.isEnabled = false
        serviceScope.launch {
            val result = repo.saveVocabularyWithOptionalContext(
                expression = word,
                contextSentence = ctx,
                sourceTitle = src,
                sourceType = ContextSourceType.OVERLAY
            )
            saveButton?.isEnabled = true
            result.onSuccess {
                clearDraftFields()
                showFeedback(getString(R.string.overlay_saved_feedback), isError = false)
                delay(900L)
                removePanel()
            }.onFailure { err ->
                // Keep entered text intact on failure
                showFeedback(
                    err.localizedMessage ?: getString(R.string.overlay_error_save_failed),
                    isError = true
                )
            }
        }
    }

    private fun showFeedback(message: String, isError: Boolean) {
        feedbackText?.apply {
            text = message
            setTextColor(Color.parseColor(if (isError) "#B3261E" else "#2E7D6B"))
            visibility = View.VISIBLE
        }
    }

    private fun saveDraftFromFields() {
        draftExpression = expressionInput?.text?.toString().orEmpty()
        draftContext = contextInput?.text?.toString().orEmpty()
        draftSource = sourceInput?.text?.toString().orEmpty()
    }

    private fun clearDraftFields() {
        draftExpression = ""
        draftContext = ""
        draftSource = ""
        expressionInput?.setText("")
        contextInput?.setText("")
        sourceInput?.setText("")
    }

    private fun clampPanelToBounds() {
        val view = panelView ?: return
        val params = panelParams ?: return
        val (maxW, _) = screenDimensions()
        params.width = (maxW - dpToPx(32)).coerceAtMost(dpToPx(360))
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    private fun removePanel() {
        voiceController?.stopListening()
        voiceJob?.cancel()
        voiceJob = null
        panelView?.let { runCatching { windowManager.removeView(it) } }
        panelView = null
        panelParams = null
        expressionInput = null
        contextInput = null
        sourceInput = null
        feedbackText = null
        saveButton = null
        micButton = null
    }

    private fun removeBubble() {
        bubbleView?.let { runCatching { windowManager.removeView(it) } }
        bubbleView = null
        bubbleParams = null
    }

    private fun stopOverlayCleanly() {
        AppContainer.setOverlayRunning(false)
        removePanel()
        removeBubble()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun screenDimensions(): Pair<Int, Int> {
        val metrics = resources.displayMetrics
        return metrics.widthPixels to metrics.heightPixels
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    companion object {
        const val ACTION_START = "com.example.contexta.OVERLAY_START"
        const val ACTION_STOP = "com.example.contexta.OVERLAY_STOP"
        private const val CHANNEL_ID = "contexta_floating_capture"
        private const val NOTIFICATION_ID = 4101
    }
}
