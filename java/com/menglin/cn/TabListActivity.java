package com.menglin.cn;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class TabListActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int dp = (int) getResources().getDisplayMetrics().density;

        // 深色模式
        boolean dark = false;
        try {
            SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
            int darkMode = prefs.getInt(SettingsActivity.KEY_DARK_MODE, 0);
            if (darkMode == 2) dark = true;
            else if (darkMode == 0) {
                int ui = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                dark = (ui == Configuration.UI_MODE_NIGHT_YES);
            }
        } catch (Throwable t) {}

        int titleColor = dark ? 0xFFFFFFFF : 0xFF1C1C1E;
        int subColor = dark ? 0xFFAAAAAA : 0xFF8E8E93;
        int cardBg = dark ? 0xFF2D2D2D : 0xFFFFFFFF;
        int activeBg = 0xFF1E88E5;
        int accentColor = activeBg;

        // 外层：半透明背景，点击外部关闭
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackgroundColor(dark ? 0xCC0A0A0A : 0xCCE8E8EE);
        outer.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        outer.setGravity(Gravity.BOTTOM);
        outer.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });

        // 主容器
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setBackgroundColor(cardBg);
        try {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(cardBg);
            bg.setCornerRadii(new float[]{20 * dp, 20 * dp, 20 * dp, 20 * dp, 0, 0, 0, 0});
            inner.setBackgroundDrawable(bg);
        } catch (Throwable t) {}
        LinearLayout.LayoutParams innerLp = new LinearLayout.LayoutParams(-1, -1);
        inner.setLayoutParams(innerLp);
        inner.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { /* 吞掉点击 */ }
        });

        // 顶部：标题 + 数量
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(18 * dp, 14 * dp, 18 * dp, 10 * dp);
        header.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

        TextView title = new TextView(this);
        title.setText("标签页");
        title.setTextColor(titleColor);
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, -2, 1.0f);
        header.addView(title, titleLp);

        TextView count = new TextView(this);
        int n = TabManager.TABS == null ? 0 : TabManager.TABS.size();
        count.setText("共 " + n + " 个");
        count.setTextColor(subColor);
        count.setTextSize(13);
        header.addView(count, new LinearLayout.LayoutParams(-2, -2));
        inner.addView(header);

        // 列表
        ScrollView sv = new ScrollView(this);
        sv.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1.0f));

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

        if (TabManager.TABS != null && TabManager.TABS.size() > 0) {
            for (int i = 0; i < TabManager.TABS.size(); i++) {
                final int idx = i;
                final TabManager.Tab tab = TabManager.TABS.get(i);
                View row = makeTabRow(tab, idx, titleColor, subColor, cardBg, activeBg, dp);
                list.addView(row);
            }
        }

        sv.addView(list);
        inner.addView(sv);

        // 底部：新建按钮
        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, 10 * dp, 0, 18 * dp);
        footer.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

        TextView plusBtn = new TextView(this);
        plusBtn.setText("+ 新建标签页");
        plusBtn.setTextColor(0xFFFFFFFF);
        plusBtn.setTextSize(15);
        plusBtn.setGravity(Gravity.CENTER);
        plusBtn.setPadding(24 * dp, 12 * dp, 24 * dp, 12 * dp);
        try {
            GradientDrawable pbg = new GradientDrawable();
            pbg.setColor(accentColor);
            pbg.setCornerRadius(22 * dp);
            plusBtn.setBackgroundDrawable(pbg);
        } catch (Throwable t) {}
        plusBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new android.app.AlertDialog.Builder(TabListActivity.this)
                        .setTitle("新建标签页")
                        .setItems(new CharSequence[]{"新建普通标签页", "新建隐私标签页（无痕）"},
                                new android.content.DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(android.content.DialogInterface dialog, int which) {
                                        if (which == 0) TabManager.PENDING_ACTION = 1;
                                        else TabManager.PENDING_ACTION = 2;
                                        try {
                                            Intent i = new Intent(TabListActivity.this, MainActivity.class);
                                            i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                            startActivity(i);
                                        } catch (Throwable t) {}
                                        finish();
                                    }
                                })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });
        footer.addView(plusBtn, new LinearLayout.LayoutParams(-2, -2));
        inner.addView(footer);

        outer.addView(inner);
        setContentView(outer);
    }

    private View makeTabRow(final TabManager.Tab tab, final int index,
                            int titleColor, int subColor, int cardBg, int activeBg, int dp) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 6 * dp, 12 * dp, 6 * dp);
        row.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

        boolean isActive = (index == TabManager.CURRENT_TAB_INDEX);
        if (isActive) {
            try {
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(activeBg);
                bg.setCornerRadius(10 * dp);
                row.setBackgroundDrawable(bg);
            } catch (Throwable t) {}
        }

        // 缩略图（80dp x 60dp，左边距 12dp，圆角 8dp）
        ImageView thumbIv = new ImageView(this);
        thumbIv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        try {
            GradientDrawable thumbBg = new GradientDrawable();
            thumbBg.setColor(activeBg);
            thumbBg.setCornerRadius(8 * dp);
            thumbIv.setBackgroundDrawable(thumbBg);
        } catch (Throwable t) {
            thumbIv.setBackgroundColor(activeBg);
        }

        String title = tab.title;
        String url = tab.url;
        char c = '•';
        if (title != null && title.length() > 0) {
            char first = Character.toUpperCase(title.charAt(0));
            if (Character.isLetterOrDigit(first)) c = first;
            else if (url != null && url.length() > 0) {
                for (int i = 0; i < url.length(); i++) {
                    char ch = Character.toUpperCase(url.charAt(i));
                    if (Character.isLetterOrDigit(ch)) { c = ch; break; }
                }
            }
        }

        int thumbW = (int) (80 * dp);
        int thumbH = (int) (60 * dp);
        if (tab.thumbnail != null && !tab.thumbnail.isRecycled()) {
            try {
                Bitmap src = tab.thumbnail;
                Bitmap rounded = Bitmap.createBitmap(thumbW, thumbH, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(rounded);
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                RectF rectF = new RectF(0, 0, thumbW, thumbH);
                float radius = 8 * dp;
                paint.setColor(0xFF1E88E5);
                canvas.drawRoundRect(rectF, radius, radius, paint);
                paint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));
                Bitmap scaled = Bitmap.createScaledBitmap(src, thumbW, thumbH, true);
                canvas.drawBitmap(scaled, 0, 0, paint);
                paint.setXfermode(null);
                thumbIv.setImageBitmap(rounded);
            } catch (Throwable t) {
                thumbIv.setImageBitmap(tab.thumbnail);
            }
        } else {
            try {
                Bitmap placeholder = Bitmap.createBitmap(thumbW, thumbH, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(placeholder);
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                RectF rectF = new RectF(0, 0, thumbW, thumbH);
                float radius = 8 * dp;
                paint.setColor(activeBg);
                canvas.drawRoundRect(rectF, radius, radius, paint);
                paint.setColor(0xFFFFFFFF);
                paint.setTextSize(28 * dp);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                float textY = thumbH / 2f - (paint.descent() + paint.ascent()) / 2f;
                canvas.drawText(String.valueOf(c), thumbW / 2f, textY, paint);
                thumbIv.setImageBitmap(placeholder);
            } catch (Throwable t2) {
                thumbIv.setBackgroundColor(activeBg);
            }
        }

        LinearLayout.LayoutParams thumbLp = new LinearLayout.LayoutParams(thumbW, thumbH);
        thumbLp.leftMargin = 12 * dp;
        thumbLp.rightMargin = 0;
        row.addView(thumbIv, thumbLp);

        // 标题 + URL
        LinearLayout textWrap = new LinearLayout(this);
        textWrap.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams twLp = new LinearLayout.LayoutParams(0, -2, 1.0f);
        twLp.leftMargin = 14 * dp;
        twLp.rightMargin = 10 * dp;
        textWrap.setLayoutParams(twLp);

        TextView titleTv = new TextView(this);
        String displayTitle = title;
        if (displayTitle == null || displayTitle.length() == 0) displayTitle = "新标签页";
        if (tab.isPrivate) displayTitle = "PRIV " + displayTitle;
        titleTv.setText(displayTitle);
        titleTv.setTextColor(isActive ? 0xFFFFFFFF : titleColor);
        titleTv.setTextSize(14);
        titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
        titleTv.setMaxLines(1);
        titleTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textWrap.addView(titleTv);

        TextView urlTv = new TextView(this);
        String urlText = (url == null || url.length() == 0) ? "（空）" : url;
        urlTv.setText(urlText);
        urlTv.setTextColor(isActive ? 0xFFD0D0FF : subColor);
        urlTv.setTextSize(11);
        urlTv.setPadding(0, 3 * dp, 0, 0);
        urlTv.setMaxLines(1);
        urlTv.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        textWrap.addView(urlTv);

        row.addView(textWrap);

        // 关闭按钮
        TextView closeBtn = new TextView(this);
        closeBtn.setText("X");
        closeBtn.setTextColor(isActive ? 0xFFFFFFFF : subColor);
        closeBtn.setTextSize(20);
        closeBtn.setGravity(Gravity.CENTER);
        closeBtn.setPadding(10 * dp, 6 * dp, 10 * dp, 6 * dp);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                TabManager.PENDING_CLOSE_INDEX = index;
                try {
                    Intent i = new Intent(TabListActivity.this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(i);
                } catch (Throwable t) {}
                finish();
            }
        });
        row.addView(closeBtn, new LinearLayout.LayoutParams(-2, -2));

        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                TabManager.PENDING_SWITCH_INDEX = index;
                try {
                    Intent i = new Intent(TabListActivity.this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(i);
                } catch (Throwable t) {}
                finish();
            }
        });
        return row;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
