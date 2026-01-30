package com.pax.market.android.app.sdk.device.provider;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;

import com.pax.market.android.app.sdk.device.model.InstalledAppInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InstalledAppsProvider {
    private final Context appContext;

    public InstalledAppsProvider(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public List<InstalledAppInfo> getInstalledApps() {
        PackageManager packageManager = appContext.getPackageManager();
        if (packageManager == null) {
            return Collections.emptyList();
        }
        List<PackageInfo> packages = packageManager.getInstalledPackages(0);
        if (packages.isEmpty()) {
            return Collections.emptyList();
        }
        List<InstalledAppInfo> result = new ArrayList<>();
        for (PackageInfo packageInfo : packages) {
            if (packageInfo == null || packageInfo.applicationInfo == null) {
                continue;
            }
            ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            String packageName = packageInfo.packageName;
            String appName = String.valueOf(packageManager.getApplicationLabel(applicationInfo));
            String appVersion = getAppVersion(packageInfo);
            boolean appEnabled = applicationInfo.enabled;
            Drawable appIcon = packageManager.getApplicationIcon(applicationInfo);
            long lastUpdateTime = packageInfo.lastUpdateTime;
            long firstInstallTime = packageInfo.firstInstallTime;
            result.add(new InstalledAppInfo(packageName, appName, appVersion,
                    appEnabled, appIcon, lastUpdateTime, firstInstallTime));
        }
        return result;
    }

    private String getAppVersion(PackageInfo packageInfo) {
        if (packageInfo.versionName != null) {
            return packageInfo.versionName;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return String.valueOf(packageInfo.getLongVersionCode());
        }
        return String.valueOf(packageInfo.versionCode);
    }
}
