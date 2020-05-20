package com.example.metarapp.viewmodel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.metarapp.MetarDataManager;
import com.example.metarapp.utilities.MetarMetaData;
import com.example.metarapp.model.MetarService;
import com.example.metarapp.utilities.NetworkUtil;

import static com.example.metarapp.model.MetarService.EXTRA_CODE;
import static com.example.metarapp.model.MetarService.EXTRA_DECODED_DATA;
import static com.example.metarapp.model.MetarService.EXTRA_NETWORK_STATUS;

public class MetarViewModel extends ViewModel {

    public MutableLiveData<String> decodedData = new MutableLiveData<>();
    public MutableLiveData<String> mCode = new MutableLiveData<>();
    static final String TAG = "MetarViewModel";
    private Context context;

    public MetarViewModel(Context context) {
        this.context = context;
        registerNetworkReceiver();
        MetarDataManager.getInstance();
    }

    MutableLiveData<String> getDecodedData() {
        if (decodedData == null) {
            decodedData = new MutableLiveData<>();
        }
        return decodedData;
    }

    public void onSendClicked() {
        Log.i("MetarViewModel", "onSendClicked: Code = " + mCode.getValue());
        startMetarService();
    }

    public void onUpdateCacheClicked() {
        MetarDataManager.getInstance().updateCache();
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

    private void updateUi(String code, String decodedData, int networkStatus) {
        MetarMetaData m = new MetarMetaData();
        switch (networkStatus) {
            case NetworkUtil.NETWORK_STATUS_AIRPORT_NOT_FOUND:
                m.setMetadata("Airport not found, Please check ICAO code");
                break;
            case NetworkUtil.NETWORK_STATUS_NO_INTERNET_CONNECTION:
                m.setMetadata("No internet connectivity, check your network connection");
                String cache = MetarDataManager.getInstance().getIfCachedDataAvailable(code);
                if (cache != null)
                    m.setMetadata("No internet connectivity, Check you internet connection and try again !!!\n\nLast available update\n--------------------------------------\n"
                            + cache);
                break;
            case NetworkUtil.NETWORK_STATUS_INTERNET_CONNECTION_OK:
                m.setMetadata(decodedData);
                break;
        }
        getDecodedData().setValue(m.getMetadata());
    }

    private class NetworkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive: ");
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
