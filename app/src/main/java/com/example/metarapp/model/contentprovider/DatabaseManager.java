package com.example.metarapp.model.contentprovider;

import android.content.ContentValues;
import android.database.Cursor;

import com.example.metarapp.MetarBrowserApp;
import com.example.metarapp.model.MetarDataManager;
import com.example.metarapp.utilities.MetarData;

import java.util.HashMap;

public class DatabaseManager {
    private static DatabaseManager sInstance;

    private DatabaseManager() {
        fetchCachedDataFromDB();
    }

    public static DatabaseManager getInstance() {
        if (sInstance == null) {
            sInstance = new DatabaseManager();
        }
        return sInstance;
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

    void onQueryComplete(int token, Cursor cursor) {

        if (token == 0) {
            if (MetarDataManager.getInstance().getMetarHashMap().isEmpty()) {
                parseMetarData(cursor);
            }
        } else if (token == 1) {
            String[] list = MetarDataManager.getInstance().getFilteredStationList();
            if (list == null || list.length == 0) {
                parseFilteredStationList(cursor);
            }
        }
    }

    void onInsertComplete() {
    }

    void onUpdateComplete() {
    }

    void onDeleteComplete() {
    }

    public void fetchFilteredStationListFromDB() {
        new MetarAsyncQueryHandler(this, MetarBrowserApp.getInstance().getApplicationContext().getContentResolver())
                .startQuery(1
                        , null
                        , MetarContentProvider.CONTENT_URI
                        , new String[]{MetarContentProvider.COLUMN_CODE}
                        , MetarContentProvider.COLUMN_CODE + " like ?"
                        , new String[]{"ED%"}
                        , MetarContentProvider.COLUMN_CODE + " ASC");
    }

    private void parseMetarData(Cursor cursor) {

        HashMap<String, MetarData> metarMap = new HashMap<>();

        if(cursor != null && cursor.moveToFirst()) {
            do {
                MetarData metarData = new MetarData();
                metarData.setCode(cursor.getString(cursor.getColumnIndex(MetarContentProvider.COLUMN_CODE)));
                metarData.setStationName(cursor.getString(cursor.getColumnIndex(MetarContentProvider.COLUMN_STATION_NAME)));
                metarData.setDecodedData(cursor.getString(cursor.getColumnIndex(MetarContentProvider.COLUMN_DATA)));
                metarData.setRawData(cursor.getString(cursor.getColumnIndex(MetarContentProvider.COLUMN_RAW_DATA)));
                metarData.setLastUpdatedTime(cursor.getString(cursor.getColumnIndex(MetarContentProvider.COLUMN_LAST_UPDATED_TIME)));

                metarMap.put(cursor.getString(cursor.getColumnIndex(MetarContentProvider.COLUMN_CODE)),metarData);

            } while (cursor.moveToNext());

            MetarDataManager.getInstance().setMetarHashMap(metarMap);
        }
    }

    private void parseFilteredStationList(Cursor cursor) {
        String[] stationList;
        if(cursor != null && cursor.moveToFirst()) {
            stationList = new String[cursor.getCount()];
            int index = 0;

            do {
                stationList[index] = cursor.getString(cursor.getColumnIndex(MetarContentProvider.COLUMN_CODE));
                index++;
            } while (cursor.moveToNext());
            MetarDataManager.getInstance().setFilteredStationList(stationList);
        }
    }

    public void startDBUpdate(String code, MetarData metarData) {
        ContentValues values = new ContentValues();
        values.put(MetarContentProvider.COLUMN_DATA, metarData.getDecodedData());
        values.put(MetarContentProvider.COLUMN_RAW_DATA, metarData.getRawData());
        values.put(MetarContentProvider.COLUMN_STATION_NAME, metarData.getStationName());
        values.put(MetarContentProvider.COLUMN_LAST_UPDATED_TIME, metarData.getLastUpdatedTime());

        new MetarAsyncQueryHandler(this, MetarBrowserApp.getInstance().getApplicationContext().getContentResolver())
                .startUpdate(0,
                        null,
                        MetarContentProvider.CONTENT_URI,
                        values,
                        MetarContentProvider.COLUMN_CODE + "=?",
                        new String[]{code});


    }

    public void startDBInsert(String code, MetarData metarData) {
        ContentValues values = new ContentValues();
        values.put(MetarContentProvider.COLUMN_CODE, code);
        values.put(MetarContentProvider.COLUMN_DATA, metarData.getDecodedData());
        values.put(MetarContentProvider.COLUMN_RAW_DATA, metarData.getRawData());
        values.put(MetarContentProvider.COLUMN_STATION_NAME, metarData.getStationName());
        values.put(MetarContentProvider.COLUMN_LAST_UPDATED_TIME, metarData.getLastUpdatedTime());

        new MetarAsyncQueryHandler(this, MetarBrowserApp.getInstance().getApplicationContext().getContentResolver())
                .startInsert(0, null, MetarContentProvider.CONTENT_URI, values);


    }
}
