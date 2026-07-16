package com.menglin.cn;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class HistoryActivity extends Activity {

    public static class Item {
        public String url;
        public String title;
        public String name;
        public long time;
        public boolean isDownload;
    }

    private ArrayList<Item> parseItems(String json) {
        ArrayList<Item> items = new ArrayList<Item>();
        if (json == null || json.length() < 3) return items;
        try {
            String inner = json.substring(1, json.length() - 1);
            if (inner.length() == 0) return items;
            ArrayList<String> parts = splitJsonObjects(inner);
            for (String p : parts) {
                if (p == null) continue;
                Item it = new Item();
                it.time = extractLongField(p, "t");
                it.url = extractStringField(p, "u");
                it.title = extractStringField(p, "title");
                it.name = extractStringField(p, "name");
                if (it.url != null && it.url.length() > 0) items.add(it);
            }
        } catch (Throwable t) {}
        return items;
    }

    private static ArrayList<String> splitJsonObjects(String inner) {
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

    private static String extractStringField(String json, String key) {
        try {
            String marker = "\"" + key + "\":";
            int idx = json.indexOf(marker);
            if (idx < 0) return "";
            int start = idx + marker.length();
            if (start >= json.length()) return "";
            if (json.charAt(start) != '\"') return "";
            int end = json.indexOf("\"", start + 1);
            while (end > 0 && json.charAt(end - 1) == '\\') end = json.indexOf("\"", end + 1);
            if (end < 0) return json.substring(start + 1);
            String s = json.substring(start + 1, end);
            return s.replace("\\\"", "\"").replace("\\\\", "\\");
        } catch (Throwable t) { return ""; }
    }

    private static long extractLongField(String json, String key) {
        try {
            String marker = "\"" + key + "\":";
            int idx = json.indexOf(marker);
            if (idx < 0) return 0;
            int start = idx + marker.length();
            int end = json.indexOf(",", start);
            if (end < 0) end = json.length();
            String num = json.substring(start, end).trim();
            return Long.parseLong(num);
        } catch (Throwable t) { return 0; }
    }

    private boolean showDownload = false;

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

        int dp = (int) getResources().getDisplayMetrics().density;

        boolean dark = false;
        try {
            int ui = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            dark = (ui == Configuration.UI_MODE_NIGHT_YES);
        } catch (Throwable t) {}

        final int bgColor = dark ? 0xFF1A1A1A : 0xFFF2F2F7;
        final int cardBg = dark ? 0xFF2D2D2D : 0xFFFFFFFF;
        final int titleColor = dark ? 0xFFFFFFFF : 0xFF1C1C1E;
        final int subColor = dark ? 0xFFAAAAAA : 0xFF8E8E93;
        final int accentColor = 0xFF1E88E5;
        final int dividerColor = dark ? 0xFF1A1A1A : 0xFFF2F2F7;

        // 根布局
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bgColor);
        root.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));

        // 状态栏占位
        try {
            int statusBarHeight = 0;
            int rid = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (rid > 0) statusBarHeight = getResources().getDimensionPixelSize(rid);
            if (statusBarHeight <= 0) statusBarHeight = (int)(25 * dp);
            View statusSpacer = new View(this);
            statusSpacer.setLayoutParams(new LinearLayout.LayoutParams(-1, statusBarHeight));
            statusSpacer.setBackgroundColor(cardBg);
            root.addView(statusSpacer);
        } catch (Throwable t) {}

        // 标题栏
        LinearLayout titleBar = new LinearLayout(this);
        titleBar.setOrientation(LinearLayout.HORIZONTAL);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setBackgroundColor(cardBg);
        titleBar.setLayoutParams(new LinearLayout.LayoutParams(-1, (int)(48 * dp)));
        titleBar.setPadding(12 * dp, 0, 8 * dp, 0);

        TextView titleText = new TextView(this);
        titleText.setText(showDownload ? "下载历史" : "浏览历史");
        titleText.setTextColor(titleColor);
        titleText.setTextSize(16);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, -2, 1.0f);
        titleBar.addView(titleText, titleLp);

        final TextView toggleBtn = new TextView(this);
        toggleBtn.setText(showDownload ? "历史" : "下载");
        toggleBtn.setTextColor(accentColor);
        toggleBtn.setTextSize(14);
        toggleBtn.setGravity(Gravity.CENTER);
        toggleBtn.setPadding(12 * dp, 0, 12 * dp, 0);
        titleBar.addView(toggleBtn, new LinearLayout.LayoutParams(-2, -1));

        TextView clearBtn = new TextView(this);
        clearBtn.setText("清空");
        clearBtn.setTextColor(accentColor);
        clearBtn.setTextSize(14);
        clearBtn.setGravity(Gravity.CENTER);
        clearBtn.setPadding(12 * dp, 0, 12 * dp, 0);
        titleBar.addView(clearBtn, new LinearLayout.LayoutParams(-2, -1));

        root.addView(titleBar);

        // 内容容器（可滚动）
        ScrollView sv = new ScrollView(this);
        sv.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1.0f));

        final LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        sv.addView(list);
        root.addView(sv);
        setContentView(root);

        // 渲染列表
        final Runnable render = new Runnable() {
            @Override public void run() {
                try {
                    titleText.setText(showDownload ? "下载历史" : "浏览历史");
                    toggleBtn.setText(showDownload ? "历史" : "下载");
                    list.removeAllViews();
                    SharedPreferences hp = getSharedPreferences(MainActivity.HISTORY_PREFS, MODE_PRIVATE);
                    String key = showDownload ? MainActivity.DOWNLOAD_KEY : MainActivity.HISTORY_KEY;
                    String json = hp.getString(key, "[]");
                    ArrayList<Item> items = parseItems(json);

                    if (items.size() == 0) {
                        TextView empty = new TextView(HistoryActivity.this);
                        empty.setText(showDownload ? "暂无下载记录" : "暂无浏览历史");
                        empty.setTextColor(subColor);
                        empty.setTextSize(15);
                        empty.setGravity(Gravity.CENTER);
                        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(-1, (int)(120 * dp));
                        elp.topMargin = 40 * dp;
                        list.addView(empty, elp);
                        return;
                    }

                    int cardDp = (int) getResources().getDisplayMetrics().density;
                    SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());

                    for (int i = 0; i < items.size(); i++) {
                        final Item it = items.get(i);
                        LinearLayout row = new LinearLayout(HistoryActivity.this);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setGravity(Gravity.CENTER_VERTICAL);
                        row.setBackgroundColor(cardBg);
                        row.setPadding(16 * cardDp, 14 * cardDp, 16 * cardDp, 14 * cardDp);

                        // 左侧图标
                        TextView icon = new TextView(HistoryActivity.this);
                        String dispTitle = (it.title != null && it.title.length() > 0) ? it.title : it.url;
                        if (showDownload) dispTitle = (it.name != null && it.name.length() > 0) ? it.name : it.url;
                        char first = '•';
                        try {
                            String d = dispTitle;
                            if (d.length() > 0) {
                                for (int k = 0; k < d.length(); k++) {
                                    char c = Character.toUpperCase(d.charAt(k));
                                    if (Character.isLetterOrDigit(c)) { first = c; break; }
                                }
                            }
                        } catch (Throwable tt) {}
                        icon.setText(String.valueOf(first));
                        icon.setTextSize(14);
                        icon.setTextColor(0xFFFFFFFF);
                        icon.setGravity(Gravity.CENTER);
                        try {
                            android.graphics.drawable.GradientDrawable ig = new android.graphics.drawable.GradientDrawable();
                            ig.setColor(accentColor);
                            ig.setCornerRadius(14 * cardDp);
                            icon.setBackgroundDrawable(ig);
                        } catch (Throwable tt) {
                            icon.setBackgroundColor(accentColor);
                        }
                        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams((int)(36 * cardDp), (int)(36 * cardDp));
                        list.addView(row);
                        row.addView(icon, ilp);

                        // 右侧文字
                        LinearLayout textWrap = new LinearLayout(HistoryActivity.this);
                        textWrap.setOrientation(LinearLayout.VERTICAL);
                        LinearLayout.LayoutParams twlp = new LinearLayout.LayoutParams(0, -2, 1.0f);
                        twlp.leftMargin = 12 * cardDp;
                        twlp.rightMargin = 8 * cardDp;
                        row.addView(textWrap, twlp);

                        TextView titleTv = new TextView(HistoryActivity.this);
                        titleTv.setText(dispTitle);
                        titleTv.setTextColor(titleColor);
                        titleTv.setTextSize(14);
                        titleTv.setMaxLines(1);
                        titleTv.setEllipsize(TextUtils.TruncateAt.END);
                        titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
                        textWrap.addView(titleTv);

                        TextView urlTv = new TextView(HistoryActivity.this);
                        urlTv.setText(it.url);
                        urlTv.setTextColor(subColor);
                        urlTv.setTextSize(12);
                        urlTv.setMaxLines(1);
                        urlTv.setEllipsize(TextUtils.TruncateAt.MIDDLE);
                        urlTv.setPadding(0, 2 * cardDp, 0, 0);
                        textWrap.addView(urlTv);

                        if (it.time > 0) {
                            TextView timeTv = new TextView(HistoryActivity.this);
                            String ts;
                            try { ts = sdf.format(new Date(it.time)); } catch (Throwable tt) { ts = ""; }
                            timeTv.setText(ts);
                            timeTv.setTextColor(subColor);
                            timeTv.setTextSize(11);
                            timeTv.setPadding(0, 2 * cardDp, 0, 0);
                            textWrap.addView(timeTv);
                        }

                        // 点击事件
                        row.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View v) {
                                try {
                                    if (showDownload) {
                                        // 下载记录：尝试打开本地文件
                                        String fileName = (it.name != null && it.name.length() > 0) ? it.name : "";
                                        if (fileName.length() == 0) {
                                            int si = it.url.lastIndexOf('/');
                                            if (si >= 0 && si < it.url.length() - 1) fileName = it.url.substring(si + 1);
                                        }
                                        if (fileName.length() > 0) {
                                            java.io.File downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                                            java.io.File file = new java.io.File(downloadDir, fileName);
                                            if (file.exists()) {
                                                String path = file.getAbsolutePath().toLowerCase();
                                                String mimeType = "*/*";
                                                if (path.endsWith(".apk")) mimeType = "application/vnd.android.package-archive";
                                                else if (path.endsWith(".mp3") || path.endsWith(".wav") || path.endsWith(".m4a")) mimeType = "audio/*";
                                                else if (path.endsWith(".mp4") || path.endsWith(".avi") || path.endsWith(".mkv")) mimeType = "video/*";
                                                else if (path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png") || path.endsWith(".gif")) mimeType = "image/*";
                                                else if (path.endsWith(".txt") || path.endsWith(".log")) mimeType = "text/plain";
                                                else if (path.endsWith(".zip") || path.endsWith(".rar") || path.endsWith(".7z")) mimeType = "application/zip";
                                                else if (path.endsWith(".pdf")) mimeType = "application/pdf";

                                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                                intent.setDataAndType(Uri.fromFile(file), mimeType);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                Intent chooser = Intent.createChooser(intent, "选择打开方式");
                                                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                startActivity(chooser);
                                            } else {
                                                // 文件不存在，打开下载目录让用户选择
                                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                                intent.setDataAndType(Uri.parse(downloadDir.getAbsolutePath()), "resource/folder");
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                Intent chooser = Intent.createChooser(intent, "选择文件管理器");
                                                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                try {
                                                    startActivity(chooser);
                                                } catch (Throwable tt) {
                                                    Toast.makeText(HistoryActivity.this, "文件未找到：" + fileName, Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        } else {
                                            Toast.makeText(HistoryActivity.this, "无法确定文件名", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        // 浏览历史：返回浏览器并打开URL
                                        Intent bi = new Intent(HistoryActivity.this, MainActivity.class);
                                        bi.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                        bi.putExtra("open_url", it.url);
                                        try {
                                            startActivity(bi);
                                        } catch (Throwable tt) {
                                            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(it.url));
                                            Intent chooser = Intent.createChooser(i, "选择浏览器");
                                            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            try { startActivity(chooser); } catch (Throwable ttt) {}
                                        }
                                    }
                                    finish();
                                } catch (Throwable t) {
                                    Toast.makeText(HistoryActivity.this, "无法打开", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                        // 分隔线
                        if (i < items.size() - 1) {
                            View divider = new View(HistoryActivity.this);
                            divider.setBackgroundColor(dividerColor);
                            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-1, (int)(0.5 * cardDp));
                            list.addView(divider, dlp);
                        }
                    }
                } catch (Throwable t) {
                    Toast.makeText(HistoryActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                }
            }
        };
        render.run();

        // 切换按钮
        toggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                showDownload = !showDownload;
                render.run();
            }
        });

        // 清空按钮
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    SharedPreferences hp = getSharedPreferences(MainActivity.HISTORY_PREFS, MODE_PRIVATE);
                    String key = showDownload ? MainActivity.DOWNLOAD_KEY : MainActivity.HISTORY_KEY;
                    hp.edit().putString(key, "[]").apply();
                    render.run();
                    Toast.makeText(HistoryActivity.this, "已清空", Toast.LENGTH_SHORT).show();
                } catch (Throwable t) {}
            }
        });
    }
}
