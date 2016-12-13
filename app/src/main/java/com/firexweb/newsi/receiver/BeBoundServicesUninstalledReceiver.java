package com.firexweb.newsi.receiver;


import android.content.Context;
import android.content.SharedPreferences;

import com.bebound.sdk.receiver.BeBoundServicesStateChangedReceiver;
import com.firexweb.newsi.BaseActivity;


/**
 * Created by MINH NGUYEN on 12/1/2016.
 */

public class BeBoundServicesUninstalledReceiver extends BeBoundServicesStateChangedReceiver {
    public final static String KEY_SERVICES_UNINSTALLED = "service_uninstall";
    SharedPreferences pref;
    @Override
    public void onBeBoundServicesInstalled(Context context) {
        //pref = context.getSharedPreferences(BaseActivity.PREFERENCES_APP, Context.MODE_PRIVATE);
        saveStatus(context, KEY_SERVICES_UNINSTALLED, false);
    }

    @Override
    public void onBeBoundServicesUninstalled(Context context) {
        //pref = context.getSharedPreferences(BaseActivity.PREFERENCES_APP, Context.MODE_PRIVATE);
        saveStatus(context, KEY_SERVICES_UNINSTALLED, true);
    }

    @Override
    public void onBeBoundServicesUpdated(Context context) {
        //pref = context.getSharedPreferences(BaseActivity.PREFERENCES_APP, Context.MODE_PRIVATE);
    }
    private void saveStatus(Context context, String key, boolean status){
        pref = context.getSharedPreferences(BaseActivity.PREFERENCES_APP, Context.MODE_PRIVATE);
        pref.edit().putBoolean(key, status).apply();
    }
}
