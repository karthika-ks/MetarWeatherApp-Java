package com.example.metarapp.model.scheduler;

import android.os.Bundle;
import android.util.Log;

import com.example.metarapp.MetarBrowserApp;
import com.example.metarapp.utilities.MetarData;
import com.example.metarapp.utilities.NetworkUtil;

import java.io.IOException;

import static com.example.metarapp.utilities.Constants.EXTRA_CODE;
import static com.example.metarapp.utilities.Constants.EXTRA_DECODED_DATA;
import static com.example.metarapp.utilities.Constants.EXTRA_METAR_DATA;
import static com.example.metarapp.utilities.Constants.EXTRA_NETWORK_STATUS;
import static com.example.metarapp.utilities.Constants.NETWORK_STATUS_AIRPORT_NOT_FOUND;
import static com.example.metarapp.utilities.Constants.NETWORK_STATUS_NO_INTERNET_CONNECTION;

public class DataDownloadTask implements DataDownloadRunnable.TaskRunnableDownloadMethods {

    private static final String TAG = DataDownloadTask.class.getSimpleName();
    private String mStationCode;
    private Runnable mDownloadRunnable;
    private Bundle mMetarData;
    private Thread mCurrentThread;
    private static DataDownloadManager sDataManager;

    public DataDownloadTask() {
        mDownloadRunnable = new DataDownloadRunnable(this);
    }

    public void initializeDownloadTask(DataDownloadManager dataManager, String stationCode) {
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
        MetarData data = new MetarData();
        data.setCode(code);

        try {
            if (new NetworkUtil().isNetworkConnected(MetarBrowserApp.getInstance().getApplicationContext())) {
                metarData = NetworkUtil.readDecodedDataFromServer(code);
            } else {

                metarData = new Bundle();
                metarData.putParcelable(EXTRA_METAR_DATA, data);
                metarData.putInt(EXTRA_NETWORK_STATUS, NETWORK_STATUS_NO_INTERNET_CONNECTION);
                Log.i(TAG, "downloadMetarData: No internet connection");
            }

        } catch (IOException e) {
            Log.e(TAG, "downloadMetarData: ", e);
            metarData = new Bundle();
            metarData.putParcelable(EXTRA_METAR_DATA, data);
            metarData.putInt(EXTRA_NETWORK_STATUS, NETWORK_STATUS_AIRPORT_NOT_FOUND);
        }
        return metarData;
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

    Runnable getDownloadRunnable() {
        return mDownloadRunnable;
    }
}
