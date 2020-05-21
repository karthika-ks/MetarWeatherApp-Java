package com.example.metarapp.viewmodel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.metarapp.MetarDataManager;
import com.example.metarapp.utilities.MetarMetaData;
import com.example.metarapp.model.MetarService;
import com.example.metarapp.utilities.NetworkUtil;

import static com.example.metarapp.model.MetarService.EXTRA_CODE;
import static com.example.metarapp.model.MetarService.EXTRA_DECODED_DATA;
import static com.example.metarapp.model.MetarService.EXTRA_NETWORK_STATUS;

public class MetarViewModel extends ViewModel {

    public MutableLiveData<String> decodedData = new MutableLiveData<>();
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

    public MetarViewModel(Context context) {
        this.context = context;

        detailedViewVisibility.setValue(false);
        hasNetworkConnectivity.setValue(true);
        hasCachedDataAvailability.setValue(false);
        isStationAvailable.setValue(true);
        isDownloadProgress.setValue(false);

        registerNetworkReceiver();
    }

    public static MetarViewModel getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new MetarViewModel(context);
        }
        return sInstance;
    }

    MutableLiveData<String> getDecodedData() {
        if (decodedData == null) {
            decodedData = new MutableLiveData<>();
        }
        return decodedData;
    }

    public MutableLiveData<String> getMutableCode() {
        if (mEditTextCodeEntry == null) {
            mEditTextCodeEntry = new MutableLiveData<>();
        }
        return mEditTextCodeEntry;
    }

    MutableLiveData<Boolean> getMutableViewVisibility() {
        if (detailedViewVisibility == null) {
            detailedViewVisibility = new MutableLiveData<>();
        }
        return detailedViewVisibility;
    }

    public void onSendClicked() {
        Log.i("MetarViewModel", "onSendClicked: Code = " + mEditTextCodeEntry.getValue());
        detailedViewVisibility.setValue(false);
        hasNetworkConnectivity.setValue(true);
        hasCachedDataAvailability.setValue(false);
        isStationAvailable.setValue(true);
        isDownloadProgress.setValue(true);
        startMetarService(mEditTextCodeEntry.getValue().toUpperCase());
    }

    public void onTextChanged(Editable editable) {

    }

    public void startMetarService(String code){
        Intent cbIntent =  new Intent();
        cbIntent.setClass(context, MetarService.class);
        cbIntent.putExtra("code", code);
        cbIntent.putExtra(MetarService.SERVICE_ACTION, MetarService.FETCH_METAR_DATA);
        context.startService(cbIntent);
    }

    private void registerNetworkReceiver() {
        NetworkReceiver networkReceiver = new NetworkReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MetarService.ACTION_NETWORK_RESPONSE);
        LocalBroadcastManager.getInstance(context).registerReceiver(networkReceiver, filter);
    }

    private void updateUi(String code, String decodedData, int networkStatus) {
        MetarMetaData m = new MetarMetaData();
        isDownloadProgress.setValue(false);

        switch (networkStatus) {
            case NetworkUtil.NETWORK_STATUS_AIRPORT_NOT_FOUND:
                m.setMetadata("Airport not found, Please check ICAO code");
                isStationAvailable.setValue(false);
                break;
            case NetworkUtil.NETWORK_STATUS_NO_INTERNET_CONNECTION:
                hasNetworkConnectivity.setValue(false);
                String cache = MetarDataManager.getInstance().getIfCachedDataAvailable(code);
                if (cache != null && !cache.isEmpty()) {
                    detailedViewVisibility.setValue(true);
                    hasCachedDataAvailability.setValue(true);
                    m.setMetadata(cache);
                }
                break;
            case NetworkUtil.NETWORK_STATUS_INTERNET_CONNECTION_OK:
                m.setMetadata(decodedData);
                detailedViewVisibility.setValue(true);
                break;
        }
        getDecodedData().setValue(m.getMetadata());
        mICAOCode.setValue(code);
    }

    public void clearMutableValues() {
        decodedData.setValue("");
        mICAOCode.setValue("");
        mEditTextCodeEntry.setValue("");
        detailedViewVisibility.setValue(false);
        hasNetworkConnectivity.setValue(true);
        hasCachedDataAvailability.setValue(false);
        isStationAvailable.setValue(true);
    }

    private class NetworkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive: ");
            if (intent.getAction() == MetarService.ACTION_NETWORK_RESPONSE) {
                String decodedData = intent.getStringExtra("decoded_data");
                String code = intent.getStringExtra("code");
                int networkStatus = intent.getIntExtra(MetarService.EXTRA_NETWORK_STATUS, 0);
                updateUi(code, decodedData, networkStatus);

                if (networkStatus == NetworkUtil.NETWORK_STATUS_INTERNET_CONNECTION_OK) {

                    Bundle bundle = new Bundle();
                    bundle.putString(EXTRA_CODE, code);
                    bundle.putString(EXTRA_DECODED_DATA, decodedData);
                    bundle.putInt(EXTRA_NETWORK_STATUS, NetworkUtil.NETWORK_STATUS_INTERNET_CONNECTION_OK);
                    MetarDataManager.getInstance().saveMetarData(bundle);

                }
            }
        }
    }
}
