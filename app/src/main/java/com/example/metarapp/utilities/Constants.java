package com.example.metarapp.utilities;

import java.util.concurrent.TimeUnit;

public final class Constants {
    public static final String ACTION_NETWORK_RESPONSE = "com.action.response.network";
    public static final String ACTION_LIST_FETCH_RESPONSE = "com.action.response.list_fetch";

    public static final String SERVICE_ACTION = "SERVICE_ACTION";
    public static final String FETCH_GERMAN_STATION_LIST = "FETCH_GERMAN_STATION_LIST";
    public static final String FETCH_METAR_DATA = "FETCH_METAR_DATA";
    public static final String CHECK_INTERNET_AVAILABILITY = "CHECK_INTERNET_AVAILABILITY";

    public static final String EXTRA_CODE = "code";
    public static final String EXTRA_DECODED_DATA = "decoded_data";
    public static final String EXTRA_NETWORK_STATUS = "network_status";
    public static final String EXTRA_STATION_NAME = "station_name";
    public static final String EXTRA_RAW_DATA = "raw_data";
    public static final String EXTRA_STATION_LIST = "station_list";
    public static final String EXTRA_METAR_DATA = "metar_data";

    public static final int KEEP_ALIVE_TIME = 10;
    public static final TimeUnit KEEP_ALIVE_TIME_UNIT =  TimeUnit.SECONDS;
    public static final int CORE_POOL_SIZE = 8;
    public static final int MAXIMUM_POOL_SIZE = 8;
    public static final int NETWORK_RESPONSE_WAITING_TIME = 250; // Time unit in milliseconds
    public static final int POST_NETWORK_SERVICE_TIME = 5; // Time unit in milliseconds

    public static final int DOWNLOAD_STARTED = 0;
    public static final int DOWNLOAD_COMPLETE = 1;

    public static final String PREF_NAME_GERMAN_LIST = "GERMAN_LIST_STATUS";
    public static final String PREF_KEY_IS_FILTERED_LIST_AVAILABLE = "IS_FILTERED_LIST_AVAILABLE";
    public static final String PREF_KEY_DOWNLOAD_STATUS = "DOWNLOAD_STATUS";
    public static final String PREF_KEY_UPDATE_STATUS = "UPDATE_STATUS";
    public static final String PREF_KEY_HAS_INTERNET_CONNECTIVITY = "HAS_INTERNET_CONNECTIVITY";

    public static final int NETWORK_STATUS_NO_INTERNET_CONNECTION = 0;
    public static final int NETWORK_STATUS_INTERNET_CONNECTION_OK = 1;
    public static final int NETWORK_STATUS_AIRPORT_NOT_FOUND = 2;

    public static final String FILTER_STRING_GERMAN = "ED";

    public static final String METAR_LABEL_RAW_DATA = "ob:";

    public static final String UNKNOWN_STATION = "Station Not Available";
    public static final String SELECT_ITEM = "Select an item...";
}
