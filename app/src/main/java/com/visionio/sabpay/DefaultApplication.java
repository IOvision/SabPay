package com.visionio.sabpay;

import android.app.Application;

import io.paperdb.Paper;

public class DefaultApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Paper.init(getApplicationContext());

    }

}
