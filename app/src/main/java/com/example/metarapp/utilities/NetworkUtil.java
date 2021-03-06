package com.example.metarapp.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;

import com.example.metarapp.MetarBrowserApp;

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

import static android.content.Context.MODE_PRIVATE;
import static com.example.metarapp.utilities.Constants.EXTRA_METAR_DATA;
import static com.example.metarapp.utilities.Constants.EXTRA_NETWORK_STATUS;
import static com.example.metarapp.utilities.Constants.FILTER_STRING_GERMAN;
import static com.example.metarapp.utilities.Constants.METAR_LABEL_RAW_DATA;
import static com.example.metarapp.utilities.Constants.NETWORK_STATUS_AIRPORT_NOT_FOUND;
import static com.example.metarapp.utilities.Constants.NETWORK_STATUS_INTERNET_CONNECTION_OK;
import static com.example.metarapp.utilities.Constants.PREF_KEY_HAS_INTERNET_CONNECTIVITY;
import static com.example.metarapp.utilities.Constants.PREF_NAME_GERMAN_LIST;
import static com.example.metarapp.utilities.Constants.UNKNOWN_STATION;

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

                if (urlc.getResponseCode() == 200 || urlc.getResponseCode() == 429) {
                    setInternetStatus(true);
                    return true;
                } else {
                    setInternetStatus(false);
                    return false;
                }

            } catch (IOException e) {
                setInternetStatus(false);
                return false;
            }
        }

        setInternetStatus(false);
        return false;
    }

    private void setInternetStatus(boolean internetConnectivity) {

        SharedPreferences pref = MetarBrowserApp.getInstance().getApplicationContext().getSharedPreferences(PREF_NAME_GERMAN_LIST, MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(PREF_KEY_HAS_INTERNET_CONNECTIVITY, internetConnectivity);
        editor.apply();
    }

    public static Bundle readDecodedDataFromServer(String code) throws IOException {

        URL url = getDecodedDataUrl(code);
        Bundle bundle = new Bundle();
        MetarData data = new MetarData();
        data.setCode(code);

        try {

            InputStream inputStream = NetworkUtil.getHttpConnection(url).getInputStream();

            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                data = getMetarData(reader, code);

                bundle.putParcelable(EXTRA_METAR_DATA, data);
                bundle.putInt(EXTRA_NETWORK_STATUS, NETWORK_STATUS_INTERNET_CONNECTION_OK);
            }
        } catch (FileNotFoundException e) {
            bundle.putParcelable(EXTRA_METAR_DATA, data);
            bundle.putInt(EXTRA_NETWORK_STATUS, NETWORK_STATUS_AIRPORT_NOT_FOUND);
            return bundle;
        }

        return bundle;
    }

    private static MetarData getMetarData(BufferedReader reader, String code) throws IOException {
        MetarData data = new MetarData();
        String line = "";
        StringBuilder builder = new StringBuilder();

        data.setCode(code);

        reader.mark(1000);

        if ((line = reader.readLine()) != null) {
            builder.append(line).append('\n');
            if (line.indexOf('(') != -1) {
                String stationName = line.substring(0, line.indexOf('('));
                data.setStationName(stationName);
            } else {
                data.setStationName(UNKNOWN_STATION);
            }
        }

        if ((line = reader.readLine()) != null) {
            builder.append(line).append('\n');
            data.setLastUpdatedTime(line);
        }

        while ((line = reader.readLine()) != null) {
            builder.append(line).append('\n');

            if (line.startsWith(METAR_LABEL_RAW_DATA)) {
                String rawData = line.substring(METAR_LABEL_RAW_DATA.length() + 1);
                data.setRawData(rawData);
            }
        }
        reader.close();

        data.setDecodedData(builder.toString());
        return data;
    }

    public List<String> parseStationNamesFromServer() throws IOException {

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
                        stationCodeList.add(code);
                    }
                    }
                }
            }
        return stationCodeList;
    }
}
