package com.pax.market.android.app.sdk.goinsight.internal;

import android.os.Build;

import com.pax.market.android.app.sdk.device.model.InstalledAppInfo;
import com.pax.market.android.app.sdk.device.provider.DeviceInfoProvider;
import com.pax.market.android.app.sdk.device.provider.InstalledAppsProvider;
import com.pax.market.android.app.sdk.goinsight.dto.DatasetAssociateColsResponse;
import com.pax.market.android.app.sdk.goinsight.internal.key.AppCollectKey;
import com.pax.market.android.app.sdk.goinsight.internal.key.BasicIngestionKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GoInsightAssociateCollector {
    private final DeviceInfoProvider deviceInfoProvider;
    private final InstalledAppsProvider installedAppsProvider;

    public GoInsightAssociateCollector(DeviceInfoProvider deviceInfoProvider,
                                       InstalledAppsProvider installedAppsProvider) {
        this.deviceInfoProvider = deviceInfoProvider;
        this.installedAppsProvider = installedAppsProvider;
    }

    /**
     * Returns one list with one map: biz + device + app merged into a single map.
     * So the result is a single list containing a single map, not two lists or list+list.
     */
    public List<Map<String, Object>> appendDeviceData(List<Map<String, Object>> list,
                                                      DatasetAssociateColsResponse config) {
        Map<String, Object> merged = new HashMap<>();
        if (list != null) {
            for (Map<String, Object> m : list) {
                if (m != null && !m.isEmpty()) {
                    merged.putAll(m);
                }
            }
        }
        Map<String, Object> deviceData = buildDeviceDataMap(config);
        if (!deviceData.isEmpty()) {
            merged.putAll(deviceData);
        }
        List<Map<String, Object>> appMaps = buildInstalledAppsData(config);
        for (Map<String, Object> appMap : appMaps) {
            if (appMap != null && !appMap.isEmpty()) {
                merged.putAll(appMap);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        if (!merged.isEmpty()) {
            result.add(merged);
        }
        return result;
    }

    public Map<String, Object> buildDeviceDataMap(DatasetAssociateColsResponse config) {
        if (config == null) {
            return new HashMap<>();
        }
        Set<BasicIngestionKey> basicKeys = BasicIngestionKey.fromKeys(config.getBasicIngestionCols());
        if (basicKeys.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Object> map = new HashMap<>();
        for (BasicIngestionKey key : basicKeys) {
            map.put(key.getKey(), key.getValue(deviceInfoProvider));
        }
        return map;
    }

    private List<Map<String, Object>> buildInstalledAppsData(DatasetAssociateColsResponse config) {
        if (config == null) {
            return new ArrayList<>();
        }
        List<DatasetAssociateColsResponse.AppAssociateColumn> appColumns =
                config.getAppIngestionCols();
        if (appColumns == null || appColumns.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, InstalledAppInfo> appsByPackage =
                buildAppsByPackage(installedAppsProvider.getInstalledApps());

        List<Map<String, Object>> result = new ArrayList<>();
        for (DatasetAssociateColsResponse.AppAssociateColumn column : appColumns) {
            if (column == null) {
                continue;
            }
            String packageName = column.getPackageName();
            if (isNullOrEmpty(packageName)) {
                continue;
            }
            String key = column.getIngestionColName();
            if (isNullOrEmpty(key)) {
                continue;
            }
            AppCollectKey collectKey = AppCollectKey.fromKey(column.getCollectColName());
            if (collectKey == null) {
                continue;
            }
            InstalledAppInfo app = appsByPackage.get(packageName);
            if (app == null) {
                if (!isFirmwarePackage(packageName)) {
                    continue;
                }
                app = createPlaceholderApp(packageName);
            }
            Object value = collectKey.getValue(app);
            Map<String, Object> map = new HashMap<>();
            map.put(key, value);
            result.add(map);
        }
        return result;
    }

    private Map<String, InstalledAppInfo> buildAppsByPackage(List<InstalledAppInfo> apps) {
        Map<String, InstalledAppInfo> appsByPackage = new HashMap<>();
        if (apps == null || apps.isEmpty()) {
            return appsByPackage;
        }
        for (InstalledAppInfo app : apps) {
            if (app != null && !isNullOrEmpty(app.getPackageName())) {
                appsByPackage.put(app.getPackageName(), app);
            }
        }
        return appsByPackage;
    }

    private InstalledAppInfo createPlaceholderApp(String packageName) {
        String type = isFirmwarePackage(packageName) ? "Firmware" : null;
        return new InstalledAppInfo(
                packageName,
                isFirmwarePackage(packageName) ? getFirmwareVersion() : null,
                null,
                type,
                false,
                null,
                null,
                null
        );
    }

    private boolean isFirmwarePackage(String packageName) {
        return "firmware".equalsIgnoreCase(packageName);
    }

    private String getFirmwareVersion() {
        String display = Build.DISPLAY;
        if (display != null && !display.trim().isEmpty()) {
            return display;
        }
        String incremental = Build.VERSION.INCREMENTAL;
        if (incremental != null && !incremental.trim().isEmpty()) {
            return incremental;
        }
        return Build.VERSION.RELEASE;
    }

    private boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
