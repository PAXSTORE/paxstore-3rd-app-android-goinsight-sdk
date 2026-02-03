package com.pax.market.android.app.sdk.goinsight.internal.key;

import android.graphics.drawable.Drawable;

import com.pax.market.android.app.sdk.device.model.InstalledAppInfo;

import java.util.HashMap;
import java.util.Map;

public enum AppCollectKey {
    APP_NAME("appName") {
        @Override
        public Object getValue(InstalledAppInfo app, IconEncoder iconEncoder) {
            return app.getAppName();
        }
    },
    APP_VERSION("appVersion") {
        @Override
        public Object getValue(InstalledAppInfo app, IconEncoder iconEncoder) {
            return app.getAppVersion();
        }
    },
    APP_STATUS("appStatus") {
        @Override
        public Object getValue(InstalledAppInfo app, IconEncoder iconEncoder) {
            return app.getAppInstallationStatus();
        }
    },
    INSTALLATION_TIME("installationTime") {
        @Override
        public Object getValue(InstalledAppInfo app, IconEncoder iconEncoder) {
            return app.getAppFirstInstalledTime();
        }
    },
    APP_ICON("appIcon") {
        @Override
        public Object getValue(InstalledAppInfo app, IconEncoder iconEncoder) {
            Drawable icon = app.getAppIcon();
            return iconEncoder == null ? null : iconEncoder.encode(icon);
        }
    },
    LAST_INSTALLATION_TIME("lastInstallationTime") {
        @Override
        public Object getValue(InstalledAppInfo app, IconEncoder iconEncoder) {
            return app.getAppUpdatedTime();
        }
    };

    private static final Map<String, AppCollectKey> INDEX = buildIndex();

    private final String key;

    AppCollectKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public abstract Object getValue(InstalledAppInfo app, IconEncoder iconEncoder);

    public static AppCollectKey fromKey(String key) {
        return INDEX.get(key);
    }

    private static Map<String, AppCollectKey> buildIndex() {
        Map<String, AppCollectKey> map = new HashMap<>();
        for (AppCollectKey key : values()) {
            map.put(key.getKey(), key);
        }
        return map;
    }

    public interface IconEncoder {
        String encode(Drawable drawable);
    }
}
