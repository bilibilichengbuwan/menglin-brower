# 梦林浏览器开源空壳版魔改说明

## 一、项目概述

本项目将原「欲蓝浏览器」改造为开源空壳版「梦林浏览器」，作为基础浏览器框架发布。

- **包名**：`com.menglin.cn`
- **应用名**：梦林浏览器
- **版本**：`versionCode=1`，`versionName=1.0`，构建脚本中 `VERSION="v1.0"`
- **目标**：保留浏览器核心框架（WebView、地址栏、标签页、历史记录、下载、收藏夹、设置），移除 AI、插件、更新检查、外链网盘上传、欲蓝安全等隐私/非开源功能。

## 二、已移除的功能列表

| 功能 | 说明 |
|------|------|
| AI 助手 / 翻译网页 / 网页内容总结 | 删除 `callAI()`、`translateWebPage()`、`summarizeWebPage()` 及相关调用 |
| 插件扩展系统 | 删除 `PluginActivity`、字段 `cachedPluginJs` / `pluginsEnabled`、方法 `reloadPlugins()` |
| 欲蓝安全 / URL 安全检查 | 删除 `showSecurityDialog()`、`checkUrlSafety()` 等安全弹窗与拦截逻辑 |
| 检查更新 / 自动更新 | 删除 `checkForUpdate()`、`showUpdateDialog()`、启动时自动检查更新调用 |
| 彩虹外链网盘上传 | 删除 `build.sh` 中的上传逻辑与 curl 调用 |
| 主页 AI 快捷入口 | 删除 `assets/homepage.html` 中的 AI 列表区块与相关样式 |
| 底部工具栏盾牌按钮 | 删除安全按钮，仅保留返回、前进、主页、标签页、菜单 |

## 三、保留的核心功能列表

- WebView 网页浏览（前进/后退/刷新/主页）
- 地址栏搜索（支持搜索引擎设置）
- 多标签页管理（`TabManager` + `TabListActivity`）
- 历史记录（`HistoryActivity`）
- 下载列表与系统下载器（`DownloadActivity`、`TabDownloadListener`）
- 收藏夹（主页对话框中添加/删除）
- 设置：`主页`、`搜索引擎`、`深色模式`、`无图模式`、`JavaScript`、`Cookie`、`缓存`、`文字大小`、`浏览器标识(UA)`、`清除数据` 等

## 四、文件修改说明

### 1. `/workspace/menglin-browser/AndroidManifest.xml`
- `package="com.menglin.cn"`
- `android:label="梦林浏览器"`
- `android:versionCode="1"`、`android:versionName="1.0"`
- 移除 `PluginActivity` 声明

### 2. `/workspace/menglin-browser/java/com/menglin/cn/MainActivity.java`
- 包名已改为 `com.menglin.cn`
- 删除字段：`AI_PROXY_URL`、`currentModelFlag`、`UPDATE_CHECK_URL`、`APP_VERSION_CODE`、`cachedPluginJs`、`pluginsEnabled`、`homepageRefreshReceiver`
- 删除方法：`showSecurityDialog()`、`checkUrlSafety()`、`callAI()`、`reloadPlugins()`、`checkForUpdate()`、`showUpdateDialog()`
- 简化 `showCustomMenu()` 菜单项为 `{历史记录, 下载列表, 设置}`，移除翻译/总结/安全/更新入口
- 底部工具栏移除盾牌安全按钮
- `onCreate` 中移除启动自动检查更新
- `onPause` 中移除 `homepageRefreshReceiver` 注册/注销
- `WebViewClient` 中移除插件 JS 注入
- 下载流程中移除 `isDownloadSafe` / `showDownloadSecurityWarning`，直接调用系统下载
- 最终约 **1995 行**

### 3. `/workspace/menglin-browser/java/com/menglin/cn/SettingsActivity.java`
- 删除 `KEY_PLUGINS_ENABLED` 常量与「插件扩展」开关
- 删除 AI、插件、更新、安全相关设置项
- 「关于」版本显示改为 `1.0`
- 保留：搜索引擎、主页、深色模式、文字大小、JS、图片、Cookie、缓存、UA、清除数据等

### 4. `/workspace/menglin-browser/java/com/menglin/cn/TabManager.java`
- 仅保留 `Tab` 基本结构：`id`、`webView`、`title`、`url`、`isPrivate`、`thumbnail`
- 无 `pluginEnabled` / `pluginJs` 字段

