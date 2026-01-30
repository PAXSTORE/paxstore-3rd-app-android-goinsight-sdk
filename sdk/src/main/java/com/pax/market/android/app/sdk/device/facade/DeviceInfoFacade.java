package com.pax.market.android.app.sdk.device.facade;

import android.app.Activity;
import android.content.Context;

import com.pax.market.android.app.sdk.device.permission.DevicePermissionManager;
import com.pax.market.android.app.sdk.device.provider.DeviceInfoProvider;
import com.pax.market.android.app.sdk.device.provider.InstalledAppsProvider;

import java.util.List;

/**
 * Facade for unified device/app info access.
 */
public class DeviceInfoFacade {
    private final DeviceInfoProvider deviceInfoProvider;
    private final InstalledAppsProvider installedAppsProvider;

    public DeviceInfoFacade(Context context) {
        this.deviceInfoProvider = new DeviceInfoProvider(context);
        this.installedAppsProvider = new InstalledAppsProvider(context);
    }

    public List<String> getRequiredPermissions() {
        return DevicePermissionManager.getRequiredPermissions();
    }

    public boolean hasAllRequiredPermissions(Context context) {
        return DevicePermissionManager.hasAllRequiredPermissions(context);
    }

    public void requestAllRequiredPermissions(Activity activity, int requestCode) {
        DevicePermissionManager.requestAllRequiredPermissions(activity, requestCode);
    }

    public DeviceInfoProvider deviceInfo() {
        return deviceInfoProvider;
    }

    public InstalledAppsProvider installedApps() {
        return installedAppsProvider;
    }
}
