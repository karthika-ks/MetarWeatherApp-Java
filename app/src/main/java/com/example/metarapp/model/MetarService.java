package com.example.metarapp.model;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.metarapp.MetarBrowserApp;
import com.example.metarapp.utilities.NetworkUtil;

import java.io.IOException;

import static com.example.metarapp.utilities.NetworkUtil.NETWORK_STATUS_AIRPORT_NOT_FOUND;
import static com.example.metarapp.utilities.NetworkUtil.NETWORK_STATUS_NO_INTERNET_CONNECTION;

public class MetarService extends IntentService {

    private static final String TAG = "MetarService";
    public static final String ACTION_NETWORK_RESPONSE = "Network_Response";
    public static final String ACTION_LIST_FETCH_RESPONSE = "German_List_Response";
    public static final String SERVICE_ACTION = "SERVICE_ACTION";
    public static final String FETCH_GERMAN_STATION_LIST = "FETCH_GERMAN_STATION_LIST";
    public static final String FETCH_METAR_DATA = "FETCH_METAR_DATA";
    public static final String EXTRA_CODE = "code";
    public static final String EXTRA_DECODED_DATA = "decoded_data";
    public static final String EXTRA_NETWORK_STATUS = "network_status";
    public static final String EXTRA_STATION_LIST = "station_list";
    public MetarService() {
        super("Metar Intent Service");
    }


    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.i(TAG, "onHandleIntent: ");

        if (intent.getStringExtra(SERVICE_ACTION).equals(FETCH_GERMAN_STATION_LIST)) {
//            try {
//                String[] codeList = new NetworkUtil().parseStationNamesFromUrl().toArray(new String[0]);
//                Log.i(TAG, "onHandleIntent: List size = " + codeList.length);
//                sendGermanStationList(codeList);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        } else if (intent.getStringExtra(SERVICE_ACTION).equals(FETCH_METAR_DATA)) {

            String code = intent.getStringExtra("code");
            // Network calls
            Bundle metarData = null;
            try {
                if (!new NetworkUtil().isNetworkConnected(getApplicationContext())) {
                    sendMetarDetailsFromServer(code, "", NETWORK_STATUS_NO_INTERNET_CONNECTION);
                } else {
                    metarData = requestMetarDataFromServer(code);
                    sendMetarDetailsFromServer(code, metarData.getString(EXTRA_DECODED_DATA), metarData.getInt(EXTRA_NETWORK_STATUS));
                }

            } catch (IOException e) {
                sendMetarDetailsFromServer(code, "", NETWORK_STATUS_AIRPORT_NOT_FOUND);
            }
        }
    }

    private void sendMetarDetailsFromServer(String code, String decodedData, int networkStatus) {
        Log.i(TAG, "sendMetarDetailsFromServer: Code " + code);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(MetarBrowserApp.getInstance().getApplicationContext());
        Intent intent = new Intent();
        intent.setAction(ACTION_NETWORK_RESPONSE);
        intent.putExtra(EXTRA_CODE, code);
        intent.putExtra(EXTRA_DECODED_DATA, decodedData);
        intent.putExtra(EXTRA_NETWORK_STATUS, networkStatus);
        localBroadcastManager.sendBroadcast(intent);
    }

    private void sendGermanStationList(String[] stationList) {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(MetarBrowserApp.getInstance().getApplicationContext());
        Intent intent = new Intent();
        intent.setAction(ACTION_LIST_FETCH_RESPONSE);
        intent.putExtra(EXTRA_STATION_LIST, stationList);
        localBroadcastManager.sendBroadcast(intent);
    }

    private Bundle requestMetarDataFromServer(String code) throws IOException {
        Log.i(TAG, "requestMetarDataFromServer: code - " + code);
        return NetworkUtil.readDecodedDataFromUrl(code);
    }
}
