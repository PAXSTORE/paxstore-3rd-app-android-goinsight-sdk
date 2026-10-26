package com.pax.market.android.app.sdk.device.permission;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.Arrays;
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
    /**
     * Vendor-specific permission present only on some customized ROMs.
     * Never treat it as a cross-device required permission: on stock Android the system
     * does not recognize it, so checkSelfPermission() would always return DENIED.
     * Use {@link #canQueryInstalledApps(Context)} for capability probing instead.
     */
    public static final String PERMISSION_GET_INSTALLED_APPS = "com.android.permission.GET_INSTALLED_APPS";
    /** Signature/privileged only; used to read IMEI/ICCID on API 29+. Not requestable by normal apps. */
    @SuppressLint("InlinedApi")
    public static final String PERMISSION_READ_PRIVILEGED_PHONE_STATE = "android.permission.READ_PRIVILEGED_PHONE_STATE";

    /**
     * Runtime (dangerous) permissions that must be requested dynamically on API 23+.
     * Only cross-device standard permissions belong here; normal / vendor permissions must not.
     */
    private static final String[] RUNTIME_PERMISSIONS = new String[] {
            PERMISSION_READ_PHONE_STATE,
            PERMISSION_ACCESS_FINE_LOCATION,
            PERMISSION_ACCESS_COARSE_LOCATION
    };

    private DevicePermissionManager() {
    }

    /**
     * Returns the runtime (dangerous) permissions that need to be requested dynamically.
     * Normal permissions (ACCESS_NETWORK_STATE / ACCESS_WIFI_STATE / QUERY_ALL_PACKAGES) and the
     * vendor permission (GET_INSTALLED_APPS) are intentionally excluded: they are granted at
     * install time (or vendor-only) and cannot be requested at runtime.
     */
    public static List<String> getRuntimePermissions() {
        return new ArrayList<>(Arrays.asList(RUNTIME_PERMISSIONS));
    }

    /**
     * Kept for backward compatibility; now delegates to {@link #getRuntimePermissions()}.
     */
    public static List<String> getRequiredPermissions() {
        return getRuntimePermissions();
    }

    /**
     * Whether all runtime (dangerous) permissions are granted. Vendor / normal permissions are
     * not part of this check, so it will not falsely return false on stock Android devices.
     */
    public static boolean hasAllRequiredPermissions(Context context) {
        for (String permission : getRuntimePermissions()) {
            if (!hasPermission(context, permission)) {
                return false;
            }
        }
        return true;
    }

    /** Requests only the missing runtime (dangerous) permissions. */
    public static void requestAllRequiredPermissions(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        List<String> missing = new ArrayList<>();
        for (String permission : getRuntimePermissions()) {
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
        try {
            return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Whether the app can enumerate installed packages.
     * Prefers the vendor permission when the ROM provides it, otherwise falls back to
     * QUERY_ALL_PACKAGES (API 30+). On API &lt; 30 package visibility is unrestricted, so
     * {@link #hasPermission(Context, String)} already returns true for QUERY_ALL_PACKAGES.
     */
    public static boolean canQueryInstalledApps(Context context) {
        if (hasPermission(context, PERMISSION_GET_INSTALLED_APPS)) {
            return true;
        }
        return hasPermission(context, PERMISSION_QUERY_ALL_PACKAGES);
    }
}
