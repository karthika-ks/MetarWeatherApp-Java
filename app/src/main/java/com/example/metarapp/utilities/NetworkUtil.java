package com.example.metarapp.utilities;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static com.example.metarapp.model.MetarService.EXTRA_CODE;
import static com.example.metarapp.model.MetarService.EXTRA_DECODED_DATA;
import static com.example.metarapp.model.MetarService.EXTRA_NETWORK_STATUS;

public class NetworkUtil {
    private static final String TAG = NetworkUtil.class.getSimpleName();

    public static final int NETWORK_STATUS_NO_INTERNET_CONNECTION = 0;
    public static final int NETWORK_STATUS_INTERNET_CONNECTION_OK = 1;
    public static final int NETWORK_STATUS_AIRPORT_NOT_FOUND = 2;

    private static HttpURLConnection getHttpConnection(URL url) throws IOException {
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setUseCaches(false);
        httpConn.setDoInput(true); // true if we want to read server's response
        httpConn.setDoOutput(false); // false indicates this is a GET request
        httpConn.setReadTimeout(60 * 1000);
        httpConn.setConnectTimeout(60 * 1000);
        return httpConn;
    }

    private static URL getDecodedDataUrl(String code) throws MalformedURLException {
        return new URL("https://tgftp.nws.noaa.gov/data/observations/metar/decoded/" + code + ".TXT");
    }

    public boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
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

    public static Bundle readDecodedDataFromUrl(String code) throws IOException {
        Log.i(TAG, "readDecodedDataFromUrl: Code " + code);
        URL url = getDecodedDataUrl(code);
        StringBuilder builder = new StringBuilder();
        Bundle bundle = new Bundle();

        try {
            InputStream inputStream = NetworkUtil.getHttpConnection(url).getInputStream();
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                String line = "";
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append('\n');
                }
                reader.close();

                Log.i(TAG, "readDecodedDataFromUrl: Code " + code + ", Response - " + builder.toString());
                bundle.putString(EXTRA_CODE, code);
                bundle.putString(EXTRA_DECODED_DATA, builder.toString());
                bundle.putInt(EXTRA_NETWORK_STATUS, NETWORK_STATUS_INTERNET_CONNECTION_OK);
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "readDecodedDataFromUrl: FileNotFoundException", e);
            bundle.putString(EXTRA_CODE, code);
            bundle.putString(EXTRA_DECODED_DATA, "");
            bundle.putInt(EXTRA_NETWORK_STATUS, NETWORK_STATUS_AIRPORT_NOT_FOUND);
            return bundle;
        }
        return bundle;
    }
}
