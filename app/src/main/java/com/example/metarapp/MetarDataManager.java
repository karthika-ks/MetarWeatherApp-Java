package com.example.metarapp;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.metarapp.model.MetarService;
import com.example.metarapp.model.contentprovider.MetarContentProvider;
import com.example.metarapp.model.contentprovider.MetarHandler;

import java.util.HashMap;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static android.content.Context.MODE_PRIVATE;
import static com.example.metarapp.model.MetarService.EXTRA_CODE;
import static com.example.metarapp.model.MetarService.EXTRA_DECODED_DATA;
import static com.example.metarapp.model.MetarService.EXTRA_NETWORK_STATUS;
import static com.example.metarapp.model.MetarService.EXTRA_STATION_LIST;
import static com.example.metarapp.utilities.NetworkUtil.NETWORK_STATUS_INTERNET_CONNECTION_OK;

public class MetarDataManager {
    public static final int DOWNLOAD_STARTED = 0;
    public static final int DOWNLOAD_COMPLETE = 1;
    private static final String TAG = MetarDataManager.class.getSimpleName();
    private static final int KEEP_ALIVE_TIME = 10;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT;
    private static final int CORE_POOL_SIZE = 8;
    private static final int MAXIMUM_POOL_SIZE = 8;

    private final BlockingQueue<Runnable> mDownloadWorkQueue;
    private final Queue<MetarDataTask> mMetarDataTaskWorkQueue;
    private final ThreadPoolExecutor mDownloadThreadPool;
    private Handler mHandler;
    private static MetarDataManager sInstance = null;
    private HashMap<String, String> mMetarHashMap;
    private String[] mStationList;
    private int completedThreadCount = 0;
    AtomicInteger mDownloadStatus = new AtomicInteger();

    static {

        KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
        sInstance = new MetarDataManager();
    }

    private MetarDataManager() {
        mDownloadWorkQueue = new LinkedBlockingDeque<Runnable>(100);
        mMetarDataTaskWorkQueue = new LinkedBlockingQueue<MetarDataTask>();
        mMetarHashMap = new HashMap<>();
        mDownloadThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mDownloadWorkQueue);
        mDownloadStatus.set(DOWNLOAD_COMPLETE);
        registerNetworkReceiver();

        new MetarHandler(this, MetarBrowserApp.getInstance().getApplicationContext().getContentResolver())
                .startQuery(0
                        , null
                        , MetarContentProvider.CONTENT_URI
                        , null
                        , null
                        , null
                        , null);

        startMetarService();

