# 滚动提示器（ScrollPrompter）

一款 Android 平板用的提词器 App：把文稿放大显示，按可调速度自动向上滚动，适合演讲、录制、直播提词。

## 当前版本 v1.0（基础版）

已实现功能（P0）：

- ✅ 字体大小调节（24~90sp，SeekBar 实时预览）
- ✅ 滚动速度调节（30ms~800ms 区间）
- ✅ 字体样式跟随系统默认 Typeface
- ✅ 屏幕亮度 4 档循环切换（自动 / 低 / 中 / 高）
- ✅ 播放 / 暂停大按钮
- ✅ 文稿编辑与本地持久化（SharedPreferences）
- ✅ 点击文字区左/右半屏快速前进/后退
- ✅ 全屏沉浸式 + 保持屏幕常亮

## 后续迭代计划（P1/P2）

- 离线语音识别同步（SpeechRecognizer + 文本匹配）
- 高亮当前阅读位置（SpannableString 动态着色）
- 语音识别结果置信度过滤
- 定时提醒、多页管理、蓝牙遥控等

## 技术栈

- Kotlin + Android（View + ViewBinding）
- minSdk 21 / targetSdk 34 / compileSdk 34
- AGP 8.5.2 + Gradle 8.7

## 构建（GitHub Actions）

推送到 `main` 分支自动构建；也可在 Actions 页手动 Run workflow。
构建成功后，在对应构建的 Artifacts 下载 `ScrollPrompter-APK`。
