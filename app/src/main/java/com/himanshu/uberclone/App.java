package com.himanshu.uberclone;

import android.app.Application;

import com.parse.Parse;

public class App extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();

        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId("phlBdp3XpUnCPSSCMeUb2dU4ldWfKOO9wpBEY4sw")
                // if defined
                .clientKey("TLNhZcSHtkV9ldnUzsl9b6zqLSxJM1KkjiAQLr6g")
                .server("https://parseapi.back4app.com/")
                .build()
        );
    }
}
