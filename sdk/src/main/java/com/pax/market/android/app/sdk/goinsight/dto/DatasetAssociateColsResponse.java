package com.pax.market.android.app.sdk.goinsight.dto;

import java.io.Serializable;
import java.util.List;

public class DatasetAssociateColsResponse implements Serializable {
    private List<String> basicIngestionCols;
    private List<AppAssociateColumn> appIngestionCols;

    public List<String> getBasicIngestionCols() {
        return basicIngestionCols;
    }

    public void setBasicIngestionCols(List<String> basicIngestionCols) {
        this.basicIngestionCols = basicIngestionCols;
    }

    public List<AppAssociateColumn> getAppIngestionCols() {
        return appIngestionCols;
    }

    public void setAppIngestionCols(List<AppAssociateColumn> appIngestionCols) {
        this.appIngestionCols = appIngestionCols;
    }

    public static class AppAssociateColumn implements Serializable {
        private String ingestionColName;
        private String packageName;
        private String collectColName;

        public String getIngestionColName() {
            return ingestionColName;
        }

        public void setIngestionColName(String ingestionColName) {
            this.ingestionColName = ingestionColName;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public String getCollectColName() {
            return collectColName;
        }

        public void setCollectColName(String collectColName) {
            this.collectColName = collectColName;
        }
    }
}
