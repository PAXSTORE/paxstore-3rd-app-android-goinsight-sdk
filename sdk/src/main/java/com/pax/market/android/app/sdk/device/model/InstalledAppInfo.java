package com.pax.market.android.app.sdk.device.model;

public final class InstalledAppInfo {
    private final String packageName;
    private final String appName;
    private final String appVersion;
    private final String type;
    private final boolean appEnabled;
    private final String appIcon;
    private final Long lastUpdateTime;
    private final Long firstInstallTime;

    public InstalledAppInfo(String packageName,
                            String appName,
                            String appVersion,
                            String type,
                            boolean appEnabled,
                            String appIcon,
                            Long lastUpdateTime,
                            Long firstInstallTime) {
        this.packageName = packageName;
        this.appName = appName;
        this.appVersion = appVersion;
        this.type = type;
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

    public String getType() {
        return type;
    }

    public String getAppInstallationStatus() {
        return appEnabled ? "Installed" : "Uninstalled";
    }

    public boolean isAppEnabled() {
        return appEnabled;
    }

    public String getAppIcon() {
        return appIcon;
    }

    public Long getAppUpdatedTime() {
        return lastUpdateTime;
    }

    public Long getAppFirstInstalledTime() {
        return firstInstallTime;
    }
}
