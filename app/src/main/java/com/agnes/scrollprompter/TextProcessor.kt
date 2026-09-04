package com.agnes.scrollprompter

/**
 * 文本处理工具类
 * 负责文本规范化、分行、以及（后续迭代的）高亮区间计算。
 *
 * 当前版本（1.0）仅提供基础规范化与统计能力，
 * 高亮着色、语音匹配定位等能力在后续版本迭代中接入。
 */
object TextProcessor {

    /** 规范化文本：统一换行符、去除首尾空白 */
    fun normalize(text: String): String {
        return text
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .trim()
    }

    /** 按行拆分（供后续高亮/行定位使用） */
    fun splitByLine(text: String): List<String> {
        return normalize(text).split("\n")
    }

    /** 计算总行数 */
    fun lineCount(text: String): Int = splitByLine(text).size

    /** 是否为空内容 */
    fun isEmpty(text: String): Boolean = normalize(text).isEmpty()
}
