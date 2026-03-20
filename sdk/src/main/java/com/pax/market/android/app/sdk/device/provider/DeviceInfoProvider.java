package com.pax.market.android.app.sdk.device.provider;

import static android.telephony.TelephonyManager.NETWORK_TYPE_TD_SCDMA;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.usage.StorageStatsManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.CellSignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyCallback;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.Log;

import com.pax.market.android.app.sdk.device.model.DeviceState;
import com.pax.market.android.app.sdk.device.model.NetworkType;
import com.pax.market.android.app.sdk.device.permission.DevicePermissionManager;
import com.pax.market.android.app.sdk.goinsight.internal.key.BasicIngestionKey;

import java.io.File;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DeviceInfoProvider {
    private static final String TAG = "DeviceInfoProvider";
    private static final int API_LEVEL_SIGNAL_STRENGTHS_LISTENER = 31;
    private static final Map<String, BasicIngestionValueProvider> CUSTOM_BASIC_INGESTION_VALUE_PROVIDERS =
            new ConcurrentHashMap<>();

    private final Context appContext;
    private volatile Integer cachedDefaultSignalLevel = null;
    private volatile boolean defaultSignalCallbackRegistered = false;

    public DeviceInfoProvider(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public static void registerBasicIngestionValueProvider(BasicIngestionKey key,
                                                           BasicIngestionValueProvider provider) {
        String normalizedKey = normalizeBasicIngestionKey(key);
        if (normalizedKey == null) {
            throw new IllegalArgumentException("key is empty");
        }
        if (provider == null) {
            CUSTOM_BASIC_INGESTION_VALUE_PROVIDERS.remove(normalizedKey);
            return;
        }
        CUSTOM_BASIC_INGESTION_VALUE_PROVIDERS.put(normalizedKey, provider);
    }

    public static void unregisterBasicIngestionValueProvider(BasicIngestionKey key) {
        String normalizedKey = normalizeBasicIngestionKey(key);
        if (normalizedKey != null) {
            CUSTOM_BASIC_INGESTION_VALUE_PROVIDERS.remove(normalizedKey);
        }
    }

    public static void clearBasicIngestionValueProviders() {
        CUSTOM_BASIC_INGESTION_VALUE_PROVIDERS.clear();
    }

    public Object getBasicIngestionValue(String key) {
        String normalizedKey = normalizeBasicIngestionKey(key);
        if (normalizedKey == null) {
            return null;
        }
        BasicIngestionValueProvider customProvider =
                CUSTOM_BASIC_INGESTION_VALUE_PROVIDERS.get(normalizedKey);
        if (customProvider != null) {
            try {
                return customProvider.getValue(this);
            } catch (Exception e) {
                Log.w(TAG, "Custom basic ingestion value provider failed for key: " + normalizedKey, e);
            }
        }
        BasicIngestionKey basicIngestionKey = BasicIngestionKey.fromKey(normalizedKey);
        return basicIngestionKey == null ? null : basicIngestionKey.getValue(this);
    }

    private static String normalizeBasicIngestionKey(BasicIngestionKey key) {
        if (key == null || TextUtils.isEmpty(key.getKey())) {
            return null;
        }
        return key.getKey().trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeBasicIngestionKey(String key) {
        if (TextUtils.isEmpty(key)) {
            return null;
        }
        return key.trim().toLowerCase(Locale.ROOT);
    }

    public String getLanguage() {
        return Locale.getDefault().getDisplayLanguage();
    }

    public String getTimeZoneId() {
        return TimeZone.getDefault().getID();
    }

    /** Returns timezone offset string e.g. "GMT+08:00", "GMT-05:00". */
    public String getTimeZoneOffsetDisplay() {
        int offsetMs = TimeZone.getDefault().getRawOffset();
        int offsetMinutes = offsetMs / (60 * 1000);
        int hours = offsetMinutes / 60;
        int minutes = Math.abs(offsetMinutes % 60);
        String sign = offsetMinutes >= 0 ? "+" : "-";
        return String.format(Locale.ROOT, "GMT%s%02d:%02d", sign, Math.abs(hours), minutes);
    }

    @SuppressLint("HardwareIds")
    public String getImei() {
        if (!DevicePermissionManager.hasPermission(appContext,
                DevicePermissionManager.PERMISSION_READ_PHONE_STATE)) {
            return null;
        }
        TelephonyManager telephonyManager =
                (TelephonyManager) appContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager == null) {
            return null;
        }
        return getImeiCompat(telephonyManager);
    }

    @SuppressLint("HardwareIds")
    private String getImeiCompat(TelephonyManager telephonyManager) {
        if (telephonyManager == null) {
            return null;
        }
        try {
            String deviceId = telephonyManager.getDeviceId();
            if (!TextUtils.isEmpty(deviceId)) {
                return deviceId;
            }
        } catch (SecurityException ignored) {
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                String imei = telephonyManager.getImei();
                if (!TextUtils.isEmpty(imei)) {
                    return imei;
                }
            } catch (SecurityException ignored) {
            }
            if (DevicePermissionManager.hasPermission(appContext,
                    DevicePermissionManager.PERMISSION_READ_PRIVILEGED_PHONE_STATE)) {
                try {
                    for (int slot = 0; slot <= 1; slot++) {
                        String imei = telephonyManager.getImei(slot);
                        if (!TextUtils.isEmpty(imei)) {
                            return imei;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            try {
                String meid = telephonyManager.getMeid();
                if (!TextUtils.isEmpty(meid)) {
                    return meid;
                }
            } catch (SecurityException ignored) {
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    String meid = telephonyManager.getMeid(0);
                    if (!TextUtils.isEmpty(meid)) {
                        return meid;
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    /**
     * @param includeIpv6 true to collect both IPv4 and IPv6, false to collect IPv4 only
     */
    public String getTerminalIp(boolean includeIpv6) {
        try {
            Set<String> ips = new LinkedHashSet<>();
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            if (nis == null) {
                return null;
            }
            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                if (ni == null || !ni.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                if (addresses == null) {
                    continue;
                }
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address == null || address.isLoopbackAddress()) {
                        continue;
                    }
                    String ip = getIpString(address, includeIpv6);
                    if (ip != null && !ip.isEmpty()) {
                        ips.add(ip);
                    }
                }
            }
            return ips.isEmpty() ? null : String.join(",", ips);
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Collects both IPv4 and IPv6 by default. */
    public String getTerminalIp() {
        return getTerminalIp(true);
    }

    private String getIpString(InetAddress address, boolean includeIpv6) {
        if (address instanceof Inet4Address) {
            return address.getHostAddress();
        }
        if (includeIpv6 && address instanceof Inet6Address) {
            String raw = address.getHostAddress();
            if (raw != null) {
                int scopeIdx = raw.indexOf('%');
                return scopeIdx < 0 ? raw.toUpperCase() : raw.substring(0, scopeIdx).toUpperCase();
            }
        }
        return null;
    }

    public String isGravitySensorSupported() {
        SensorManager sensorManager =
                (SensorManager) appContext.getSystemService(Context.SENSOR_SERVICE);
        boolean supported = sensorManager != null
                && sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null;
        return supported ? DeviceState.SUPPORTED.getValue() : DeviceState.UNSUPPORTED.getValue();
    }

    public String isBluetoothSupported() {
        boolean supported = BluetoothAdapter.getDefaultAdapter() != null;
        return supported ? DeviceState.SUPPORTED.getValue() : DeviceState.UNSUPPORTED.getValue();
    }

    public String getAndroidVersion() {
        return Build.VERSION.RELEASE;
    }

    public long getTotalStorageBytes() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                StorageStatsManager storageStatsManager =
                        (StorageStatsManager) appContext.getSystemService(Context.STORAGE_STATS_SERVICE);
                UUID uuid = StorageManager.UUID_DEFAULT;
                return storageStatsManager.getTotalBytes(uuid);
            } catch (Exception e) {
                Log.w(TAG, "VERSION.O getTotalInternalStorageSize err: " + e);
            }
        }

        // Android 7.1
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            try {
                StorageManager storageManager =
                        (StorageManager) appContext.getSystemService(Context.STORAGE_SERVICE);
                Method method =
                        StorageManager.class.getMethod("getPrimaryStorageSize");
                Object result = method.invoke(storageManager);
                if (result instanceof Long) {
                    return (Long) result;
                }
            } catch (Exception e) {
                Log.w(TAG, "VERSION.N_MR1 getTotalInternalStorageSize err: " + e);
            }
        }
        File file = Environment.getDataDirectory();
        StatFs statFs = new StatFs(file.getPath());
        return statFs.getBlockSizeLong() * statFs.getBlockCountLong();
    }

    public long getStorageUsageBytes() {
        long total = getTotalStorageBytes();
        long available = getAvailableStorageBytes();
        if (total <= 0L) {
            return 0L;
        }
        return Math.max(0L, total - available);
    }

    public long getTotalRamBytes() {
        ActivityManager activityManager =
                (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            return 0L;
        }
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo.totalMem;
    }

    public long getRamUsageBytes() {
        ActivityManager activityManager =
                (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            return 0L;
        }
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        if (memoryInfo.totalMem <= 0L) {
            return 0L;
        }
        return Math.max(0L, memoryInfo.totalMem - memoryInfo.availMem);
    }

    public NetworkType getNetworkType() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return NetworkType.NETWORK_UNKNOWN;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return NetworkType.NETWORK_NO;
            }
            NetworkCapabilities capabilities =
                    connectivityManager.getNetworkCapabilities(network);
            if (capabilities == null) {
                return NetworkType.NETWORK_UNKNOWN;
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return NetworkType.NETWORK_WIFI;
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return resolveMobileNetworkType(getNetworkSubtype());
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return NetworkType.NETWORK_ETHERNET;
            }
            return NetworkType.NETWORK_UNKNOWN;
        }
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info == null || !info.isConnected()) {
            return NetworkType.NETWORK_NO;
        }
        int type = info.getType();
        if (type == ConnectivityManager.TYPE_WIFI) {
            return NetworkType.NETWORK_WIFI;
        }
        if (type == ConnectivityManager.TYPE_MOBILE) {
            return resolveMobileNetworkType(info.getSubtype());
        }
        if (type == ConnectivityManager.TYPE_ETHERNET) {
            return NetworkType.NETWORK_ETHERNET;
        }
        return NetworkType.NETWORK_UNKNOWN;
    }

    /** Returns network type string for ingestion, with "NETWORK_" prefix stripped. */
    public String getNetworkTypeDisplay() {
        NetworkType networkType = getNetworkType();
        return networkType == null ? null : networkType.name().replace("NETWORK_", "");
    }

    private int getNetworkSubtype() {
        TelephonyManager telephonyManager = getDefaultTelephonyManager();
        if (telephonyManager == null) {
            return TelephonyManager.NETWORK_TYPE_UNKNOWN;
        }
        try {
            return telephonyManager.getNetworkType();
        } catch (SecurityException ignored) {
            return TelephonyManager.NETWORK_TYPE_UNKNOWN;
        }
    }

    private NetworkType resolveMobileNetworkType(int subtype) {
        if (subtype == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
            return NetworkType.NETWORK_UNKNOWN;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && subtype == TelephonyManager.NETWORK_TYPE_NR) {
            return NetworkType.NETWORK_5G;
        }
        if (subtype == TelephonyManager.NETWORK_TYPE_LTE) {
            return NetworkType.NETWORK_4G;
        }
        switch (subtype) {
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case NETWORK_TYPE_TD_SCDMA:
                return NetworkType.NETWORK_3G;
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return NetworkType.NETWORK_2G;
            default:
                return NetworkType.NETWORK_UNKNOWN;
        }
    }

    public Integer getSignalStrengthLevel() {
        if (!isSimSupported()) {
            return null;
        }
        return getSignalLevelForTelephonyManager(getDefaultTelephonyManager());
    }

    public Integer getSignalIntensityCarrier1Level() {
        if (!isSimSupported()) {
            return null;
        }
        return getSignalLevelForSubscriptionIndex(0);
    }

    public Integer getSignalIntensityCarrier2Level() {
        if (!isSimSupported()) {
            return null;
        }
        return getSignalLevelForSubscriptionIndex(1);
    }

    public String getCellId() {
        return getCellIdForTelephonyManager(getDefaultTelephonyManager());
    }

    public boolean isSimSupported() {
        TelephonyManager telephonyManager = getDefaultTelephonyManager();
        if (telephonyManager == null) {
            return false;
        }
        int state = telephonyManager.getSimState();
        return state == TelephonyManager.SIM_STATE_READY;
    }

    public String getSimOperator() {
        TelephonyManager telephonyManager = getDefaultTelephonyManager();
        return telephonyManager == null ? null : telephonyManager.getSimOperatorName();
    }

    public String getSim1Operator() {
        return getOperatorNameForSubscriptionIndex(0);
    }

    public String getSim2Operator() {
        return getOperatorNameForSubscriptionIndex(1);
    }

    /**
     * Returns SIM ICCID. On Android 10 (API 29) and above, non-null result typically requires
     * READ_PRIVILEGED_PHONE_STATE (system/signature apps only); otherwise returns null.
     */
    @SuppressLint("HardwareIds")
    public String getIccid() {
        String iccid = getIccidForSubscriptionIndex(0);
        if (!TextUtils.isEmpty(iccid)) {
            return iccid;
        }
        TelephonyManager telephonyManager = getDefaultTelephonyManager();
        if (telephonyManager == null) {
            return null;
        }
        if (!DevicePermissionManager.hasPermission(appContext,
                DevicePermissionManager.PERMISSION_READ_PHONE_STATE)) {
            return null;
        }
        try {
            iccid = telephonyManager.getSimSerialNumber();
        } catch (SecurityException ignored) {
            iccid = null;
        }
        if (!TextUtils.isEmpty(iccid)) {
            return iccid;
        }
        return null;
    }

    public String getSim1Iccid() {
        return getIccidForSubscriptionIndex(0);
    }

    public String getSim2Iccid() {
        return getIccidForSubscriptionIndex(1);
    }

    public String isGpsEnabled() {
        LocationManager locationManager =
                (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return DeviceState.DISABLED.getValue();
        }
        try {
            boolean enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            return enabled ? DeviceState.ENABLED.getValue() : DeviceState.DISABLED.getValue();
        } catch (Exception ignored) {
            return DeviceState.DISABLED.getValue();
        }
    }

    @SuppressLint("MissingPermission")
    public String getLocation() {
        if (!hasLocationPermission()) {
            return null;
        }
        LocationManager locationManager =
                (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return null;
        }
        Location location = null;
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if (location == null && locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        } catch (SecurityException ignored) {
            return null;
        }
        if (location == null) {
            return null;
        }
        return String.format(Locale.ROOT, "%f,%f", location.getLatitude(), location.getLongitude());
    }

    private long getAvailableStorageBytes() {
        StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
        return statFs.getBlockSizeLong() * statFs.getAvailableBlocksLong();
    }

    private TelephonyManager getDefaultTelephonyManager() {
        return (TelephonyManager) appContext.getSystemService(Context.TELEPHONY_SERVICE);
    }

    private boolean hasLocationPermission() {
        return DevicePermissionManager.hasPermission(appContext,
                DevicePermissionManager.PERMISSION_ACCESS_FINE_LOCATION)
                || DevicePermissionManager.hasPermission(appContext,
                DevicePermissionManager.PERMISSION_ACCESS_COARSE_LOCATION);
    }

    private Integer getSignalLevelForSubscriptionIndex(int index) {
        TelephonyManager telephonyManager = getTelephonyManagerForSubscriptionIndex(index);
        return getSignalLevelForTelephonyManager(telephonyManager);
    }

    @SuppressLint("MissingPermission")
    private Integer getSignalLevelForTelephonyManager(TelephonyManager telephonyManager) {
        if (telephonyManager == null) {
            return null;
        }
        if (!DevicePermissionManager.hasPermission(appContext,
                DevicePermissionManager.PERMISSION_READ_PHONE_STATE)) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasLocationPermission()) {
            return null;
        }
        TelephonyManager defaultTm = getDefaultTelephonyManager();
        if (Build.VERSION.SDK_INT >= API_LEVEL_SIGNAL_STRENGTHS_LISTENER && telephonyManager == defaultTm) {
            ensureDefaultSignalStrengthCallbackRegistered(defaultTm);
            if (cachedDefaultSignalLevel != null) {
                return cachedDefaultSignalLevel;
            }
        }
        return getSignalLevelFromCellInfo(telephonyManager);
    }

    private void ensureDefaultSignalStrengthCallbackRegistered(TelephonyManager telephonyManager) {
        if (telephonyManager == null || defaultSignalCallbackRegistered) {
            return;
        }
        if (Build.VERSION.SDK_INT < API_LEVEL_SIGNAL_STRENGTHS_LISTENER) {
            return;
        }
        synchronized (this) {
            if (defaultSignalCallbackRegistered) {
                return;
            }
            try {
                TelephonyCallback callback = new SignalStrengthTelephonyCallback(this);
                telephonyManager.registerTelephonyCallback(appContext.getMainExecutor(), callback);
                defaultSignalCallbackRegistered = true;
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * Gets signal level (0-4) from CellInfo. Prefers registered cells; also considers all cells
     * for max level so the result is closer to the system status bar (which often shows best signal).
     */
    private Integer getSignalLevelFromCellInfo(TelephonyManager telephonyManager) {
        try {
            List<CellInfo> cellInfos = telephonyManager.getAllCellInfo();
            if (cellInfos == null) {
                return null;
            }
            int maxLevel = -1;
            int maxLevelRegistered = -1;
            for (CellInfo cellInfo : cellInfos) {
                if (cellInfo == null) {
                    continue;
                }
                Integer level = getLevelFromCellInfo(cellInfo);
                if (level != null && level > maxLevel) {
                    maxLevel = level;
                }
                if (cellInfo.isRegistered() && level != null && level > maxLevelRegistered) {
                    maxLevelRegistered = level;
                }
            }
            int result = maxLevelRegistered >= 0 ? maxLevelRegistered : maxLevel;
            if (result < 0) {
                return null;
            }
            return Math.min(4, result);
        } catch (SecurityException ignored) {
            return null;
        }
    }

    private Integer getLevelFromCellInfo(CellInfo cellInfo) {
        CellSignalStrength strength = null;
        if (cellInfo instanceof CellInfoGsm) {
            strength = ((CellInfoGsm) cellInfo).getCellSignalStrength();
        } else if (cellInfo instanceof CellInfoLte) {
            strength = ((CellInfoLte) cellInfo).getCellSignalStrength();
        } else if (cellInfo instanceof CellInfoWcdma) {
            strength = ((CellInfoWcdma) cellInfo).getCellSignalStrength();
        } else if (cellInfo instanceof CellInfoCdma) {
            strength = ((CellInfoCdma) cellInfo).getCellSignalStrength();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo instanceof CellInfoNr) {
            strength = ((CellInfoNr) cellInfo).getCellSignalStrength();
        }
        if (strength == null) {
            return null;
        }
        int level = strength.getLevel();
        int levelFromRsrp = getLevelFromRsrp(strength);
        return levelFromRsrp >= 0 ? Math.max(level, levelFromRsrp) : level;
    }

    /**
     * Maps RSRP/dBm to 0-4 using thresholds that tend to match system status bar display.
     * getLevel() can be stricter than the UI; this gives a more aligned value for LTE/NR.
     */
    private int getLevelFromRsrp(CellSignalStrength strength) {
        int dbm;
        try {
            dbm = strength.getDbm();
        } catch (Throwable ignored) {
            return -1;
        }
        if (dbm >= 0 || dbm < -140) {
            return -1;
        }
        if (dbm >= -85) return 4;
        if (dbm >= -95) return 3;
        if (dbm >= -105) return 2;
        if (dbm >= -115) return 1;
        return 0;
    }

    private String getCellIdForTelephonyManager(TelephonyManager telephonyManager) {
        if (telephonyManager == null || !hasLocationPermission()) {
            return null;
        }
        try {
            CellLocation cellLocation = telephonyManager.getCellLocation();
            if (cellLocation instanceof GsmCellLocation) {
                return String.valueOf(((GsmCellLocation) cellLocation).getCid());
            }
            if (cellLocation instanceof CdmaCellLocation) {
                return String.valueOf(((CdmaCellLocation) cellLocation).getBaseStationId());
            }
        } catch (SecurityException ignored) {
            return null;
        }
        return null;
    }

    private String getCellIdFromCellInfo(CellInfo cellInfo) {
        if (cellInfo instanceof CellInfoGsm) {
            return String.valueOf(((CellInfoGsm) cellInfo).getCellIdentity().getCid());
        }
        if (cellInfo instanceof CellInfoLte) {
            return String.valueOf(((CellInfoLte) cellInfo).getCellIdentity().getCi());
        }
        if (cellInfo instanceof CellInfoWcdma) {
            return String.valueOf(((CellInfoWcdma) cellInfo).getCellIdentity().getCid());
        }
        if (cellInfo instanceof CellInfoCdma) {
            return String.valueOf(((CellInfoCdma) cellInfo).getCellIdentity().getBasestationId());
        }
        return null;
    }

    private String getOperatorNameForSubscriptionIndex(int index) {
        TelephonyManager telephonyManager = getTelephonyManagerForSubscriptionIndex(index);
        return telephonyManager == null ? null : telephonyManager.getSimOperatorName();
    }

    private String getIccidForSubscriptionIndex(int index) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            return null;
        }
        if (!DevicePermissionManager.hasPermission(appContext,
                DevicePermissionManager.PERMISSION_READ_PHONE_STATE)) {
            return null;
        }
        SubscriptionInfo info = getSubscriptionInfoByIndex(index);
        if (info == null) {
            return null;
        }
        return info.getIccId();
    }

    @SuppressLint("NewApi")
    private TelephonyManager getTelephonyManagerForSubscriptionIndex(int index) {
        SubscriptionInfo info = getSubscriptionInfoByIndex(index);
        if (info == null) {
            return index == 0 ? getDefaultTelephonyManager() : null;
        }
        return getTelephonyManagerForSubscriptionId(info.getSubscriptionId());
    }

    private SubscriptionInfo getSubscriptionInfoByIndex(int index) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            return null;
        }
        if (!DevicePermissionManager.hasPermission(appContext,
                DevicePermissionManager.PERMISSION_READ_PHONE_STATE)) {
            return null;
        }
        SubscriptionManager subscriptionManager =
                (SubscriptionManager) appContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        if (subscriptionManager == null) {
            return null;
        }
        try {
            List<SubscriptionInfo> infos = subscriptionManager.getActiveSubscriptionInfoList();
            if (infos == null || infos.isEmpty() || index < 0 || index >= infos.size()) {
                return null;
            }
            return infos.get(index);
        } catch (SecurityException ignored) {
            return null;
        }
    }

    private TelephonyManager getTelephonyManagerForSubscriptionId(int subscriptionId) {
        TelephonyManager telephonyManager = getDefaultTelephonyManager();
        if (telephonyManager == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return telephonyManager;
        }
        return telephonyManager.createForSubscriptionId(subscriptionId);
    }

    void setCachedDefaultSignalLevel(Integer level) {
        this.cachedDefaultSignalLevel = level;
    }

    @android.annotation.SuppressLint("NewApi")
    private static class SignalStrengthTelephonyCallback extends TelephonyCallback
            implements TelephonyCallback.SignalStrengthsListener {
        private final DeviceInfoProvider provider;

        SignalStrengthTelephonyCallback(DeviceInfoProvider p) {
            provider = p;
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            if (signalStrength != null) {
                provider.setCachedDefaultSignalLevel(signalStrength.getLevel());
            }
        }
    }
}
