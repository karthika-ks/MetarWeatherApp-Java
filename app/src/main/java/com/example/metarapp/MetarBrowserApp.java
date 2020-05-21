package com.example.metarapp;

import android.app.Application;

import com.example.metarapp.model.MetarDataManager;

public class MetarBrowserApp extends Application {

    private static MetarBrowserApp mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        MetarDataManager.getInstance();
    }

    public static synchronized MetarBrowserApp getInstance() {
        return mInstance;
    }
}
