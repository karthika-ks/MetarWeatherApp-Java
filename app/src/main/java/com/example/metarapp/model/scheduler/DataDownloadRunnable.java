package com.example.metarapp.model.scheduler;

import android.os.Bundle;
import android.util.Log;

import static com.example.metarapp.utilities.Constants.DOWNLOAD_COMPLETE;
import static com.example.metarapp.utilities.Constants.DOWNLOAD_STARTED;

public class DataDownloadRunnable implements Runnable {

    private static final String TAG = DataDownloadRunnable.class.getSimpleName();
    final TaskRunnableDownloadMethods mDataDownloadTask;

    interface TaskRunnableDownloadMethods {
        void setDownloadThread(Thread currentThread);
        void handleDownloadState(int state);
        Bundle downloadMetarData(String code);
        void setMetarData(Bundle bundle);
        Bundle getMetarData();
        String getStationCode();
    }

    DataDownloadRunnable(TaskRunnableDownloadMethods methods) {
        mDataDownloadTask = methods;
    }
    @Override
    public void run() {
        Log.i(TAG, "run: ");

        mDataDownloadTask.setDownloadThread(Thread.currentThread());
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        mDataDownloadTask.handleDownloadState(DOWNLOAD_STARTED);

        Bundle metarData;
        String code = mDataDownloadTask.getStationCode();
        metarData = mDataDownloadTask.downloadMetarData(code);
        mDataDownloadTask.setMetarData(metarData);

        mDataDownloadTask.handleDownloadState(DOWNLOAD_COMPLETE);
    }
}
