package com.menglin.cn;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.net.http.SslError;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainActivity extends Activity {

    public static final String HISTORY_PREFS = "YulanBrowserHistory";
    public static final String HISTORY_KEY = "history_json";
    public static final String DOWNLOAD_KEY = "download_json";
    public static final int MAX_HISTORY = 200;

    private FrameLayout webViewContainer;
    private ProgressBar progressBar;
    private EditText urlBar;
    private TextView btnTabs;
    private SharedPreferences prefs;
    private Handler handler;

    // 文件上传相关
    private ValueCallback<Uri> uploadMessage;
    private ValueCallback<Uri[]> uploadMessagesAboveL;
    private static final int REQUEST_FILE_PICKER = 1001;

    private boolean isHomepage(String url) {
        if (url == null || url.length() == 0) return true;
        try {
            String hp = SettingsActivity.getHomepage(this);
            if (url.equals(hp)) return true;
        } catch (Throwable t) {}
        return url.startsWith("file:///android_asset/homepage") || url.equals("about:blank");
    }

    // 加载主页（动态注入搜索引擎和深色模式）
    private void loadHomepage(android.webkit.WebView wv) {
        if (wv == null) return;
        try {
            String hp = SettingsActivity.getHomepage(this);
            wv.loadUrl(hp);
        } catch (Throwable t) {
            try {
                wv.loadUrl("about:blank");
            } catch (Throwable tt) {}
        }
    }

    // 获取主页JavaScript注入代码
    private String getHomepageInjection() {
        try {
            String searchUrl = SettingsActivity.getSearchUrl(this);
            int darkMode = prefs.getInt(SettingsActivity.KEY_DARK_MODE, 0);
            boolean dark = false;
            if (darkMode == 2) dark = true;
            else if (darkMode == 0) {
                int ui = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                dark = (ui == Configuration.UI_MODE_NIGHT_YES);
            }
            return "javascript:(function(){ try { window.__YULAN_SEARCH__ = '" + searchUrl + "'; window.__YULAN_DARK__ = " + (dark ? "true" : "false") + "; if (window.__YULAN_DARK__ === true) document.body.classList.add('dark-applied'); else document.body.classList.add('light-applied'); } catch(e){} })();";
        } catch (Throwable t) {
            return "";
        }
    }

    // 更新URL栏：主页不显示内容；访问网站时显示HTML标题
    private void setUrlBar(String url) {
        try {
            if (urlBar == null) return;
            if (url == null || url.length() == 0) return;
            if (isHomepage(url)) {
                urlBar.setText("");
                return;
            }
            TabManager.Tab tt = currentTab();
            String title = null;
            if (tt != null && tt.title != null && tt.title.length() > 0) {
                title = tt.title;
            }
            if (title != null && title.length() > 0) {
                urlBar.setText(title);
            } else {
                urlBar.setText(url);
            }
        } catch (Throwable t) {}
    }

    // 自定义底部菜单（替代原生AlertDialog）
    private void showCustomMenu() {
        try {
            if (isFinishing()) return;
            final int dp2 = (int) getResources().getDisplayMetrics().density;
            boolean dark = false;
            try {
                int darkMode = prefs.getInt(SettingsActivity.KEY_DARK_MODE, 0);
                if (darkMode == 2) dark = true;
                else if (darkMode == 0) {
                    int ui = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                    dark = (ui == Configuration.UI_MODE_NIGHT_YES);
                }
            } catch (Throwable t) {}
            final int titleColor = dark ? 0xFFFFFFFF : 0xFF1C1C1E;
            final int subColor = dark ? 0xFFAAAAAA : 0xFF8E8E93;
            final int cardBg = dark ? 0xFF2D2D2D : 0xFFFFFFFF;
            final int accentColor = 0xFF1E88E5;
            final int itemBg = dark ? 0xFF2D2D2D : 0xFFFFFFFF;

            android.app.Dialog dialog = new android.app.Dialog(MainActivity.this, android.R.style.Theme_Translucent_NoTitleBar);
            try { dialog.getWindow().setGravity(android.view.Gravity.BOTTOM); } catch (Throwable tt) {}
            LinearLayout root = new LinearLayout(MainActivity.this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackgroundColor(0x60000000);
            root.setGravity(android.view.Gravity.BOTTOM);

            LinearLayout content = new LinearLayout(MainActivity.this);
            content.setOrientation(LinearLayout.VERTICAL);
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setColor(cardBg);
            gd.setCornerRadii(new float[]{ 16*dp2, 16*dp2, 16*dp2, 16*dp2, 0, 0, 0, 0 });
            content.setBackgroundDrawable(gd);
            content.setPadding(0, 12*dp2, 0, 8*dp2);

            String[] items = {"历史记录", "下载列表", "设置"};
            String[] icons = {"史", "载", "设"};

            for (int i = 0; i < items.length; i++) {
                final int idx = i;
                LinearLayout row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                row.setPadding(20*dp2, 14*dp2, 16*dp2, 14*dp2);
                row.setBackgroundColor(itemBg);
                row.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        try { dialog.dismiss(); } catch (Throwable tt) {}
                        try {
                            if (idx == 0) {
                                Intent it = new Intent(MainActivity.this, HistoryActivity.class);
                                startActivity(it);
                            } else if (idx == 1) {
                                Intent it = new Intent(MainActivity.this, DownloadActivity.class);
                                startActivity(it);
                            } else {
                                Intent it = new Intent(MainActivity.this, SettingsActivity.class);
                                startActivity(it);
                            }
                        } catch (Throwable tt) {}
                    }
                });
                TextView iconTv = new TextView(MainActivity.this);
                iconTv.setText(icons[i]);
                iconTv.setTextSize(18);
                LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                ip.rightMargin = 12*dp2;
                row.addView(iconTv, ip);
                TextView tv = new TextView(MainActivity.this);
                tv.setText(items[i]);
                tv.setTextColor(titleColor);
                tv.setTextSize(15);
                tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                row.addView(tv);
                content.addView(row);
                if (i < items.length - 1) {
                    View line = new View(MainActivity.this);
                    line.setBackgroundColor(dark ? 0xFF1A1A1A : 0xFFF2F2F7);
                    content.addView(line, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, (int)(0.5f * dp2))));
                }
            }

            root.addView(content, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            dialog.setContentView(root);
            try {
                android.view.Window w = dialog.getWindow();
                android.view.WindowManager.LayoutParams params = w.getAttributes();
                params.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                params.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                w.setAttributes(params);
                w.setBackgroundDrawableResource(android.R.color.transparent);
            } catch (Throwable tt) {}
            dialog.show();
            root.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    try { dialog.dismiss(); } catch (Throwable tt) {}
                }
            });
        } catch (Throwable t) {}
    }

    // 全屏搜索界面：底部输入框，上方可滚动历史+收藏
    private void showUrlDialog() {
        try {
            if (MainActivity.this.isFinishing()) return;
            int dp2 = (int) getResources().getDisplayMetrics().density;

            boolean dark = false;
            try {
                int darkMode = prefs.getInt(SettingsActivity.KEY_DARK_MODE, 0);
                if (darkMode == 2) dark = true;
                else if (darkMode == 0) {
                    int ui = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                    dark = (ui == Configuration.UI_MODE_NIGHT_YES);
                }
            } catch (Throwable t) {}
            final int titleColor = dark ? 0xFFFFFFFF : 0xFF1C1C1E;
            final int subColor = dark ? 0xFFAAAAAA : 0xFF8E8E93;
            final int dialogBg = dark ? 0xFF121212 : 0xFFFFFFFF;
            final int accentColor = 0xFF1E88E5;
            final int btnBg = dark ? 0xFF2A2A2A : 0xFFF2F2F7;
            final int borderColor = dark ? 0xFF3A3A3A : 0xFFE5E5EA;

            final Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
            try {
                android.view.Window w = dialog.getWindow();
                if (w != null) {
                    android.view.WindowManager.LayoutParams lp = w.getAttributes();
                    lp.width = -1;
                    lp.height = -1;
                    w.setAttributes(lp);
                    w.setFormat(android.graphics.PixelFormat.RGBA_8888);
                    w.setWindowAnimations(android.R.style.Animation_Dialog);
                    w.setBackgroundDrawableResource(android.R.color.white);
                    if (dark) w.setBackgroundDrawableResource(android.R.color.black);
                }
            } catch (Throwable t) {}

            LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackgroundColor(dialogBg);

            LinearLayout topBar = new LinearLayout(this);
            topBar.setOrientation(LinearLayout.HORIZONTAL);
            topBar.setGravity(Gravity.CENTER_VERTICAL);
            topBar.setPadding(16 * dp2, 12 * dp2, 16 * dp2, 8 * dp2);
            topBar.setBackgroundColor(dialogBg);

            TextView titleTV = new TextView(this);
            titleTV.setText("搜索");
            titleTV.setTextSize(16);
            titleTV.setTextColor(titleColor);
            titleTV.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams titleLP = new LinearLayout.LayoutParams(0, -2, 1.0f);
            topBar.addView(titleTV, titleLP);

            TextView cancelBtn = new TextView(this);
            cancelBtn.setText("取消");
            cancelBtn.setTextColor(accentColor);
            cancelBtn.setTextSize(15);
            cancelBtn.setGravity(Gravity.CENTER);
            cancelBtn.setPadding(16 * dp2, 6 * dp2, 16 * dp2, 6 * dp2);
            cancelBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    try { dialog.dismiss(); } catch (Throwable tt) {}
                }
            });
            topBar.addView(cancelBtn);
            root.addView(topBar, new LinearLayout.LayoutParams(-1, -2));

            ScrollView sv = new ScrollView(this);
            sv.setFillViewport(true);

            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(16 * dp2, 4 * dp2, 16 * dp2, 16 * dp2);

            String curUrl = "";
            String curTitle = "";
            try {
                TabManager.Tab tt = currentTab();
                if (tt != null) {
                    if (tt.url != null) curUrl = tt.url;
                    if (tt.title != null && tt.title.length() > 0) curTitle = tt.title;
                }
            } catch (Throwable t) {}

            if (curUrl.length() > 0 && !isHomepage(curUrl)) {
                LinearLayout favItem = new LinearLayout(this);
                favItem.setOrientation(LinearLayout.HORIZONTAL);
                favItem.setGravity(Gravity.CENTER_VERTICAL);
                favItem.setPadding(12 * dp2, 12 * dp2, 12 * dp2, 12 * dp2);
                try {
                    GradientDrawable bg = new GradientDrawable();
                    bg.setColor(btnBg);
                    bg.setCornerRadius(14 * dp2);
                    favItem.setBackgroundDrawable(bg);
                } catch (Throwable t) {}

                TextView icon = new TextView(this);
                icon.setText("\u2605");
                icon.setTextSize(14);
                icon.setTextColor(accentColor);
                icon.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams iconLP = new LinearLayout.LayoutParams((int)(36 * dp2), (int)(36 * dp2));
                iconLP.rightMargin = 12 * dp2;
                favItem.addView(icon, iconLP);

                LinearLayout tw = new LinearLayout(this);
                tw.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams twLp = new LinearLayout.LayoutParams(0, -2, 1.0f);

                TextView tt1 = new TextView(this);
                tt1.setText("收藏当前页");
                tt1.setTextColor(titleColor);
                tt1.setTextSize(14);
                tw.addView(tt1);

                TextView tt2 = new TextView(this);
                tt2.setText(curTitle.length() > 0 ? curTitle : curUrl);
                tt2.setTextColor(subColor);
                tt2.setTextSize(11);
                tt2.setMaxLines(1);
                tt2.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
                tt2.setPadding(0, 2 * dp2, 0, 0);
                tw.addView(tt2);
                favItem.addView(tw, twLp);

                TextView addTv = new TextView(this);
                addTv.setText("+");
                addTv.setTextColor(accentColor);
                addTv.setTextSize(20);
                addTv.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams addLP = new LinearLayout.LayoutParams(-2, -2);
                addLP.leftMargin = 8 * dp2;
                favItem.addView(addTv, addLP);

                final String finalUrl = curUrl;
                final String finalTitle = curTitle;
                favItem.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        addFavorite(finalTitle.length() > 0 ? finalTitle : finalUrl, finalUrl);
                        Toast.makeText(MainActivity.this, "已收藏", Toast.LENGTH_SHORT).show();
                    }
                });

                LinearLayout.LayoutParams rowLP = new LinearLayout.LayoutParams(-1, -2);
                rowLP.topMargin = 6 * dp2;
                content.addView(favItem, rowLP);
            }

            TextView histTitle = new TextView(this);
            histTitle.setText("最近访问");
            histTitle.setTextColor(subColor);
            histTitle.setTextSize(13);
            histTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams htLP = new LinearLayout.LayoutParams(-1, -2);
            htLP.topMargin = 16 * dp2;
            content.addView(histTitle, htLP);

            ArrayList<String[]> recentItems = new ArrayList<String[]>();
            try {
                SharedPreferences hp = getSharedPreferences(HISTORY_PREFS, MODE_PRIVATE);
                String json = hp.getString(HISTORY_KEY, "[]");
                if (json != null && json.length() > 2) {
                    ArrayList<String> items = splitJsonArray(json.substring(1, json.length() - 1));
                    for (int i = 0; i < Math.min(5, items.size()); i++) {
                        String it = items.get(i);
                        if (it == null) continue;
                        String u = extractStrField(it, "u");
                        String t = extractStrField(it, "title");
                        if (u == null || u.length() == 0) continue;
                        recentItems.add(new String[]{t, u});
                    }
                }
            } catch (Throwable t) {}

            if (recentItems.size() > 0) {
                for (int i = 0; i < recentItems.size(); i++) {
                    final String[] row = recentItems.get(i);
                    LinearLayout item = makeDialogItem(row, titleColor, subColor, btnBg, accentColor, dp2);
                    item.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            try { if (currentTab() != null && currentTab().webView != null) currentTab().webView.loadUrl(row[1]); } catch (Throwable t) {}
                            try { dialog.dismiss(); } catch (Throwable t) {}
                        }
                    });
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                    lp.topMargin = 6 * dp2;
                    content.addView(item, lp);
                }
            } else {
                TextView empty = new TextView(this);
                empty.setText("暂无最近记录");
                empty.setTextColor(subColor);
                empty.setTextSize(13);
                empty.setPadding(0, 8 * dp2, 0, 8 * dp2);
                content.addView(empty);
            }

            TextView favTitle = new TextView(this);
            favTitle.setText("我的收藏");
            favTitle.setTextColor(subColor);
            favTitle.setTextSize(13);
            favTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams favTitleLP = new LinearLayout.LayoutParams(-1, -2);
            favTitleLP.topMargin = 20 * dp2;
            content.addView(favTitle, favTitleLP);

            final ArrayList<String[]> favs = getFavorites();
            if (favs.size() > 0) {
                for (int i = 0; i < favs.size(); i++) {
                    final int idx = i;
                    final String[] row = favs.get(i);
                    LinearLayout item = makeDialogItem(row, titleColor, subColor, btnBg, 0xFFE53935, dp2);
                    item.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            try { if (currentTab() != null && currentTab().webView != null) currentTab().webView.loadUrl(row[1]); } catch (Throwable t) {}
                            try { dialog.dismiss(); } catch (Throwable t) {}
                        }
                    });
                    item.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override public boolean onLongClick(View v) {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("删除收藏")
                                    .setMessage("删除 \"" + (row[0] == null || row[0].length() == 0 ? row[1] : row[0]) + "\" ？")
                                    .setPositiveButton("删除", new android.content.DialogInterface.OnClickListener() {
                                        @Override public void onClick(android.content.DialogInterface d, int w) {
                                            removeFavoriteAt(idx);
                                            Toast.makeText(MainActivity.this, "已删除", Toast.LENGTH_SHORT).show();
                                            try { dialog.dismiss(); } catch (Throwable t) {}
                                            showUrlDialog();
                                        }
                                    })
                                    .setNegativeButton("取消", null)
                                    .show();
                            return true;
                        }
                    });
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                    lp.topMargin = 6 * dp2;
                    content.addView(item, lp);
                }
            } else {
                TextView emptyFav = new TextView(this);
                emptyFav.setText("暂无收藏");
                emptyFav.setTextColor(subColor);
                emptyFav.setTextSize(13);
                emptyFav.setPadding(0, 8 * dp2, 0, 8 * dp2);
                content.addView(emptyFav);
            }

            sv.addView(content, new LinearLayout.LayoutParams(-1, -2));
            root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1.0f));

            final EditText input = new EditText(this);
            input.setHint("输入网址或搜索...");
            if (curUrl.length() > 0 && !isHomepage(curUrl)) {
                input.setText(curUrl);
                try { input.setSelection(curUrl.length()); } catch (Throwable t) {}
            }
            input.setTextColor(titleColor);
            input.setHintTextColor(subColor);
            input.setTextSize(15);
            input.setSingleLine(true);
            input.setImeOptions(EditorInfo.IME_ACTION_GO);
            input.setBackgroundColor(0x00000000);
            int inPad = 14 * dp2;
            input.setPadding(inPad, 10 * dp2, inPad, 10 * dp2);
            try {
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(btnBg);
                bg.setCornerRadius(22 * dp2);
                bg.setStroke(1 * dp2, borderColor);
                input.setBackgroundDrawable(bg);
            } catch (Throwable t) {}
            LinearLayout.LayoutParams inputLP = new LinearLayout.LayoutParams(-1, -2);
            inputLP.setMargins(16 * dp2, 8 * dp2, 16 * dp2, 20 * dp2);
            root.addView(input, inputLP);

            input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_GO) {
                        String text = input.getText() == null ? "" : input.getText().toString().trim();
                        if (text.length() == 0) { try { dialog.dismiss(); } catch (Throwable t) {} return true; }
                        String url;
                        if (text.startsWith("http://") || text.startsWith("https://") ||
                                text.startsWith("about:") || text.startsWith("file://")) {
                            url = text;
                        } else if (text.contains(".") && !text.contains(" ")) {
                            url = "http://" + text;
                        } else {
                            url = SettingsActivity.getSearchUrl(MainActivity.this) + Uri.encode(text);
                        }
                        try { if (currentTab() != null && currentTab().webView != null) currentTab().webView.loadUrl(url); } catch (Throwable t) {}
                        try { dialog.dismiss(); } catch (Throwable t) {}
                        return true;
                    }
                    return false;
                }
            });

            dialog.setContentView(root);
            dialog.show();
            try { input.requestFocus(); } catch (Throwable t) {}

            try {
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.toggleSoftInput(android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT, 0);
            } catch (Throwable t) {}

        } catch (Throwable t) {
            Toast.makeText(this, "操作失败", Toast.LENGTH_SHORT).show();
        }
    }

    // 弹窗内单个列表项
    private LinearLayout makeDialogItem(String[] row, int titleColor, int subColor, int btnBg, int iconColor, int dp) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(10 * dp, 10 * dp, 10 * dp, 10 * dp);
        try {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(btnBg);
            bg.setCornerRadius(12 * dp);
            item.setBackgroundDrawable(bg);
        } catch (Throwable t) {}

        TextView icon = new TextView(this);
        char c = '\u2022';
        if (row[0] != null && row[0].length() > 0) {
            char fc = Character.toUpperCase(row[0].charAt(0));
            if (Character.isLetterOrDigit(fc)) c = fc;
        } else if (row[1] != null && row[1].length() > 0) {
            for (int k = 0; k < row[1].length(); k++) {
                char fc = Character.toUpperCase(row[1].charAt(k));
                if (Character.isLetterOrDigit(fc)) { c = fc; break; }
            }
        }
        icon.setText(String.valueOf(c));
        icon.setTextSize(12);
        icon.setGravity(Gravity.CENTER);
        icon.setTextColor(0xFFFFFFFF);
        icon.setTypeface(null, android.graphics.Typeface.BOLD);
        try {
            GradientDrawable ibg = new GradientDrawable();
            ibg.setColor(iconColor);
            ibg.setCornerRadius(10 * dp);
            icon.setBackgroundDrawable(ibg);
        } catch (Throwable t) {}
        icon.setPadding(8 * dp, 8 * dp, 8 * dp, 8 * dp);
        LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams((int)(28 * dp), (int)(28 * dp));
        icLp.rightMargin = 12 * dp;
        item.addView(icon, icLp);

        LinearLayout textWrap = new LinearLayout(this);
        textWrap.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams twLp = new LinearLayout.LayoutParams(0, -2, 1.0f);
        textWrap.setLayoutParams(twLp);

        TextView t1 = new TextView(this);
        t1.setText(row[0] == null || row[0].length() == 0 ? row[1] : row[0]);
        t1.setTextColor(titleColor);
        t1.setTextSize(14);
        t1.setMaxLines(1);
        t1.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textWrap.addView(t1);

        TextView t2 = new TextView(this);
        t2.setText(row[1]);
        t2.setTextColor(subColor);
        t2.setTextSize(11);
        t2.setMaxLines(1);
        t2.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        t2.setPadding(0, 2 * dp, 0, 0);
        textWrap.addView(t2);
        item.addView(textWrap);

        return item;
    }

    // 收藏夹持久化
    public static final String FAVORITES_KEY = "yulan_favorites";

    private void addFavorite(String title, String url) {
        try {
            String existing = prefs.getString(FAVORITES_KEY, "[]");
            String entry = "{\"t\":" + System.currentTimeMillis()
                    + ",\"title\":\"" + escapeJson(title) + "\""
                    + ",\"url\":\"" + escapeJson(url) + "\"}";
            String newJson;
            if (existing.length() > 2) {
                newJson = "[" + entry + "," + existing.substring(1);
            } else {
                newJson = "[" + entry + "]";
            }
            prefs.edit().putString(FAVORITES_KEY, newJson).apply();
        } catch (Throwable t) {}
    }

    private ArrayList<String[]> getFavorites() {
        ArrayList<String[]> result = new ArrayList<String[]>();
        try {
            String json = prefs.getString(FAVORITES_KEY, "[]");
            if (json != null && json.length() > 2) {
                ArrayList<String> items = splitJsonArray(json.substring(1, json.length() - 1));
                for (String it : items) {
                    if (it == null) continue;
                    String u = extractStrField(it, "url");
                    String t = extractStrField(it, "title");
                    if (u == null || u.length() == 0) continue;
                    result.add(new String[]{t == null ? "" : t, u});
                }
            }
        } catch (Throwable t) {}
        return result;
    }

    private void removeFavoriteAt(int index) {
        try {
            String json = prefs.getString(FAVORITES_KEY, "[]");
            if (json.length() <= 2) return;
            ArrayList<String> items = splitJsonArray(json.substring(1, json.length() - 1));
            if (index < 0 || index >= items.size()) return;
            items.remove(index);
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < items.size(); i++) {
                sb.append("{").append(items.get(i)).append("}");
                if (i < items.size() - 1) sb.append(",");
            }
            sb.append("]");
            prefs.edit().putString(FAVORITES_KEY, sb.toString()).apply();
        } catch (Throwable t) {}
    }

    private void addHistory(String url, String title) {
        if (url == null || url.length() == 0) return;
        if (isHomepage(url)) return;
        if (url.startsWith("file://") || url.startsWith("about:")) return;
        try {
            TabManager.Tab cur = currentTab();
            if (cur != null && cur.isPrivate) return;
        } catch (Throwable tt) {}
        try {
            SharedPreferences hp = getSharedPreferences(HISTORY_PREFS, MODE_PRIVATE);
            String json = hp.getString(HISTORY_KEY, "[]");
            String entry = "{\"t\":" + System.currentTimeMillis()
                    + ",\"u\":\"" + escapeJson(url) + "\""
                    + ",\"title\":\"" + escapeJson(title == null || title.length() == 0 ? url : title) + "\"}";
            String newJson = "[" + entry;
            int count = 1;
            if (json != null && json.length() > 2) {
                String inner = json.substring(1, json.length() - 1);
                if (inner.length() > 0) {
                    ArrayList<String> items = splitJsonArray(inner);
                    for (String it : items) {
                        if (it == null) continue;
                        if (it.contains("\"u\":\"" + escapeJson(url) + "\"")) continue;
                        if (count >= MAX_HISTORY) break;
                        newJson += ",{" + it + "}";
                        count++;
                    }
                }
            }
            newJson += "]";
            hp.edit().putString(HISTORY_KEY, newJson).apply();
        } catch (Throwable t) {}
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 20);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String extractStrField(String json, String key) {
        if (json == null || key == null) return "";
        try {
            String search = "\"" + key + "\":\"";
            int idx = json.indexOf(search);
            if (idx < 0) return "";
            int start = idx + search.length();
            StringBuilder sb = new StringBuilder();
            boolean escaped = false;
            for (int i = start; i < json.length(); i++) {
                char c = json.charAt(i);
                if (escaped) {
                    sb.append(c);
                    escaped = false;
                    continue;
                }
                if (c == '\\') {
                    escaped = true;
                    continue;
                }
                if (c == '"') break;
                sb.append(c);
            }
            return sb.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    private ArrayList<String> splitJsonArray(String inner) {
        ArrayList<String> result = new ArrayList<String>();
        try {
            int depth = 0;
            int start = -1;
            boolean inStr = false;
            for (int i = 0; i < inner.length(); i++) {
                char c = inner.charAt(i);
                if (c == '\"' && (i == 0 || inner.charAt(i - 1) != '\\')) {
                    inStr = !inStr;
                    continue;
                }
                if (inStr) continue;
                if (c == '{') {
                    depth++;
                    if (depth == 1) start = i + 1;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0 && start >= 0) {
                        result.add(inner.substring(start, i));
                        start = -1;
                    }
                }
            }
        } catch (Throwable t) {}
        return result;
    }

    // 每个 Tab 用专用的 WebViewClient（非匿名类）
    public static class TabClient extends WebViewClient {
        public TabManager.Tab tab;
        public MainActivity activity;
        private java.util.HashSet<String> fallbackUrls = new java.util.HashSet<String>();

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            try {
            if (tab != null) tab.url = url;
            if (activity != null) {
                activity.progressBar.setVisibility(View.VISIBLE);
                activity.progressBar.setProgress(0);
                activity.setUrlBar(url);
            }
            } catch (Throwable t) {}
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            try {
            if (tab != null) {
                tab.url = url;
                try {
                    String t = view.getTitle();
                    if (t != null && t.length() > 0) tab.title = t;
                } catch (Throwable tt) {}
                try {
                    int w = view.getWidth();
                    int h = view.getHeight();
                    if (w > 0 && h > 0) {
                        float max = 180f;
                        float scale = Math.min(1f, max / Math.max(w, h));
                        int nw = Math.max(1, (int)(w * scale));
                        int nh = Math.max(1, (int)(h * scale));
                        android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(nw, nh, android.graphics.Bitmap.Config.RGB_565);
                        android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);
                        canvas.drawColor(0xFFFFFFFF);
                        canvas.scale(scale, scale);
                        view.draw(canvas);
                        tab.thumbnail = bmp;
                    }
                } catch (Throwable tt) {}
            }
            if (activity != null) {
                activity.progressBar.setVisibility(View.GONE);
                activity.setUrlBar(url);
                if (activity.isHomepage(url)) {
                    try {
                        String injection = activity.getHomepageInjection();
                        if (injection != null && injection.length() > 0) {
                            try {
                                view.evaluateJavascript(injection, null);
                            } catch (Throwable tt) {
                                view.loadUrl(injection);
                            }
                        }
                    } catch (Throwable tt) {}
                }
                try { activity.addHistory(url, tab == null ? null : tab.title); } catch (Throwable tt) {}
            }
            } catch (Throwable t) {}
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            try {
                if (failingUrl != null && (failingUrl.startsWith("file://") || failingUrl.startsWith("about:"))) return;
            } catch (Throwable tt) {}
            try {
            if (failingUrl != null && failingUrl.startsWith("https://") && !fallbackUrls.contains(failingUrl)) {
                fallbackUrls.add(failingUrl);
                String httpUrl = "http://" + failingUrl.substring(8);
                view.stopLoading();
                try { view.loadUrl(httpUrl); return; } catch (Throwable tt) {}
            }
            } catch (Throwable t) {}
            try {
                if (activity != null) {
                    view.stopLoading();
                    String errCode = "";
                    switch (errorCode) {
                        case android.webkit.WebViewClient.ERROR_HOST_LOOKUP: errCode = "ERROR_HOST_LOOKUP"; break;
                        case android.webkit.WebViewClient.ERROR_CONNECT: errCode = "ERROR_CONNECT"; break;
                        case android.webkit.WebViewClient.ERROR_TIMEOUT: errCode = "ERROR_TIMEOUT"; break;
                        case android.webkit.WebViewClient.ERROR_FILE_NOT_FOUND: errCode = "ERROR_FILE_NOT_FOUND"; break;
                        case android.webkit.WebViewClient.ERROR_BAD_URL: errCode = "ERROR_BAD_URL"; break;
                        case android.webkit.WebViewClient.ERROR_UNSUPPORTED_AUTH_SCHEME: errCode = "ERROR_UNSUPPORTED_AUTH_SCHEME"; break;
                        case android.webkit.WebViewClient.ERROR_AUTHENTICATION: errCode = "ERROR_AUTHENTICATION"; break;
                        case android.webkit.WebViewClient.ERROR_IO: errCode = "ERROR_IO"; break;
                        default: errCode = "ERROR_UNKNOWN"; break;
                    }
                    String errMsg = "";
                    if (description != null && description.length() > 0) {
                        errMsg = Uri.encode(description);
                    }
                    String errUrl = "";
                    if (failingUrl != null) {
                        errUrl = Uri.encode(failingUrl);
                    }
                    String errPageUrl = "file:///android_asset/error.html?code=" + errCode + "&msg=" + errMsg + "&url=" + errUrl;
                    view.loadUrl(errPageUrl);
                    if (tab != null) tab.url = failingUrl;
                }
            } catch (Throwable t) {}
        }

        @Override
        public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
            try {
                String url = view.getUrl();
                if (url != null && url.startsWith("https://")) {
                    String httpUrl = "http://" + url.substring(8);
                    try { handler.cancel(); view.stopLoading(); view.loadUrl(httpUrl); return; } catch (Throwable tt) {}
                }
                final android.app.Dialog dialog = new android.app.Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
                android.widget.LinearLayout root = new android.widget.LinearLayout(activity);
                root.setOrientation(android.widget.LinearLayout.VERTICAL);
                root.setBackgroundColor(0x88000000);
                root.setGravity(android.view.Gravity.CENTER);
                android.widget.LinearLayout card = new android.widget.LinearLayout(activity);
                card.setOrientation(android.widget.LinearLayout.VERTICAL);
                int dpd = (int) activity.getResources().getDisplayMetrics().density;
                android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
                gd.setColor(0xFF2D2D2D);
                gd.setCornerRadius(18 * dpd);
                card.setBackgroundDrawable(gd);
                card.setPadding(22 * dpd, 24 * dpd, 22 * dpd, 18 * dpd);
                android.widget.TextView title = new android.widget.TextView(activity);
                title.setText("\u26A0 网站不安全");
                title.setTextColor(0xFFFFFFFF);
                title.setTextSize(18);
                title.setTypeface(null, android.graphics.Typeface.BOLD);
                title.setGravity(android.view.Gravity.CENTER);
                card.addView(title, new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
                android.widget.TextView msg = new android.widget.TextView(activity);
                msg.setText("\n该网站的安全证书有问题，继续访问可能有安全风险。建议返回或不要输入敏感信息。");
                msg.setTextColor(0xFFCCCCCC);
                msg.setTextSize(14);
                msg.setGravity(android.view.Gravity.CENTER);
                android.widget.LinearLayout.LayoutParams msgLp = new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                msgLp.topMargin = 10 * dpd;
                card.addView(msg, msgLp);
                android.widget.LinearLayout btns = new android.widget.LinearLayout(activity);
                btns.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                btns.setGravity(android.view.Gravity.CENTER);
                android.widget.LinearLayout.LayoutParams btnsLp = new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                btnsLp.topMargin = 24 * dpd;
                card.addView(btns, btnsLp);
                android.widget.TextView backBtn = new android.widget.TextView(activity);
                backBtn.setText("返回");
                backBtn.setTextColor(0xFFFFFFFF);
                backBtn.setTextSize(15);
                backBtn.setGravity(android.view.Gravity.CENTER);
                backBtn.setTypeface(null, android.graphics.Typeface.BOLD);
                android.graphics.drawable.GradientDrawable backGd = new android.graphics.drawable.GradientDrawable();
                backGd.setColor(0xFF1E88E5);
                backGd.setCornerRadius(22 * dpd);
                backBtn.setBackgroundDrawable(backGd);
                backBtn.setPadding(0, 12 * dpd, 0, 12 * dpd);
                android.widget.LinearLayout.LayoutParams backLp = new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                backLp.rightMargin = 8 * dpd;
                backBtn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        try { dialog.dismiss(); handler.cancel(); } catch (Throwable t) {}
                        try {
                            if (view.canGoBack()) view.goBack();
                            else view.loadUrl("file:///android_asset/homepage.html");
                        } catch (Throwable t) {}
                    }
                });
                btns.addView(backBtn, backLp);
                android.widget.TextView proceedBtn = new android.widget.TextView(activity);
                proceedBtn.setText("继续访问");
                proceedBtn.setTextColor(0xFFCCCCCC);
                proceedBtn.setTextSize(15);
                proceedBtn.setGravity(android.view.Gravity.CENTER);
                android.graphics.drawable.GradientDrawable proGd = new android.graphics.drawable.GradientDrawable();
                proGd.setColor(0xFF3A3A3A);
                proGd.setCornerRadius(22 * dpd);
                proceedBtn.setBackgroundDrawable(proGd);
                proceedBtn.setPadding(0, 12 * dpd, 0, 12 * dpd);
                android.widget.LinearLayout.LayoutParams proLp = new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                proLp.leftMargin = 8 * dpd;
                proceedBtn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        try { dialog.dismiss(); handler.proceed(); } catch (Throwable t) {}
                    }
                });
                btns.addView(proceedBtn, proLp);
                root.addView(card, new android.widget.LinearLayout.LayoutParams((int)(320 * dpd), android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
                dialog.setContentView(root);
                dialog.setCancelable(false);
                try {
                    android.view.Window w = dialog.getWindow();
                    android.view.WindowManager.LayoutParams params = w.getAttributes();
                    params.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                    params.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                    w.setAttributes(params);
                } catch (Throwable tt) {}
                dialog.show();
            } catch (Throwable t) {}
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url == null) return false;
            try {
                if (url.startsWith("http://") || url.startsWith("https://") ||
                    url.startsWith("file://") || url.startsWith("about:")) {
                    view.loadUrl(url);
                    return true;
                }
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                if (activity != null) activity.startActivity(i);
                return true;
            } catch (Throwable t) {
                return true;
            }
        }
    }

    public static class TabChromeClient extends WebChromeClient {
        public TabManager.Tab tab;
        public MainActivity activity;

        @Override public void onProgressChanged(WebView view, int newProgress) {
            try {
            if (activity != null) {
                activity.progressBar.setProgress(newProgress);
                if (newProgress >= 100) activity.progressBar.setVisibility(View.GONE);
            }
            } catch (Throwable t) {}
        }

        @Override public void onReceivedTitle(WebView view, String title) {
            try {
                if (tab != null && title != null && title.length() > 0) tab.title = title;
            } catch (Throwable t) {}
        }

        @SuppressWarnings("unused")
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
            try {
                if (activity != null) {
                    activity.uploadMessage = uploadMsg;
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    activity.startActivityForResult(Intent.createChooser(i, "选择文件"), REQUEST_FILE_PICKER);
                }
            } catch (Throwable t) {
                if (uploadMsg != null) uploadMsg.onReceiveValue(null);
            }
        }

        @SuppressWarnings("unused")
        public void openFileChooser(ValueCallback<Uri> uploadMsg) {
            openFileChooser(uploadMsg, "", "");
        }

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
            try {
                if (activity != null) {
                    if (activity.uploadMessagesAboveL != null) {
                        activity.uploadMessagesAboveL.onReceiveValue(null);
                    }
                    activity.uploadMessagesAboveL = filePathCallback;
                    Intent i = fileChooserParams.createIntent();
                    i.setType("*/*");
                    activity.startActivityForResult(Intent.createChooser(i, "选择文件"), REQUEST_FILE_PICKER);
                }
            } catch (Throwable t) {
                activity.uploadMessagesAboveL = null;
                return false;
            }
            return true;
        }
    }

    public static class TabDownloadListener implements DownloadListener {
        public TabManager.Tab tab;
        public MainActivity activity;

        @Override
        public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                     String mimetype, long contentLength) {
            final String fileName = guessFileName(url, contentDisposition);
            final String finalUrl = url;
            final String finalUserAgent = userAgent;
            final String finalMime = mimetype;

            try {
                activity.showDownloadDialog(fileName, url, userAgent, mimetype, contentLength);
            } catch (Throwable tt) {
                doStartDownload(activity, finalUrl, finalUserAgent, finalMime, fileName, Environment.DIRECTORY_DOWNLOADS);
            }
        }

        private static void doStartDownload(MainActivity act, String url, String userAgent, String mimetype,
                                            String fileName, String dirType) {
            try {
                android.app.DownloadManager.Request req =
                        new android.app.DownloadManager.Request(Uri.parse(url));
                req.setMimeType(mimetype == null ? "application/octet-stream" : mimetype);
                if (userAgent != null) req.addRequestHeader("User-Agent", userAgent);
                if (act != null) {
                    TabManager.Tab t = act.currentTab();
                    if (t != null && t.url != null) req.addRequestHeader("Referer", t.url);
                }
                try {
                    req.setNotificationVisibility(
                            android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                } catch (Throwable t) {}
                try {
                    req.setDestinationInExternalPublicDir(dirType, fileName);
                } catch (Throwable t2) {
                    req.setDestinationInExternalFilesDir(act, null, fileName);
                }
                android.app.DownloadManager dm =
                        (android.app.DownloadManager) act.getSystemService(DOWNLOAD_SERVICE);
                if (dm != null) {
                    dm.enqueue(req);
                    Toast.makeText(act, "正在下载: " + fileName, Toast.LENGTH_LONG).show();
                    try { act.addDownloadHistory(url, fileName); } catch (Throwable tt) {}
                    return;
                }
            } catch (Throwable t) {}
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                act.startActivity(Intent.createChooser(i, "选择下载方式"));
                try { act.addDownloadHistory(url, fileName); } catch (Throwable tt) {}
            } catch (Throwable t) {
                Toast.makeText(act, "下载失败", Toast.LENGTH_LONG).show();
            }
        }

        private static String guessFileName(String url, String contentDisposition) {
            try {
                if (contentDisposition != null) {
                    int idx = contentDisposition.indexOf("filename=");
                    if (idx >= 0) {
                        String n = contentDisposition.substring(idx + 9)
                                .replace("\"", "").trim();
                        if (n.length() > 0) return n;
                    }
                }
            } catch (Throwable t) {}
            try {
                return android.webkit.URLUtil.guessFileName(url, null, null);
            } catch (Throwable t) {
                    return "download_file";
                }
        }
    }

    // 显示下载确认弹窗
    private void showDownloadDialog(final String fileName, final String url, final String userAgent,
                                     final String mimetype, final long contentLength) {
        if (this.isFinishing()) return;

        try {
            final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);

            int dp2 = (int) getResources().getDisplayMetrics().density;

            LinearLayout titleView = new LinearLayout(this);
            titleView.setOrientation(LinearLayout.VERTICAL);
            titleView.setPadding(16 * dp2, 16 * dp2, 16 * dp2, 8 * dp2);

            TextView titleTv = new TextView(this);
            titleTv.setText("下载文件");
            titleTv.setTextSize(18);
            titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
            titleTv.setTextColor(0xFF1C1C1E);
            titleView.addView(titleTv);

            TextView fileNameTv = new TextView(this);
            String displayName = fileName;
            if (displayName.length() > 40) {
                displayName = displayName.substring(0, 37) + "...";
            }
            fileNameTv.setText(displayName);
            fileNameTv.setTextSize(14);
            fileNameTv.setTextColor(0xFF8E8E93);
            fileNameTv.setPadding(0, 6 * dp2, 0, 0);
            titleView.addView(fileNameTv);

            if (contentLength > 0) {
                TextView sizeTv = new TextView(this);
                sizeTv.setText(formatFileSize(contentLength));
                sizeTv.setTextSize(12);
                sizeTv.setTextColor(0xFF8E8E93);
                sizeTv.setPadding(0, 2 * dp2, 0, 0);
                titleView.addView(sizeTv);
            }

            builder.setCustomTitle(titleView);

            final LinearLayout contentView = new LinearLayout(this);
            contentView.setOrientation(LinearLayout.VERTICAL);
            contentView.setPadding(16 * dp2, 8 * dp2, 16 * dp2, 8 * dp2);

            TextView folderLabel = new TextView(this);
            folderLabel.setText("保存位置");
            folderLabel.setTextSize(13);
            folderLabel.setTextColor(0xFF6D6D72);
            folderLabel.setPadding(0, 4 * dp2, 0, 8 * dp2);
            contentView.addView(folderLabel);

            final String[] folderNames = {"下载", "图片", "视频", "音乐", "文档"};
            final String[] folderDirs = {
                    Environment.DIRECTORY_DOWNLOADS,
                    Environment.DIRECTORY_PICTURES,
                    Environment.DIRECTORY_MOVIES,
                    Environment.DIRECTORY_MUSIC,
                    Environment.DIRECTORY_DOCUMENTS
            };

            final int[] selectedIdx = {0};
            final ArrayList<TextView> allIndicators = new ArrayList<TextView>();

            for (int i = 0; i < folderNames.length; i++) {
                final int idx = i;
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(8 * dp2, 10 * dp2, 8 * dp2, 10 * dp2);

                final TextView indicator = new TextView(this);
                indicator.setTextSize(14);
                indicator.setText(idx == 0 ? "\u25CF" : "\u25CB");
                indicator.setTextColor(idx == 0 ? 0xFF0A84FF : 0xFF8E8E93);
                indicator.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams indLp = new LinearLayout.LayoutParams((int)(24 * dp2), -2);
                row.addView(indicator, indLp);
                allIndicators.add(indicator);

                TextView nameTv = new TextView(this);
                nameTv.setText(folderNames[i]);
                nameTv.setTextSize(15);
                nameTv.setTextColor(0xFF1C1C1E);
                LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, -2, 1.0f);
                row.addView(nameTv, nameLp);

                row.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        selectedIdx[0] = idx;
                        for (int j = 0; j < allIndicators.size(); j++) {
                            TextView tv = allIndicators.get(j);
                            tv.setText(j == idx ? "\u25CF" : "\u25CB");
                            tv.setTextColor(j == idx ? 0xFF0A84FF : 0xFF8E8E93);
                        }
                    }
                });

                contentView.addView(row);
            }

            builder.setView(contentView);

            builder.setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface dialog, int which) {
                    try {
                        final String dirType = folderDirs[selectedIdx[0]];
                        TabDownloadListener.doStartDownload(MainActivity.this, url, userAgent, mimetype, fileName, dirType);
                    } catch (Throwable t) {
                        Toast.makeText(MainActivity.this, "下载启动失败", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            builder.setNegativeButton("取消", new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            builder.show();
        } catch (Throwable t) {
            TabDownloadListener.doStartDownload(this, url, userAgent, mimetype, fileName, Environment.DIRECTORY_DOWNLOADS);
        }
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "";
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIdx = 0;
        double s = size;
        while (s >= 1024 && unitIdx < units.length - 1) {
            s /= 1024;
            unitIdx++;
        }
        return String.format("%.1f %s", s, units[unitIdx]);
    }

    private void addDownloadHistory(String url, String fileName) {
        if (url == null) return;
        try {
            TabManager.Tab cur = currentTab();
            if (cur != null && cur.isPrivate) return;
        } catch (Throwable tt) {}
        try {
            SharedPreferences hp = getSharedPreferences(HISTORY_PREFS, MODE_PRIVATE);
            String json = hp.getString(DOWNLOAD_KEY, "[]");
            String entry = "{\"t\":" + System.currentTimeMillis()
                    + ",\"u\":\"" + escapeJson(url) + "\""
                    + ",\"name\":\"" + escapeJson(fileName == null ? url : fileName) + "\"}";
            String newJson = "[" + entry;
            int count = 1;
            if (json != null && json.length() > 2) {
                String inner = json.substring(1, json.length() - 1);
                if (inner.length() > 0) {
                    ArrayList<String> items = splitJsonArray(inner);
                    for (String it : items) {
                        if (it == null) continue;
                        if (count >= 100) break;
                        newJson += ",{" + it + "}";
                        count++;
                    }
                }
            }
            newJson += "]";
            hp.edit().putString(DOWNLOAD_KEY, newJson).apply();
        } catch (Throwable t) {}
    }

    private static final int REQUEST_NOTIFICATION_PERMISSION = 2001;
    private static final int REQUEST_DEFAULT_BROWSER_ROLE = 2002;

    // 申请默认浏览器
    private void requestDefaultBrowser() {
        try {
            if (isDefaultBrowser(this)) return;
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                try {
                    android.app.role.RoleManager roleManager = getSystemService(android.app.role.RoleManager.class);
                    if (roleManager != null && roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_BROWSER)) {
                        if (!roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_BROWSER)) {
                            android.content.Intent intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_BROWSER);
                            startActivityForResult(intent, REQUEST_DEFAULT_BROWSER_ROLE);
                            return;
                        }
                    }
                } catch (Throwable t) {}
            }
            
            try {
                android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                Toast.makeText(this, "请选择梦林浏览器", Toast.LENGTH_LONG).show();
            } catch (Throwable t) {
                try {
                    android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    Toast.makeText(this, "请在设置中设为默认浏览器", Toast.LENGTH_LONG).show();
                } catch (Throwable tt) {
                    Toast.makeText(this, "请在系统设置中手动设置", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Throwable t) {
            if (prefs != null) {
                prefs.edit().putInt("asked_default_browser", 2).apply();
            }
        }
    }

    // 检查是否为默认浏览器
    public static boolean isDefaultBrowser(android.content.Context ctx) {
        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("https://menglinbrowser.com"));
            android.content.pm.ResolveInfo info = ctx.getPackageManager().resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
            if (info != null && info.activityInfo.packageName.equals(ctx.getPackageName())) return true;
        } catch (Throwable t) {}
        return false;
    }

    // 申请通知权限（Android 13+）
    private boolean hasNotificationPermission() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                return checkSelfPermission("android.permission.POST_NOTIFICATIONS") == android.content.pm.PackageManager.PERMISSION_GRANTED;
            }
        } catch (Throwable t) {}
        return true;
    }

    private void ensureNotificationChannel() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                android.app.NotificationChannel channel = nm.getNotificationChannel("default");
                if (channel == null) {
                    channel = new android.app.NotificationChannel("default", "梦林浏览器", android.app.NotificationManager.IMPORTANCE_LOW);
                    channel.setDescription("应用通知");
                    nm.createNotificationChannel(channel);
                }
            }
        } catch (Throwable t) {}
    }

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
            prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        } catch (Throwable t) { prefs = null; }
        handler = new Handler();

        // 首次安装申请默认浏览器
        try {
            int asked = prefs.getInt("asked_default_browser", 0);
            if (asked == 0) {
                prefs.edit().putInt("asked_default_browser", 1).apply();
                requestDefaultBrowser();
            }
        } catch (Throwable t) {}

        float density = getResources().getDisplayMetrics().density;
        int dp = (int) density;

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        boolean isSmallScreen = screenWidth < 300;

        boolean dark = false;
        try {
            int darkMode = prefs.getInt(SettingsActivity.KEY_DARK_MODE, 0);
            if (darkMode == 2) dark = true;
            else if (darkMode == 0) {
                int ui = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                dark = (ui == Configuration.UI_MODE_NIGHT_YES);
            }
        } catch (Throwable t) {}

        int bgColor = dark ? 0xFF1A1A1A : 0xFFF2F2F7;
        int cardBg = dark ? 0xFF2D2D2D : 0xFFFFFFFF;
        int titleColor = dark ? 0xFFFFFFFF : 0xFF1C1C1E;
        int subColor = dark ? 0xFFAAAAAA : 0xFF8E8E93;
        int accentColor = 0xFF1E88E5;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bgColor);
        root.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));

        try {
            int statusBarHeight = 0;
            int rid = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (rid > 0) statusBarHeight = getResources().getDimensionPixelSize(rid);
            if (statusBarHeight <= 0) statusBarHeight = (int)(25 * dp);
            View statusBarSpacer = new View(this);
            statusBarSpacer.setLayoutParams(new LinearLayout.LayoutParams(-1, statusBarHeight));
            statusBarSpacer.setBackgroundColor(cardBg);
            root.addView(statusBarSpacer);
        } catch (Throwable t) {}

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setVisibility(View.GONE);
        try {
            progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(accentColor));
        } catch (Throwable t) {}
        int progressHeight = isSmallScreen ? (int)(2 * dp) : (int)(3 * dp);
        root.addView(progressBar, new LinearLayout.LayoutParams(-1, progressHeight));

        webViewContainer = new FrameLayout(this);
        LinearLayout.LayoutParams wvLp = new LinearLayout.LayoutParams(-1, -1);
        wvLp.weight = 1.0f;
        root.addView(webViewContainer, wvLp);

        urlBar = new EditText(this);
        urlBar.setHint("搜索或输入网址");
        urlBar.setTextColor(titleColor);
        urlBar.setHintTextColor(subColor);
        urlBar.setTextSize(isSmallScreen ? 12 : 14);
        urlBar.setSingleLine(true);
        urlBar.setFocusable(false);
        urlBar.setFocusableInTouchMode(false);
        urlBar.setCursorVisible(false);
        urlBar.setBackgroundColor(0x00000000);
        int urlBarPadding = isSmallScreen ? (int)(6 * dp) : (int)(10 * dp);
        urlBar.setPadding(urlBarPadding, (int)(5 * dp), urlBarPadding, (int)(5 * dp));
        try {
            GradientDrawable urlBg = new GradientDrawable();
            urlBg.setColor(dark ? 0xFF1E1E1E : 0xFFF0F0F0);
            urlBg.setCornerRadius(isSmallScreen ? (int)(16 * dp) : (int)(20 * dp));
            urlBar.setBackgroundDrawable(urlBg);
        } catch (Throwable t) {}
        int urlBarHeight = isSmallScreen ? (int)(32 * dp) : (int)(40 * dp);
        LinearLayout.LayoutParams urlBarLp = new LinearLayout.LayoutParams(-1, urlBarHeight);
        urlBarLp.leftMargin = (int)(6 * dp);
        urlBarLp.rightMargin = (int)(6 * dp);
        urlBarLp.topMargin = (int)(4 * dp);
        root.addView(urlBar, urlBarLp);

        urlBar.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                showUrlDialog();
            }
        });

        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER);
        bottomBar.setBackgroundColor(cardBg);
        int bottomBarHeight = isSmallScreen ? (int)(40 * dp) : (int)(48 * dp);
        bottomBar.setLayoutParams(new LinearLayout.LayoutParams(-1, bottomBarHeight));
        root.addView(bottomBar);

        TextView backBtn = new TextView(this);
        backBtn.setText("\u2190");
        backBtn.setTextColor(accentColor);
        backBtn.setTextSize(isSmallScreen ? 16 : 20);
        backBtn.setGravity(Gravity.CENTER);
        bottomBar.addView(backBtn, makeNavLp(dp, isSmallScreen));

        TextView forwardBtn = new TextView(this);
        forwardBtn.setText("\u2192");
        forwardBtn.setTextColor(accentColor);
        forwardBtn.setTextSize(isSmallScreen ? 16 : 20);
        forwardBtn.setGravity(Gravity.CENTER);
        bottomBar.addView(forwardBtn, makeNavLp(dp, isSmallScreen));

        TextView homeBtn = new TextView(this);
        homeBtn.setText("\u2302");
        homeBtn.setTextColor(accentColor);
        homeBtn.setTextSize(isSmallScreen ? 14 : 18);
        homeBtn.setGravity(Gravity.CENTER);
        bottomBar.addView(homeBtn, makeNavLp(dp, isSmallScreen));

        btnTabs = new TextView(this);
        btnTabs.setText(String.valueOf(TabManager.TABS.size()));
        btnTabs.setTextColor(accentColor);
        btnTabs.setTextSize(isSmallScreen ? 11 : 13);
        btnTabs.setGravity(Gravity.CENTER);
        btnTabs.setTypeface(null, android.graphics.Typeface.BOLD);
        try {
            android.graphics.drawable.GradientDrawable tabsBg = new android.graphics.drawable.GradientDrawable();
            tabsBg.setColor(0x00000000);
            tabsBg.setCornerRadius(isSmallScreen ? (int)(6 * dp) : (int)(8 * dp));
            tabsBg.setStroke((int)(1.5f * dp), accentColor, (int)(3 * dp), (int)(3 * dp));
            btnTabs.setBackgroundDrawable(tabsBg);
        } catch (Throwable t) {
            btnTabs.setBackgroundColor(0x221E88E5);
        }
        int tabsDim = isSmallScreen ? (int)(28 * dp) : (int)(36 * dp);
        LinearLayout.LayoutParams tabsLp = new LinearLayout.LayoutParams(0, tabsDim, 1.0f);
        tabsLp.gravity = Gravity.CENTER;
        bottomBar.addView(btnTabs, tabsLp);

        final TextView menuBtn = new TextView(this);
        menuBtn.setText("\u22EE");
        menuBtn.setTextColor(accentColor);
        menuBtn.setTextSize(isSmallScreen ? 18 : 22);
        menuBtn.setGravity(Gravity.CENTER);
        bottomBar.addView(menuBtn, makeNavLp(dp, isSmallScreen));

        setContentView(root);

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    TabManager.Tab t = currentTab();
                    if (t != null && t.webView != null && t.webView.canGoBack()) t.webView.goBack();
                } catch (Throwable t) {}
            }
        });
        forwardBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    TabManager.Tab t = currentTab();
                    if (t != null && t.webView != null && t.webView.canGoForward()) t.webView.goForward();
                } catch (Throwable t) {}
            }
        });
        homeBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    TabManager.Tab t = currentTab();
                    if (t != null && t.webView != null) loadHomepage(t.webView);
                } catch (Throwable tt) {}
            }
        });
        btnTabs.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    Intent i = new Intent(MainActivity.this, TabListActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(i);
                } catch (Throwable t) {}
            }
        });
        menuBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                showCustomMenu();
            }
        });
        urlBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) { go(); return true; }
                return false;
            }
        });

        if (TabManager.TABS.isEmpty()) {
            TabManager.Tab t = createNewTab(false);
            TabManager.TABS.add(t);
            TabManager.CURRENT_TAB_INDEX = 0;
            attachCurrentTab();
            String openUrl = getIntent().getDataString();
            if (openUrl == null || openUrl.length() == 0) {
                openUrl = getIntent().getStringExtra("open_url");
            }
            if (openUrl != null && openUrl.length() > 0) {
                t.webView.loadUrl(openUrl);
                getIntent().removeExtra("open_url");
            } else {
                loadHomepage(t.webView);
            }
        } else {
            attachCurrentTab();
            try {
                String openUrl2 = getIntent().getStringExtra("open_url");
                if (openUrl2 != null && openUrl2.length() > 0) {
                    TabManager.Tab tt = currentTab();
                    if (tt != null && tt.webView != null) tt.webView.loadUrl(openUrl2);
                    getIntent().removeExtra("open_url");
                }
            } catch (Throwable tt) {}
        }
        updateTabsCount();
    }

    private LinearLayout.LayoutParams makeNavLp(int dp, boolean isSmallScreen) {
        int btnHeight = isSmallScreen ? (int)(32 * dp) : (int)(40 * dp);
        int margin = isSmallScreen ? (int)(1 * dp) : (int)(2 * dp);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, btnHeight, 1.0f);
        lp.leftMargin = margin;
        lp.rightMargin = margin;
        return lp;
    }

    private TabManager.Tab currentTab() {
        try {
            if (TabManager.TABS.isEmpty()) return null;
            if (TabManager.CURRENT_TAB_INDEX < 0 || TabManager.CURRENT_TAB_INDEX >= TabManager.TABS.size())
                TabManager.CURRENT_TAB_INDEX = 0;
            return TabManager.TABS.get(TabManager.CURRENT_TAB_INDEX);
        } catch (Throwable t) { return null; }
    }

    private TabManager.Tab createNewTab(boolean isPrivate) {
        TabManager.Tab tab = new TabManager.Tab();
        tab.id = TabManager.NEXT_TAB_ID++;
        tab.isPrivate = isPrivate;
        tab.title = isPrivate ? "隐私标签页" : "新标签页";
        tab.url = "";

        WebView wv = new WebView(this);
        tab.webView = wv;

        try {
            WebSettings s = wv.getSettings();
            boolean js = prefs == null ? true : (prefs.getInt(SettingsActivity.KEY_JS_ENABLED, 1) == 1);
            boolean imgs = prefs == null ? true : (prefs.getInt(SettingsActivity.KEY_IMAGES_ENABLED, 1) == 1);
            boolean cache = prefs == null ? true : (prefs.getInt(SettingsActivity.KEY_CACHE_ENABLED, 1) == 1);
            boolean autoFit = prefs == null ? true : (prefs.getInt(SettingsActivity.KEY_AUTO_FIT, 1) == 1);

            s.setJavaScriptEnabled(js);
            s.setDomStorageEnabled(true);
            s.setDatabaseEnabled(!isPrivate);
            s.setLoadsImagesAutomatically(imgs);
            s.setBlockNetworkImage(false);
            s.setBuiltInZoomControls(true);
            s.setDisplayZoomControls(false);
            s.setSupportZoom(true);
            s.setLoadWithOverviewMode(true);
            s.setUseWideViewPort(true);
            s.setDefaultTextEncodingName("utf-8");
            s.setMediaPlaybackRequiresUserGesture(false);
            s.setJavaScriptCanOpenWindowsAutomatically(false);
            s.setSupportMultipleWindows(false);
            s.setTextZoom(prefs == null ? 100 : prefs.getInt(SettingsActivity.KEY_TEXT_ZOOM, 100));
            s.setGeolocationEnabled(false);
            s.setNeedInitialFocus(true);

            try { s.setRenderPriority(WebSettings.RenderPriority.HIGH); } catch (Throwable t) {}

            try {
                s.setCacheMode(cache && !isPrivate ? WebSettings.LOAD_DEFAULT : WebSettings.LOAD_NO_CACHE);
                if (!isPrivate && cache) {
                    try {
                        String cacheDir = MainActivity.this.getApplicationContext().getDir("webview_cache", 0).getPath();
                        try {
                            java.lang.reflect.Method setAppCacheEnabled = WebSettings.class.getMethod("setAppCacheEnabled", boolean.class);
                            setAppCacheEnabled.invoke(s, true);
                        } catch (Throwable tt) {}
                        try {
                            java.lang.reflect.Method setAppCachePath = WebSettings.class.getMethod("setAppCachePath", String.class);
                            setAppCachePath.invoke(s, cacheDir);
                        } catch (Throwable tt) {}
                        try {
                            java.lang.reflect.Method setAppCacheMaxSize = WebSettings.class.getMethod("setAppCacheMaxSize", long.class);
                            setAppCacheMaxSize.invoke(s, 20L * 1024 * 1024);
                        } catch (Throwable tt) {}
                    } catch (Throwable tt) {}
                }
            } catch (Throwable t) {}

            s.setAllowFileAccess(false);
            s.setAllowContentAccess(false);
            try { s.setAllowFileAccessFromFileURLs(false); } catch (Throwable t) {}
            try { s.setAllowUniversalAccessFromFileURLs(false); } catch (Throwable t) {}

            try { s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE); } catch (Throwable t) {}

            if (autoFit) {
                s.setLoadWithOverviewMode(true);
                s.setUseWideViewPort(true);
            }

            try { wv.setLayerType(View.LAYER_TYPE_HARDWARE, null); } catch (Throwable t) {}

            if (isPrivate) {
                try {
                    CookieManager cm = CookieManager.getInstance();
                    cm.setAcceptCookie(false);
                    cm.setAcceptThirdPartyCookies(wv, false);
                } catch (Throwable t) {}
            } else {
                try {
                    boolean cookies = prefs.getInt(SettingsActivity.KEY_COOKIES_ENABLED, 1) == 1;
                    CookieManager cm = CookieManager.getInstance();
                    cm.setAcceptCookie(cookies);
                    cm.setAcceptThirdPartyCookies(wv, cookies);
                } catch (Throwable t) {}
            }
        } catch (Throwable t) {}

        try {
            TabClient client = new TabClient();
            client.tab = tab;
            client.activity = this;
            wv.setWebViewClient(client);

            TabChromeClient chrome = new TabChromeClient();
            chrome.tab = tab;
            chrome.activity = this;
            wv.setWebChromeClient(chrome);

            TabDownloadListener dl = new TabDownloadListener();
            dl.tab = tab;
            dl.activity = this;
            wv.setDownloadListener(dl);
        } catch (Throwable t) {}

        try {
            wv.addJavascriptInterface(new HomepageBgInterface(), "YulanApp");
        } catch (Throwable t) {}

        return tab;
    }

    // JavaScript接口：供主页读取背景图片和导航
    public class HomepageBgInterface {

        @android.webkit.JavascriptInterface
        public String getHomepageBg() {
            try {
                if (prefs != null) {
                    return prefs.getString(SettingsActivity.KEY_HOMEPAGE_BG, "");
                }
            } catch (Throwable t) {}
            return "";
        }

        @android.webkit.JavascriptInterface
        public void loadUrl(final String url) {
            try {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            TabManager.Tab t = currentTab();
                            if (t != null && t.webView != null) {
                                t.webView.loadUrl(url);
                            }
                        } catch (Throwable tt) {}
                    }
                });
            } catch (Throwable t) {}
        }

        @android.webkit.JavascriptInterface
        public void showTabList() {
            try {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Intent i = new Intent(MainActivity.this, TabListActivity.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                            startActivity(i);
                        } catch (Throwable t) {}
                    }
                });
            } catch (Throwable t) {}
        }

        @android.webkit.JavascriptInterface
        public void showMenu() {
            try {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showCustomMenu();
                    }
                });
            } catch (Throwable t) {}
        }

        @android.webkit.JavascriptInterface
        public void setHomepageBg(final String dataUrl) {
            try {
                if (prefs != null && dataUrl != null) {
                    prefs.edit().putString(SettingsActivity.KEY_HOMEPAGE_BG, dataUrl).apply();
                }
            } catch (Throwable t) {}
        }
    }

    private void attachCurrentTab() {
        TabManager.Tab tab = currentTab();
        if (tab == null || tab.webView == null) return;
        try {
            webViewContainer.removeAllViews();
            if (tab.webView.getParent() != null)
                ((ViewGroup) tab.webView.getParent()).removeView(tab.webView);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -1);
            webViewContainer.addView(tab.webView, lp);
            setUrlBar(tab.url);
        } catch (Throwable t) {}
    }

    private void updateTabsCount() {
        try {
            int n = TabManager.TABS.size();
            btnTabs.setText(String.valueOf(n));
        } catch (Throwable t) {}
    }

    private void go() {
        try {
            String input = urlBar.getText() == null ? "" : urlBar.getText().toString().trim();
            if (input.length() == 0) return;
            String url;
            if (input.startsWith("http://") || input.startsWith("https://") ||
                    input.startsWith("about:") || input.startsWith("file://")) {
                url = input;
            } else if (input.contains(".") && !input.contains(" ")) {
                url = "http://" + input;
            } else {
                url = SettingsActivity.getSearchUrl(this) + Uri.encode(input);
            }
            TabManager.Tab t = currentTab();
            if (t != null && t.webView != null) t.webView.loadUrl(url);
        } catch (Throwable t) {
            Toast.makeText(this, "访问失败", Toast.LENGTH_SHORT).show();
        }
    }

    // 文件上传支持
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_DEFAULT_BROWSER_ROLE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "已设为默认浏览器", Toast.LENGTH_SHORT).show();
            } else {
                if (prefs != null) {
                    prefs.edit().putInt("asked_default_browser", 2).apply();
                }
            }
            return;
        }
        
        if (requestCode == REQUEST_FILE_PICKER) {
            if (resultCode == RESULT_CANCELED) {
            } else if (resultCode == RESULT_OK) {
                if (data != null) {
                    Uri uri = data.getData();
                    if (uri != null) {
                        if (uploadMessage != null) {
                            uploadMessage.onReceiveValue(uri);
                            uploadMessage = null;
                        }
                        if (uploadMessagesAboveL != null) {
                            uploadMessagesAboveL.onReceiveValue(new Uri[]{uri});
                            uploadMessagesAboveL = null;
                        }
                    }
                }
            }
            try {
                TabManager.Tab t = currentTab();
                if (t != null && t.webView != null) t.webView.requestFocus();
            } catch (Throwable tt) {}
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        try {
            String openUrl = intent.getStringExtra("open_url");
            if (openUrl != null && openUrl.length() > 0) {
                TabManager.Tab t = currentTab();
                if (t != null && t.webView != null) {
                    t.webView.loadUrl(openUrl);
                }
            }
        } catch (Throwable t) {}
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean needReload = false;

        if (TabManager.PENDING_CLOSE_INDEX >= 0) {
            int idx = TabManager.PENDING_CLOSE_INDEX;
            TabManager.PENDING_CLOSE_INDEX = -1;
            try { closeTab(idx); needReload = true; } catch (Throwable t) {}
        }

        if (TabManager.PENDING_SWITCH_INDEX >= 0) {
            int idx = TabManager.PENDING_SWITCH_INDEX;
            TabManager.PENDING_SWITCH_INDEX = -1;
            if (idx >= 0 && idx < TabManager.TABS.size()) {
                TabManager.CURRENT_TAB_INDEX = idx;
                attachCurrentTab();
                needReload = true;
            }
        }

        if (TabManager.PENDING_ACTION == 1 || TabManager.PENDING_ACTION == 2) {
            boolean isPrivate = (TabManager.PENDING_ACTION == 2);
            TabManager.PENDING_ACTION = 0;
            TabManager.Tab t = createNewTab(isPrivate);
            TabManager.TABS.add(t);
            TabManager.CURRENT_TAB_INDEX = TabManager.TABS.size() - 1;
            attachCurrentTab();
            loadHomepage(t.webView);
            needReload = true;
        }

        try {
            String openUrl = getIntent().getStringExtra("open_url");
            if (openUrl != null && openUrl.length() > 0) {
                TabManager.Tab t = currentTab();
                if (t != null && t.webView != null) t.webView.loadUrl(openUrl);
                getIntent().removeExtra("open_url");
                needReload = true;
            }
        } catch (Throwable t) {}

        if (needReload) {
            updateTabsCount();
            TabManager.Tab cur = currentTab();
            if (cur != null) setUrlBar(cur.url);
        }

        try { if (currentTab() != null && currentTab().webView != null) currentTab().webView.onResume(); } catch (Throwable t) {}
    }

    private void closeTab(int idx) {
        if (idx < 0 || idx >= TabManager.TABS.size()) return;
        TabManager.Tab tab = TabManager.TABS.get(idx);
        try {
            if (tab.webView != null) {
                if (tab.webView.getParent() != null)
                    ((ViewGroup) tab.webView.getParent()).removeView(tab.webView);
                tab.webView.stopLoading();
                tab.webView.removeAllViews();
                tab.webView.destroy();
                tab.webView = null;
            }
        } catch (Throwable t) {}
        TabManager.TABS.remove(idx);

        if (TabManager.TABS.isEmpty()) {
            TabManager.Tab t = createNewTab(false);
            TabManager.TABS.add(t);
            TabManager.CURRENT_TAB_INDEX = 0;
            attachCurrentTab();
            loadHomepage(t.webView);
            return;
        }

        if (TabManager.CURRENT_TAB_INDEX > idx) TabManager.CURRENT_TAB_INDEX--;
        if (TabManager.CURRENT_TAB_INDEX >= TabManager.TABS.size())
            TabManager.CURRENT_TAB_INDEX = TabManager.TABS.size() - 1;
        attachCurrentTab();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        try {
            TabManager.Tab t = currentTab();
            if (keyCode == KeyEvent.KEYCODE_BACK && t != null && t.webView != null && t.webView.canGoBack()) {
                t.webView.goBack();
                return true;
            }
        } catch (Throwable t) {}
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try { TabManager.Tab t = currentTab(); if (t != null && t.webView != null) t.webView.onPause(); } catch (Throwable t) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
