package com.example.metarapp;

import android.os.Bundle;
import android.util.Log;

public class MetarDataDownloadRunnable implements Runnable {

    private static final String TAG = MetarDataDownloadRunnable.class.getSimpleName();
    final TaskRunnableDownloadMethods mMetarDataTask;
    static final int DOWNLOAD_STARTED = 0;
    static final int DOWNLOAD_COMPLETED = 1;

    interface TaskRunnableDownloadMethods {
        void setDownloadThread(Thread currentThread);
        void handleDownloadState(int state);
        Bundle downloadMetarData(String code);
        void setMetarData(Bundle bundle);
        Bundle getMetarData();
        String getStationCode();
    }

    MetarDataDownloadRunnable(TaskRunnableDownloadMethods methods) {
        mMetarDataTask = methods;
    }
    @Override
    public void run() {
        Log.i(TAG, "run: ");
        mMetarDataTask.setDownloadThread(Thread.currentThread());
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mMetarDataTask.handleDownloadState(DOWNLOAD_STARTED);
        Bundle metarData = null;
        String code = mMetarDataTask.getStationCode();
        metarData = mMetarDataTask.downloadMetarData(code);
        mMetarDataTask.setMetarData(metarData);
        mMetarDataTask.handleDownloadState(DOWNLOAD_COMPLETED);
    }
}
