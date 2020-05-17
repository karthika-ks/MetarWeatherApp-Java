package com.example.metarapp.viewmodel;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.metarapp.model.contentprovider.MetarContentProvider;
import com.example.metarapp.model.contentprovider.MetarHandler;
import com.example.metarapp.utilities.MetarMetaData;
import com.example.metarapp.model.MetarService;

import java.util.HashMap;

public class MetarViewModel extends ViewModel {
    public MutableLiveData<String> decodedData = new MutableLiveData<>();
    public MutableLiveData<String> mCode = new MutableLiveData<>();
    static final String TAG = "MetarViewModel";
    private HashMap<String, String> metarHashMap;
    private Context context;

    public MetarViewModel() {
    }

    public void setContext(Context context) {
        this.context = context;
        registerNetworkReceiver();
        metarHashMap = new HashMap<>();
//        viewDB();
    }

    MutableLiveData<String> getDecodedData() {
        if (decodedData == null) {
            decodedData = new MutableLiveData<>();
        }
        return decodedData;
    }

    public void onSendClicked() {
        Log.i("MetarViewModel", "onSendClicked: Code = " + mCode.getValue());

        // Call network call to get the metar details from the server
        startMetarService();
    }

    public void startMetarService(){
        Intent cbIntent =  new Intent();
        cbIntent.setClass(context, MetarService.class);
        cbIntent.putExtra("code", mCode.getValue().toUpperCase());
        context.startService(cbIntent);
    }

    private void registerNetworkReceiver() {
        NetworkReceiver networkReceiver = new NetworkReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MetarService.ACTION_NETWORK_RESPONSE);
        context.registerReceiver(networkReceiver, filter);
    }

    public void onQueryComplete(Cursor cursor) {
        Log.i(TAG, "onQueryComplete: ");

        if (metarHashMap.isEmpty()) {
            getCachedMetarData(cursor);
        }

//        if(cursor != null && cursor.moveToFirst()) {
//            StringBuilder strBuild=new StringBuilder();
//            do {
//                strBuild.append("\n*****************************************************\n")
//                        .append(cursor.getString(cursor.getColumnIndex(MetarContentProvider.COLUMN_CODE)))
//                        .append("-").append(cursor.getString(cursor.getColumnIndex(MetarContentProvider.COLUMN_DATA)));
//            } while (cursor.moveToNext());
//            Log.i(TAG, "onQueryComplete: " + strBuild.toString());
//        }
    }

    public void onInsertComplete() {
        Log.i(TAG, "onInsertComplete: ");
//        viewDB();
    }

    public void onUpdateComplete() {
        Log.i(TAG, "onUpdateComplete: ");
//        viewDB();
    }

    public void onDeleteComplete() {
        Log.i(TAG, "onDeleteComplete: ");
    }

    private void saveMetarDataToDB(String code, String decodedData) {
        if (!checkIfExist(code)) {

            ContentValues values = new ContentValues();
            values.put(MetarContentProvider.COLUMN_CODE, code);
            values.put(MetarContentProvider.COLUMN_DATA, decodedData);

            new MetarHandler(this, context.getContentResolver()).startInsert(0, null, MetarContentProvider.CONTENT_URI, values);
            metarHashMap.put(code, decodedData);

        } else {

            if (decodedData.hashCode() == metarHashMap.get(code).hashCode()) {

                Log.i(TAG, "saveMetarDataToDB: No need to update DB and list");
            } else {

                ContentValues values = new ContentValues();
                values.put(MetarContentProvider.COLUMN_DATA, decodedData);

                new MetarHandler(this, context.getContentResolver()).startUpdate(0,
                        null,
                        MetarContentProvider.CONTENT_URI,
                        values,
                        MetarContentProvider.COLUMN_CODE + "=?",
                        new String[]{code});
                metarHashMap.put(code, decodedData);
            }
        }
    }

    private void getCachedMetarData(Cursor cursor) {

        if(cursor != null && cursor.moveToFirst()) {
            do {
                metarHashMap.put(cursor.getString(cursor.getColumnIndex(MetarContentProvider.COLUMN_CODE)),
                        cursor.getString(cursor.getColumnIndex(MetarContentProvider.COLUMN_DATA)));
            } while (cursor.moveToNext());
            Log.i(TAG, "getCachedMetarData: " + metarHashMap.size());
        }
    }

    private boolean checkIfExist(String code) {
        if (metarHashMap.get(code) != null) {
            return true;
        }
        return false;
    }

    private void viewDB() {
        new MetarHandler(this, context.getContentResolver()).startQuery(0, null, MetarContentProvider.CONTENT_URI, null, null, null, null);
    }

    private void updateUi(String code, String decodedData, int networkStatus) {
        MetarMetaData m = new MetarMetaData();
        switch (networkStatus) {
            case MetarService.NETWORK_STATUS_AIRPORT_NOT_FOUND:
                m.setMetadata("Airport not found, Please check ICAO code");
                break;
            case MetarService.NETWORK_STATUS_NO_INTERNET_CONNECTION:
                m.setMetadata("No internet connectivity, check your network connection");
                String cache = getIfCachedDataAvailable(code);
                if (cache != null)
                    m.setMetadata("No internet connectivity, Check you internet connection and try again !!!\n\nLast available update\n--------------------------------------\n"
                            + cache);
                break;
            case MetarService.NETWORK_STATUS_INTERNET_CONNECTION_OK:
                m.setMetadata(decodedData);
                break;
        }
        getDecodedData().setValue(m.getMetadata());
    }

    private String getIfCachedDataAvailable(String code) {
        return metarHashMap.get(code);
    }

    private class NetworkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String decodedData = intent.getStringExtra("decoded_data");
            String code = intent.getStringExtra("code");
            int networkStatus = intent.getIntExtra(MetarService.EXTRA_NETWORK_STATUS, 0);
            updateUi(code, decodedData, networkStatus);
            if (networkStatus == MetarService.NETWORK_STATUS_INTERNET_CONNECTION_OK) {
                saveMetarDataToDB(code, decodedData);
            }
        }
    }
}
