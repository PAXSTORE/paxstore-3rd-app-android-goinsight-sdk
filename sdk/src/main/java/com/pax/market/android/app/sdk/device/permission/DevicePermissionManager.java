package com.pax.market.android.app.sdk.device.permission;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Centralized runtime permission handling for device info.
 * Covers both dangerous (runtime) and normal permissions declared in the SDK manifest.
 */
public final class DevicePermissionManager {
    public static final String PERMISSION_READ_PHONE_STATE = Manifest.permission.READ_PHONE_STATE;
    public static final String PERMISSION_ACCESS_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    public static final String PERMISSION_ACCESS_COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    public static final String PERMISSION_ACCESS_NETWORK_STATE = Manifest.permission.ACCESS_NETWORK_STATE;
    public static final String PERMISSION_ACCESS_WIFI_STATE = Manifest.permission.ACCESS_WIFI_STATE;
    /** QUERY_ALL_PACKAGES added in API 30; use string to support minSdk &lt; 30 */
    @SuppressLint("InlinedApi")
    public static final String PERMISSION_QUERY_ALL_PACKAGES = Manifest.permission.QUERY_ALL_PACKAGES;

    private static final String[] BASE_REQUIRED_PERMISSIONS = new String[] {
            PERMISSION_READ_PHONE_STATE,
            PERMISSION_ACCESS_FINE_LOCATION,
            PERMISSION_ACCESS_COARSE_LOCATION,
            PERMISSION_ACCESS_NETWORK_STATE,
            PERMISSION_ACCESS_WIFI_STATE
    };

    private static final int API_LEVEL_QUERY_ALL_PACKAGES = 30;

    private DevicePermissionManager() {
    }

    /** Returns required permissions for current API level. QUERY_ALL_PACKAGES only on API 30+. */
    private static List<String> getRequiredPermissionsForCurrentApi() {
        List<String> permissions = new ArrayList<>();
        Collections.addAll(permissions, BASE_REQUIRED_PERMISSIONS);
        if (Build.VERSION.SDK_INT >= API_LEVEL_QUERY_ALL_PACKAGES) {
            permissions.add(PERMISSION_QUERY_ALL_PACKAGES);
        }
        return permissions;
    }

    public static List<String> getRequiredPermissions() {
        return getRequiredPermissionsForCurrentApi();
    }

    public static boolean hasAllRequiredPermissions(Context context) {
        for (String permission : getRequiredPermissionsForCurrentApi()) {
            if (!hasPermission(context, permission)) {
                return false;
            }
        }
        return true;
    }

    public static void requestAllRequiredPermissions(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        List<String> missing = new ArrayList<>();
        for (String permission : getRequiredPermissionsForCurrentApi()) {
            if (!hasPermission(activity, permission)) {
                missing.add(permission);
            }
        }
        if (!missing.isEmpty()) {
            activity.requestPermissions(missing.toArray(new String[0]), requestCode);
        }
    }

    public static boolean hasPermission(Context context, String permission) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        // QUERY_ALL_PACKAGES exists from API 30; on older versions package visibility does not apply
        if (PERMISSION_QUERY_ALL_PACKAGES.equals(permission) && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return true;
        }
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }
}
