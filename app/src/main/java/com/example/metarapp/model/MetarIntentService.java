package com.example.metarapp.model;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.metarapp.MetarBrowserApp;
import com.example.metarapp.utilities.NetworkUtil;

import java.io.IOException;
import java.util.Objects;

import static com.example.metarapp.utilities.Constants.ACTION_LIST_FETCH_RESPONSE;
import static com.example.metarapp.utilities.Constants.ACTION_NETWORK_RESPONSE;
import static com.example.metarapp.utilities.Constants.DOWNLOAD_STARTED;
import static com.example.metarapp.utilities.Constants.EXTRA_CODE;
import static com.example.metarapp.utilities.Constants.EXTRA_DECODED_DATA;
import static com.example.metarapp.utilities.Constants.EXTRA_NETWORK_STATUS;
import static com.example.metarapp.utilities.Constants.EXTRA_STATION_LIST;
import static com.example.metarapp.utilities.Constants.FETCH_GERMAN_STATION_LIST;
import static com.example.metarapp.utilities.Constants.FETCH_METAR_DATA;
import static com.example.metarapp.utilities.Constants.NETWORK_STATUS_AIRPORT_NOT_FOUND;
import static com.example.metarapp.utilities.Constants.NETWORK_STATUS_NO_INTERNET_CONNECTION;
import static com.example.metarapp.utilities.Constants.PREF_KEY_DOWNLOAD_STATUS;
import static com.example.metarapp.utilities.Constants.PREF_KEY_IS_AVAILABLE;
import static com.example.metarapp.utilities.Constants.PREF_NAME_GERMAN_LIST;
import static com.example.metarapp.utilities.Constants.SERVICE_ACTION;

public class MetarIntentService extends IntentService {

    private static final String TAG = "MetarService";

    public MetarIntentService() {
        super("Metar Intent Service");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.i(TAG, "onHandleIntent: ");

        assert intent != null;
        if (Objects.equals(intent.getStringExtra(SERVICE_ACTION), FETCH_GERMAN_STATION_LIST)) {
            try {
                String[] codeList = new NetworkUtil().parseStationNamesFromServer().toArray(new String[0]);
                Log.i(TAG, "onHandleIntent: List size = " + codeList.length);
                sendFilteredStationList(codeList);

            } catch (IOException e) {
                Log.e(TAG, "onHandleIntent: ", e);
            }
        } else if (Objects.equals(intent.getStringExtra(SERVICE_ACTION), FETCH_METAR_DATA)) {

            String code = intent.getStringExtra(EXTRA_CODE);

            // Network calls
            Bundle metarData;
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

    private void sendFilteredStationList(String[] stationList) {
        Log.i(TAG, "sendFilteredStationList: stationList.length = " + stationList.length);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(MetarBrowserApp.getInstance().getApplicationContext());
        Intent intent = new Intent();
        intent.setAction(ACTION_LIST_FETCH_RESPONSE);
        intent.putExtra(EXTRA_STATION_LIST, stationList);
        localBroadcastManager.sendBroadcast(intent);
    }

    private Bundle requestMetarDataFromServer(String code) throws IOException {
        Log.i(TAG, "requestMetarDataFromServer: code - " + code);
        return NetworkUtil.readDecodedDataFromServer(code);
    }
}
