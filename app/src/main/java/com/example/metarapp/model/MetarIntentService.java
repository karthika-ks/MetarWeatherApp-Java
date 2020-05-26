package com.example.metarapp.model;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.metarapp.MetarBrowserApp;
import com.example.metarapp.utilities.MetarData;
import com.example.metarapp.utilities.NetworkUtil;

import java.io.IOException;
import java.util.Objects;

import static com.example.metarapp.utilities.Constants.ACTION_LIST_FETCH_RESPONSE;
import static com.example.metarapp.utilities.Constants.ACTION_NETWORK_RESPONSE;
import static com.example.metarapp.utilities.Constants.CHECK_INTERNET_AVAILABILITY;
import static com.example.metarapp.utilities.Constants.EXTRA_CODE;
import static com.example.metarapp.utilities.Constants.EXTRA_METAR_DATA;
import static com.example.metarapp.utilities.Constants.EXTRA_NETWORK_STATUS;
import static com.example.metarapp.utilities.Constants.EXTRA_STATION_LIST;
import static com.example.metarapp.utilities.Constants.FETCH_GERMAN_STATION_LIST;
import static com.example.metarapp.utilities.Constants.FETCH_METAR_DATA;
import static com.example.metarapp.utilities.Constants.NETWORK_STATUS_AIRPORT_NOT_FOUND;
import static com.example.metarapp.utilities.Constants.NETWORK_STATUS_INTERNET_CONNECTION_OK;
import static com.example.metarapp.utilities.Constants.NETWORK_STATUS_NO_INTERNET_CONNECTION;
import static com.example.metarapp.utilities.Constants.SERVICE_ACTION;

public class MetarIntentService extends IntentService {

    private static final String TAG = MetarIntentService.class.getSimpleName();

    public MetarIntentService() {
        super("Metar Intent Service");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        assert intent != null;
        if (Objects.equals(intent.getStringExtra(SERVICE_ACTION), FETCH_GERMAN_STATION_LIST)) {
            boolean internetConnectivity = new NetworkUtil().isNetworkConnected(getApplicationContext());

            try {
                if (internetConnectivity) {
                    String[] codeList = new NetworkUtil().parseStationNamesFromServer().toArray(new String[0]);

                    for (String station : codeList) {
                        MetarData metarData = new MetarData();
                        metarData.setCode(station);
                        MetarDataManager.getInstance().saveMetarDataDownloaded( NETWORK_STATUS_INTERNET_CONNECTION_OK, metarData);
                    }

                    sendFilteredStationList(NETWORK_STATUS_INTERNET_CONNECTION_OK,codeList);

                } else {
                    sendFilteredStationList(NETWORK_STATUS_NO_INTERNET_CONNECTION, new String[0]);
                }

            } catch (IOException e) {
                Log.e(TAG, "onHandleIntent: ", e);
            }
        } else if (Objects.equals(intent.getStringExtra(SERVICE_ACTION), FETCH_METAR_DATA)) {

            String code = intent.getStringExtra(EXTRA_CODE);

            // Network calls
            Bundle metarData;
            MetarData data = new MetarData();
            data.setCode(code);

            try {

                if (!new NetworkUtil().isNetworkConnected(getApplicationContext())) {
                    sendMetarDetailsFromServer(NETWORK_STATUS_NO_INTERNET_CONNECTION, data);
                } else {
                    metarData = requestMetarDataFromServer(code);
                    sendMetarDetailsFromServer(metarData.getInt(EXTRA_NETWORK_STATUS), (MetarData) metarData.getParcelable(EXTRA_METAR_DATA));
                }

            } catch (IOException e) {
                sendMetarDetailsFromServer(NETWORK_STATUS_AIRPORT_NOT_FOUND, data);
            }
        } else if (Objects.equals(intent.getStringExtra(SERVICE_ACTION), CHECK_INTERNET_AVAILABILITY)) {
            boolean internetConnectivity = new NetworkUtil().isNetworkConnected(getApplicationContext());
            Log.i(TAG, "onHandleIntent: internetConnectivity " + internetConnectivity);
        }
    }

    private void sendMetarDetailsFromServer(int networkStatus, MetarData metarData) {

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(MetarBrowserApp.getInstance().getApplicationContext());
        Intent intent = new Intent();
        intent.setAction(ACTION_NETWORK_RESPONSE);
        intent.putExtra(EXTRA_NETWORK_STATUS, networkStatus);
        intent.putExtra(EXTRA_METAR_DATA, metarData);
        localBroadcastManager.sendBroadcast(intent);
    }

    private void sendFilteredStationList(int networkStatus, String[] stationList) {

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(MetarBrowserApp.getInstance().getApplicationContext());
        Intent intent = new Intent();
        intent.setAction(ACTION_LIST_FETCH_RESPONSE);
        intent.putExtra(EXTRA_NETWORK_STATUS, networkStatus);
        intent.putExtra(EXTRA_STATION_LIST, stationList);
        localBroadcastManager.sendBroadcast(intent);
    }

    private Bundle requestMetarDataFromServer(String code) throws IOException {
        return NetworkUtil.readDecodedDataFromServer(code);
    }
}
