package com.example.metarapp;

import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ScheduledDownloadManager {
    private Runnable longRunning = new Runnable() {
        @Override
        public void run() {
            Log.d(">>>", "longRunning started");
            if (!MetarDataManager.getInstance().getMetarHashMap().isEmpty()) {

                MetarDataManager.getInstance().updateCache();

                while (true) {
                    if (MetarDataManager.getInstance().getDownloadStatus().get() == MetarDataManager.DOWNLOAD_COMPLETE)
                        break;
                }
            }
            Log.d(">>>", "longRunning finished");
        }
    };

    // and here is valuable logic
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    ScheduledFuture<?> schedulerHandler;

    public void startSchedular() {
        Log.d(">>>", "startSchedular");
        schedulerHandler = scheduler.scheduleAtFixedRate(longRunning, 0, 10, TimeUnit.SECONDS);
    }
}
