package com.pax.market.android.app.sdk.goinsight;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;

import com.pax.market.android.app.sdk.device.model.InstalledAppInfo;
import com.pax.market.android.app.sdk.device.model.LocationInfo;
import com.pax.market.android.app.sdk.device.model.NetworkType;
import com.pax.market.android.app.sdk.device.provider.DeviceInfoProvider;
import com.pax.market.android.app.sdk.device.provider.InstalledAppsProvider;
import com.pax.market.api.sdk.java.api.sync.GoInsightApi;
import com.pax.market.api.sdk.java.base.dto.SdkObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoInsightBizDataApi {
    private static final int MAX_ICON_BYTES = 30 * 1024;
    private static final int ICON_SIZE_PX = 64;
    private static final String DEFAULT_PREFIX = "device_";

    private final DeviceInfoProvider deviceInfoProvider;
    private final InstalledAppsProvider installedAppsProvider;
    private GoInsightApi goInsightApi;

    public GoInsightBizDataApi(Context context) {
        this.deviceInfoProvider = new DeviceInfoProvider(context);
        this.installedAppsProvider = new InstalledAppsProvider(context);
    }

    public SdkObject syncTerminalBizDataWithDeviceInfo(GoInsightApi goInsightApi,
                                                       List<Map<String, Object>> list)
            throws Exception {
        if (goInsightApi == null) {
            throw new IllegalArgumentException("goInsightApi is null");
        }
        List<Map<String, Object>> mergedList = appendDeviceData(list);
        return goInsightApi.syncTerminalBizData(mergedList);
    }

    public List<Map<String, Object>> appendDeviceData(List<Map<String, Object>> list) {
        return appendDeviceData(list, DEFAULT_PREFIX);
    }

    public List<Map<String, Object>> appendDeviceData(List<Map<String, Object>> list,
                                                      String keyPrefix) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            result.addAll(list);
        }
        result.add(buildDeviceDataMap(keyPrefix));
        return result;
    }

    public Map<String, Object> buildDeviceDataMap(String keyPrefix) {
        String prefix = keyPrefix == null ? DEFAULT_PREFIX : keyPrefix;
        Map<String, Object> map = new HashMap<>();

        map.put(prefix + "language", deviceInfoProvider.getLanguage());
        map.put(prefix + "timeZone", deviceInfoProvider.getTimeZoneId());
        map.put(prefix + "imei", deviceInfoProvider.getImei());
        map.put(prefix + "terminalIp", deviceInfoProvider.getTerminalIp());
        map.put(prefix + "gravitySensorSupported", deviceInfoProvider.isGravitySensorSupported());
        map.put(prefix + "bluetoothSupported", deviceInfoProvider.isBluetoothSupported());
        map.put(prefix + "androidVersion", deviceInfoProvider.getAndroidVersion());
        map.put(prefix + "totalStorage", deviceInfoProvider.getTotalStorageBytes());
        map.put(prefix + "storageUsage", deviceInfoProvider.getStorageUsageBytes());
        map.put(prefix + "totalRam", deviceInfoProvider.getTotalRamBytes());
        map.put(prefix + "ramUsage", deviceInfoProvider.getRamUsageBytes());
        NetworkType networkType = deviceInfoProvider.getNetworkType();
        map.put(prefix + "networkType", networkType == null ? null : networkType.name());
        map.put(prefix + "signalStrength", deviceInfoProvider.getSignalStrengthLevel());
        map.put(prefix + "signalIntensityCarrier1",
                deviceInfoProvider.getSignalIntensityCarrier1Level());
        map.put(prefix + "signalIntensityCarrier2",
                deviceInfoProvider.getSignalIntensityCarrier2Level());
        map.put(prefix + "cellId", deviceInfoProvider.getCellId());
        map.put(prefix + "simSupported", deviceInfoProvider.isSimSupported());
        map.put(prefix + "simOperator", deviceInfoProvider.getSimOperator());
        map.put(prefix + "sim1Operator", deviceInfoProvider.getSim1Operator());
        map.put(prefix + "sim2Operator", deviceInfoProvider.getSim2Operator());
        map.put(prefix + "iccid", deviceInfoProvider.getIccid());
        map.put(prefix + "sim1Iccid", deviceInfoProvider.getSim1Iccid());
        map.put(prefix + "sim2Iccid", deviceInfoProvider.getSim2Iccid());
        map.put(prefix + "gpsEnabled", deviceInfoProvider.isGpsEnabled());

        LocationInfo locationInfo = deviceInfoProvider.getLocation();
        map.put(prefix + "latitude",
                locationInfo == null ? null : locationInfo.getLatitude());
        map.put(prefix + "longitude",
                locationInfo == null ? null : locationInfo.getLongitude());

        map.put(prefix + "installedApps", buildInstalledAppsData());
        return map;
    }

    private List<Map<String, Object>> buildInstalledAppsData() {
        List<InstalledAppInfo> apps = installedAppsProvider.getInstalledApps();
        if (apps == null || apps.isEmpty()) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (InstalledAppInfo app : apps) {
            Map<String, Object> map = new HashMap<>();
            map.put("appName", app.getAppName());
            map.put("appVersion", app.getAppVersion());
            map.put("appInstallationStatus", app.getAppInstallationStatus());
            map.put("appIcon", encodeIcon(app.getAppIcon()));
            map.put("appUpdatedTime", app.getAppUpdatedTime());
            map.put("appFirstInstalledTime", app.getAppFirstInstalledTime());
            result.add(map);
        }
        return result;
    }

    private String encodeIcon(Drawable drawable) {
        try {
            Bitmap bitmap = toBitmap(drawable, ICON_SIZE_PX, ICON_SIZE_PX);
            if (bitmap == null) {
                return null;
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            byte[] data = outputStream.toByteArray();
            if (data.length > MAX_ICON_BYTES) {
                return null;
            }
            return "data:image/png;base64," + Base64.encodeToString(data, Base64.NO_WRAP);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Bitmap toBitmap(Drawable drawable, int width, int height) {
        if (drawable == null) {
            return null;
        }
        if (drawable instanceof BitmapDrawable) {
            Bitmap source = ((BitmapDrawable) drawable).getBitmap();
            if (source != null) {
                return Bitmap.createScaledBitmap(source, width, height, true);
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }

}
