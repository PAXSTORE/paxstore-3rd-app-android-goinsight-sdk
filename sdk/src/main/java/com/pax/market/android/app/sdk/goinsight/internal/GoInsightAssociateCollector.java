package com.pax.market.android.app.sdk.goinsight.internal;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;

import com.pax.market.android.app.sdk.device.model.InstalledAppInfo;
import com.pax.market.android.app.sdk.device.provider.DeviceInfoProvider;
import com.pax.market.android.app.sdk.device.provider.InstalledAppsProvider;
import com.pax.market.android.app.sdk.goinsight.dto.DatasetAssociateColsResponse;
import com.pax.market.android.app.sdk.goinsight.internal.key.AppCollectKey;
import com.pax.market.android.app.sdk.goinsight.internal.key.BasicIngestionKey;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GoInsightAssociateCollector {
    private static final int MAX_ICON_BYTES = 30 * 1024;
    private static final int ICON_SIZE_PX = 64;

    private final DeviceInfoProvider deviceInfoProvider;
    private final InstalledAppsProvider installedAppsProvider;

    public GoInsightAssociateCollector(DeviceInfoProvider deviceInfoProvider,
                                       InstalledAppsProvider installedAppsProvider) {
        this.deviceInfoProvider = deviceInfoProvider;
        this.installedAppsProvider = installedAppsProvider;
    }

    public List<Map<String, Object>> appendDeviceData(List<Map<String, Object>> list,
                                                      DatasetAssociateColsResponse config) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            result.addAll(list);
        }
        Map<String, Object> deviceData = buildDeviceDataMap(config);
        if (!deviceData.isEmpty()) {
            result.add(deviceData);
        }
        return result;
    }

    public Map<String, Object> buildDeviceDataMap(DatasetAssociateColsResponse config) {
        if (config == null) {
            return new HashMap<>();
        }
        Set<BasicIngestionKey> basicKeys = BasicIngestionKey.fromKeys(config.getBasicIngestionCols());
        boolean hasAppCols = config.getAppIngestionCols() != null
                && !config.getAppIngestionCols().isEmpty();
        if (basicKeys.isEmpty() && !hasAppCols) {
            return new HashMap<>();
        }

        Map<String, Object> map = new HashMap<>();
        for (BasicIngestionKey key : basicKeys) {
            map.put(key.getKey(), key.getValue(deviceInfoProvider));
        }
        if (hasAppCols) {
            map.put("installedApps", buildInstalledAppsData(config));
        }
        return map;
    }

    private List<Map<String, Object>> buildInstalledAppsData(DatasetAssociateColsResponse config) {
        if (config == null || config.getAppIngestionCols() == null
                || config.getAppIngestionCols().isEmpty()) {
            return new ArrayList<>();
        }
        List<InstalledAppInfo> apps = installedAppsProvider.getInstalledApps();
        if (apps == null || apps.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, List<DatasetAssociateColsResponse.AppAssociateColumn>> columnsByPackage =
                new HashMap<>();
        List<DatasetAssociateColsResponse.AppAssociateColumn> globalColumns = new ArrayList<>();
        for (DatasetAssociateColsResponse.AppAssociateColumn column : config.getAppIngestionCols()) {
            if (column == null) {
                continue;
            }
            String packageName = column.getPackageName();
            if (isNullOrEmpty(packageName)) {
                globalColumns.add(column);
                continue;
            }
            List<DatasetAssociateColsResponse.AppAssociateColumn> list =
                    columnsByPackage.get(packageName);
            if (list == null) {
                list = new ArrayList<>();
                columnsByPackage.put(packageName, list);
            }
            list.add(column);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (InstalledAppInfo app : apps) {
            List<DatasetAssociateColsResponse.AppAssociateColumn> columns = new ArrayList<>();
            List<DatasetAssociateColsResponse.AppAssociateColumn> packageColumns =
                    columnsByPackage.get(app.getPackageName());
            if (packageColumns != null) {
                columns.addAll(packageColumns);
            }
            if (!globalColumns.isEmpty()) {
                columns.addAll(globalColumns);
            }
            if (columns.isEmpty()) {
                continue;
            }
            Map<String, Object> map = new HashMap<>();
            for (DatasetAssociateColsResponse.AppAssociateColumn column : columns) {
                AppCollectKey collectKey = AppCollectKey.fromKey(column.getCollectColName());
                if (collectKey == null) {
                    continue;
                }
                Object value = collectKey.getValue(app, this::encodeIcon);
                String key = isNullOrEmpty(column.getIngestionColName())
                        ? collectKey.getKey()
                        : column.getIngestionColName();
                if (!isNullOrEmpty(key)) {
                    map.put(key, value);
                }
            }
            if (!map.isEmpty()) {
                result.add(map);
            }
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

    private boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
