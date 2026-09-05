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

    // ---- 跟随屏幕（控制栏横竖屏贴底；关闭则播放时自动隐藏）----
    fun getFollowScreen(): Boolean = prefs.getBoolean(KEY_FOLLOW_SCREEN, true)

    fun saveFollowScreen(on: Boolean) = prefs.edit().putBoolean(KEY_FOLLOW_SCREEN, on).apply()

    // ---- 语音跟随灵敏度倍率（2.0~6.0）----
    fun getSensitivity(): Float = prefs.getFloat(KEY_SENSITIVITY, 3f)

    fun saveSensitivity(v: Float) = prefs.edit().putFloat(KEY_SENSITIVITY, v).apply()

    // ---- 文稿粘贴历史（JSON 数组，最新在前，最多 30 条）----
    fun getHistory(): List<Pair<Long, String>> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val arr = org.json.JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Pair(o.getLong("t"), o.getString("x"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addHistory(text: String) {
        if (text.isBlank()) return
        val list = getHistory().toMutableList()
        if (list.isNotEmpty() && list[0].second == text) return  // 与最近一条相同不重复记
        list.add(0, Pair(System.currentTimeMillis(), text))
        while (list.size > 30) list.removeAt(list.size - 1)
        val arr = org.json.JSONArray()
        list.forEach { p ->
            arr.put(org.json.JSONObject().put("t", p.first).put("x", p.second))
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "scroll_prompter_settings"
        private const val KEY_TEXT = "prompter_text"
        private const val KEY_FONT_SIZE = "font_size_sp"
        private const val KEY_SCROLL_INTERVAL = "scroll_interval_ms"
        private const val KEY_BRIGHTNESS = "brightness_value"
        private const val KEY_TEXT_COLOR = "text_color_index"
        private const val KEY_FOLLOW_SCREEN = "follow_screen"
        private const val KEY_SENSITIVITY = "mic_sensitivity"
        private const val KEY_HISTORY = "paste_history"
    }
}
