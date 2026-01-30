package com.pax.market.android.app.sdk.device.model;

import android.graphics.drawable.Drawable;

public final class InstalledAppInfo {
    private final String packageName;
    private final String appName;
    private final String appVersion;
    private final boolean appEnabled;
    private final Drawable appIcon;
    private final long lastUpdateTime;
    private final long firstInstallTime;

    public InstalledAppInfo(String packageName,
                            String appName,
                            String appVersion,
                            boolean appEnabled,
                            Drawable appIcon,
                            long lastUpdateTime,
                            long firstInstallTime) {
        this.packageName = packageName;
        this.appName = appName;
        this.appVersion = appVersion;
        this.appEnabled = appEnabled;
        this.appIcon = appIcon;
        this.lastUpdateTime = lastUpdateTime;
        this.firstInstallTime = firstInstallTime;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getAppName() {
        return appName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getAppInstallationStatus() {
        return appEnabled ? "ENABLED" : "DISABLED";
    }

    public boolean isAppEnabled() {
        return appEnabled;
    }

    public Drawable getAppIcon() {
        return appIcon;
    }

    public long getAppUpdatedTime() {
        return lastUpdateTime;
    }

    public long getAppFirstInstalledTime() {
        return firstInstallTime;
    }
}
