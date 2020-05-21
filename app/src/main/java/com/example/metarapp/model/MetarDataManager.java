package com.example.metarapp.model;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.metarapp.MetarBrowserApp;
import com.example.metarapp.model.contentprovider.MetarContentProvider;
import com.example.metarapp.model.contentprovider.MetarAsyncQueryHandler;
import com.example.metarapp.model.scheduler.DataDownloadManager;

import java.util.HashMap;

import static android.content.Context.MODE_PRIVATE;
import static com.example.metarapp.utilities.Constants.ACTION_LIST_FETCH_RESPONSE;
import static com.example.metarapp.utilities.Constants.DOWNLOAD_COMPLETE;
import static com.example.metarapp.utilities.Constants.DOWNLOAD_STARTED;
import static com.example.metarapp.utilities.Constants.EXTRA_CODE;
import static com.example.metarapp.utilities.Constants.EXTRA_DECODED_DATA;
import static com.example.metarapp.utilities.Constants.EXTRA_NETWORK_STATUS;
import static com.example.metarapp.utilities.Constants.EXTRA_STATION_LIST;
import static com.example.metarapp.utilities.Constants.FETCH_GERMAN_STATION_LIST;
import static com.example.metarapp.utilities.Constants.NETWORK_STATUS_INTERNET_CONNECTION_OK;
import static com.example.metarapp.utilities.Constants.PREF_KEY_DOWNLOAD_STATUS;
import static com.example.metarapp.utilities.Constants.PREF_KEY_IS_AVAILABLE;
import static com.example.metarapp.utilities.Constants.PREF_NAME_GERMAN_LIST;
import static com.example.metarapp.utilities.Constants.SERVICE_ACTION;

public class MetarDataManager {

    private static final String TAG = MetarDataManager.class.getSimpleName();

    private static MetarDataManager sInstance;
    private HashMap<String, String> mMetarDataHashMap;
    private String[] mFilteredStationList;

    static {
        sInstance = new MetarDataManager();
    }

    private MetarDataManager() {
        mMetarDataHashMap = new HashMap<>();
        DataDownloadManager.getInstance();
        registerNetworkReceiver();
        fetchCachedDataFromDB();
        fetchFilteredStationList();
    }

