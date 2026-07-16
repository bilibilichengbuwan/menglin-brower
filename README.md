# 梦林浏览器（Menglin Browser）

此版本由欲蓝浏览器修改而来，部分功能有所缺失，例如 AI 助手等。

一个基于 Android WebView 的轻量级开源浏览器空壳框架。

> 本项目由「欲蓝浏览器」剥离隐私相关功能后开源，仅保留浏览器核心框架，适合作为浏览器类 App 的二次开发起点。

---

## 基本信息

- **包名**：`com.menglin.cn`
- **应用名**：梦林浏览器
- **版本**：`1.0`（`versionCode=1`）
- **最低 SDK**：21（Android 5.0）
- **目标 SDK**：34（Android 14）

---

## 功能特性

### 已保留的核心功能

- 🌐 网页浏览（前进 / 后退 / 刷新 / 主页）
- 🔍 地址栏搜索，支持自定义搜索引擎
- 📑 多标签页管理
- 📜 历史记录
- ⬇️ 文件下载
- 🔖 收藏夹
- ⚙️ 基础设置（主页、搜索引擎、深色模式、无图模式、JS、Cookie、缓存、文字大小、UA 等）

### 已移除的功能

为保证开源版本不包含隐私或闭源服务依赖，以下功能已被移除：

- AI 助手（翻译、总结、问答）
- 插件扩展系统
- 欲蓝安全 / URL 安全检测 / 下载安全拦截
- 自动更新 / 版本检查
- 服务器 API 调用
- 第三方网盘上传逻辑

> 如果你需要这些功能，可以参考源码自行接入自己的后端服务。

---

## 项目结构

```
menglin-browser/
├── AndroidManifest.xml          # 应用配置
├── build.sh                     # 构建脚本
├── assets/                      # 主页、启动页等静态资源
│   ├── ai-assistant.html
│   ├── error.html
│   ├── homepage.html
│   ├── snake.html
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
├── gen_icon.py                  # 图标生成脚本
├── icon_source.jpg              # 图标源文件
├── icon_source.png              # 图标源文件
└── README.md                    # 本文件
```

---

## 编译说明

### 环境要求

- Linux 环境
- OpenJDK 17 或更高版本
- Android SDK（build-tools 35.0.0，platform android-34）

### 1. 安装 Android SDK

如果你的系统还没有 Android SDK，可以通过命令行工具安装：

```bash
# 创建 SDK 目录
mkdir -p /workspace/android-sdk/cmdline-tools
cd /workspace/android-sdk/cmdline-tools

# 下载命令行工具（以 11076708 版本为例，可到官网查找最新版）
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mv cmdline-tools latest

# 安装所需组件
export ANDROID_HOME=/workspace/android-sdk
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --sdk_root=$ANDROID_HOME \
  "build-tools;35.0.0" \
  "platforms;android-34"
```

> 你可以把 SDK 放到任意位置，只需要设置好 `ANDROID_HOME` 环境变量，或者修改 `build.sh` 中的默认路径。

### 2. 配置环境变量

`build.sh` 会自动检测 `JAVA_HOME` 和 `ANDROID_HOME` 环境变量。如果未设置，则使用以下默认值：

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=/workspace/android-sdk
```

如果你的 Java 或 Android SDK 不在默认路径，请在运行构建前设置：

```bash
export JAVA_HOME=/你的/java/路径
export ANDROID_HOME=/你的/android-sdk/路径
```

### 3. 一键构建

```bash
# 进入项目目录（根据你实际的克隆路径调整）
cd /path/to/menglin-browser

# 运行构建脚本
bash build.sh
```

构建成功后，APK 输出为：

```
menglin-browser-v1.0.apk
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
