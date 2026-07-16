package com.menglin.cn;

import android.graphics.Bitmap;
import android.webkit.WebView;

import java.util.ArrayList;
import java.util.List;

public class TabManager {

    public static final List<Tab> TABS = new ArrayList<Tab>();
    public static int CURRENT_TAB_INDEX = 0;
    public static int NEXT_TAB_ID = 1;

    public static int PENDING_ACTION = 0;
    public static int PENDING_SWITCH_INDEX = -1;
    public static int PENDING_CLOSE_INDEX = -1;

    public static class Tab {
        public int id;
        public WebView webView;
        public String title;
        public String url;
        public boolean isPrivate;
        public Bitmap thumbnail;
    }
}
