package com.example.metarapp.model;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.metarapp.MetarBrowserApp;
import com.example.metarapp.model.contentprovider.DatabaseManager;
import com.example.metarapp.model.contentprovider.MetarContentProvider;
import com.example.metarapp.model.contentprovider.MetarAsyncQueryHandler;
import com.example.metarapp.model.scheduler.DataDownloadManager;
import com.example.metarapp.utilities.MetarData;

import java.util.HashMap;
import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;
import static com.example.metarapp.utilities.Constants.ACTION_LIST_FETCH_RESPONSE;
import static com.example.metarapp.utilities.Constants.CHECK_INTERNET_AVAILABILITY;
import static com.example.metarapp.utilities.Constants.DOWNLOAD_COMPLETE;
import static com.example.metarapp.utilities.Constants.DOWNLOAD_STARTED;
import static com.example.metarapp.utilities.Constants.EXTRA_STATION_LIST;
import static com.example.metarapp.utilities.Constants.FETCH_GERMAN_STATION_LIST;
import static com.example.metarapp.utilities.Constants.NETWORK_STATUS_INTERNET_CONNECTION_OK;
import static com.example.metarapp.utilities.Constants.PREF_KEY_DOWNLOAD_STATUS;
import static com.example.metarapp.utilities.Constants.PREF_KEY_IS_FILTERED_LIST_AVAILABLE;
import static com.example.metarapp.utilities.Constants.PREF_NAME_GERMAN_LIST;
import static com.example.metarapp.utilities.Constants.SERVICE_ACTION;

public class MetarDataManager implements INetworkConnectivityListener {

    private static final String TAG = MetarDataManager.class.getSimpleName();

    private static MetarDataManager sInstance;
    private DatabaseManager mDatabaseManager;
    private HashMap<String, MetarData> mMetarDataHashMap;
    private String[] mFilteredStationList;

    static {
        sInstance = new MetarDataManager();
    }

    private MetarDataManager() {
        mMetarDataHashMap = new HashMap<>();
        mDatabaseManager = DatabaseManager.getInstance();
        DataDownloadManager.getInstance().registerNetworkListener(this);
        registerNetworkReceiver();
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

        if (!pref.getBoolean(PREF_KEY_IS_FILTERED_LIST_AVAILABLE, false)) {

            ConnectivityManager cm = (ConnectivityManager) MetarBrowserApp.getInstance().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            assert cm != null;
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

            if (activeNetwork != null && activeNetwork.isConnected()) {
                SharedPreferences.Editor editor = pref.edit();
                editor.putInt(PREF_KEY_DOWNLOAD_STATUS, DOWNLOAD_STARTED);
                editor.apply();

                startMetarService(FETCH_GERMAN_STATION_LIST);
            }

        } else {
            // Fetch code from Database
            if (mFilteredStationList == null || mFilteredStationList.length == 0) {
                mDatabaseManager.fetchFilteredStationListFromDB();
            }
        }
    }

    private void startMetarService(String serviceAction) {
        Intent cbIntent = new Intent();
        cbIntent.setClass(MetarBrowserApp.getInstance().getApplicationContext(), MetarIntentService.class);
        cbIntent.putExtra(SERVICE_ACTION, serviceAction);
        MetarBrowserApp.getInstance().getApplicationContext().startService(cbIntent);
    }

    private void saveFilteredStationList(String[] germanStations) {
        mFilteredStationList = germanStations;
        for (String station : germanStations) {
            mMetarDataHashMap.put(station, null);
        }

        SharedPreferences pref = MetarBrowserApp.getInstance().getApplicationContext().getSharedPreferences(PREF_NAME_GERMAN_LIST, MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(PREF_KEY_IS_FILTERED_LIST_AVAILABLE, true);
        editor.putInt(PREF_KEY_DOWNLOAD_STATUS, DOWNLOAD_COMPLETE);
        editor.apply();
    }

    public boolean checkIfExist(String code) {
        if (mMetarDataHashMap.get(code) != null
                && !Objects.requireNonNull(Objects.requireNonNull(mMetarDataHashMap.get(code)).getRawData().isEmpty())) {
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

    public void setFilteredStationList(String[] stationList) {
        if (stationList != null && stationList.length != 0) {
            mFilteredStationList = stationList;
        }
    }

    public HashMap<String, MetarData> getMetarHashMap() {
        return mMetarDataHashMap;
    }

    public void setMetarHashMap(HashMap<String, MetarData> metarHashMap) {
        if (metarHashMap != null && !metarHashMap.isEmpty()) {
            mMetarDataHashMap.putAll(metarHashMap);
        }
    }

    public void saveMetarDataDownloaded(int networkStatus, MetarData metarData) {

        if (metarData != null) {

            String code = metarData.getCode();
            String decodedData = metarData.getDecodedData();

            if (networkStatus == NETWORK_STATUS_INTERNET_CONNECTION_OK) {

                if (!mMetarDataHashMap.containsKey(code)) {
                    mDatabaseManager.startDBInsert(code, metarData);
                    mMetarDataHashMap.put(code, metarData);
                } else {

                    MetarData data = mMetarDataHashMap.get(code);
                    if (data != null) {
                        if (decodedData.hashCode() != data.getDecodedData().hashCode()) {
                            mDatabaseManager.startDBUpdate(code, metarData);
                            mMetarDataHashMap.put(code, metarData);
                        }
                    } else {
                        mDatabaseManager.startDBUpdate(code, metarData);
                        mMetarDataHashMap.put(code, metarData);
                    }
                }
            }
        }
    }

    public MetarData getIfCachedDataAvailable(String code) {
        return mMetarDataHashMap.get(code);
    }

    @Override
    public void onConnected() {
        startMetarService(CHECK_INTERNET_AVAILABILITY);
        fetchFilteredStationList();
    }

    @Override
    public void onDisconnected() {
        startMetarService(CHECK_INTERNET_AVAILABILITY);
    }

    private class NetworkReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (Objects.equals(intent.getAction(), ACTION_LIST_FETCH_RESPONSE)) {
                String[] stationArray = intent.getStringArrayExtra(EXTRA_STATION_LIST);

                if (stationArray != null && stationArray.length != 0) {
                    saveFilteredStationList(stationArray);
                }
            }
        }
    }
}
