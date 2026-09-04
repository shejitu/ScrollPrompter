package com.agnes.scrollprompter

import android.os.Handler
import android.os.Looper
import android.widget.ScrollView

/**
 * 滚动控制（Handler + Runnable）
 *
 * 按固定步长向下滚动 ScrollView，滚动间隔（速度）可调。
 * 每次改变速度时先 removeCallbacks 再重新 post，避免旧 timer 残留。
 */
class ScrollManager(
    private val scrollView: ScrollView
) {
    private val handler = Handler(Looper.getMainLooper())

    /** 是否正在播放 */
    var isPlaying = false
        private set

    /** 滚动间隔（毫秒），30~800ms 区间 */
    var intervalMs: Long = 80L

    /** 每次滚动的像素步长 */
    var stepPx: Int = 2

    /** 到底/进度回调，参数为当前已滚动像素 */
    var onProgress: ((Int) -> Unit)? = null

    /** 滚动到末尾时的回调 */
    var onFinished: (() -> Unit)? = null

    private val scrollRunnable = object : Runnable {
        override fun run() {
            if (!isPlaying) return
            val maxScroll = scrollView.childCount.takeIf { it > 0 }
                ?.let { scrollView.getChildAt(0).height - scrollView.height }
                ?.coerceAtLeast(0) ?: 0
            val current = scrollView.scrollY

            if (current >= maxScroll) {
                // 已到底：停止并回调
                isPlaying = false
                onProgress?.invoke(current)
                onFinished?.invoke()
                return
            }

            val next = (current + stepPx).coerceAtMost(maxScroll)
            scrollView.scrollTo(0, next)
            onProgress?.invoke(next)
            handler.postDelayed(this, intervalMs)
        }
    }

    /** 开始播放 */
    fun play() {
        if (isPlaying) return
        isPlaying = true
        startTimer()
    }

    /** 暂停 */
    fun pause() {
        isPlaying = false
        stopTimer()
    }

    /** 切换播放/暂停，返回切换后的播放状态 */
    fun toggle(): Boolean {
        if (isPlaying) pause() else play()
        return isPlaying
    }

    /** 重置到顶部 */
    fun reset() {
        pause()
        scrollView.scrollTo(0, 0)
        onProgress?.invoke(0)
    }

    /** 前进一小段（手动） */
    fun stepForward() {
        scrollView.smoothScrollBy(0, 40)
    }

    /** 后退一小段（手动） */
    fun stepBackward() {
        scrollView.smoothScrollBy(0, -40)
    }

    private fun startTimer() {
        stopTimer()
        handler.post(scrollRunnable)
    }

    private fun stopTimer() {
        handler.removeCallbacks(scrollRunnable)
    }
}
