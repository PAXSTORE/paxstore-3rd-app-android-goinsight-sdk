package com.pax.market.android.app.sdk.device.model;

public enum DeviceState {
    SUPPORTED("Supported"),
    UNSUPPORTED("Unsupported"),
    ENABLED("Enabled"),
    DISABLED("Disabled");

    private final String value;

    DeviceState(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
