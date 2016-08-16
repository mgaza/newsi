package com.firexweb.newsi.sync;

import android.util.Log;

import com.bebound.common.C;
import com.bebound.common.api.connectivity.ConnectivityApi;
import com.bebound.sdk.BeBound;
import com.bebound.sdk.application.BeBoundApplicationDelegate;
import com.bebound.sdk.event.application.authentication.BeAppAuthEventBus;
import com.bebound.sdk.listener.BeAppAuthenticatedListener;
import com.firexweb.newsi.MainSystem;


public class Auth {
    /*
            Finals
     */
    public final static int MODE_AIRPLANE = 0;
    public final static int MODE_DATA = 1;
    public final static int MODE_GSM = 2; // sms
    public final static int MODE_OFFLINE = 3;
    public final static int MODE_WIFI = 4;
    private static boolean AUTH_STATUS = false;
    private static String LOG_AUTH = "BeBoundAuth";

    public static boolean checkAuth() {
        if (BeBound.isBeAppAuthenticated()) {
            AUTH_STATUS = true;
            Log.d(LOG_AUTH, "BeBound is checked and authenticated!");
        } else {
            BeAppAuthEventBus.getInstance().register(new BeAppAuthenticatedListener() {

                @Override
                public void onBeAppAuthenticated() {
                    AUTH_STATUS = true;
                    Log.d(LOG_AUTH, "BeBound is checked and authenticated!");
                    MainSystem.fetchNewsOperation();
                }

                @Override
                public void onSessionExpired() {

                }
            });
        }
        return AUTH_STATUS;
    }

    public static int getMode() {
        int connectivity = ConnectivityApi.getCurrentConnectivity(BeBoundApplicationDelegate.getApplicationInstance());
        switch (connectivity) {
            case C.Connectivity.AIRPLANE:
                return MODE_AIRPLANE;
            case C.Connectivity.DATA:
                return MODE_DATA;
            case C.Connectivity.GSM:
                return MODE_GSM;
            case C.Connectivity.OFFLINE:
                return MODE_OFFLINE;
            case C.Connectivity.WIFI:
                return MODE_WIFI;
            default:
                return -1;
        }
    }


}
