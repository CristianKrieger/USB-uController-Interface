package com.kriegersoftware.demos.usbinterface.application;

import android.app.Application;

import com.testflightapp.lib.TestFlight;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        TestFlight.takeOff(this, "075819ff-cb0c-4ae0-815a-eb0476422397");
    }
}