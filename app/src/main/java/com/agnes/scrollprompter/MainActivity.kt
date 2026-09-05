package com.agnes.scrollprompter

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.agnes.scrollprompter.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: SettingManager
    private lateinit var scrollManager: ScrollManager

    // 当前亮度档位（-1=自动，0.3=低，0.5=中，0.8=高）
    private var brightnessValue: Float = -1f

    // ---- 语音跟随 ----
    private val micManager = MicManager()
    private var voiceFollowOn = false
    private var lastSpeechTime = 0L
    private var promptFinished = false  // 文稿已滚到末尾，避免语音跟随反复重启

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settings = SettingManager(this)
        scrollManager = ScrollManager(binding.scrollView)

        setupImmersiveMode()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        loadSettings()
        setupControls()
        setupScrollCallbacks()
        setupMicPermission()
    }

    private val requestMicPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startVoiceFollow()
            } else {
                Toast.makeText(this, R.string.mic_denied, Toast.LENGTH_SHORT).show()
            }
        }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun setupMicPermission() {
        binding.btnMic.setOnClickListener {
            if (!hasMicPermission()) {
                requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                toggleVoiceFollow()
            }
        }
        micManager.onLevel = { level ->
            binding.root.post { onMicLevel(level) }
        }
    }

    // ---- 初始化 ----

    private fun setupImmersiveMode() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.let {
                it.hide(android.view.WindowInsets.Type.systemBars())
                it.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        }
    }

    /** 加载已保存的设置与文稿 */
    private fun loadSettings() {
        // 文稿
        val text = settings.getText()
        if (TextProcessor.isEmpty(text)) {
            binding.prompterText.setText(R.string.empty_hint)
        } else {
            binding.prompterText.text = text
        }

        // 字体大小（24~90sp）
        val fontSp = settings.getFontSizeSp()
        applyFontSize(fontSp)
        binding.fontSeekbar.progress = (fontSp - FONT_MIN_SP).toInt().coerceIn(0, FONT_MAX - FONT_MIN_SP)
        binding.fontValue.text = "${fontSp.toInt()}sp"

        // 滚动速度（间隔 16~800ms，值越大越慢；极速端配合自动加倍步长）
        val interval = settings.getScrollIntervalMs()
        scrollManager.intervalMs = interval
        binding.speedSeekbar.progress = (SPEED_MAX_MS - interval).toInt().coerceIn(0, SPEED_RANGE)
        binding.speedValue.text = formatSpeed(interval)

        // 亮度
        brightnessValue = settings.getBrightnessValue()
        applyBrightness(brightnessValue)

        // 文字颜色
        setupColorSwatches()
        selectTextColor(settings.getTextColorIndex(), save = false)
    }

    private fun setupControls() {
        // 字体大小 SeekBar
        binding.fontSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val sp = FONT_MIN_SP + progress
                applyFontSize(sp.toFloat())
                binding.fontValue.text = "${sp}sp"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                settings.saveFontSize(FONT_MIN_SP + binding.fontSeekbar.progress.toFloat())
            }
        })

        // 字体大小直接输入（回车确认，失焦也生效）
        binding.fontInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        binding.fontInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { applyFontInput(); true } else false
        }
        binding.fontInput.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) applyFontInput() }

        // 滚动速度 SeekBar（progress 越大 → 间隔越小 → 越快）
        binding.speedSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val interval = (SPEED_MAX_MS - progress).coerceAtLeast(SPEED_MIN_MS)
                scrollManager.intervalMs = interval.toLong()
                binding.speedValue.text = formatSpeed(interval.toLong())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                settings.saveScrollInterval(scrollManager.intervalMs)
            }
        })

        // 播放/暂停
        binding.btnPlay.setOnClickListener { togglePlay() }

        // 亮度循环切换（4 档）
        binding.btnBrightness.setOnClickListener { cycleBrightness() }

        // 设置面板开关
        binding.btnSettings.setOnClickListener {
            val visible = binding.settingsPanel.visibility == View.VISIBLE
            binding.settingsPanel.visibility = if (visible) View.GONE else View.VISIBLE
        }

        // 编辑文稿
        binding.btnEdit.setOnClickListener { showEditDialog() }

        // 点击文字区快速前后跳转（左半屏后退 / 右半屏前进）
        binding.scrollView.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                promptFinished = false  // 手动回跳后允许语音跟随继续
                val half = view.width / 2f
                if (event.x < half) scrollManager.stepBackward() else scrollManager.stepForward()
            }
            false // 不消费事件，保留手动拖动滚动能力
        }
    }

    private fun setupScrollCallbacks() {
        scrollManager.onFinished = {
            promptFinished = true
            binding.btnPlay.setImageResource(R.drawable.ic_play)
            binding.btnPlay.setBackgroundResource(R.drawable.bg_play_round)
        }
    }

    // ---- 语音跟随（朗读→滚动，停顿→暂停） ----

    private fun toggleVoiceFollow() {
        if (voiceFollowOn) stopVoiceFollow(showToast = true) else startVoiceFollow()
    }

    private fun startVoiceFollow() {
        if (voiceFollowOn) return
        if (!micManager.start()) {
            Toast.makeText(this, R.string.mic_failed, Toast.LENGTH_SHORT).show()
            return
        }
        voiceFollowOn = true
        lastSpeechTime = System.currentTimeMillis()
        promptFinished = false
        binding.btnMic.setBackgroundResource(R.drawable.bg_mic_active)
        Toast.makeText(this, R.string.mic_on_hint, Toast.LENGTH_SHORT).show()
    }

    private fun stopVoiceFollow(showToast: Boolean) {
        if (!voiceFollowOn) return
        voiceFollowOn = false
        micManager.stop()
        scrollManager.pause()
        binding.btnMic.setBackgroundResource(R.drawable.bg_button_round)
        binding.btnPlay.setImageResource(R.drawable.ic_play)
        binding.btnPlay.setBackgroundResource(R.drawable.bg_play_round)
        if (showToast) Toast.makeText(this, R.string.mic_off_hint, Toast.LENGTH_SHORT).show()
    }

    /** 主线程处理音量：检测到朗读自动滚动，静默超过阈值自动暂停 */
    private fun onMicLevel(level: Float) {
        if (!voiceFollowOn) return
        val now = System.currentTimeMillis()
        val speaking = level > micManager.speechThreshold()
        if (speaking) {
            lastSpeechTime = now
            if (!scrollManager.isPlaying && !promptFinished) {
                scrollManager.play()
                binding.btnPlay.setImageResource(R.drawable.ic_pause)
                binding.btnPlay.setBackgroundResource(R.drawable.bg_pause_round)
            }
        } else if (scrollManager.isPlaying && now - lastSpeechTime > SILENCE_PAUSE_MS) {
            scrollManager.pause()
            binding.btnPlay.setImageResource(R.drawable.ic_play)
            binding.btnPlay.setBackgroundResource(R.drawable.bg_play_round)
        }
    }

    // ---- 播放控制 ----

    private fun togglePlay() {
        // 语音跟随中按播放键 = 退出跟随，改为手动控制
        if (voiceFollowOn) {
            stopVoiceFollow(showToast = true)
            return
        }
        promptFinished = false
        val playing = scrollManager.toggle()
        if (playing) {
            binding.btnPlay.setImageResource(R.drawable.ic_pause)
            binding.btnPlay.setBackgroundResource(R.drawable.bg_pause_round)
        } else {
            binding.btnPlay.setImageResource(R.drawable.ic_play)
            binding.btnPlay.setBackgroundResource(R.drawable.bg_play_round)
        }
    }

    // ---- 字体 ----

    private fun applyFontSize(sp: Float) {
        binding.prompterText.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        // 字体样式跟随系统默认
        binding.prompterText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    /** 应用输入框中的字体数值（12~200sp，超出滑条范围也允许） */
    private fun applyFontInput() {
        val value = binding.fontInput.text.toString().toFloatOrNull() ?: return
        val sp = value.coerceIn(FONT_INPUT_MIN, FONT_INPUT_MAX)
        applyFontSize(sp)
        settings.saveFontSize(sp)
        binding.fontValue.text = "${sp.toInt()}sp"
        if (sp >= FONT_MIN_SP && sp <= FONT_MAX) {
            binding.fontSeekbar.progress = (sp - FONT_MIN_SP).toInt()
        }
    }

    // ---- 文字颜色 ----

    private fun setupColorSwatches() {
        val swatches = listOf(
            binding.colorWhite, binding.colorYellow, binding.colorGreen,
            binding.colorCyan, binding.colorOrange, binding.colorPink
        )
        swatches.forEachIndexed { index, view ->
            view.setOnClickListener { selectTextColor(index, save = true) }
        }
    }

    private fun swatchColors(): List<Int> = listOf(
        ContextCompat.getColor(this, R.color.prompter_text),   // 白（默认）
        ContextCompat.getColor(this, R.color.text_yellow),
        ContextCompat.getColor(this, R.color.text_green),
        ContextCompat.getColor(this, R.color.text_cyan),
        ContextCompat.getColor(this, R.color.text_orange),
        ContextCompat.getColor(this, R.color.text_pink)
    )

    private fun selectTextColor(index: Int, save: Boolean) {
        val swatches = listOf(
            binding.colorWhite, binding.colorYellow, binding.colorGreen,
            binding.colorCyan, binding.colorOrange, binding.colorPink
        )
        val safeIndex = index.coerceIn(0, swatches.lastIndex)
        binding.prompterText.setTextColor(swatchColors()[safeIndex])
        swatches.forEachIndexed { i, view ->
            if (i == safeIndex) {
                view.setBackgroundResource(R.drawable.bg_color_swatch_ring)
                view.animate().scaleX(1.3f).scaleY(1.3f).alpha(1f).setDuration(120).start()
            } else {
                view.setBackgroundResource(R.drawable.bg_color_swatch)
                view.animate().scaleX(1f).scaleY(1f).alpha(0.55f).setDuration(120).start()
            }
        }
        if (save) settings.saveTextColorIndex(safeIndex)
    }

    // ---- 亮度 ----

    private fun cycleBrightness() {
        brightnessValue = when (brightnessValue) {
            -1f -> 0.3f   // 低
            0.3f -> 0.5f  // 中
            0.5f -> 0.8f  // 高
            else -> -1f   // 自动
        }
        applyBrightness(brightnessValue)
        settings.saveBrightness(brightnessValue)
    }

    private fun applyBrightness(value: Float) {
        val lp = window.attributes
        lp.screenBrightness = value
        window.attributes = lp
    }

    // ---- 编辑文稿 ----

    private fun showEditDialog() {
        val input = android.widget.EditText(this)
        input.setText(if (TextProcessor.isEmpty(settings.getText())) "" else settings.getText())
        input.setTextColor(ContextCompat.getColor(this, R.color.prompter_text))
        input.setHintTextColor(ContextCompat.getColor(this, R.color.prompter_text_dim))
        input.setHint(R.string.hint_edit_text)
        input.textSize = 16f
        input.gravity = android.view.Gravity.TOP or android.view.Gravity.START
        input.setBackgroundResource(R.drawable.bg_edit)
        input.minLines = 8
        input.setPadding(24, 20, 24, 20)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.edit_title)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.edit_save) { _, _ ->
                val newText = input.text.toString()
                settings.saveText(newText)
                if (TextProcessor.isEmpty(newText)) {
                    binding.prompterText.setText(R.string.empty_hint)
                } else {
                    binding.prompterText.text = TextProcessor.normalize(newText)
                }
                scrollManager.reset()
                promptFinished = false
            }
            .show()
    }

    // ---- 工具 ----

    private fun formatSpeed(intervalMs: Long): String = "${intervalMs}ms"

    override fun onPause() {
        super.onPause()
        // 离开界面：释放麦克风并停止滚动，避免后台占用与继续滚动
        stopVoiceFollow(showToast = false)
        if (scrollManager.isPlaying) {
            scrollManager.pause()
            binding.btnPlay.setImageResource(R.drawable.ic_play)
            binding.btnPlay.setBackgroundResource(R.drawable.bg_play_round)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scrollManager.pause()
        micManager.stop()
    }

    companion object {
        private const val FONT_MIN_SP = 24
        private const val FONT_MAX = 90
        private const val FONT_INPUT_MIN = 12f
        private const val FONT_INPUT_MAX = 200f
        private const val SPEED_MAX_MS = 800
        private const val SPEED_MIN_MS = 16
        private const val SPEED_RANGE = 784  // 800 - 16
        private const val SILENCE_PAUSE_MS = 1500L  // 静默多久后自动暂停
    }
}
