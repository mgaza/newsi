package com.firexweb.newsi.sync;

import android.app.Application;

import com.bebound.common.model.BeAppConfig;
import com.bebound.sdk.BeBound;
import com.firexweb.newsi.R;

public class Newsi extends Application {
    private static final String AUTH_FAILED = "Sorry, We Have Problem with Authentication!";
    private static final String REQUEST_FAILED = "Sorry, request faild to be sent!";

    @Override
    public void onCreate() {
        super.onCreate();
        BeBound.init(this, new BeAppConfig() {
            @Override
            public String getBeAppKey() {
                return "dy9eiDKyuqa42EhnFTrquyXQVPjJ5G7FTs3g7SVeKFFcBhcO8vk14Q==";
            }

            @Override
            public long getBeAppId() {
                return 127; // app id
            }

            @Override
            public int getBeAppManifestResId() {
                return R.xml.newsi;
            }
        });
    }

}
