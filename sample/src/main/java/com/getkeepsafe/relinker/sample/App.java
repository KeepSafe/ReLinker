package com.getkeepsafe.relinker.sample;

import android.app.Application;
import android.content.Context;

public class App extends Application {
    public static Context sContext;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = this;
    }
}
