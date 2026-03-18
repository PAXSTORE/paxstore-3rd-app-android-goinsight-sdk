package com.pax.market.android.app.sdk.goinsight.internal.key;

import com.pax.market.android.app.sdk.device.model.InstalledAppInfo;

import java.util.HashMap;
import java.util.Map;

public enum AppCollectKey {
    APP_NAME("appName") {
        @Override
        public Object getValue(InstalledAppInfo app) {
            return app.getAppName();
        }
    },
    APP_VERSION("appVersion") {
        @Override
        public Object getValue(InstalledAppInfo app) {
            return app.getAppVersion();
        }
    },
    APP_STATUS("appStatus") {
        @Override
        public Object getValue(InstalledAppInfo app) {
            return app.getAppInstallationStatus();
        }
    },
    TYPE("type") {
        @Override
        public Object getValue(InstalledAppInfo app) {
            return app.getType();
        }
    },
    INSTALLATION_TIME("installationTime") {
        @Override
        public Object getValue(InstalledAppInfo app) {
            return app.getAppFirstInstalledTime();
        }
    },
    APP_ICON("appIcon") {
        @Override
        public Object getValue(InstalledAppInfo app) {
            return app.getAppIcon();
        }
    },
    LAST_INSTALLATION_TIME("lastInstallationTime") {
        @Override
        public Object getValue(InstalledAppInfo app) {
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

    public abstract Object getValue(InstalledAppInfo app);

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

}