        mHandler = new Handler(Looper.getMainLooper()) {

            @Override
            public void handleMessage(Message inputMessage) {

                MetarDataTask metarDataTask = (MetarDataTask) inputMessage.obj;
                String stationCode = metarDataTask.getStationCode();

                switch (inputMessage.what) {
                    case DOWNLOAD_STARTED:
                        Log.i(TAG, "handleMessage: Download has started for station " + stationCode);
                        if (mDownloadStatus.get() == DOWNLOAD_COMPLETE)
                            mDownloadStatus.set(DOWNLOAD_STARTED);
                        break;
                    case DOWNLOAD_COMPLETE:
                        Log.i(TAG, "handleMessage: Download for station " + stationCode + " has completed");
                        recycleTask(metarDataTask);
                        // Save data to DB and to list
                        saveMetarData(metarDataTask.getMetarData());
                        completedThreadCount++;
                        if (completedThreadCount == mMetarHashMap.size()) {
                            onUpdateCacheCompleted();
                        }
                        break;
                    default:
                        super.handleMessage(inputMessage);
                }
            }
        };
        new ScheduledDownloadManager().startSchedular();
    }

    private void registerNetworkReceiver() {
        NetworkReceiver networkReceiver = new NetworkReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MetarService.ACTION_LIST_FETCH_RESPONSE);
        LocalBroadcastManager.getInstance(MetarBrowserApp.getInstance().getApplicationContext()).registerReceiver(networkReceiver, filter);
    }

    public void startMetarService(){
        SharedPreferences pref =MetarBrowserApp.getInstance().getApplicationContext().getSharedPreferences("GERMAN_LIST_STATUS", MODE_PRIVATE);
        if (!pref.getBoolean("IS_AVAILABLE", false)) {
            Intent cbIntent = new Intent();
            cbIntent.setClass(MetarBrowserApp.getInstance().getApplicationContext(), MetarService.class);
            cbIntent.putExtra(MetarService.SERVICE_ACTION, MetarService.FETCH_GERMAN_STATION_LIST);
            MetarBrowserApp.getInstance().getApplicationContext().startService(cbIntent);
        } else {
            // Fetch code from Database
            Log.i(TAG, "startMetarService: Code is already available");
            new MetarHandler(this, MetarBrowserApp.getInstance().getApplicationContext().getContentResolver())
                    .startQuery(1
                            , null
                            , MetarContentProvider.CONTENT_URI
                            , new String[]{MetarContentProvider.COLUMN_CODE}
                            , MetarContentProvider.COLUMN_CODE + " like ?"
                            , new String[]{"ED%"}
                            , MetarContentProvider.COLUMN_CODE + " ASC");
        }
    }

    private void savePredefinedStationsToMap(String[] germanStations) {
        mStationList = germanStations;
        for (String station : germanStations) {
            mMetarHashMap.put(station, "");
        }

        SharedPreferences pref =MetarBrowserApp.getInstance().getApplicationContext().getSharedPreferences("GERMAN_LIST_STATUS", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("IS_AVAILABLE", true);
        editor.putInt("DOWNLOAD_STATUS", 1); // 0 - DOWNLOADING, 1 - DOWNLOADED
        editor.apply();
    }

    public String[] getStationList() {
        return mStationList;
    }

    private void onUpdateCacheCompleted() {
        Log.i(TAG, "onUpdateCacheCompleted: ");
        completedThreadCount = 0;
        mDownloadStatus.set(DOWNLOAD_COMPLETE);
    }

    public static MetarDataManager getInstance() {
        return sInstance;
    }

    public AtomicInteger getDownloadStatus() {
        return mDownloadStatus;
    }

    public void handleState(MetarDataTask metarDataTask, int state) {
        switch (state) {
            case DOWNLOAD_STARTED:
                break;
            case DOWNLOAD_COMPLETE:
                Message completeMessage = mHandler.obtainMessage(state, metarDataTask);
                completeMessage.sendToTarget();
                break;
            default:
                mHandler.obtainMessage(state, metarDataTask).sendToTarget();
                break;
        }
    }

    public static void cancelAll() {

        MetarDataTask[] taskArray = new MetarDataTask[sInstance.mDownloadWorkQueue.size()];
        sInstance.mDownloadWorkQueue.toArray(taskArray);
        int taskArraylen = taskArray.length;

        synchronized (sInstance) {
            for (int taskArrayIndex = 0; taskArrayIndex < taskArraylen; taskArrayIndex++) {

                Thread thread = taskArray[taskArrayIndex].mThreadThis;
                if (null != thread) {
                    thread.interrupt();
                }
            }
        }
    }

    static public void removeDownload(MetarDataTask downloaderTask, String code) {

        if (downloaderTask != null && downloaderTask.getStationCode().equals(code)) {

            synchronized (sInstance) {
                Thread thread = downloaderTask.getCurrentThread();

                if (null != thread)
                    thread.interrupt();
            }

            sInstance.mDownloadThreadPool.remove(downloaderTask.getDownloadRunnable());
        }
    }

    private MetarDataTask startDownload(String stationCode) {
    Log.i(TAG, "startDownload: Code " + stationCode);
        MetarDataTask downloadTask = sInstance.mMetarDataTaskWorkQueue.poll();
        if (null == downloadTask) {
            downloadTask = new MetarDataTask();
        }

        downloadTask.initializeDownloadTask(MetarDataManager.sInstance, stationCode);
        sInstance.mDownloadThreadPool.execute(downloadTask.getDownloadRunnable());

        return downloadTask;
    }

    public void updateCache() {
        Set<String> availableStations = mMetarHashMap.keySet();
        if (!availableStations.isEmpty()) {
            mDownloadStatus.set(DOWNLOAD_STARTED);
            for (String stationCode : availableStations) {
                startDownload(stationCode);
            }
        }
    }

    public void setCachedDataToHashMap(Cursor cursor) {

        if(cursor != null && cursor.moveToFirst()) {
            do {
                mMetarHashMap.put(cursor.getString(cursor.getColumnIndex(MetarContentProvider.COLUMN_CODE)),
                        cursor.getString(cursor.getColumnIndex(MetarContentProvider.COLUMN_DATA)));
            } while (cursor.moveToNext());
            Log.i(TAG, "getCachedMetarData: " + mMetarHashMap.size());
        }
    }

    public void setStationCodeToHashMap(Cursor cursor) {

        if(cursor != null && cursor.moveToFirst()) {
            mStationList = new String[cursor.getCount()];
            int index = 0;
            do {
                mStationList[index] = cursor.getString(cursor.getColumnIndex(MetarContentProvider.COLUMN_CODE));
                index++;
            } while (cursor.moveToNext());
            Log.i(TAG, "setStationCodeToHashMap: " + mStationList.length);
        }
    }

    public HashMap<String, String> getMetarHashMap() {
        return mMetarHashMap;
    }

    public void saveMetarData(Bundle data) {
        if (data != null) {

            String code = data.getString(EXTRA_CODE);
            String decodedData = data.getString(EXTRA_DECODED_DATA);
            int networkStatus = data.getInt(EXTRA_NETWORK_STATUS);

            if (networkStatus == NETWORK_STATUS_INTERNET_CONNECTION_OK) {

                if (!checkIfExist(code)) {

                    ContentValues values = new ContentValues();
                    values.put(MetarContentProvider.COLUMN_CODE, code);
                    values.put(MetarContentProvider.COLUMN_DATA, decodedData);

                    new MetarHandler(this, MetarBrowserApp.getInstance().getApplicationContext().getContentResolver()).startInsert(0, null, MetarContentProvider.CONTENT_URI, values);
                    mMetarHashMap.put(code, decodedData);

                } else {

                    if (decodedData.hashCode() == mMetarHashMap.get(code).hashCode()) {

                        Log.i(TAG, "saveMetarDataToDB: No need to update DB and list");
                    } else {

                        ContentValues values = new ContentValues();
                        values.put(MetarContentProvider.COLUMN_DATA, decodedData);

                        new MetarHandler(this, MetarBrowserApp.getInstance().getApplicationContext().getContentResolver()).startUpdate(0,
                                null,
                                MetarContentProvider.CONTENT_URI,
                                values,
                                MetarContentProvider.COLUMN_CODE + "=?",
                                new String[]{code});
                        mMetarHashMap.put(code, decodedData);
                    }
                }
            }
        }
    }

    public boolean checkIfExist(String code) {
        if (mMetarHashMap.get(code) != null && !mMetarHashMap.get(code).isEmpty()) {
            return true;
        }
        return false;
    }

    void recycleTask(MetarDataTask downloadTask) {
        // Puts the task object back into the queue for re-use.
        mMetarDataTaskWorkQueue.offer(downloadTask);
    }

    public void onQueryComplete(int token, Cursor cursor) {
        Log.i(TAG, "onQueryComplete: token = " + token);
        if (cursor != null) {
            Log.i(TAG, "onQueryComplete: Cursor count = " + cursor.getCount());
        }

        if (token == 0) {
            if (mMetarHashMap.isEmpty()) {
                setCachedDataToHashMap(cursor);
            }
        } else if (token == 1) {
            if (mStationList == null || mStationList.length == 0) {
                setStationCodeToHashMap(cursor);
            }
        }
    }

    public void onInsertComplete() {
        Log.i(TAG, "onInsertComplete: ");
    }

    public void onUpdateComplete() {
        Log.i(TAG, "onUpdateComplete: ");
    }

    public void onDeleteComplete() {
        Log.i(TAG, "onDeleteComplete: ");
    }

    public String getIfCachedDataAvailable(String code) {
        return mMetarHashMap.get(code);
    }

    private class NetworkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MetarService.ACTION_LIST_FETCH_RESPONSE)) {
                String[] stationArray = intent.getStringArrayExtra(EXTRA_STATION_LIST);
                if (stationArray != null && stationArray.length != 0) {
                    savePredefinedStationsToMap(stationArray);
                }
            }
        }
    }
}
