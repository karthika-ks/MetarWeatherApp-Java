package com.example.metarapp.model.scheduler;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.example.metarapp.MetarBrowserApp;
import com.example.metarapp.model.MetarDataManager;
import com.example.metarapp.utilities.MetarData;

import java.util.HashMap;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import static android.content.Context.MODE_PRIVATE;
import static com.example.metarapp.utilities.Constants.CORE_POOL_SIZE;
import static com.example.metarapp.utilities.Constants.DOWNLOAD_COMPLETE;
import static com.example.metarapp.utilities.Constants.DOWNLOAD_STARTED;
import static com.example.metarapp.utilities.Constants.EXTRA_METAR_DATA;
import static com.example.metarapp.utilities.Constants.EXTRA_NETWORK_STATUS;
import static com.example.metarapp.utilities.Constants.KEEP_ALIVE_TIME;
import static com.example.metarapp.utilities.Constants.KEEP_ALIVE_TIME_UNIT;
import static com.example.metarapp.utilities.Constants.MAXIMUM_POOL_SIZE;
import static com.example.metarapp.utilities.Constants.PREF_KEY_UPDATE_STATUS;
import static com.example.metarapp.utilities.Constants.PREF_NAME_GERMAN_LIST;

public class DataDownloadManager {
    private static final String TAG = DataDownloadManager.class.getSimpleName();

    private static DataDownloadManager sInstance;

    private final BlockingQueue<Runnable> mDownloadWorkQueue;
    private final Queue<DataDownloadTask> mDataDownloadTaskWorkQueue;
    private final ThreadPoolExecutor mDownloadThreadPool;

    private int completedThreadCount = 0;
    private Handler mHandler;
    private AtomicInteger mDownloadStatus = new AtomicInteger();

    static {
        sInstance = new DataDownloadManager();
    }

    private DataDownloadManager() {
        mDownloadWorkQueue = new LinkedBlockingDeque<Runnable>(100);
        mDataDownloadTaskWorkQueue = new LinkedBlockingQueue<DataDownloadTask>();
        mDownloadThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mDownloadWorkQueue);

        mDownloadStatus.set(DOWNLOAD_COMPLETE);

        mHandler = new Handler(Looper.getMainLooper()) {

            @Override
            public void handleMessage(Message inputMessage) {

                DataDownloadTask dataDownloadTask = (DataDownloadTask) inputMessage.obj;
                String stationCode = dataDownloadTask.getStationCode();

                switch (inputMessage.what) {

                    case DOWNLOAD_STARTED: {
                        Log.i(TAG, "handleMessage: Download has started for station " + stationCode);
                        if (mDownloadStatus.get() == DOWNLOAD_COMPLETE)
                            mDownloadStatus.set(DOWNLOAD_STARTED);
                    }
                    break;

                    case DOWNLOAD_COMPLETE: {
                        Log.i(TAG, "handleMessage: Download for station " + stationCode + " has completed");
                        recycleTask(dataDownloadTask);
                        // Save data to DB and to list
                        Bundle bundle = dataDownloadTask.getMetarData();
                        MetarData metarData = bundle.getParcelable(EXTRA_METAR_DATA);
                        MetarDataManager.getInstance().saveMetarDataDownloaded(bundle.getInt(EXTRA_NETWORK_STATUS), metarData);

                        completedThreadCount++;

                        if (completedThreadCount == MetarDataManager.getInstance().getMetarHashMap().size())
                            onUpdateCacheCompleted();
                    }
                    break;

                    default:
                        super.handleMessage(inputMessage);
                        break;
                }
            }
        };

        new DownloadSchedulerHandler().startScheduler();
    }

    private void recycleTask(DataDownloadTask downloadTask) {
        // Puts the task object back into the queue for re-use.
        mDataDownloadTaskWorkQueue.offer(downloadTask);
    }

    private void startDownload(String stationCode) {
        Log.i(TAG, "startDownload: Code " + stationCode);

        DataDownloadTask downloadTask = sInstance.mDataDownloadTaskWorkQueue.poll();

        if (null == downloadTask) {
            downloadTask = new DataDownloadTask();
        }

        downloadTask.initializeDownloadTask(DataDownloadManager.sInstance, stationCode);
        sInstance.mDownloadThreadPool.execute(downloadTask.getDownloadRunnable());
    }

    private void onUpdateCacheCompleted() {
        Log.i(TAG, "onUpdateCacheCompleted: ");
        completedThreadCount = 0;
        mDownloadStatus.set(DOWNLOAD_COMPLETE);

        SharedPreferences pref = MetarBrowserApp.getInstance().getApplicationContext().getSharedPreferences(PREF_NAME_GERMAN_LIST, MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(PREF_KEY_UPDATE_STATUS, DOWNLOAD_STARTED);
        editor.apply();
    }

    void updateCache() {
        Set<String> availableStations = MetarDataManager.getInstance().getMetarHashMap().keySet();

        if (!availableStations.isEmpty()) {

            mDownloadStatus.set(DOWNLOAD_STARTED);

            SharedPreferences pref = MetarBrowserApp.getInstance().getApplicationContext().getSharedPreferences(PREF_NAME_GERMAN_LIST, MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putInt(PREF_KEY_UPDATE_STATUS, DOWNLOAD_STARTED);
            editor.apply();

            for (String stationCode : availableStations) {
                startDownload(stationCode);
            }
        }
    }

    AtomicInteger getDownloadStatus() {
        return mDownloadStatus;
    }

    void handleState(DataDownloadTask dataDownloadTask, int state) {
        switch (state) {
            case DOWNLOAD_STARTED:
                break;

            case DOWNLOAD_COMPLETE:
                Message completeMessage = mHandler.obtainMessage(state, dataDownloadTask);
                completeMessage.sendToTarget();
                break;

            default:
                mHandler.obtainMessage(state, dataDownloadTask).sendToTarget();
                break;
        }
    }

    public static DataDownloadManager getInstance() {
        return sInstance;
    }
}
