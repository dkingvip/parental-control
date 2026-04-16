@echo off
chcp 65001 >nul
echo ==========================================
echo   家长控制 App 一键编译脚本
echo ==========================================
echo.

:: 检查 Android Studio 是否安装
if exist "%LOCALAPPDATA%\Android\Sdk" (
    set "ANDROID_SDK=%LOCALAPPDATA%\Android\Sdk"
) else if exist "C:\Users\%USERNAME%\AppData\Local\Android\Sdk" (
    set "ANDROID_SDK=C:\Users\%USERNAME%\AppData\Local\Android\Sdk"
) else (
    echo [错误] 未找到 Android SDK！
    echo 请先安装 Android Studio：https://developer.android.com/studio
    pause
    exit /b 1
)

echo [✓] 找到 Android SDK: %ANDROID_SDK%
set "ANDROID_HOME=%ANDROID_SDK%"

:: 查找 gradlew
set "PROJECT_DIR=%~dp0"
cd /d "%PROJECT_DIR%"

if not exist "ChildApp\gradlew" (
    echo [错误] 找不到 ChildApp 项目！
    echo 请确保此脚本在 parental-control 文件夹内
    pause
    exit /b 1
)

echo.
echo ==========================================
echo   正在编译 儿童端 App (ChildApp)
echo ==========================================
cd ChildApp
call gradlew.bat assembleDebug --no-daemon

if %ERRORLEVEL% neq 0 (
    echo [错误] 儿童端编译失败！
    pause
    exit /b 1
)

echo [✓] 儿童端编译成功！
copy "app\build\outputs\apk\debug\app-debug.apk" "..\ChildApp-debug.apk" >nul
cd ..

echo.
echo ==========================================
echo   正在编译 家长端 App (ParentApp)
echo ==========================================
cd ParentApp
call gradlew.bat assembleDebug --no-daemon

if %ERRORLEVEL% neq 0 (
    echo [错误] 家长端编译失败！
    pause
    exit /b 1
)

echo [✓] 家长端编译成功！
copy "app\build\outputs\apk\debug\app-debug.apk" "..\ParentApp-debug.apk" >nul
cd ..

echo.
echo ==========================================
echo   编译完成！
echo ==========================================
echo.
echo APK 文件位置：
echo   - 儿童端: %PROJECT_DIR%ChildApp-debug.apk
echo   - 家长端: %PROJECT_DIR%ParentApp-debug.apk
echo.
echo 安装方法：
echo   1. 把 APK 传到手机/平板
echo   2. 允许安装未知来源应用
echo   3. 点击安装
echo.
pause