    private void registerNetworkReceiver() {
        NetworkReceiver networkReceiver = new NetworkReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_LIST_FETCH_RESPONSE);
        LocalBroadcastManager.getInstance(MetarBrowserApp.getInstance().getApplicationContext()).registerReceiver(networkReceiver, filter);
    }

    private void fetchFilteredStationList() {

        SharedPreferences pref = MetarBrowserApp.getInstance().getApplicationContext().getSharedPreferences(PREF_NAME_GERMAN_LIST, MODE_PRIVATE);

        if (!pref.getBoolean(PREF_KEY_IS_AVAILABLE, false)) {

            SharedPreferences.Editor editor = pref.edit();
            editor.putInt(PREF_KEY_DOWNLOAD_STATUS, DOWNLOAD_STARTED);
            editor.apply();

            startMetarService();

        } else {
            // Fetch code from Database
            Log.i(TAG, "getFilteredStationList: list is already available");
            fetchFilteredStationListFromDB();
        }
    }

    private void fetchCachedDataFromDB() {
        new MetarAsyncQueryHandler(this, MetarBrowserApp.getInstance().getApplicationContext().getContentResolver())
                .startQuery(0
                        , null
                        , MetarContentProvider.CONTENT_URI
                        , null
                        , null
                        , null
                        , null);
    }

    private void fetchFilteredStationListFromDB() {
        new MetarAsyncQueryHandler(this, MetarBrowserApp.getInstance().getApplicationContext().getContentResolver())
                .startQuery(1
                        , null
                        , MetarContentProvider.CONTENT_URI
                        , new String[]{MetarContentProvider.COLUMN_CODE}
                        , MetarContentProvider.COLUMN_CODE + " like ?"
                        , new String[]{"ED%"}
                        , MetarContentProvider.COLUMN_CODE + " ASC");
    }

    private void startMetarService() {
        Intent cbIntent = new Intent();
        cbIntent.setClass(MetarBrowserApp.getInstance().getApplicationContext(), MetarIntentService.class);
        cbIntent.putExtra(SERVICE_ACTION, FETCH_GERMAN_STATION_LIST);
        MetarBrowserApp.getInstance().getApplicationContext().startService(cbIntent);
    }

    private void saveFilteredStationList(String[] germanStations) {
        mFilteredStationList = germanStations;
        for (String station : germanStations) {
            mMetarDataHashMap.put(station, "");
        }

        SharedPreferences pref = MetarBrowserApp.getInstance().getApplicationContext().getSharedPreferences(PREF_NAME_GERMAN_LIST, MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(PREF_KEY_IS_AVAILABLE, true);
        editor.putInt(PREF_KEY_DOWNLOAD_STATUS, DOWNLOAD_COMPLETE);
        editor.apply();
    }

    private void saveCachedDataToHashMap(Cursor cursor) {

        if(cursor != null && cursor.moveToFirst()) {
            do {
                mMetarDataHashMap.put(cursor.getString(cursor.getColumnIndex(MetarContentProvider.COLUMN_CODE)),
                        cursor.getString(cursor.getColumnIndex(MetarContentProvider.COLUMN_DATA)));
            } while (cursor.moveToNext());
            Log.i(TAG, "getCachedMetarData: " + mMetarDataHashMap.size());
        }
    }

    private void saveStationCodeToArray(Cursor cursor) {

        if(cursor != null && cursor.moveToFirst()) {
            mFilteredStationList = new String[cursor.getCount()];
            int index = 0;

            do {
                mFilteredStationList[index] = cursor.getString(cursor.getColumnIndex(MetarContentProvider.COLUMN_CODE));
                index++;
            } while (cursor.moveToNext());

            Log.i(TAG, "setStationCodeToHashMap: " + mFilteredStationList.length);
        }
    }

    private boolean checkIfExist(String code) {
        if (mMetarDataHashMap.get(code) != null && !mMetarDataHashMap.get(code).isEmpty()) {
            return true;
        }
        return false;
    }

    public static MetarDataManager getInstance() {
        return sInstance;
    }

    public String[] getFilteredStationList() {
        return mFilteredStationList;
    }

    public HashMap<String, String> getMetarHashMap() {
        return mMetarDataHashMap;
    }

    public void saveMetarDataDownloaded(Bundle data) {
        if (data != null) {

            String code = data.getString(EXTRA_CODE);
            String decodedData = data.getString(EXTRA_DECODED_DATA);
            int networkStatus = data.getInt(EXTRA_NETWORK_STATUS);

            if (networkStatus == NETWORK_STATUS_INTERNET_CONNECTION_OK) {

                if (!checkIfExist(code)) {

                    ContentValues values = new ContentValues();
                    values.put(MetarContentProvider.COLUMN_CODE, code);
                    values.put(MetarContentProvider.COLUMN_DATA, decodedData);

                    new MetarAsyncQueryHandler(this, MetarBrowserApp.getInstance().getApplicationContext().getContentResolver())
                            .startInsert(0, null, MetarContentProvider.CONTENT_URI, values);
                    mMetarDataHashMap.put(code, decodedData);

                } else {

                    if (decodedData.hashCode() == mMetarDataHashMap.get(code).hashCode()) {
                        Log.i(TAG, "saveMetarDataToDB: No need to update DB and list");
                    } else {

                        ContentValues values = new ContentValues();
                        values.put(MetarContentProvider.COLUMN_DATA, decodedData);

                        new MetarAsyncQueryHandler(this, MetarBrowserApp.getInstance().getApplicationContext().getContentResolver())
                                .startUpdate(0,
                                        null,
                                        MetarContentProvider.CONTENT_URI,
                                        values,
                                        MetarContentProvider.COLUMN_CODE + "=?",
                                        new String[]{code});
                        mMetarDataHashMap.put(code, decodedData);
                    }
                }
            }
        }
    }

    public String getIfCachedDataAvailable(String code) {
        return mMetarDataHashMap.get(code);
    }

    public void onQueryComplete(int token, Cursor cursor) {
        Log.i(TAG, "onQueryComplete: token = " + token);
        if (cursor != null) {
            Log.i(TAG, "onQueryComplete: Cursor count = " + cursor.getCount());
        }

        if (token == 0) {
            if (mMetarDataHashMap.isEmpty()) {
                saveCachedDataToHashMap(cursor);
            }
        } else if (token == 1) {
            if (mFilteredStationList == null || mFilteredStationList.length == 0) {
                saveStationCodeToArray(cursor);
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

    private class NetworkReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(ACTION_LIST_FETCH_RESPONSE)) {
                String[] stationArray = intent.getStringArrayExtra(EXTRA_STATION_LIST);

                if (stationArray != null && stationArray.length != 0) {
                    saveFilteredStationList(stationArray);
                }
            }
        }
    }
}