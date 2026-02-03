package com.pax.market.android.app.sdk.goinsight;

import android.content.Context;
import com.pax.market.android.app.sdk.goinsight.dto.DatasetAssociateColsResponse;
import com.pax.market.android.app.sdk.goinsight.internal.GoInsightAssociateCollector;
import com.pax.market.android.app.sdk.device.provider.DeviceInfoProvider;
import com.pax.market.android.app.sdk.device.provider.InstalledAppsProvider;
import com.pax.market.api.sdk.java.api.sync.GoInsightApi;
import com.pax.market.api.sdk.java.base.constant.Constants;
import com.pax.market.api.sdk.java.base.dto.SdkObject;
import com.pax.market.api.sdk.java.base.request.SdkRequest;
import com.pax.market.api.sdk.java.base.util.JsonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

public class GoInsightAssociateApi extends GoInsightApi {
    private static final String DATASET_ASSOCIATE_CONFIG_URL = "dataset/associate/config";
    private static final long CONFIG_REFRESH_INTERVAL_MS = 24L * 60 * 60 * 1000;
    private static final Logger LOGGER = LoggerFactory.getLogger(GoInsightAssociateApi.class);

    private final GoInsightAssociateCollector dataCollector;
    private volatile DatasetAssociateColsResponse cachedConfig;
    private volatile long lastConfigFetchAtMs;
    private final AtomicInteger configFetchFailureCount = new AtomicInteger();

    public GoInsightAssociateApi(String baseUrl,
                                 String appKey,
                                 String appSecret,
                                 String terminalSN,
                                 TimeZone timeZone) {
        super(baseUrl, appKey, appSecret, terminalSN, timeZone);
        this.dataCollector = null;
    }

    public GoInsightAssociateApi(Context context,
                                 String baseUrl,
                                 String appKey,
                                 String appSecret,
                                 String terminalSN,
                                 TimeZone timeZone) {
        super(baseUrl, appKey, appSecret, terminalSN, timeZone);
        DeviceInfoProvider deviceInfoProvider = new DeviceInfoProvider(context);
        InstalledAppsProvider installedAppsProvider = new InstalledAppsProvider(context);
        this.dataCollector = new GoInsightAssociateCollector(deviceInfoProvider,
                installedAppsProvider);
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

    public SdkObject syncTerminalBizDataWithDeviceInfo(GoInsightApi goInsightApi,
                                                       List<Map<String, Object>> list)
            throws Exception {
        if (goInsightApi == null) {
            throw new IllegalArgumentException("goInsightApi is null");
        }
        DatasetAssociateColsResponse config = getDatasetAssociateConfigWithCache();
        List<Map<String, Object>> mergedList = appendDeviceData(list, config);
        return goInsightApi.syncTerminalBizData(mergedList);
    }

    public SdkObject syncTerminalBizDataWithDeviceInfo(List<Map<String, Object>> list)
            throws Exception {
        DatasetAssociateColsResponse config = getDatasetAssociateConfigWithCache();
        List<Map<String, Object>> mergedList = appendDeviceData(list, config);
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
                configFetchFailureCount.incrementAndGet();
                LOGGER.warn("GoInsightAssociate config is null, use cache");
            } catch (Exception ex) {
                configFetchFailureCount.incrementAndGet();
                LOGGER.warn("GoInsightAssociate config fetch failed, use cache", ex);
            }
            return cachedConfig == null ? new DatasetAssociateColsResponse() : cachedConfig;
        }
    }

    private void updateConfigCache(DatasetAssociateColsResponse response) {
        cachedConfig = response;
        lastConfigFetchAtMs = System.currentTimeMillis();
    }

    public int getConfigFetchFailureCount() {
        return configFetchFailureCount.get();
    }
}
