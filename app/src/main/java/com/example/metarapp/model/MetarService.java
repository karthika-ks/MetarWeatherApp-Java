package com.example.metarapp.model;

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MetarService extends IntentService {
    private static final String TAG = "MetarService";
    private ConnectivityManager connectivityManager;
    public static final String ACTION_NETWORK_RESPONSE = "Network_Response";
    public MetarService() {
        super("Metar Intent Service");
    }


    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String code = intent.getStringExtra("code");
        // Network calls
        String decodedData = "";
        try {
            decodedData = requestMetarDataFromServer(code);
        } catch (IOException e) {
            e.printStackTrace();
            sendMetarDetailsFromServer(code, "Airport not found");
        }
        sendMetarDetailsFromServer(code, decodedData);
    }

    private void sendMetarDetailsFromServer(String code, String decodedData) {
        Intent intent = new Intent();
        intent.setAction(ACTION_NETWORK_RESPONSE);
        intent.putExtra("code", code);
        intent.putExtra("decoded_data", decodedData);
        sendBroadcast(intent);
    }

    private String requestMetarDataFromServer(String code) throws IOException {
        Log.i(TAG, "requestMetarDataFromServer: code - " + code);

        if (!isNetworkConnected()) {
            return "No network connection";
        }
        URL url = new URL("https://tgftp.nws.noaa.gov/data/observations/metar/decoded/" + code + ".TXT");
//        URL url = new URL("https://tgftp.nws.noaa.gov/data/observations/metar/decoded/");
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setUseCaches(false);

        httpConn.setDoInput(true); // true if we want to read server's response
        httpConn.setDoOutput(false); // false indicates this is a GET request

        InputStream inputStream = null;
        if (httpConn != null) {
            try {
                inputStream = httpConn.getInputStream();
            } catch (FileNotFoundException e) {
                Log.e(TAG, "requestMetarDataFromServer: FileNotFoundException", e);
                return "Airport not found";
            }

        } else {
            throw new IOException("Connection is not established.");
        }

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
            return builder.toString();
        }
        return "";
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
                urlc.setConnectTimeout(1000); // mTimeout is in seconds
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
