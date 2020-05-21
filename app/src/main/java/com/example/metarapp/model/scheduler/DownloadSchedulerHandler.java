package com.example.metarapp.model.scheduler;

import android.util.Log;

import com.example.metarapp.model.MetarDataManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.example.metarapp.utilities.Constants.DOWNLOAD_COMPLETE;

public class DownloadSchedulerHandler {
    private static final String TAG = DownloadSchedulerHandler.class.getSimpleName();

    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Update started");
            if (!MetarDataManager.getInstance().getMetarHashMap().isEmpty()) {

                DataDownloadManager.getInstance().updateCache();

                while (true) {
                    if (DataDownloadManager.getInstance().getDownloadStatus().get() == DOWNLOAD_COMPLETE)
                        break;
                }
            }
            Log.d(TAG, "Update finished");
        }
    };

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    ScheduledFuture<?> schedulerHandler;

    public void startScheduler() {
        Log.d(TAG, "startScheduler");
        schedulerHandler = scheduler.scheduleAtFixedRate(updateRunnable, 0, 10, TimeUnit.SECONDS);
    }
}
