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
            if (!MetarDataManager.getInstance().getMetarHashMap().isEmpty()) {

                DataDownloadManager.getInstance().updateCache();

                while (true) {
                    if (DataDownloadManager.getInstance().getDownloadStatus().get() == DOWNLOAD_COMPLETE)
                        break;
                }
            }
        }
    };

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    ScheduledFuture<?> schedulerHandler;

    public void startScheduler() {
        schedulerHandler = scheduler.scheduleAtFixedRate(updateRunnable, 5, 10, TimeUnit.SECONDS);
    }
}
