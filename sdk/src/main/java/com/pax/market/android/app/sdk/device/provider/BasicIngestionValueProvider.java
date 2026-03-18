package com.pax.market.android.app.sdk.device.provider;

@FunctionalInterface
public interface BasicIngestionValueProvider {
    Object getValue(DeviceInfoProvider provider);
}
