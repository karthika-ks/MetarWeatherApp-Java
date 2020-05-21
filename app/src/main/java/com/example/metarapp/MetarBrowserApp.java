package com.example.metarapp;

import android.app.Application;
import android.content.res.Resources;

import com.example.metarapp.utilities.NetworkUtil;

import java.io.IOException;

public class MetarBrowserApp extends Application {
    private static MetarBrowserApp mInstance;
    private static Resources mResources;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        mResources = getResources();
        MetarDataManager.getInstance();
    }

    public static synchronized MetarBrowserApp getInstance() {
        return mInstance;
    }
    public static Resources getResourceses() {
        return mResources;
    }
}
