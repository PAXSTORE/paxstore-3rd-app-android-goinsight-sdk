package com.pax.market.android.app.sdk.goinsight;

import android.content.Context;
import android.content.SharedPreferences;

import com.pax.market.android.app.sdk.goinsight.dto.DatasetAssociateColsResponse;
import com.pax.market.android.app.sdk.goinsight.internal.GoInsightAssociateCollector;
import com.pax.market.android.app.sdk.device.provider.DeviceInfoProvider;
import com.pax.market.android.app.sdk.device.provider.InstalledAppsProvider;
import com.pax.market.api.sdk.java.api.sync.GoInsightApi;
import com.pax.market.api.sdk.java.base.constant.Constants;
import com.pax.market.api.sdk.java.base.dto.SdkObject;
import com.pax.market.api.sdk.java.base.request.SdkRequest;
import com.google.gson.Gson;
import com.pax.market.api.sdk.java.base.util.JsonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class GoInsightAssociateApi extends GoInsightApi {
    private static final String DATASET_ASSOCIATE_CONFIG_URL = "v1/3rdApps/goInsight/dataset/associate/config";
    private static final long CONFIG_REFRESH_INTERVAL_MS = 60 * 1000;
    private static final String SP_NAME = "go_insight_associate";
    private static final String SP_KEY_CACHED_CONFIG = "cached_config";
    private static final String SP_KEY_LAST_CONFIG_FETCH_MS = "last_config_fetch_ms";
    private static final Logger LOGGER = LoggerFactory.getLogger(GoInsightAssociateApi.class);
    private static final Gson GSON = new Gson();

    private Context appContext;
    private GoInsightAssociateCollector dataCollector;
    private volatile DatasetAssociateColsResponse cachedConfig;
    private volatile long lastConfigFetchAtMs;

    public GoInsightAssociateApi(Context context,
                                 String baseUrl,
                                 String appKey,
                                 String appSecret,
                                 String terminalSN,
                                 TimeZone timeZone) {
        super(baseUrl, appKey, appSecret, terminalSN, timeZone);
        if (context == null) {
            return;
        }
        this.appContext = context.getApplicationContext();
        DeviceInfoProvider deviceInfoProvider = new DeviceInfoProvider(context);
        InstalledAppsProvider installedAppsProvider = new InstalledAppsProvider(context);
        this.dataCollector = new GoInsightAssociateCollector(deviceInfoProvider,
                installedAppsProvider);
        loadConfigFromSp();
    }

    /**
     * Get dataset associate config.
     *
     * @return DatasetAssociateColsResponse
     */
    public DatasetAssociateColsResponse getDatasetAssociateConfig() {
        SdkRequest request = new SdkRequest(DATASET_ASSOCIATE_CONFIG_URL);
        request.setRequestMethod(SdkRequest.RequestMethod.GET);
        request.addHeader(Constants.REQ_HEADER_SN, getTerminalSN());
        DatasetAssociateColsResponse response =
                JsonUtils.fromJson(call(request), DatasetAssociateColsResponse.class);
        updateConfigCache(response);
        return response;
    }

    public SdkObject syncTerminalBizDataWithDeviceInfo(List<Map<String, Object>> list) {
        DatasetAssociateColsResponse config = getDatasetAssociateConfigWithCache();
        List<Map<String, Object>> mergedList = appendDeviceData(list, config);
        LOGGER.warn("syncTerminalBizDataWithDeviceInfo mergedList: {}", GSON.toJson(mergedList));

        return syncTerminalBizData(mergedList);
    }

    public List<Map<String, Object>> appendDeviceData(List<Map<String, Object>> list,
                                                      DatasetAssociateColsResponse config) {
        return getDataCollector().appendDeviceData(list, config);
    }

    private GoInsightAssociateCollector getDataCollector() {
        if (dataCollector == null) {
            throw new IllegalStateException("Context is required for device info");
        }
        return dataCollector;
    }

    private DatasetAssociateColsResponse getDatasetAssociateConfigWithCache() {
        long now = System.currentTimeMillis();
        DatasetAssociateColsResponse localConfig = cachedConfig;
        if (localConfig != null && now - lastConfigFetchAtMs < CONFIG_REFRESH_INTERVAL_MS) {
            return localConfig;
        }
        synchronized (this) {
            long checkNow = System.currentTimeMillis();
            if (cachedConfig != null
                    && checkNow - lastConfigFetchAtMs < CONFIG_REFRESH_INTERVAL_MS) {
                return cachedConfig;
            }
            try {
                DatasetAssociateColsResponse response = getDatasetAssociateConfig();
                if (response != null) {
                    return response;
                }
                LOGGER.warn("GoInsightAssociate config is null, use cache");
            } catch (Exception ex) {
                LOGGER.warn("GoInsightAssociate config fetch failed, use cache", ex);
            }
            return cachedConfig == null ? new DatasetAssociateColsResponse() : cachedConfig;
        }
    }

    private void updateConfigCache(DatasetAssociateColsResponse response) {
        cachedConfig = response;
        lastConfigFetchAtMs = System.currentTimeMillis();
        saveConfigToSp(response, lastConfigFetchAtMs);
    }

    private SharedPreferences getSp() {
        if (appContext == null) {
            return null;
        }
        return appContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

    private void loadConfigFromSp() {
        SharedPreferences sp = getSp();
        if (sp == null) {
            return;
        }
        try {
            String json = sp.getString(SP_KEY_CACHED_CONFIG, null);
            if (json != null && !json.isEmpty()) {
                DatasetAssociateColsResponse config = GSON.fromJson(json, DatasetAssociateColsResponse.class);
                if (config != null) {
                    cachedConfig = config;
                    lastConfigFetchAtMs = sp.getLong(SP_KEY_LAST_CONFIG_FETCH_MS, 0L);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Load GoInsightAssociate config from SP failed", e);
        }
    }

    private void saveConfigToSp(DatasetAssociateColsResponse config, long fetchTimeMs) {
        SharedPreferences sp = getSp();
        if (sp == null || config == null) {
            return;
        }
        try {
            String json = GSON.toJson(config);
            if (json != null) {
                sp.edit()
                        .putString(SP_KEY_CACHED_CONFIG, json)
                        .putLong(SP_KEY_LAST_CONFIG_FETCH_MS, fetchTimeMs)
                        .apply();
            }
        } catch (Exception e) {
            LOGGER.warn("Save GoInsightAssociate config to SP failed", e);
        }
    }
}
