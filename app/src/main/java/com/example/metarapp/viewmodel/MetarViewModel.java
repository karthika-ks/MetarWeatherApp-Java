package com.example.metarapp.viewmodel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewModel;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.metarapp.MetarBrowserApp;
import com.example.metarapp.model.MetarDataManager;
import com.example.metarapp.model.MetarIntentService;
import com.example.metarapp.utilities.MetarData;

import static com.example.metarapp.utilities.Constants.ACTION_NETWORK_RESPONSE;
import static com.example.metarapp.utilities.Constants.EXTRA_CODE;
import static com.example.metarapp.utilities.Constants.EXTRA_METAR_DATA;
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
    public MutableLiveData<String> mRawData = new MutableLiveData<>();
    public MutableLiveData<String> mStationName = new MutableLiveData<>();
    public MutableLiveData<String> mLastUpdatedTime = new MutableLiveData<>();

    private String mCurrentBoundedActivity;
    private static final String TAG = "MetarViewModel";
    private String lastSelectedStationCode = "";
    private String mRequestCallingActivity;

    public MetarViewModel() {
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
        LocalBroadcastManager.getInstance(MetarBrowserApp.getInstance().getApplicationContext()).registerReceiver(networkReceiver, filter);
    }

    private void updateUi(int networkStatus, MetarData metarData) {
        isDownloadProgress.setValue(false);
        mICAOCode.setValue(metarData.getCode());

        switch (networkStatus) {
            case NETWORK_STATUS_AIRPORT_NOT_FOUND:
                isStationAvailable.setValue(false);
                break;
            case NETWORK_STATUS_NO_INTERNET_CONNECTION:
                hasNetworkConnectivity.setValue(false);

                MetarData cache = MetarDataManager.getInstance().getIfCachedDataAvailable(metarData.getCode());

                if (cache != null) {
                    String cachedData = cache.getDecodedData();
                    String cachedRawData = cache.getRawData();
                    String lastUpdatedTime = cache.getLastUpdatedTime();
                    String stationName = cache.getStationName();

                    if (cachedData != null && !cachedData.isEmpty()) {
                        detailedViewVisibility.setValue(true);
                        hasCachedDataAvailability.setValue(true);
                        mDecodedData.setValue(cachedData);
                        mRawData.setValue(cachedRawData);
                        mLastUpdatedTime.setValue(lastUpdatedTime);
                        mStationName.setValue(stationName);
                    }
                }
                break;
            case NETWORK_STATUS_INTERNET_CONNECTION_OK:
                mDecodedData.setValue(metarData.getDecodedData());
                mRawData.setValue(metarData.getRawData());
                mLastUpdatedTime.setValue(metarData.getLastUpdatedTime());
                mStationName.setValue(metarData.getStationName());
                detailedViewVisibility.setValue(true);
                break;
        }
    }

    public void setCurrentBoundedActivity(String activity) {
        mCurrentBoundedActivity = activity;
    }

    public void onSendClicked() {
        if (mEditTextCodeEntry.getValue() != null && !mEditTextCodeEntry.getValue().isEmpty()) {
            startMetarService(mEditTextCodeEntry.getValue().toUpperCase());
        }
    }

    public void onRefreshClicked() {
        if (!lastSelectedStationCode.isEmpty()) {
            startMetarService(lastSelectedStationCode);
        }
    }

    public void startMetarService(String code) {
        lastSelectedStationCode = code;
        mRequestCallingActivity = mCurrentBoundedActivity;
        startBusyIndicator();
        Intent cbIntent =  new Intent();
        cbIntent.setClass(MetarBrowserApp.getInstance().getApplicationContext(), MetarIntentService.class);
        cbIntent.putExtra(EXTRA_CODE, code);
        cbIntent.putExtra(SERVICE_ACTION, FETCH_METAR_DATA);
        MetarBrowserApp.getInstance().getApplicationContext().startService(cbIntent);

        if (MetarDataManager.getInstance().checkIfExist(code)) {
            MetarData metarData = MetarDataManager.getInstance().getIfCachedDataAvailable(code);

            if (metarData != null) {
                updateUi(NETWORK_STATUS_INTERNET_CONNECTION_OK, metarData);
            }
        }
    }

    public void registerLifeCycleObserver(Lifecycle lifecycle) {
        lifecycle.addObserver(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void clearMutableValues() {
        mDecodedData.setValue("");
        mICAOCode.setValue("");
        mStationName.setValue("");
        mLastUpdatedTime.setValue("");
        mEditTextCodeEntry.setValue("");
        detailedViewVisibility.setValue(false);
        hasNetworkConnectivity.setValue(true);
        hasCachedDataAvailability.setValue(false);
        isStationAvailable.setValue(true);
        isDownloadProgress.setValue(false);
        lastSelectedStationCode = "";
        mCurrentBoundedActivity = "";
        mRequestCallingActivity = "";
    }

    private class NetworkReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(ACTION_NETWORK_RESPONSE)) {
                if (mCurrentBoundedActivity.equals(mRequestCallingActivity)) {
                    int networkStatus = intent.getIntExtra(EXTRA_NETWORK_STATUS, NETWORK_STATUS_NO_INTERNET_CONNECTION);
                    MetarData metarData = intent.getParcelableExtra(EXTRA_METAR_DATA);

                    if (metarData != null) {
                        updateUi(networkStatus, metarData);

                        if (networkStatus == NETWORK_STATUS_INTERNET_CONNECTION_OK) {
                            MetarDataManager.getInstance().saveMetarDataDownloaded(networkStatus, metarData);
                        }
                    }
                }
            }
        }
    }
}
