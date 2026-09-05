package com.agnes.scrollprompter

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

/**
 * 麦克风音量检测引擎（语音跟随停顿的核心）
 *
 * 用 AudioRecord 持续采集 PCM，计算 RMS 音量并回调。
 * 不做语音识别——只判断「是否在说话」：
 *   音量持续高于阈值 → 朗读中 → 提词滚动
 *   静默超过一段时间 → 停顿 → 暂停滚动
 *
 * 自适应阈值：噪声底缓慢跟随环境音，朗读音量一般为噪声底的数倍。
 */
class MicManager {

    /** 音量回调（归一化 RMS，0~1，约每 64ms 一次） */
    var onLevel: ((Float) -> Unit)? = null

    private var audioRecord: AudioRecord? = null
    @Volatile private var running = false
    private var thread: Thread? = null

    /** 噪声底（自适应，缓慢跟随环境） */
    @Volatile var noiseFloor: Float = 0.02f
        private set

    /** 灵敏度倍率：越大越难触发（2.0 最灵敏 ~ 6.0 最迟钝），设置面板可调 */
    var thresholdMultiplier: Float = 3f

    /** 朗读判定阈值 = 噪声底 × 灵敏度倍率 + 底限 */
    fun speechThreshold(): Float = noiseFloor * thresholdMultiplier + 0.015f

    @SuppressLint("MissingPermission") // 调用前已确保授予 RECORD_AUDIO
    fun start(): Boolean {
        if (running) return true
        val sampleRate = 16000
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf <= 0) return false
        return try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                minBuf * 2
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                return false
            }
            audioRecord = record
            running = true
            thread = Thread {
                val buf = ShortArray(1024)
                record.startRecording()
                while (running) {
                    val n = record.read(buf, 0, buf.size)
                    if (n > 0) {
                        var sum = 0.0
                        for (i in 0 until n) sum += buf[i] * buf[i].toDouble()
                        val rms = (kotlin.math.sqrt(sum / n) / 32767.0).toFloat()
                        // 噪声底：向下快速跟踪、向上极慢恢复（只被持续响声抬高）
                        noiseFloor = if (rms < noiseFloor) {
                            noiseFloor * 0.7f + rms * 0.3f
                        } else {
                            noiseFloor + (rms - noiseFloor) * 0.002f
                        }
                        onLevel?.invoke(rms)
                    }
                }
                record.stop()
            }.also { it.start() }
            true
        } catch (e: Exception) {
            running = false
            false
        }
    }

    fun stop() {
        running = false
        thread?.join(500)
        thread = null
        audioRecord?.release()
        audioRecord = null
    }

    fun isRunning(): Boolean = running
}
