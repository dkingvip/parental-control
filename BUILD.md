# 一键编译 APK

## 方法 1：使用批处理脚本（推荐）

### 前提条件
1. 安装 [Android Studio](https://developer.android.com/studio)（安装时自动配置 SDK）
2. 首次打开 Android Studio 时完成 SDK 下载

### 编译步骤

1. **双击运行** `build-apk.bat`
2. 等待自动编译（首次约 10-20 分钟，需要下载依赖）
3. 编译完成后，在同目录下找到：
   - `ChildApp-debug.apk` - 儿童端
   - `ParentApp-debug.apk` - 家长端

### 安装到设备

**方式 A：USB 安装**
```bash
# 连接手机/平板，开启 USB 调试
adb install ChildApp-debug.apk
adb install ParentApp-debug.apk
```

**方式 B：手动安装**
1. 把 APK 文件传到手机/平板（微信、QQ、数据线）
2. 在设备上点击 APK 文件
3. 允许安装未知来源应用
4. 完成安装

---

## 方法 2：使用 Android Studio GUI

如果批处理脚本失败，使用 Android Studio 图形界面：

### 编译儿童端
1. 打开 Android Studio
2. File → Open → 选择 `ChildApp` 文件夹
3. 等待 Gradle 同步完成
4. Build → Build Bundle(s) / APK(s) → Build APK(s)
5. 点击右下角弹出的 "locate" 找到 APK

### 编译家长端
同上，选择 `ParentApp` 文件夹

---

## 方法 3：使用 GitHub Actions（无需本地环境）

如果没有 Android 开发环境，可以用 GitHub 免费编译：

1. 把项目上传到 GitHub 仓库
2. 我已为你配置好 `.github/workflows/build.yml`
3. 每次推送代码，GitHub 自动编译 APK
4. 在 Actions 页面下载编译好的 APK

---

## 常见问题

### Q: 提示 "找不到 Android SDK"
> 先打开 Android Studio 一次，让它自动下载 SDK

### Q: Gradle 下载很慢/失败
> 修改 `gradle/wrapper/gradle-wrapper.properties`：
> ```
> distributionUrl=https\://mirrors.cloud.tencent.com/gradle/gradle-8.0-bin.zip
> ```

### Q: 编译报错 "VPNService"
> 这是正常的，儿童端需要真机测试，但不影响编译 APK

### Q: 安装时提示 "解析包错误"
> 确保设备 Android 版本 >= 8.0 (API 26)

---

## 文件说明

| 文件 | 说明 |
|------|------|
| `ChildApp-debug.apk` | 儿童平板端（被控端）|
| `ParentApp-debug.apk` | 家长手机端（控制端）|
| `build-apk.bat` | Windows 一键编译脚本 |
| `build-apk.ps1` | PowerShell 编译脚本 |
