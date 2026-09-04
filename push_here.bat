@echo off
chcp 65001 >nul
REM ============================================================
REM  滚动提示器 · 一键推送到 GitHub（双击运行）
REM  注意：请在“能上网的本机”双击此文件，不要在 WorkBuddy 终端里跑
REM        （WorkBuddy 沙箱禁了外网，会连不上 GitHub）。
REM  前置：已在 GitHub 新建空仓库 ScrollPrompter，并已生成 PAT。
REM ============================================================
cd /d "%~dp0"

echo.
echo 即将推送到： https://github.com/shejitu/ScrollPrompter.git
echo 用户名请输入： shejitu
echo 密码处请“粘贴”你的 GitHub Personal Access Token（PAT）
echo   —— 粘贴时屏幕不显示任何字符，属正常现象，直接回车即可。
echo.

git push -u origin main

echo.
if errorlevel 1 (
  echo [失败] 常见原因：PAT 无效/权限不足，或仓库名不对/远程地址未配置。
  echo   1) 确认已在 GitHub 创建同名仓库 ScrollPrompter（空仓库，不要勾 README）
  echo   2) 重新生成带 repo 权限的 PAT 再试。
) else (
  echo [成功] 已推送！去 https://github.com/shejitu/ScrollPrompter/actions 看自动构建。
  echo   构建完成后进入该次构建 → 底部 Artifacts 下载 ScrollPrompter-APK。
)
echo.
pause
