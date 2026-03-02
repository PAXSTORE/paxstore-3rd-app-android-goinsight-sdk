package com.pax.market.android.app.sdk;

import com.pax.market.android.app.sdk.goinsight.GoInsightAssociateApi;
import com.pax.market.api.sdk.java.base.exception.NotInitException;

import java.util.TimeZone;


public class StoreSdkExtention extends StoreSdk {

    private static volatile StoreSdkExtention instance;

    private GoInsightAssociateApi goInsightAssociateApi;


    public StoreSdkExtention() {
        StoreSdk.getInstance().acquireSemaphore();
        this.context = StoreSdk.getInstance().context;
        this.url = StoreSdk.getInstance().url;
        this.appKey = StoreSdk.getInstance().appKey;
        this.appSecret = StoreSdk.getInstance().appSecret;
        this.terminalSn = StoreSdk.getInstance().terminalSn;
        this.terminalModel = StoreSdk.getInstance().terminalModel;
    }

    public static StoreSdkExtention getInstance() {
        if (instance == null) {
            synchronized (StoreSdkExtention.class) {
                if (instance == null) {
                    instance = new StoreSdkExtention();
                }
            }
        }
        return instance;
    }

    /**
     * Get goInsightApi instance
     *
     * @return
     * @throws NotInitException
     */
    public GoInsightAssociateApi goInsightAssociateApi() throws NotInitException {
        if (terminalSn == null) {
            throw new NotInitException("Not initialized");
        }
        if (goInsightAssociateApi == null) {
            goInsightAssociateApi = new GoInsightAssociateApi(context, url, appKey, appSecret, terminalSn, TimeZone.getDefault());
        }
        goInsightAssociateApi.setBaseUrl(getDcUrl(context, goInsightAssociateApi.getBaseUrl(), true));
        goInsightAssociateApi.setProxyDelegate(BaseApiService.getInstance(context));
        return goInsightAssociateApi;
    }




}