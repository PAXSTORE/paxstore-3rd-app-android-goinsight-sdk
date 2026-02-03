package com.pax.market.android.app.sdk.goinsight.internal.key;

import com.pax.market.android.app.sdk.device.model.NetworkType;
import com.pax.market.android.app.sdk.device.provider.DeviceInfoProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public enum BasicIngestionKey {
    ASS_LANGUAGE("ass_language") {
        @Override
        public Object getValue(DeviceInfoProvider provider) {
            return provider.getLanguage();
        }
    },
    ASS_NETWORK("ass_network") {
        @Override
        public Object getValue(DeviceInfoProvider provider) {
            NetworkType networkType = provider.getNetworkType();
            return networkType == null ? null : networkType.name();
        }
    },
    ASS_BLUETOOTH("ass_bluetooth") {
        @Override
        public Object getValue(DeviceInfoProvider provider) {
            return provider.isBluetoothSupported();
        }
    },
    ASS_GRAVITY_SENSOR("ass_gravitySensor") {
        @Override
        public Object getValue(DeviceInfoProvider provider) {
            return provider.isGravitySensorSupported();
        }
    },
    ASS_GPS("ass_gps") {
        @Override
        public Object getValue(DeviceInfoProvider provider) {
            return provider.isGpsEnabled();
        }
    },
    ASS_IMEI("ass_imei") {
        @Override
        public Object getValue(DeviceInfoProvider provider) {
            return provider.getImei();
        }
    },
    ASS_CCID("ass_ccid") {
        @Override
        public Object getValue(DeviceInfoProvider provider) {
            return provider.getIccid();
        }
    },
    ASS_CELL_ID("ass_cellid") {
        @Override
        public Object getValue(DeviceInfoProvider provider) {
            return provider.getCellId();
        }
    },
    ASS_LOCATION("ass_location") {
        @Override
        public Object getValue(DeviceInfoProvider provider) {
            return provider.getLocation();
        }
    },
    ASS_SIM_CARD_TWO("ass_simCardTwo") {
        @Override
        public Object getValue(DeviceInfoProvider provider) {
            return provider.getSim2Operator();
        }
    },
    ASS_SIM_CARD("ass_simCard") {
        @Override
        public Object getValue(DeviceInfoProvider provider) {
            return provider.getSimOperator();
        }
    },
    ASS_SIM_ONE_ICCID("ass_simOneIccid") {
        @Override
        public Object getValue(DeviceInfoProvider provider) {
            return provider.getSim1Iccid();
        }
    },
    ASS_SIM_TWO_ICCID("ass_simTwoIccid") {
        @Override
        public Object getValue(DeviceInfoProvider provider) {
            return provider.getSim2Iccid();
        }
    },
    ASS_STORAGE_USAGE("ass_storageUsage") {
        @Override
        public Object getValue(DeviceInfoProvider provider) {
            return provider.getStorageUsageBytes();
        }
    },
    ASS_TOTAL_USAGE("ass_totalUsage") {
        @Override
        public Object getValue(DeviceInfoProvider provider) {
            return provider.getTotalStorageBytes();
        }
    },
    ASS_RAM("ass_ram") {
        @Override
        public Object getValue(DeviceInfoProvider provider) {
            return provider.getRamUsageBytes();
        }
    },
    ASS_SIGNAL_INTENSITY("ass_signalIntensity") {
        @Override
        public Object getValue(DeviceInfoProvider provider) {
            return provider.getSignalStrengthLevel();
        }
    },
    ASS_TOTAL_RAM("ass_totalRam") {
        @Override
        public Object getValue(DeviceInfoProvider provider) {
            return provider.getTotalRamBytes();
        }
    },
    ASS_SIGNAL_INTENSITY_ONE("ass_signalIntensityOne") {
        @Override
        public Object getValue(DeviceInfoProvider provider) {
            return provider.getSignalIntensityCarrier1Level();
        }
    },
    ASS_SIGNAL_INTENSITY_TWO("ass_signalIntensityTwo") {
        @Override
        public Object getValue(DeviceInfoProvider provider) {
            return provider.getSignalIntensityCarrier2Level();
        }
    };

    private static final Map<String, BasicIngestionKey> INDEX = buildIndex();

    private final String key;

    BasicIngestionKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public abstract Object getValue(DeviceInfoProvider provider);

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
