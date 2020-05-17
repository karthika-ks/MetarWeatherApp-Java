package com.example.metarapp.model;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MetarService extends IntentService {
    private static final String TAG = "MetarService";
    public static final String ACTION_NETWORK_RESPONSE = "Network_Response";
    public static final int NETWORK_STATUS_NO_INTERNET_CONNECTION = 0;
    public static final int NETWORK_STATUS_INTERNET_CONNECTION_OK = 1;
    public static final int NETWORK_STATUS_AIRPORT_NOT_FOUND = 2;
    public static final String EXTRA_CODE = "code";
    public static final String EXTRA_DECODED_DATA = "decoded_data";
    public static final String EXTRA_NETWORK_STATUS = "network_status";
    public MetarService() {
        super("Metar Intent Service");
    }


    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String code = intent.getStringExtra("code");
        // Network calls
        Bundle metarData = null;
        try {
            if (!isNetworkConnected()) {
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
        Intent intent = new Intent();
        intent.setAction(ACTION_NETWORK_RESPONSE);
        intent.putExtra(EXTRA_CODE, code);
        intent.putExtra(EXTRA_DECODED_DATA, decodedData);
        intent.putExtra(EXTRA_NETWORK_STATUS, networkStatus);
        sendBroadcast(intent);
    }

    private Bundle requestMetarDataFromServer(String code) throws IOException {
        Log.i(TAG, "requestMetarDataFromServer: code - " + code);
        Bundle bundle = new Bundle();

        InputStream inputStream;
        try {
            Log.i(TAG, "requestMetarDataFromServer: before getInputStream");
            inputStream = getHttpConnection(code).getInputStream();
            Log.i(TAG, "requestMetarDataFromServer: after getInputStream");

            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        inputStream));
                StringBuilder builder = new StringBuilder();

                String line = "";
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append('\n');
                }
                reader.close();
                Log.i(TAG, "requestMetarDataFromServer: Response - " + builder.toString());
                bundle.putString(EXTRA_CODE, code);
                bundle.putString(EXTRA_DECODED_DATA, builder.toString());
                bundle.putInt(EXTRA_NETWORK_STATUS, NETWORK_STATUS_INTERNET_CONNECTION_OK);
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "requestMetarDataFromServer: FileNotFoundException", e);
            bundle.putString(EXTRA_CODE, code);
            bundle.putString(EXTRA_DECODED_DATA, "");
            bundle.putInt(EXTRA_NETWORK_STATUS, NETWORK_STATUS_AIRPORT_NOT_FOUND);
            return bundle;
        }
        return bundle;
    }

    private HttpURLConnection getHttpConnection(String code) throws IOException {
        URL url = new URL("https://tgftp.nws.noaa.gov/data/observations/metar/decoded/" + code + ".TXT");
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setUseCaches(false);
        httpConn.setDoInput(true); // true if we want to read server's response
        httpConn.setDoOutput(false); // false indicates this is a GET request
        httpConn.setReadTimeout(1000);
        httpConn.setConnectTimeout(1000);
        return httpConn;
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        assert cm != null;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            try {
                URL url = new URL("https://www.google.com/");
                HttpURLConnection urlc = (HttpURLConnection)url.openConnection();
                urlc.setRequestProperty("User-Agent", "test");
                urlc.setRequestProperty("Connection", "close");
                urlc.setConnectTimeout(100); // mTimeout is in seconds
                urlc.connect();
                Log.i(TAG, "isNetworkConnected: Response code : " + urlc.getResponseCode());
                if (urlc.getResponseCode() == 200) {
                    return true;
                } else {
                    return false;
                }
            } catch (IOException e) {
                Log.i("warning", "Error checking internet connection", e);
                return false;
            }
        }

        return false;
    }
}
