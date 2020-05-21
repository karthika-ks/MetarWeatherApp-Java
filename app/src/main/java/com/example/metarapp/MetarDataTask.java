package com.example.metarapp;

import android.os.Bundle;
import android.util.Log;

import com.example.metarapp.utilities.NetworkUtil;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.example.metarapp.model.MetarService.EXTRA_CODE;
import static com.example.metarapp.model.MetarService.EXTRA_DECODED_DATA;
import static com.example.metarapp.model.MetarService.EXTRA_NETWORK_STATUS;
import static com.example.metarapp.utilities.NetworkUtil.NETWORK_STATUS_AIRPORT_NOT_FOUND;
import static com.example.metarapp.utilities.NetworkUtil.NETWORK_STATUS_INTERNET_CONNECTION_OK;
import static com.example.metarapp.utilities.NetworkUtil.NETWORK_STATUS_NO_INTERNET_CONNECTION;

public class MetarDataTask implements MetarDataDownloadRunnable.TaskRunnableDownloadMethods {

    private static final String TAG = MetarDataTask.class.getSimpleName();
    private String mStationCode;
    Thread mThreadThis;
    private Runnable mDownloadRunnable;
    Bundle mMetarData;
    private Thread mCurrentThread;
    private static MetarDataManager sDataManager;

    MetarDataTask() {
        mDownloadRunnable = new MetarDataDownloadRunnable(this);
    }

    public void initializeDownloadTask(MetarDataManager dataManager, String stationCode) {
        sDataManager = dataManager;
        mStationCode = stationCode;
    }

    public void setCurrentThread(Thread thread) {
        synchronized(sDataManager) {
            mCurrentThread = thread;
        }
    }

    @Override
    public void setDownloadThread(Thread currentThread) {
        setCurrentThread(currentThread);
    }

    @Override
    public void handleDownloadState(int state) {
        Log.i(TAG, "handleDownloadState: " + state);
        sDataManager.handleState(this, state);
    }

    @Override
    public Bundle downloadMetarData(String code) {
        Log.i(TAG, "downloadMetarData: code " + code);
        Bundle metarData;
        try {
            if (new NetworkUtil().isNetworkConnected(MetarBrowserApp.getInstance().getApplicationContext())) {
                metarData = readDecodedDataFromUrl(code);
            } else {
                metarData = new Bundle();
                metarData.putString(EXTRA_CODE, code);
                metarData.putString(EXTRA_DECODED_DATA, "");
                metarData.putInt(EXTRA_NETWORK_STATUS, NETWORK_STATUS_NO_INTERNET_CONNECTION);
                Log.i(TAG, "downloadMetarData: No internet connection");
            }

        } catch (IOException e) {
            Log.e(TAG, "downloadMetarData: ", e);
            metarData = new Bundle();
            metarData.putString(EXTRA_CODE, code);
            metarData.putString(EXTRA_DECODED_DATA, "");
            metarData.putInt(EXTRA_NETWORK_STATUS, NETWORK_STATUS_AIRPORT_NOT_FOUND);
        }
        return metarData;
    }

    private static Bundle readDecodedDataFromUrl(String code) throws IOException {
        Log.i(TAG, "readDecodedDataFromUrl: Code " + code);
        URL url = new URL("https://tgftp.nws.noaa.gov/data/observations/metar/decoded/" + code + ".TXT");
        StringBuilder builder = new StringBuilder();
        Bundle bundle = new Bundle();

        try {
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setUseCaches(false);
            httpConn.setDoInput(true); // true if we want to read server's response
            httpConn.setDoOutput(false); // false indicates this is a GET request
            httpConn.setReadTimeout(60*1000);
            httpConn.setConnectTimeout(60*1000);

            InputStream inputStream = httpConn.getInputStream();

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

    @Override
    public void setMetarData(Bundle bundle) {
        mMetarData = bundle;
    }

    @Override
    public Bundle getMetarData() {
        return mMetarData;
    }

    @Override
    public String getStationCode() {
        return mStationCode;
    }

    public Thread getCurrentThread() {
        synchronized(sDataManager) {
            return mCurrentThread;
        }
    }

    public Runnable getDownloadRunnable() {
        return mDownloadRunnable;
    }
}
