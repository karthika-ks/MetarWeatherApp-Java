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

public class MetarViewModel extends ViewModel {
    public MutableLiveData<String> decodedData = new MutableLiveData<>();
    public MutableLiveData<String> mCode = new MutableLiveData<>();
    static final String TAG = "MetarViewModel";
    private Context context;

    public MetarViewModel() {
    }

    public void setContext(Context context) {
        this.context = context;
        registerNetworkReceiver();
    }

    MutableLiveData<String> getDecodedData() {
        Log.i("MetarViewModel", "getDecodedData: ");
        if (decodedData == null) {
            decodedData = new MutableLiveData<>();
            decodedData.setValue("Initial");
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
        cbIntent.putExtra("code", mCode.getValue());
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

        if(cursor != null && cursor.moveToFirst()) {
            StringBuilder strBuild=new StringBuilder();
            do {
                strBuild.append("\n")
                        .append(cursor.getString(cursor.getColumnIndex(MetarContentProvider.COLUMN_CODE)))
                        .append("-").append(cursor.getString(cursor.getColumnIndex(MetarContentProvider.COLUMN_DATA)));
            } while (cursor.moveToNext());
            Log.i(TAG, "onQueryComplete: " + strBuild.toString());
        }
    }

    public void onInsertComplete() {
        Log.i(TAG, "onInsertComplete: ");
        viewDB();
    }

    public void onUpdateComplete() {
        Log.i(TAG, "onUpdateComplete: ");
    }

    public void onDeleteComplete() {
        Log.i(TAG, "onDeleteComplete: ");
    }

    private void saveMetarDataToDB(String code, String decodedData) {
        ContentValues values = new ContentValues();
        values.put(MetarContentProvider.COLUMN_CODE, code);
        values.put(MetarContentProvider.COLUMN_DATA, decodedData);

        new MetarHandler(this, context.getContentResolver()).startInsert(0, null, MetarContentProvider.CONTENT_URI, values);
    }

    private void viewDB() {
        new MetarHandler(this, context.getContentResolver()).startQuery(0, null, MetarContentProvider.CONTENT_URI, null, null, null, null);
    }

    private class NetworkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String decodedData = intent.getStringExtra("decoded_data");
            String code = intent.getStringExtra("code");
            MetarMetaData m = new MetarMetaData();
            m.setMetadata(decodedData);
            getDecodedData().setValue(m.getMetadata());

            saveMetarDataToDB(code, decodedData);
        }
    }
}