### 5. `/workspace/menglin-browser/build.sh`
- `PROJ=/workspace/menglin-browser`
- `VERSION="v1.0"`
- 输出 APK：`menglin-browser-v1.0.apk`
- 删除彩虹外链网盘上传逻辑
- 保留资源编译、Java 编译、dex、打包、对齐、签名流程

### 6. `/workspace/menglin-browser/res/values/strings.xml`
- `<string name="app_name">梦林浏览器</string>`

### 7. `/workspace/menglin-browser/assets/splash.html`
- 标题、名称改为「梦林浏览器」
- 副标题改为 `Menglin Browser`
- Slogan 改为「开源 · 简洁 · 快速」

### 8. `/workspace/menglin-browser/assets/homepage.html`
- 标题改为「梦林浏览器」
- 删除「多 AI 快捷入口」HTML 区块、CSS 样式与渲染脚本
- 保留搜索框、快捷方式、主页背景设置等核心功能

### 9. `/workspace/menglin-browser/assets/ai-assistant.html`
- 当前版本不再使用，仍保留在源码中；如需要完全清理可直接删除该文件。

## 五、如何二次开发

### 5.1 加回 AI 功能
1. 在 `MainActivity.java` 中恢复 `AI_PROXY_URL`、`currentModelFlag` 等字段。
2. 恢复 `callAI()`、`translateWebPage()`、`summarizeWebPage()` 方法。
3. 在 `HomepageBgInterface` 中恢复 `openAIAssistant()` / `chatWithAI()` / `processUrls()` 等 `@JavascriptInterface` 方法。
4. 在 `assets/homepage.html` 中恢复 AI 入口区块与 `AI_LIST` 渲染脚本。
5. 如需完整 AI 聊天页，重新接入 `assets/ai-assistant.html`。

### 5.2 加回更新检查
1. 恢复 `UPDATE_CHECK_URL`、`APP_VERSION_CODE` 常量。
2. 恢复 `checkForUpdate(boolean)` 与 `showUpdateDialog()` 方法。
3. 在 `onCreate()` 合适位置调用 `checkForUpdate(false)`。
4. 如需自动安装，保留 `REQUEST_INSTALL_PACKAGES` 权限。

### 5.3 加回插件系统
1. 恢复 `PluginActivity.java` 并在 `AndroidManifest.xml` 中声明。
2. 恢复 `cachedPluginJs`、`pluginsEnabled` 字段与 `reloadPlugins()` 方法。
3. 在 `WebViewClient.onPageFinished()` 中注入插件 JS。
4. 在 `SettingsActivity` 中添加「插件扩展」开关。

### 5.4 加回 URL 安全/网站安全
1. 恢复 `showSecurityDialog()`、`checkUrlSafety()` 等方法。
2. 在 `WebViewClient.shouldOverrideUrlLoading()` 或 `onPageStarted()` 中调用安全检查。
3. 如需下载安全，恢复 `isDownloadSafe` 与 `showDownloadSecurityWarning()`。

### 5.5 改名注意事项
- 如再次修改包名，需同步修改 `AndroidManifest.xml` 中所有 `android:name`。
- 主页 JS 接口名为 `YulanApp`，如需统一品牌可改名为 `MenglinApp`，并同步修改 `assets/homepage.html` / `error.html` / `snake.html` 中的调用。
- SharedPreferences 键名（如 `YulanBrowserPrefs`、`YulanBrowserHistory`）可按需重命名。

## 六、编译说明

### 环境要求
- Android SDK：`/workspace/android-sdk`
  - `build-tools/35.0.0`
  - `platforms/android-34`
- JDK 17：`/usr/lib/jvm/java-17-openjdk-amd64`

### 编译步骤

```bash
cd /workspace/menglin-browser
bash build.sh
```

### 输出
- APK：`/workspace/menglin-browser/menglin-browser-v1.0.apk`
- 调试签名密钥：首次构建会自动生成 `/workspace/menglin-browser/debug.keystore`

### 打包源码
```bash
cd /workspace
zip -r menglin-browser-open-source.zip menglin-browser \
  -x "menglin-browser/build/*" \
  -x "menglin-browser/*.apk" \
  -x "menglin-browser/debug.keystore" \
  -x "menglin-browser/*.idsig"
```

---

如有进一步定制需求，建议先阅读 `MainActivity.java` 中的 `TabClient`、`TabChromeClient`、`TabDownloadListener` 以及 `SettingsActivity.java` 的设置项实现，再按需扩展。
