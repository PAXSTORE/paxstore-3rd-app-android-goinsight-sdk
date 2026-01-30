package com.pax.market.android.app.sdk.device.permission;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Centralized runtime permission handling for device info.
 */
public final class DevicePermissionManager {
    public static final String PERMISSION_READ_PHONE_STATE = Manifest.permission.READ_PHONE_STATE;
    public static final String PERMISSION_ACCESS_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    public static final String PERMISSION_ACCESS_COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;

    private static final String[] REQUIRED_PERMISSIONS = new String[] {
            PERMISSION_READ_PHONE_STATE,
            PERMISSION_ACCESS_FINE_LOCATION,
            PERMISSION_ACCESS_COARSE_LOCATION
    };

    private DevicePermissionManager() {
    }

    public static List<String> getRequiredPermissions() {
        List<String> permissions = new ArrayList<>();
        Collections.addAll(permissions, REQUIRED_PERMISSIONS);
        return permissions;
    }

    public static boolean hasAllRequiredPermissions(Context context) {
        for (String permission : REQUIRED_PERMISSIONS) {
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
        for (String permission : REQUIRED_PERMISSIONS) {
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
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }
}
