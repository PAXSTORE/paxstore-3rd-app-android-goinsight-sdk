package com.pax.market.android.app.sdk.device.model;

public final class LocationInfo {
    private final double latitude;
    private final double longitude;

    public LocationInfo(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
