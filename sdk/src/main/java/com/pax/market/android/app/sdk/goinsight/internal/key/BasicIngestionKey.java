package com.pax.market.android.app.sdk.goinsight.internal.key;

import com.pax.market.android.app.sdk.device.model.DeviceState;
import com.pax.market.android.app.sdk.device.provider.BasicIngestionValueProvider;
import com.pax.market.android.app.sdk.device.provider.DeviceInfoProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public enum BasicIngestionKey {
    ASS_LANGUAGE("ass_language", DeviceInfoProvider::getLanguage),
    ASS_NETWORK("ass_network", DeviceInfoProvider::getNetworkTypeDisplay),
    ASS_BLUETOOTH("ass_bluetooth", DeviceInfoProvider::isBluetoothSupported),
    ASS_GRAVITY_SENSOR("ass_gravitysensor", DeviceInfoProvider::isGravitySensorSupported),
    ASS_GPS("ass_gps", DeviceInfoProvider::isGpsEnabled),
    ASS_IMEI("ass_imei", DeviceInfoProvider::getImei),
    ASS_CCID("ass_ccid", DeviceInfoProvider::getIccid),
    ASS_CELL_ID("ass_cellid", DeviceInfoProvider::getCellId),
    ASS_LOCATION("ass_location", DeviceInfoProvider::getLocation),
    ASS_SIM_CARD_TWO("ass_simcardtwo", DeviceInfoProvider::getSim2Operator),
    ASS_SIM_CARD("ass_simcard", DeviceInfoProvider::getSimOperator),
    ASS_SIM_ONE_ICCID("ass_simoneiccid", DeviceInfoProvider::getSim1Iccid),
    ASS_SIM_TWO_ICCID("ass_simtwoiccid", DeviceInfoProvider::getSim2Iccid),
    ASS_STORAGE_USAGE("ass_storageusage", DeviceInfoProvider::getStorageUsageBytes),
    ASS_TOTAL_USAGE("ass_totalusage", DeviceInfoProvider::getTotalStorageBytes),
    ASS_RAM("ass_ram", DeviceInfoProvider::getRamUsageBytes),
    ASS_SIGNAL_INTENSITY("ass_signalintensity", DeviceInfoProvider::getSignalStrengthLevel),
    ASS_TOTAL_RAM("ass_totalram", DeviceInfoProvider::getTotalRamBytes),
    ASS_SIGNAL_INTENSITY_ONE("ass_signalintensityone", DeviceInfoProvider::getSignalIntensityCarrier1Level),
    ASS_SIGNAL_INTENSITY_TWO("ass_signalintensitytwo", DeviceInfoProvider::getSignalIntensityCarrier2Level),
    ASS_ANDROID_VERSION("ass_androidversion", DeviceInfoProvider::getAndroidVersion),
    ASS_TIME_ZONE("ass_timezone", DeviceInfoProvider::getTimeZoneOffsetDisplay),
    ASS_NETWORK_IP("ass_networkip", provider -> provider.getTerminalIp(false)),
    ASS_SIM("ass_sim", provider -> provider.isSimSupported()
            ? DeviceState.SUPPORTED.getValue()
            : DeviceState.UNSUPPORTED.getValue()),
    ASS_SIM_CARD_ONE("ass_simcardone", DeviceInfoProvider::getSim1Operator);

    private static final Map<String, BasicIngestionKey> INDEX = buildIndex();

    private final String key;
    private final BasicIngestionValueProvider valueProvider;

    BasicIngestionKey(String key, BasicIngestionValueProvider valueProvider) {
        this.key = key;
        this.valueProvider = valueProvider;
    }

    public String getKey() {
        return key;
    }

    public Object getValue(DeviceInfoProvider provider) {
        return provider == null || valueProvider == null ? null : valueProvider.getValue(provider);
    }

    public static BasicIngestionKey fromKey(String key) {
        if (key == null) {
            return null;
        }
        return INDEX.get(key.trim().toLowerCase());
    }

    public static Set<BasicIngestionKey> fromKeys(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptySet();
        }
        Set<BasicIngestionKey> result = new HashSet<>();
        for (String key : keys) {
            BasicIngestionKey value = INDEX.get(key);
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    private static Map<String, BasicIngestionKey> buildIndex() {
        Map<String, BasicIngestionKey> map = new HashMap<>();
        for (BasicIngestionKey key : values()) {
            map.put(key.getKey(), key);
        }
        return map;
    }
}
