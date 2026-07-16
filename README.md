# 梦林浏览器（Menglin Browser）
此版本由欲蓝浏览器修改，功能一定上缺失，比如ai功能等
## 项目结构

```
menglin-browser/
├── AndroidManifest.xml          # 应用配置
├── build.sh                     # 构建脚本
├── assets/                      # 主页、启动页等静态资源
│   ├── homepage.html
│   └── splash.html
├── java/com/menglin/cn/         # Java 源码
│   ├── MainActivity.java        # 浏览器主界面
│   ├── SettingsActivity.java    # 设置页面
│   ├── HistoryActivity.java     # 历史记录
│   ├── DownloadActivity.java    # 下载列表
│   ├── TabListActivity.java     # 标签页列表
│   ├── TabManager.java          # 标签管理
│   └── SplashActivity.java      # 启动页
├── res/                         # 资源文件
└── README.md                    # 本文件
```

---

## 编译说明

### 环境要求

- Linux 环境
- OpenJDK 17
- Android SDK（build-tools 35.0.0，platform android-34）

### 目录结构准备

确保 Android SDK 位于 `/workspace/android-sdk`，或修改 `build.sh` 中的路径：

```bash
export ANDROID_HOME=/workspace/android-sdk
export BUILD_TOOLS=$ANDROID_HOME/build-tools/35.0.0
export PLATFORM=$ANDROID_HOME/platforms/android-34
```

### 一键构建

```bash
cd /workspace/menglin-browser
bash build.sh
```

构建成功后，APK 输出为：

```
/workspace/menglin-browser/menglin-browser-v1.0.apk
```

---

## 二次开发指南

### 修改包名

1. 将 `java/com/menglin/cn/` 目录重命名为你的包名目录
2. 修改所有 Java 文件顶部的 `package com.menglin.cn;`
3. 修改 `AndroidManifest.xml` 中的 `package` 和 `activity` 的 `android:name`

### 修改应用名

修改 `AndroidManifest.xml` 中的：

```xml
android:label="你的应用名"
```

### 接入自己的 AI 助手

参考 `MainActivity.java` 中的菜单逻辑，在 `showCustomMenu()` 中添加新的菜单项，并实现自己的网络请求。

### 添加版本检查

在 `MainActivity.java` 的 `onCreate()` 中合适位置调用你自己的更新检查接口。

### 添加主页插件 / JS 注入

可以在 `WebViewClient.onPageFinished()` 中通过 `webView.loadUrl("javascript:...")` 注入脚本。

---

## 注意事项

- 本项目使用系统自带 WebView，不同设备上的浏览体验可能略有差异。
- 建议在实际发布前替换应用图标、启动页、主页等资源。
- 开源版本不包含任何后端服务，所有网络功能请自行实现。

---

## 免责声明

本项目仅供学习交流使用。使用本源码产生的任何后果由使用者自行承担。

---

## License

MIT License
