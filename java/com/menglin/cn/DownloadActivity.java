package com.menglin.cn;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class DownloadActivity extends Activity {

    public static class Item {
        public String url;
        public String name;
        public long time;
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

    private void openDownloadFolder() {
        try {
            java.io.File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            Uri folderUri = Uri.parse(downloadDir.getAbsolutePath());

            // 使用Intent.createChooser让用户选择文件管理器
            Intent base = new Intent(Intent.ACTION_VIEW);
            base.setDataAndType(folderUri, "resource/folder");
            base.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            Intent chooser = Intent.createChooser(base, "选择文件管理器");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(chooser);
        } catch (Throwable t) {
            try {
                // 兜底方式
                java.io.File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(downloadDir), "*/*");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Intent chooser = Intent.createChooser(intent, "选择文件管理器");
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(chooser);
            } catch (Throwable tt) {
                Toast.makeText(this, "请在文件管理器中找到Download目录", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openFileByName(String fileName) {
        try {
            java.io.File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            java.io.File file = new java.io.File(downloadDir, fileName);

            if (file.exists()) {
                // 根据文件扩展名判断类型
                String path = file.getAbsolutePath().toLowerCase();
                String mimeType = "*/*";
                if (path.endsWith(".apk")) mimeType = "application/vnd.android.package-archive";
                else if (path.endsWith(".mp3") || path.endsWith(".wav") || path.endsWith(".m4a")) mimeType = "audio/*";
                else if (path.endsWith(".mp4") || path.endsWith(".avi") || path.endsWith(".mkv")) mimeType = "video/*";
                else if (path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png") || path.endsWith(".gif")) mimeType = "image/*";
                else if (path.endsWith(".txt") || path.endsWith(".log")) mimeType = "text/plain";
                else if (path.endsWith(".zip") || path.endsWith(".rar") || path.endsWith(".7z")) mimeType = "application/zip";
                else if (path.endsWith(".pdf")) mimeType = "application/pdf";
                else if (path.endsWith(".doc") || path.endsWith(".docx")) mimeType = "application/msword";

                // 让系统选择合适的应用打开
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(file), mimeType);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Intent chooser = Intent.createChooser(intent, "选择打开方式");
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(chooser);
            } else {
                // 文件不存在，打开下载目录
                openDownloadFolder();
                Toast.makeText(this, "文件未找到，已打开下载目录", Toast.LENGTH_SHORT).show();
            }
        } catch (Throwable t) {
            openDownloadFolder();
        }
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
        titleText.setText("下载列表");
        titleText.setTextColor(titleColor);
        titleText.setTextSize(16);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, -2, 1.0f);
        titleBar.addView(titleText, titleLp);

        // 无网络游戏按钮
        TextView gameBtn = new TextView(this);
        gameBtn.setText("游戏");
        gameBtn.setTextColor(accentColor);
        gameBtn.setTextSize(14);
        gameBtn.setGravity(Gravity.CENTER);
        gameBtn.setPadding(12 * dp, 0, 12 * dp, 0);
        titleBar.addView(gameBtn, new LinearLayout.LayoutParams(-2, -1));

        TextView clearBtn = new TextView(this);
        clearBtn.setText("清空");
        clearBtn.setTextColor(accentColor);
        clearBtn.setTextSize(14);
        clearBtn.setGravity(Gravity.CENTER);
        clearBtn.setPadding(12 * dp, 0, 12 * dp, 0);
        titleBar.addView(clearBtn, new LinearLayout.LayoutParams(-2, -1));

        root.addView(titleBar);

        // 内容容器
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
                    list.removeAllViews();
                    SharedPreferences hp = getSharedPreferences(MainActivity.HISTORY_PREFS, MODE_PRIVATE);
                    String json = hp.getString(MainActivity.DOWNLOAD_KEY, "[]");
                    ArrayList<Item> items = parseItems(json);

                    if (items.size() == 0) {
                        TextView empty = new TextView(DownloadActivity.this);
                        empty.setText("暂无下载记录");
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
                        LinearLayout row = new LinearLayout(DownloadActivity.this);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setGravity(Gravity.CENTER_VERTICAL);
                        row.setBackgroundColor(cardBg);
                        row.setPadding(16 * cardDp, 14 * cardDp, 16 * cardDp, 14 * cardDp);

                        // 左侧图标
                        TextView icon = new TextView(DownloadActivity.this);
                        String dispName = (it.name != null && it.name.length() > 0) ? it.name : it.url;
                        char first = '\u2B07';
                        try {
                            String d = dispName;
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
                            ig.setColor(0xFF4CAF50);
                            ig.setCornerRadius(14 * cardDp);
                            icon.setBackgroundDrawable(ig);
                        } catch (Throwable tt) {
                            icon.setBackgroundColor(0xFF4CAF50);
                        }
                        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams((int)(36 * cardDp), (int)(36 * cardDp));
                        list.addView(row);
                        row.addView(icon, ilp);

                        // 右侧文字
                        LinearLayout textWrap = new LinearLayout(DownloadActivity.this);
                        textWrap.setOrientation(LinearLayout.VERTICAL);
                        LinearLayout.LayoutParams twlp = new LinearLayout.LayoutParams(0, -2, 1.0f);
                        twlp.leftMargin = 12 * cardDp;
                        twlp.rightMargin = 8 * cardDp;
                        row.addView(textWrap, twlp);

                        TextView nameTv = new TextView(DownloadActivity.this);
                        nameTv.setText(dispName);
                        nameTv.setTextColor(titleColor);
                        nameTv.setTextSize(14);
                        nameTv.setMaxLines(1);
                        nameTv.setEllipsize(TextUtils.TruncateAt.END);
                        nameTv.setTypeface(null, android.graphics.Typeface.BOLD);
                        textWrap.addView(nameTv);

                        // 底部信息行：时间 + 打开位置按钮
                        LinearLayout infoRow = new LinearLayout(DownloadActivity.this);
                        infoRow.setOrientation(LinearLayout.HORIZONTAL);
                        infoRow.setGravity(Gravity.CENTER_VERTICAL);
                        infoRow.setPadding(0, (int)(4 * cardDp), 0, 0);
                        textWrap.addView(infoRow);

                        if (it.time > 0) {
                            TextView timeTv = new TextView(DownloadActivity.this);
                            String ts;
                            try { ts = sdf.format(new Date(it.time)); } catch (Throwable tt) { ts = ""; }
                            timeTv.setText(ts);
                            timeTv.setTextColor(subColor);
                            timeTv.setTextSize(11);
                            LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(0, -2, 1.0f);
                            infoRow.addView(timeTv, timeLp);
                        }

                        // 打开位置按钮
                        TextView openLocBtn = new TextView(DownloadActivity.this);
                        openLocBtn.setText("打开位置");
                        openLocBtn.setTextColor(accentColor);
                        openLocBtn.setTextSize(11);
                        openLocBtn.setGravity(Gravity.END);
                        openLocBtn.setPadding((int)(8 * cardDp), (int)(2 * cardDp), (int)(8 * cardDp), (int)(2 * cardDp));
                        infoRow.addView(openLocBtn, new LinearLayout.LayoutParams(-2, -2));

                        // 点击文件名：打开文件
                        row.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View v) {
                                String fn = (it.name != null && it.name.length() > 0) ? it.name : "";
                                if (fn.length() == 0) {
                                    // 从URL提取文件名
                                    int si = it.url.lastIndexOf('/');
                                    if (si >= 0 && si < it.url.length() - 1) fn = it.url.substring(si + 1);
                                }
                                if (fn.length() > 0) {
                                    openFileByName(fn);
                                } else {
                                    openDownloadFolder();
                                }
                            }
                        });

                        // 点击打开位置
                        openLocBtn.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View v) {
                                openDownloadFolder();
                            }
                        });

                        // 分隔线
                        if (i < items.size() - 1) {
                            View divider = new View(DownloadActivity.this);
                            divider.setBackgroundColor(dividerColor);
                            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-1, (int)(0.5 * cardDp));
                            list.addView(divider, dlp);
                        }
                    }
                } catch (Throwable t) {
                    Toast.makeText(DownloadActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                }
            }
        };

        render.run();

        // 游戏按钮
        gameBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(DownloadActivity.this);
                    builder.setTitle("无网络游戏");
                    android.webkit.WebView wv = new android.webkit.WebView(DownloadActivity.this);
                    wv.getSettings().setJavaScriptEnabled(true);
                    wv.getSettings().setDomStorageEnabled(true);
                    wv.loadUrl("file:///android_asset/snake.html");
                    builder.setView(wv);
                    builder.setPositiveButton("关闭", new android.content.DialogInterface.OnClickListener() {
                        @Override public void onClick(android.content.DialogInterface d, int which) {
                            try { d.dismiss(); } catch (Throwable tt) {}
                        }
                    });
                    builder.show();
                } catch (Throwable t) {
                    Toast.makeText(DownloadActivity.this, "游戏启动失败", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 清空按钮
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    SharedPreferences hp = getSharedPreferences(MainActivity.HISTORY_PREFS, MODE_PRIVATE);
                    hp.edit().putString(MainActivity.DOWNLOAD_KEY, "[]").apply();
                    render.run();
                    Toast.makeText(DownloadActivity.this, "已清空下载记录", Toast.LENGTH_SHORT).show();
                } catch (Throwable t) {}
            }
        });
    }
}
