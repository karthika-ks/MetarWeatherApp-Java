package com.example.metarapp.utilities;

import android.os.Parcel;
import android.os.Parcelable;

public class MetarData implements Parcelable {
    private String code;
    private String stationName;
    private String lastUpdatedTime;
    private String decodedData;
    private String rawData;

    protected MetarData(Parcel in) {
        code = in.readString();
        stationName = in.readString();
        lastUpdatedTime = in.readString();
        decodedData = in.readString();
        rawData = in.readString();
    }

    public static final Creator<MetarData> CREATOR = new Creator<MetarData>() {
        @Override
        public MetarData createFromParcel(Parcel in) {
            return new MetarData(in);
        }

        @Override
        public MetarData[] newArray(int size) {
            return new MetarData[size];
        }
    };

    public void setCode(String code) {
        if (code != null)
            this.code = code;
    }

    public String getCode() {
        return code;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        if (stationName != null) {
            this.stationName = stationName;
        }
    }

    public String getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    public void setLastUpdatedTime(String lastUpdatedTime) {
        if (lastUpdatedTime != null)
            this.lastUpdatedTime = lastUpdatedTime;
    }

    public void setDecodedData(String decodedData) {
        if (decodedData != null)
        this.decodedData = decodedData;
    }

    public String getDecodedData() {
        return decodedData;
    }

    public void setRawData(String rawData) {
        if (rawData != null)
        this.rawData = rawData;
    }

    public String getRawData() {
        return rawData;
    }

    public MetarData() {
        code = "";
        stationName = "";
        decodedData = "";
        rawData = "";
        lastUpdatedTime = "";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(code);
        dest.writeString(stationName);
        dest.writeString(decodedData);
        dest.writeString(rawData);
        dest.writeString(lastUpdatedTime);
    }
}
