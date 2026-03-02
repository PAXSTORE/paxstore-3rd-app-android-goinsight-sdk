package com.pax.market.android.app.sdk.device.provider;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Build;

import com.pax.market.android.app.sdk.device.model.InstalledAppInfo;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InstalledAppsProvider {
    private final Context appContext;

    public InstalledAppsProvider(Context context) {
        this.appContext = context.getApplicationContext();
    }

    /**
     * Returns a list of all installed apps on the device.
     * On Android 11 (API 30) and above, the host app must declare
     * {@code QUERY_ALL_PACKAGES} in its manifest to get the full list;
     * otherwise only the app itself and packages in {@code <queries>} are visible.
     */
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
            String iconMd5 = getIconFromPackageName(appContext, packageName);
            Long lastUpdateTime = packageInfo.lastUpdateTime;
            Long firstInstallTime = packageInfo.firstInstallTime;
            result.add(new InstalledAppInfo(packageName, appName, appVersion, null,
                    appEnabled, iconMd5, lastUpdateTime, firstInstallTime));
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

    private String getIconFromPackageName(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();

        ApplicationInfo appInfo;
        try {
            appInfo = pm.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
        String iconBase64 = drawableToBase64(appInfo.loadIcon(pm));
        if (iconBase64 == null || iconBase64.isEmpty()) {
            return null;
        }
        try (InputStream inputStream = new ByteArrayInputStream(iconBase64.getBytes())) {
            return Md5Utils.getInputStreamMd5(inputStream);
        } catch (IOException | NoSuchAlgorithmException ignored) {
            return null;
        }
    }

    private String drawableToBase64(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        Bitmap bitmap = Bitmap
                .createBitmap(96, 96,
                        drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                                : Bitmap.Config.RGB_565);

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, 96, 96);
        drawable.draw(canvas);
        int size = 96 * 96 * 4;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(size);
        bitmap.compress(Bitmap.CompressFormat.PNG, 50, outputStream);
        byte[] imageData = outputStream.toByteArray();
        return Base64.getEncoder().encodeToString(imageData);
    }
}
