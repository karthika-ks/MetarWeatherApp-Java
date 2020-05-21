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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.metarapp.utilities.Constants.EXTRA_CODE;
import static com.example.metarapp.utilities.Constants.EXTRA_DECODED_DATA;
import static com.example.metarapp.utilities.Constants.EXTRA_NETWORK_STATUS;
import static com.example.metarapp.utilities.Constants.FILTER_STRING_GERMAN;
import static com.example.metarapp.utilities.Constants.NETWORK_STATUS_AIRPORT_NOT_FOUND;
import static com.example.metarapp.utilities.Constants.NETWORK_STATUS_INTERNET_CONNECTION_OK;

public class NetworkUtil {

    private static final String TAG = NetworkUtil.class.getSimpleName();

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

    private static URL getStationListUrl() throws MalformedURLException {
        return new URL("https://tgftp.nws.noaa.gov/data/observations/metar/decoded/");
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
                urlc.setConnectTimeout(60 * 1000); // mTimeout is in seconds
                urlc.connect();

                Log.i(TAG, "isNetworkConnected: Response code : " + urlc.getResponseCode());

                if (urlc.getResponseCode() == 200 || urlc.getResponseCode() == 429) {
                    return true;
                } else {
                    return false;
                }

            } catch (IOException e) {
//                Log.e(TAG, "Error checking internet connection", e);
                return false;
            }
        }

        return false;
    }

    public static Bundle readDecodedDataFromServer(String code) throws IOException {
        Log.i(TAG, "readDecodedDataFromServer: Code " + code);

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

                Log.i(TAG, "readDecodedDataFromServer: Code " + code + ", Response - " + builder.toString());
                bundle.putString(EXTRA_CODE, code);
                bundle.putString(EXTRA_DECODED_DATA, builder.toString());
                bundle.putInt(EXTRA_NETWORK_STATUS, NETWORK_STATUS_INTERNET_CONNECTION_OK);
            }
        } catch (FileNotFoundException e) {
//            Log.e(TAG, "readDecodedDataFromServer: FileNotFoundException", e);
            bundle.putString(EXTRA_CODE, code);
            bundle.putString(EXTRA_DECODED_DATA, "");
            bundle.putInt(EXTRA_NETWORK_STATUS, NETWORK_STATUS_AIRPORT_NOT_FOUND);
            return bundle;
        }

        return bundle;
    }

    public List<String> parseStationNamesFromServer() throws IOException{
        Log.i(TAG, "parseStationNamesFromServer: ");

        URL url = getStationListUrl();
        List<String> htmlList = new ArrayList<>();
        List<String> stationCodeList = new ArrayList<>();

        InputStream inputStream = NetworkUtil.getHttpConnection(url).getInputStream();

        if (inputStream != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line = "";
            while ((line = reader.readLine()) != null) {
                htmlList.add(line);
            }
            reader.close();
        }

        //Read station name from array
        for (String station : htmlList) {

            Pattern p = Pattern.compile(".*\"(.*)[.].*");
            Matcher m = p.matcher(station);

            if (m.find()) {
                for (int i = 1; i <= m.groupCount(); i++) {

                    String code = m.group(i).replaceAll(">", "");

                    if (code.startsWith(FILTER_STRING_GERMAN)) {
                        Log.i(TAG, "parseStationNamesFromServer: <<<<<<<<<<<<<<<<<< " + code);
                        stationCodeList.add(code);
                    }
                    }
                }
            }
        return stationCodeList;
    }
}
