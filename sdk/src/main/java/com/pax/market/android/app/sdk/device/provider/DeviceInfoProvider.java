package com.pax.market.android.app.sdk.device.provider;

import static android.telephony.TelephonyManager.NETWORK_TYPE_TD_SCDMA;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
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

import com.pax.market.android.app.sdk.device.model.LocationInfo;
import com.pax.market.android.app.sdk.device.model.NetworkType;
import com.pax.market.android.app.sdk.device.permission.DevicePermissionManager;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class DeviceInfoProvider {
    private static final int API_LEVEL_SIGNAL_STRENGTHS_LISTENER = 31;

    private final Context appContext;
    private volatile Integer cachedDefaultSignalLevel = null;
    private volatile boolean defaultSignalCallbackRegistered = false;

    public DeviceInfoProvider(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public String getLanguage() {
        return Locale.getDefault().toString();
    }

    public String getTimeZoneId() {
        return TimeZone.getDefault().getID();
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
        }
        return null;
    }

    public String getTerminalIp() {
        try {
            for (NetworkInterface networkInterface : Collections.list(
                    NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                    if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public boolean isGravitySensorSupported() {
        SensorManager sensorManager =
                (SensorManager) appContext.getSystemService(Context.SENSOR_SERVICE);
        return sensorManager != null
                && sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null;
    }

    public boolean isBluetoothSupported() {
        return BluetoothAdapter.getDefaultAdapter() != null;
    }

    public String getAndroidVersion() {
        return Build.VERSION.RELEASE;
    }

    public long getTotalStorageBytes() {
        StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
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
        return getSignalLevelForTelephonyManager(getDefaultTelephonyManager());
    }

    public Integer getSignalIntensityCarrier1Level() {
        return getSignalLevelForSubscriptionIndex(0);
    }

    public Integer getSignalIntensityCarrier2Level() {
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
        return state != TelephonyManager.SIM_STATE_ABSENT;
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

    public boolean isGpsEnabled() {
        LocationManager locationManager =
                (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return false;
        }
        try {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ignored) {
            return false;
        }
    }

    @SuppressLint("MissingPermission")
    public LocationInfo getLocation() {
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
        return new LocationInfo(location.getLatitude(), location.getLongitude());
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

    private Integer getSignalLevelFromCellInfo(TelephonyManager telephonyManager) {
        try {
            List<CellInfo> cellInfos = telephonyManager.getAllCellInfo();
            if (cellInfos == null) {
                return null;
            }
            int maxLevel = -1;
            for (CellInfo cellInfo : cellInfos) {
                if (cellInfo != null && cellInfo.isRegistered()) {
                    Integer level = getLevelFromCellInfo(cellInfo);
                    if (level != null && level > maxLevel) {
                        maxLevel = level;
                    }
                }
            }
            return maxLevel >= 0 ? maxLevel : null;
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
        return strength != null ? strength.getLevel() : null;
    }

    private String getCellIdForTelephonyManager(TelephonyManager telephonyManager) {
        if (telephonyManager == null || !hasLocationPermission()) {
            return null;
        }
        try {
            List<CellInfo> cellInfos = telephonyManager.getAllCellInfo();
            if (cellInfos != null) {
                for (CellInfo cellInfo : cellInfos) {
                    if (cellInfo != null && cellInfo.isRegistered()) {
                        String cellId = getCellIdFromCellInfo(cellInfo);
                        if (!TextUtils.isEmpty(cellId)) {
                            return cellId;
                        }
                    }
                }
            }
        } catch (SecurityException ignored) {
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
