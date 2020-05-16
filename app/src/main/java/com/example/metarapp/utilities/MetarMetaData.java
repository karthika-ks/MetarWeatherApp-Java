package com.example.metarapp.utilities;

public class MetarMetaData {
    private String mCode;
    private String mMetadata;

    public MetarMetaData() {

    }

    public MetarMetaData(String code, String metadata) {
        mCode = code;
        mMetadata = metadata;
    }

    public void setCode(String mCode) {
        this.mCode = mCode;
    }

    public void setMetadata(String mMetadata) {
        this.mMetadata = mMetadata;
    }

    public String getCode() {
        return mCode;
    }

    public String getMetadata() {
        return mMetadata;
    }
}
