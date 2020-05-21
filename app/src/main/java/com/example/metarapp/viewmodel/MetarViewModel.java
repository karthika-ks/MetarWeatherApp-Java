package com.example.metarapp.viewmodel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewModel;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.metarapp.model.MetarDataManager;
import com.example.metarapp.model.MetarIntentService;

import static com.example.metarapp.utilities.Constants.ACTION_NETWORK_RESPONSE;
import static com.example.metarapp.utilities.Constants.EXTRA_CODE;
import static com.example.metarapp.utilities.Constants.EXTRA_DECODED_DATA;
import static com.example.metarapp.utilities.Constants.EXTRA_NETWORK_STATUS;
import static com.example.metarapp.utilities.Constants.FETCH_METAR_DATA;
import static com.example.metarapp.utilities.Constants.NETWORK_STATUS_AIRPORT_NOT_FOUND;
import static com.example.metarapp.utilities.Constants.NETWORK_STATUS_INTERNET_CONNECTION_OK;
import static com.example.metarapp.utilities.Constants.NETWORK_STATUS_NO_INTERNET_CONNECTION;
import static com.example.metarapp.utilities.Constants.SERVICE_ACTION;

public class MetarViewModel extends ViewModel implements LifecycleObserver {

    public MutableLiveData<String> mDecodedData = new MutableLiveData<>();
    public MutableLiveData<String> mEditTextCodeEntry = new MutableLiveData<>();
    public MutableLiveData<Boolean> detailedViewVisibility = new MutableLiveData<>();
    public MutableLiveData<Boolean> hasNetworkConnectivity = new MutableLiveData<>();
    public MutableLiveData<Boolean> hasCachedDataAvailability = new MutableLiveData<>();
    public MutableLiveData<Boolean> isStationAvailable = new MutableLiveData<>();
    public MutableLiveData<Boolean> isDownloadProgress = new MutableLiveData<>();
    public MutableLiveData<String> mICAOCode = new MutableLiveData<>();

    static final String TAG = "MetarViewModel";
    private Context context;
    private static MetarViewModel sInstance;

    private MetarViewModel(Context context) {
        this.context = context;

        clearMutableValues();
        registerNetworkReceiver();
    }

    private void startBusyIndicator() {
        detailedViewVisibility.setValue(false);
        hasNetworkConnectivity.setValue(true);
        hasCachedDataAvailability.setValue(false);
        isStationAvailable.setValue(true);
        isDownloadProgress.setValue(true);
    }

    private void registerNetworkReceiver() {
        NetworkReceiver networkReceiver = new NetworkReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NETWORK_RESPONSE);
        LocalBroadcastManager.getInstance(context).registerReceiver(networkReceiver, filter);
    }

    private void updateUi(String code, String decodedData, int networkStatus) {
        isDownloadProgress.setValue(false);
        mICAOCode.setValue(code);

        switch (networkStatus) {
            case NETWORK_STATUS_AIRPORT_NOT_FOUND:
                isStationAvailable.setValue(false);
                break;
            case NETWORK_STATUS_NO_INTERNET_CONNECTION:
                hasNetworkConnectivity.setValue(false);
                String cache = MetarDataManager.getInstance().getIfCachedDataAvailable(code);
                if (cache != null && !cache.isEmpty()) {
                    detailedViewVisibility.setValue(true);
                    hasCachedDataAvailability.setValue(true);
                    mDecodedData.setValue(cache);
                }
                break;
            case NETWORK_STATUS_INTERNET_CONNECTION_OK:
                mDecodedData.setValue(decodedData);
                detailedViewVisibility.setValue(true);
                break;
        }
    }

    public static MetarViewModel getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new MetarViewModel(context);
        }
        return sInstance;
    }

    public void onSendClicked() {
        Log.i(TAG, "onSendClicked: Code = " + mEditTextCodeEntry.getValue());
        startMetarService(mEditTextCodeEntry.getValue().toUpperCase());
    }

    public void startMetarService(String code) {
        startBusyIndicator();
        Intent cbIntent =  new Intent();
        cbIntent.setClass(context, MetarIntentService.class);
        cbIntent.putExtra(EXTRA_CODE, code);
        cbIntent.putExtra(SERVICE_ACTION, FETCH_METAR_DATA);
        context.startService(cbIntent);
    }

    public void registerLifeCycleObserver(Lifecycle lifecycle) {
        lifecycle.addObserver(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void clearMutableValues() {
        Log.i(TAG, "clearMutableValues: ");
        mDecodedData.setValue("");
        mICAOCode.setValue("");
        mEditTextCodeEntry.setValue("");
        detailedViewVisibility.setValue(false);
        hasNetworkConnectivity.setValue(true);
        hasCachedDataAvailability.setValue(false);
        isStationAvailable.setValue(true);
        isDownloadProgress.setValue(false);
    }

    private class NetworkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive: ");
            if (intent.getAction().equals(ACTION_NETWORK_RESPONSE)) {

                String decodedData = intent.getStringExtra(EXTRA_DECODED_DATA);
                String code = intent.getStringExtra(EXTRA_CODE);
                int networkStatus = intent.getIntExtra(EXTRA_NETWORK_STATUS, NETWORK_STATUS_NO_INTERNET_CONNECTION);
                updateUi(code, decodedData, networkStatus);

                if (networkStatus == NETWORK_STATUS_INTERNET_CONNECTION_OK) {

                    Bundle bundle = new Bundle();
                    bundle.putString(EXTRA_CODE, code);
                    bundle.putString(EXTRA_DECODED_DATA, decodedData);
                    bundle.putInt(EXTRA_NETWORK_STATUS, NETWORK_STATUS_INTERNET_CONNECTION_OK);

                    MetarDataManager.getInstance().saveMetarDataDownloaded(bundle);

                }
            }
        }
    }
}
