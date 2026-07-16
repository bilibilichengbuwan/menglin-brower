package com.menglin.cn;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.webkit.CookieManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends Activity {

    public static final String PREFS_NAME = "YulanBrowserPrefs";

    public static final String KEY_SEARCH_ENGINE = "search_engine";
    public static final String KEY_HOMEPAGE = "homepage";
    public static final String KEY_TEXT_ZOOM = "text_zoom";
    public static final String KEY_JS_ENABLED = "js_enabled";
    public static final String KEY_IMAGES_ENABLED = "images_enabled";
    public static final String KEY_COOKIES_ENABLED = "cookies_enabled";
    public static final String KEY_CACHE_ENABLED = "cache_enabled";
    public static final String KEY_AUTO_FIT = "auto_fit";
    public static final String KEY_UA_MODE = "ua_mode";
    public static final String KEY_SPLASH_ENABLED = "splash_enabled";
    public static final String KEY_DARK_MODE = "dark_mode";
    public static final String KEY_LOCATION = "location_enabled";
    public static final String KEY_BLOCK_POPUP = "block_popup";
    public static final String KEY_HOMEPAGE_BG = "homepage_bg";

    public static final String[] SEARCH_ENGINES = {
            "百度", "必应", "Google", "搜狗", "360搜索", "DuckDuckGo", "Yandex"
    };
    public static final String[] SEARCH_URLS = {
            "https://www.baidu.com/s?wd=",
            "https://www.bing.com/search?q=",
            "https://www.google.com/search?q=",
            "https://www.sogou.com/web?query=",
            "https://www.so.com/s?q=",
            "https://duckduckgo.com/?q=",
            "https://yandex.com/search/?text="
    };

    public static final String[] HOMEPAGE_LABELS = {
            "自定义", "百度", "必应", "Google", "自定义主页", "空白页"
    };
    public static final String[] HOMEPAGE_URLS = {
            "",
            "https://www.baidu.com/",
            "https://www.bing.com/",
            "https://www.google.com/",
            "file:///android_asset/homepage.html",
            "about:blank"
    };

    public static final String[] ZOOM_LABELS = {"极小", "小", "中等", "大", "很大", "特大"};
    public static final int[] ZOOM_VALUES = {75, 85, 100, 115, 130, 150};

    public static final String[] UA_LABELS = {"默认", "桌面", "iPhone", "Android", "iPad"};
    public static final String[] UA_STRINGS = {
            "",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15"
    };

    public static final String[] DARK_MODE_LABELS = {"跟随系统", "浅色", "深色"};

    private SharedPreferences prefs;
    private int dp;
    private boolean dark;
    private int bgColor;
    private int cardColor;
    private int titleColor;
    private int subColor;
    private int groupColor;
    private int accentColor;
    private int dividerColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 沉浸式状态栏
        try {
            Window w = getWindow();
            w.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
            w.setStatusBarColor(android.graphics.Color.TRANSPARENT);
            w.setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        } catch (Throwable t) {}

        try {
            prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        } catch (Throwable t) {
            prefs = null;
        }

        dp = (int) getResources().getDisplayMetrics().density;

        // 深色模式
        int darkMode = 0;
        try { darkMode = prefs.getInt(KEY_DARK_MODE, 0); } catch (Throwable t) {}
        dark = false;
        if (darkMode == 2) dark = true;
        else if (darkMode == 0) {
            int ui = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            dark = (ui == Configuration.UI_MODE_NIGHT_YES);
        }

        // Chrome风格配色：简洁卡片
        bgColor = dark ? 0xFF111111 : 0xFFF8F9FA;  // 页面背景（深色很深，浅色很淡）
        cardColor = dark ? 0xFF1E1E1E : 0xFFFFFFFF;  // 卡片白色
        titleColor = dark ? 0xFFFFFFFF : 0xFF202124;  // 标题文字（深灰黑）
        subColor = dark ? 0xFF9AA0A6 : 0xFF5F6368;    // 副标题（灰）
        groupColor = dark ? 0xFF8AB4F8 : 0xFF1A73E8;  // 分组标题（蓝色）
        accentColor = 0xFF1A73E8;                       // Google Blue
        dividerColor = dark ? 0xFF2A2A2A : 0xFFE8EAED;  // 细分隔线

        // 根布局：ScrollView + 垂直LinearLayout
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(bgColor);
        scroll.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

        // 状态栏占位（让内容从状态栏下开始）
        try {
            int statusBarHeight = 0;
            int rid = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (rid > 0) statusBarHeight = getResources().getDimensionPixelSize(rid);
            if (statusBarHeight <= 0) statusBarHeight = (int)(25 * dp);
            View statusSpacer = new View(this);
            statusSpacer.setLayoutParams(new LinearLayout.LayoutParams(-1, statusBarHeight));
            root.addView(statusSpacer);
        } catch (Throwable t) {}

        // ========== 顶部标题栏（Chrome风格：大标题+细描述） ==========
        LinearLayout headerBar = new LinearLayout(this);
        headerBar.setOrientation(LinearLayout.VERTICAL);
        headerBar.setPadding(24 * dp, 16 * dp, 16 * dp, 20 * dp);
        // 大标题
        TextView bigTitle = new TextView(this);
        bigTitle.setText("设置");
        bigTitle.setTextColor(titleColor);
        bigTitle.setTextSize(28);
        bigTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        headerBar.addView(bigTitle);
        // 副标题
        TextView subTitle = new TextView(this);
        subTitle.setText("梦林浏览器");
        subTitle.setTextColor(subColor);
        subTitle.setTextSize(13);
        subTitle.setPadding(0, 4 * dp, 0, 0);
        headerBar.addView(subTitle);
        root.addView(headerBar);

        // ========== 你和浏览器 ==========
        addSectionHeader(root, "你和浏览器");
        addChromeCard(root, new SettingsBuilder() {
            @Override public void build(LinearLayout card) {
                addChromeSwitch(card, "JavaScript", "允许网站运行 JavaScript（推荐开启）", KEY_JS_ENABLED, 1);
                addChromeDivider(card);
                addChromeSwitch(card, "显示图片", "加载并显示网页中的图片", KEY_IMAGES_ENABLED, 1);
                addChromeDivider(card);
                addChromeSwitch(card, "Cookie", "允许网站保存和读取 Cookie 数据", KEY_COOKIES_ENABLED, 1);
                addChromeDivider(card);
                addChromeSwitch(card, "缓存", "使用缓存提升加载速度", KEY_CACHE_ENABLED, 1);
                addChromeDivider(card);
                addChromeSwitch(card, "阻止弹窗", "阻止网站自动弹出窗口", KEY_BLOCK_POPUP, 1);
                addChromeDivider(card);
                addChromeSwitch(card, "位置信息", "允许网站请求你的位置（默认关闭）", KEY_LOCATION, 0);
            }
        });

        // ========== 外观 ==========
        addSectionHeader(root, "外观");
        addChromeCard(root, new SettingsBuilder() {
            @Override public void build(LinearLayout card) {
                addChromePicker(card, "主题", DARK_MODE_LABELS[getIdx(prefs, KEY_DARK_MODE, 0)], new Runnable() {
                    @Override public void run() { showChromeDialog("主题", DARK_MODE_LABELS, KEY_DARK_MODE); }
                });
                addChromeDivider(card);
                addChromePicker(card, "文字大小", ZOOM_LABELS[findZoom(prefs, KEY_TEXT_ZOOM, 100)], new Runnable() {
                    @Override public void run() { showChromeDialog("文字大小", ZOOM_LABELS, KEY_TEXT_ZOOM); }
                });
                addChromeDivider(card);
                addChromeSwitch(card, "自适应屏幕", "按屏幕宽度自动调整网页布局（推荐开启）", KEY_AUTO_FIT, 1);
            }
        });

        // ========== 搜索引擎与主页 ==========
        addSectionHeader(root, "搜索引擎与主页");
        addChromeCard(root, new SettingsBuilder() {
            @Override public void build(LinearLayout card) {
                addChromePicker(card, "默认搜索引擎", SEARCH_ENGINES[getIdx(prefs, KEY_SEARCH_ENGINE, 0)], new Runnable() {
                    @Override public void run() { showChromeDialog("搜索引擎", SEARCH_ENGINES, KEY_SEARCH_ENGINE); }
                });
                addChromeDivider(card);
                addChromePicker(card, "主页", HOMEPAGE_LABELS[getIdx(prefs, KEY_HOMEPAGE, 4)], new Runnable() {
                    @Override public void run() { showChromeDialog("主页", HOMEPAGE_LABELS, KEY_HOMEPAGE); }
                });
                addChromeDivider(card);
                addChromeAction(card, "自定义主页网址", new Runnable() {
                    @Override public void run() { showHomepageDialog(); }
                });
                addChromeDivider(card);
                addChromeAction(card, "选择主页背景图片", new Runnable() {
                    @Override public void run() { pickHomepageBg(); }
                });
                addChromeDivider(card);
                addChromeAction(card, "清除主页背景", new Runnable() {
                    @Override public void run() {
                        try { prefs.edit().remove(KEY_HOMEPAGE_BG).apply(); Toast.makeText(SettingsActivity.this, "已清除", Toast.LENGTH_SHORT).show(); } catch (Throwable t) {}
                    }
                });
            }
        });

        // ========== 高级 ==========
        addSectionHeader(root, "高级");
        addChromeCard(root, new SettingsBuilder() {
            @Override public void build(LinearLayout card) {
                addChromePicker(card, "浏览器标识 (UA)", UA_LABELS[getIdx(prefs, KEY_UA_MODE, 0)], new Runnable() {
                    @Override public void run() { showChromeDialog("浏览器标识", UA_LABELS, KEY_UA_MODE); }
                });
            }
        });

        // ========== 隐私和安全 ==========
        addSectionHeader(root, "隐私和安全");
        addChromeCard(root, new SettingsBuilder() {
            @Override public void build(LinearLayout card) {
                addChromeAction(card, "清除浏览缓存", new Runnable() {
                    @Override public void run() {
                        try {
                            android.webkit.WebView w = new android.webkit.WebView(SettingsActivity.this);
                            w.clearCache(true);
                            w.destroy();
                            Toast.makeText(SettingsActivity.this, "缓存已清除", Toast.LENGTH_SHORT).show();
                        } catch (Throwable t) {
                            Toast.makeText(SettingsActivity.this, "清除失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                addChromeDivider(card);
                addChromeAction(card, "清除 Cookie", new Runnable() {
                    @Override public void run() {
                        try {
                            CookieManager.getInstance().removeAllCookies(null);
                            Toast.makeText(SettingsActivity.this, "Cookie 已清除", Toast.LENGTH_SHORT).show();
                        } catch (Throwable t) {
                            Toast.makeText(SettingsActivity.this, "清除失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                addChromeDivider(card);
                addChromeAction(card, "恢复默认设置", new Runnable() {
                    @Override public void run() {
                        new AlertDialog.Builder(SettingsActivity.this)
                                .setTitle("确认恢复")
                                .setMessage("确定要将所有设置恢复为默认值吗？")
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override public void onClick(DialogInterface dialog, int which) {
                                        try { prefs.edit().clear().apply(); } catch (Throwable t) {}
                                        Toast.makeText(SettingsActivity.this, "已恢复默认设置", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton("取消", null)
                                .show();
                    }
                });
            }
        });

        // ========== 关于 ==========
        addSectionHeader(root, "关于");
        addChromeCard(root, new SettingsBuilder() {
            @Override public void build(LinearLayout card) {
                addChromeInfo(card, "版本", "1.0");
                addChromeDivider(card);
                addChromeInfo(card, "内核", "Android System WebView");
            }
        });

        // 底部边距
        View bottomSpacer = new View(this);
        bottomSpacer.setLayoutParams(new LinearLayout.LayoutParams(-1, 40 * dp));
        root.addView(bottomSpacer);

        scroll.addView(root);
        setContentView(scroll);
    }

    // ========== Chrome风格UI构建方法 ==========

    // 设置构建器
    private interface SettingsBuilder {
        void build(LinearLayout card);
    }

    // 分组标题
    private void addSectionHeader(LinearLayout root, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(groupColor);
        tv.setTextSize(14);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(24 * dp, 24 * dp, 16 * dp, 8 * dp);
        root.addView(tv);
    }

    // 卡片容器
    private void addChromeCard(LinearLayout root, SettingsBuilder builder) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(cardColor);
        bg.setCornerRadius(12 * dp);
        card.setBackgroundDrawable(bg);
        card.setPadding(16 * dp, 4 * dp, 16 * dp, 4 * dp);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
        cardLp.leftMargin = 16 * dp;
        cardLp.rightMargin = 16 * dp;
        cardLp.topMargin = 0;
        cardLp.bottomMargin = 0;
        root.addView(card, cardLp);

        builder.build(card);
    }

    // 细分隔线
    private void addChromeDivider(LinearLayout card) {
        View div = new View(this);
        div.setBackgroundColor(dividerColor);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, (int)(dp * 0.6f));
        lp.leftMargin = 56 * dp;
        card.addView(div, lp);
    }

    // 开关项（Switch风格）
    private void addChromeSwitch(LinearLayout card, String title, String desc, final String key, final int defValue) {
        // 水平布局
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, 12 * dp, 0, 12 * dp);

        // 左侧文字容器
        LinearLayout textWrap = new LinearLayout(this);
        textWrap.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, -2, 1f);
        textLp.rightMargin = 12 * dp;
        row.addView(textWrap, textLp);

        TextView titleTv = new TextView(this);
        titleTv.setText(title);
        titleTv.setTextColor(titleColor);
        titleTv.setTextSize(15);
        textWrap.addView(titleTv);

        TextView descTv = new TextView(this);
        descTv.setText(desc);
        descTv.setTextColor(subColor);
        descTv.setTextSize(12);
        descTv.setPadding(0, 2 * dp, 0, 0);
        textWrap.addView(descTv);

        // 右侧Switch
        final android.widget.Switch sw = new android.widget.Switch(this);
        int cur = 0;
        try { cur = prefs.getInt(key, defValue); } catch (Throwable t) {}
        sw.setChecked(cur == 1);
        sw.setTextColor(accentColor);
        sw.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    int curVal = prefs.getInt(key, defValue);
                    int newVal = curVal == 1 ? 0 : 1;
                    prefs.edit().putInt(key, newVal).apply();
                    sw.setChecked(newVal == 1);
                } catch (Throwable t) {}
            }
        });
        LinearLayout.LayoutParams swLp = new LinearLayout.LayoutParams(-2, -2);
        row.addView(sw, swLp);

        // 点击整行也能切换
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                sw.performClick();
            }
        });

        card.addView(row);
    }

    // 选择器项（类似Chrome的单行选择）
    private void addChromePicker(LinearLayout card, String title, String currentValue, Runnable onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, 14 * dp, 0, 14 * dp);

        // 标题
        TextView titleTv = new TextView(this);
        titleTv.setText(title);
        titleTv.setTextColor(titleColor);
        titleTv.setTextSize(15);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-2, -2);
        titleLp.weight = 1.0f;
        row.addView(titleTv, titleLp);

        // 当前值
        TextView valueTv = new TextView(this);
        valueTv.setText(currentValue);
        valueTv.setTextColor(subColor);
        valueTv.setTextSize(13);
        valueTv.setGravity(android.view.Gravity.RIGHT);
        LinearLayout.LayoutParams valueLp = new LinearLayout.LayoutParams(-2, -2);
        valueLp.rightMargin = 8 * dp;
        row.addView(valueTv, valueLp);

        // 右箭头（>）
        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextColor(subColor);
        arrow.setTextSize(24);
        row.addView(arrow);

        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { onClick.run(); }
        });

        card.addView(row);
    }

    // 纯操作项
    private void addChromeAction(LinearLayout card, String title, Runnable onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, 14 * dp, 0, 14 * dp);

        TextView titleTv = new TextView(this);
        titleTv.setText(title);
        titleTv.setTextColor(titleColor);
        titleTv.setTextSize(15);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, -2, 1f);
        row.addView(titleTv, titleLp);

        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextColor(subColor);
        arrow.setTextSize(24);
        row.addView(arrow);

        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { onClick.run(); }
        });

        card.addView(row);
    }

    // 信息项（只读显示）
    private void addChromeInfo(LinearLayout card, String title, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, 14 * dp, 0, 14 * dp);

        TextView titleTv = new TextView(this);
        titleTv.setText(title);
        titleTv.setTextColor(titleColor);
        titleTv.setTextSize(15);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, -2, 1f);
        row.addView(titleTv, titleLp);

        TextView valueTv = new TextView(this);
        valueTv.setText(value);
        valueTv.setTextColor(subColor);
        valueTv.setTextSize(13);
        valueTv.setGravity(android.view.Gravity.RIGHT);
        row.addView(valueTv);

        card.addView(row);
    }

    // Chrome风格选择对话框
    private void showChromeDialog(String title, final String[] labels, final String key) {
        int curIdx = 0;
        try { curIdx = prefs.getInt(key, 0); } catch (Throwable t) {}
        final int curIdxFinal = curIdx;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setSingleChoiceItems(labels, curIdxFinal, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                try {
                    prefs.edit().putInt(key, which).apply();
                    dialog.dismiss();
                    // 刷新界面
                    recreate();
                } catch (Throwable t) {}
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    // 接口：卡片内容构建器
    private interface SectionBuilder {
        void build(LinearLayout card);
    }

    // ====== 卡片 UI 构建方法 ======
    private void addSectionTitle(LinearLayout root, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(groupColor);
        tv.setTextSize(13);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(6 * dp, 20 * dp, 0, 8 * dp);
        root.addView(tv);
    }

    private void addSectionCard(LinearLayout root, SectionBuilder builder) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        try {
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setColor(cardColor);
            bg.setCornerRadius(12 * dp);
            card.setBackgroundDrawable(bg);
        } catch (Throwable t) {
            card.setBackgroundColor(cardColor);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        card.setLayoutParams(lp);
        root.addView(card);
        builder.build(card);
    }

    private void addDivider(LinearLayout card) {
        View divider = new View(this);
        divider.setBackgroundColor(dividerColor);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-1, (int)(1 * dp));
        dlp.leftMargin = 16 * dp;
        card.addView(divider, dlp);
    }

    private void addCheckboxRow(LinearLayout card, String title,
                                final String key, int defaultVal) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(16 * dp, 14 * dp, 16 * dp, 14 * dp);
        row.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(titleColor);
        t.setTextSize(15);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, -2, 1.0f);
        row.addView(t, tlp);

        CheckBox cb = new CheckBox(this);
        boolean checked = (prefs == null) ? (defaultVal == 1) : (prefs.getInt(key, defaultVal) == 1);
        cb.setChecked(checked);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try { if (prefs != null) prefs.edit().putInt(key, isChecked ? 1 : 0).apply(); } catch (Throwable t) {}
            }
        });
        row.addView(cb);
        card.addView(row);
    }

    private void addSpinnerRow(LinearLayout card, String title, String[] options,
                               int initialIdx, final String key) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(16 * dp, 12 * dp, 16 * dp, 12 * dp);
        row.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(titleColor);
        t.setTextSize(15);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, -2, 1.0f);
        row.addView(t, tlp);

        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        int sel = Math.max(0, Math.min(options.length - 1, initialIdx));
        spinner.setSelection(sel);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    if (key.equals(KEY_TEXT_ZOOM)) {
                        prefs.edit().putInt(key, ZOOM_VALUES[position]).apply();
                    } else {
                        prefs.edit().putInt(key, position).apply();
                    }
                } catch (Throwable t) {}
                if (key.equals(KEY_HOMEPAGE) && position == 0) {
                    showHomepageDialog();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        row.addView(spinner);
        card.addView(row);
    }

    private void addActionRow(LinearLayout card, String title, String rightText,
                              final Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(16 * dp, 14 * dp, 16 * dp, 14 * dp);
        row.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try { action.run(); } catch (Throwable t) {
                    Toast.makeText(SettingsActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
                }
            }
        });

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(titleColor);
        t.setTextSize(15);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, -2, 1.0f);
        row.addView(t, tlp);

        if (rightText != null && rightText.length() > 0) {
            TextView r = new TextView(this);
            r.setText(rightText);
            r.setTextColor(subColor);
            r.setTextSize(15);
            row.addView(r);
        }
        card.addView(row);
    }

    private void addInfoRow(LinearLayout card, String title, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(16 * dp, 14 * dp, 16 * dp, 14 * dp);
        row.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(titleColor);
        t.setTextSize(15);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, -2, 1.0f);
        row.addView(t, tlp);

        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(subColor);
        v.setTextSize(14);
        v.setGravity(Gravity.END);
        row.addView(v);
        card.addView(row);
    }

    // 选择主页背景图片
    private static final int REQUEST_PICK_BG = 2001;
    private void pickHomepageBg() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "选择主页背景图片"), REQUEST_PICK_BG);
        } catch (Throwable t) {
            Toast.makeText(this, "无法打开文件选择器", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_BG && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                saveBgImage(uri);
            }
        }
    }

    private void saveBgImage(Uri uri) {
        try {
            // 第一步：用inSampleSize高效读取缩略图
            android.graphics.BitmapFactory.Options decodeOpts = new android.graphics.BitmapFactory.Options();
            decodeOpts.inJustDecodeBounds = true;
            try {
                java.io.InputStream is = getContentResolver().openInputStream(uri);
                android.graphics.BitmapFactory.decodeStream(is, null, decodeOpts);
                try { is.close(); } catch (Throwable tt) {}
            } catch (Throwable tt) {}
            int origW = Math.max(1, decodeOpts.outWidth);
            int origH = Math.max(1, decodeOpts.outHeight);
            // 最大目标尺寸 540px（比1080p小一倍，大幅减小内存和base64体积）
            int maxDim = 540;
            int sample = 1;
            while ((origW / sample) > maxDim || (origH / sample) > maxDim) sample *= 2;
            decodeOpts.inJustDecodeBounds = false;
            decodeOpts.inSampleSize = sample;
            decodeOpts.inPreferredConfig = android.graphics.Bitmap.Config.RGB_565; // 比ARGB_8888省一半内存
            android.graphics.Bitmap bitmap = null;
            try {
                java.io.InputStream is = getContentResolver().openInputStream(uri);
                bitmap = android.graphics.BitmapFactory.decodeStream(is, null, decodeOpts);
                try { is.close(); } catch (Throwable tt) {}
            } catch (Throwable tt) {}
            if (bitmap == null) {
                try {
                    bitmap = android.provider.MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    if (bitmap != null) {
                        int w = bitmap.getWidth();
                        int h = bitmap.getHeight();
                        if (w > maxDim || h > maxDim) {
                            float scale = Math.min((float)maxDim / w, (float)maxDim / h);
                            bitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, (int)(w * scale), (int)(h * scale), true);
                        }
                    }
                } catch (Throwable tt) {}
            }
            if (bitmap == null) {
                Toast.makeText(this, "读取图片失败", Toast.LENGTH_SHORT).show();
                return;
            }
            // 压缩为JPEG质量50%，进一步减小体积
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] bytes = baos.toByteArray();
            String base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);
            String dataUrl = "data:image/jpeg;base64," + base64;
            // SharedPreferences单个key推荐限制约800KB，这里设为500KB确保安全
            if (dataUrl.length() > 500000) {
                Toast.makeText(this, "图片过大，请选择更小的图片", Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.edit().putString(KEY_HOMEPAGE_BG, dataUrl).apply();
            Toast.makeText(this, "主页背景已设置", Toast.LENGTH_SHORT).show();
            // 设置成功后自动刷新主页
            refreshHomepageAndGoBack();
        } catch (Throwable t) {
            Toast.makeText(this, "设置失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 刷新主页并返回
    private void refreshHomepageAndGoBack() {
        try {
            // 发送广播通知主页刷新背景
            Intent intent = new Intent("com.menglin.cn.REFRESH_HOMEPAGE");
            sendBroadcast(intent);
        } catch (Throwable t) {}
        // 关闭设置页，主页会刷新
        finish();
    }

    private void showHomepageDialog() {
        final EditText input = new EditText(this);
        input.setHint("输入网址，如 https://www.example.com");
        String existing = "file:///android_asset/homepage.html";
        try { existing = prefs.getString("custom_homepage", existing); } catch (Throwable t) {}
        input.setText(existing);
        input.setTextColor(titleColor);
        input.setHintTextColor(subColor);
        new AlertDialog.Builder(this)
                .setTitle("自定义主页")
                .setView(input)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        String url = input.getText() == null ? "" : input.getText().toString().trim();
                        if (url.isEmpty()) url = "file:///android_asset/homepage.html";
                        try { prefs.edit().putString("custom_homepage", url).apply(); } catch (Throwable t) {}
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // ====== 数据辅助 ======
    private int getIdx(SharedPreferences p, String key, int def) {
        if (p == null) return def;
        try { return p.getInt(key, def); } catch (Throwable t) { return def; }
    }

    private int findZoom(SharedPreferences p, String key, int def) {
        int v = getIdx(p, key, def);
        for (int i = 0; i < ZOOM_VALUES.length; i++)
            if (ZOOM_VALUES[i] >= v) return i;
        return 2;
    }

    // ====== Static ======
    public static String getSearchUrl(Activity activity) {
        try {
            SharedPreferences p = activity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            int idx = p.getInt(KEY_SEARCH_ENGINE, 0);
            if (idx < 0 || idx >= SEARCH_URLS.length) idx = 0;
            return SEARCH_URLS[idx];
        } catch (Throwable t) { return SEARCH_URLS[0]; }
    }

    public static String getHomepage(Activity activity) {
        try {
            SharedPreferences p = activity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            int idx = p.getInt(KEY_HOMEPAGE, 4);
            if (idx == 0) return p.getString("custom_homepage", "file:///android_asset/homepage.html");
            if (idx < 0 || idx >= HOMEPAGE_URLS.length) idx = 4;
            return HOMEPAGE_URLS[idx];
        } catch (Throwable t) { return "about:blank"; }
    }
}
