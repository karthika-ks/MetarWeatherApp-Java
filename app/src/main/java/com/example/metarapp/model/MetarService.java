package com.example.metarapp.model;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.metarapp.utilities.NetworkUtil;

import java.io.IOException;

import static com.example.metarapp.utilities.NetworkUtil.NETWORK_STATUS_AIRPORT_NOT_FOUND;
import static com.example.metarapp.utilities.NetworkUtil.NETWORK_STATUS_NO_INTERNET_CONNECTION;

public class MetarService extends IntentService {

    private static final String TAG = "MetarService";
    public static final String ACTION_NETWORK_RESPONSE = "Network_Response";
    public static final String EXTRA_CODE = "code";
    public static final String EXTRA_DECODED_DATA = "decoded_data";
    public static final String EXTRA_NETWORK_STATUS = "network_status";
    public MetarService() {
        super("Metar Intent Service");
    }


    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.i(TAG, "onHandleIntent: ");
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

    private void sendMetarDetailsFromServer(String code, String decodedData, int networkStatus) {
        Log.i(TAG, "sendMetarDetailsFromServer: Code " + code);
        Intent intent = new Intent();
        intent.setAction(ACTION_NETWORK_RESPONSE);
        intent.putExtra(EXTRA_CODE, code);
        intent.putExtra(EXTRA_DECODED_DATA, decodedData);
        intent.putExtra(EXTRA_NETWORK_STATUS, networkStatus);
        sendBroadcast(intent);
    }

    private Bundle requestMetarDataFromServer(String code) throws IOException {
        Log.i(TAG, "requestMetarDataFromServer: code - " + code);
        return NetworkUtil.readDecodedDataFromUrl(code);
    }
}
