package com.agnes.scrollprompter

import android.content.Context
import android.content.SharedPreferences

/**
 * 设置持久化（SharedPreferences）
 * 保存字体大小、滚动速度、亮度档位以及文稿内容。
 */
class SettingManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---- 文稿内容 ----
    fun getText(): String = prefs.getString(KEY_TEXT, "") ?: ""

    fun saveText(text: String) = prefs.edit().putString(KEY_TEXT, text).apply()

    // ---- 字体大小（sp）----
    fun getFontSizeSp(): Float = prefs.getFloat(KEY_FONT_SIZE, 42f)

    fun saveFontSize(sp: Float) = prefs.edit().putFloat(KEY_FONT_SIZE, sp).apply()

    // ---- 滚动间隔（ms）----
    fun getScrollIntervalMs(): Long = prefs.getLong(KEY_SCROLL_INTERVAL, 80L)

    fun saveScrollInterval(ms: Long) = prefs.edit().putLong(KEY_SCROLL_INTERVAL, ms).apply()

    // ---- 亮度档位 ----
    fun getBrightnessValue(): Float = prefs.getFloat(KEY_BRIGHTNESS, -1f)

    fun saveBrightness(value: Float) = prefs.edit().putFloat(KEY_BRIGHTNESS, value).apply()

    // ---- 文字颜色（色板索引）----
    fun getTextColorIndex(): Int = prefs.getInt(KEY_TEXT_COLOR, 0)

    fun saveTextColorIndex(index: Int) = prefs.edit().putInt(KEY_TEXT_COLOR, index).apply()

    companion object {
        private const val PREFS_NAME = "scroll_prompter_settings"
        private const val KEY_TEXT = "prompter_text"
        private const val KEY_FONT_SIZE = "font_size_sp"
        private const val KEY_SCROLL_INTERVAL = "scroll_interval_ms"
        private const val KEY_BRIGHTNESS = "brightness_value"
        private const val KEY_TEXT_COLOR = "text_color_index"
    }
}
