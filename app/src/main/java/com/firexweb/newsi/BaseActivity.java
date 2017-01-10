package com.firexweb.newsi;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bebound.common.api.user.User;
import com.bebound.common.exception.BeBoundException;
import com.bebound.sdk.BeBound;
import com.firexweb.newsi.receiver.BeBoundServicesUninstalledReceiver;
import com.firexweb.newsi.utilities.BeBoundWebService;
import com.firexweb.newsi.utilities.DialogUtil;
import com.firexweb.newsi.utilities.StoreConfig;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class BaseActivity extends AppCompatActivity {
    protected final String TAG = getClass().getSimpleName();

    public static final String PREFERENCES_APP = "AppUiPreferences";

    private static final String KEY_STORE_NAME = "storeName";
    private static final String KEY_STORE_PACKAGE = "storePackage";
    private static final String KEY_STORE_ICON_PATH = "storeIconPath";

    private static final String STORE_REDIRECTION_URL = "https://web.be-bound.com/mob/store.php";
    private static final String STORE_GET_URL = "https://web.be-bound.com/mob/store.php?v=2";
    /** Attached activity */

    /**
     * State variable to remember if the user was authenticating when he left the Activity
     */
    private boolean isUserAuthenticating = false;
    /**
     * Should the onResume method skip BeBound availability check, be very careful with this variable !!
     */
    private boolean overrideChecking = false;
    /**
     * Is Be-Bound services installed and is user authenticated, this variable is set during the activity resume phase
     */
    private boolean areBeBoundServicesReady = false;

    /**
     * Currently displayed dialog
     */
    private AlertDialog dialog = null;
    private View mDialogView = null;
    private boolean showingNoBeStoreDialog = false;

    /**
     * Store fetching call
     */
    private Call<StoreConfig> storeCall = null;

    //private Realm mRealm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overrideChecking = false;
        areBeBoundServicesReady = false;
        //mRealm = Realm.getDefaultInstance();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean beBoundServicesFound = BeBound.areBeBoundServicesFound();
        final SharedPreferences pref = getSharedPreferences(PREFERENCES_APP, Context.MODE_PRIVATE);
        if (pref.getBoolean(BeBoundServicesUninstalledReceiver.KEY_SERVICES_UNINSTALLED, false)) {
            // && beAppActivity.clearOnBeBoundServicesTokenChanged()) {
            showOnBeBoundServicesTokenChangedDialog(beBoundServicesFound);
            return;
        }
        pref.edit().putBoolean(BeBoundServicesUninstalledReceiver.KEY_SERVICES_UNINSTALLED, false).apply();

        if (!hasStoreInfo(pref) && (storeCall == null || !storeCall.isExecuted())) {
            Log.d(TAG, "Doesn't have store info, fetching...");
            BeBoundWebService service = new Retrofit.Builder()
                    .baseUrl(BeBoundWebService.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(BeBoundWebService.class);

            TelephonyManager tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            storeCall = service.getStore("2", tel.getNetworkOperator());
            storeCall.enqueue(new Callback<StoreConfig>() {
                @Override
                public void onResponse(Call<StoreConfig> call, Response<StoreConfig> response) {
                    StoreConfig.BeStore store = response.body().bestore;
                    Log.d(TAG, "Got store info " + store.name + " " + store.packageName);
                    pref.edit()
                            .putString(KEY_STORE_NAME, store.name)
                            .putString(KEY_STORE_PACKAGE, store.packageName)
                            .putString(KEY_STORE_ICON_PATH, store.logo)
                            .apply();

                    if (showingNoBeStoreDialog) {
                        Log.d(TAG, "Updating existing dialog");
                        updateNoBeStoreDialog(pref, true);
                    }
                    storeCall = null;
                }

                @Override
                public void onFailure(Call<StoreConfig> call, Throwable t) {
                    storeCall = null;
                }
            });
        }

        areBeBoundServicesReady = false;
        if (!overrideChecking) {
            if (!beBoundServicesFound) {
                Log.e(TAG, "No Be-Bound services found");
                showNoBeBoundServicesDialog(pref);
                return;
            }
            //Log.v(TAG, "Be-Store variant available : " + BeBound.getBeBoundServicesPackageName());

            User user = BeBound.getActiveUser();
            if (!(user != null && (user.isAuthenticated() || user.isSubscribed()))) {
                onUserNotAuthenticated();
                return;
            }
            Log.v(TAG, "User is authenticated");

            areBeBoundServicesReady = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //mRealm.close();
    }

    public void onUserNotAuthenticated() {
        if (isUserAuthenticating) {
            Log.e(TAG, "User cancelled authentication, finishing activity");
            finish();
            return;
        }

        Log.d(TAG, "User is not authenticated, attempting authentication");
        try {
            isUserAuthenticating = true;
            BeBound.initiateUserAuthentication();
        } catch (BeBoundException e) {
            Log.e(TAG, "Error during mapping");
            e.printStackTrace();
            finish();
        }
    }

    /**
     * No Be-Bound services installed dialog
     *
     * @param pref
     */
    protected void showNoBeBoundServicesDialog(final SharedPreferences pref) {
        if (dialog != null && dialog.isShowing()) {
            Log.e(TAG, "showNoBeBoundServicesDialog(): Already displaying a dialog, this should not happen !");
            dialog.dismiss();
            dialog = null;
            mDialogView = null;
        }

        boolean hasStoreInfo = hasStoreInfo(pref);
        if (mDialogView == null) {
            mDialogView = LayoutInflater.from(this).inflate(R.layout.dialog_no_store, null);
        }
        dialog = DialogUtil.getAlertBuilderMaterial(this, R.style.AppCompatAlertDialogStyle)
                .setView(mDialogView)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                })
                .create();
        //.build();
       /* dialog = new MaterialDialog.Builder(activity)
                .customView(R.layout.dialog_no_store, false)
                .cancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                })
                .build();*/
        updateNoBeStoreDialog(pref, hasStoreInfo);

        showingNoBeStoreDialog = true;
        dialog.show();
    }

    private void updateNoBeStoreDialog(final SharedPreferences pref, boolean hasStoreInfo) {
        if (dialog == null)
            return;
        if (mDialogView == null) {
            mDialogView = LayoutInflater.from(this).inflate(R.layout.dialog_no_store, null);
        }
        //View view = dialog.get;
//        if (view == null)
//            return;

        String title = getNoBeStoreDialogTitle(pref, hasStoreInfo);
        String message = getNoBeStoreDialogContent(pref, hasStoreInfo);
        Bitmap beStore = getNoBeStoreDialogIcon(pref, hasStoreInfo);
        Drawable beApp = null;
        try {
            beApp = getPackageManager().getApplicationIcon(getPackageName());
        } catch (PackageManager.NameNotFoundException e) {
        }

        TextView lblTitle = (TextView) mDialogView.findViewById(R.id.lbl_title);
        TextView lblContent = (TextView) mDialogView.findViewById(R.id.lbl_message);
        ImageView icBeApp = (ImageView) mDialogView.findViewById(R.id.ic_beapp);
        ImageView icBeStore = (ImageView) mDialogView.findViewById(R.id.ic_bestore);
        Button btDownload = (Button) mDialogView.findViewById(R.id.bt_download);
        //btDownload.setBackgroundColor(appConfig.getPrimaryColor());
        if (btDownload != null) {
            btDownload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!hasStoreInfo(pref)) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            TelephonyManager tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                            String networkOperator = tel.getNetworkOperator();
                            intent.setData(
                                    Uri.parse(STORE_REDIRECTION_URL + (networkOperator != null ? ("?m=" + networkOperator) : "")));
                            startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            //makeSnack(R.string.snack_application_not_found).show();
                        }
                    } else {
                        openPlayStore(pref.getString(KEY_STORE_PACKAGE, null));
                    }
                }
            });
        }
        lblTitle.setText(title);
        lblContent.setText(message);
        if (!hasStoreInfo) {
            mDialogView.findViewById(R.id.layout_store).setVisibility(View.GONE);
        } else {
            if (beApp != null) {
                icBeApp.setImageDrawable(beApp);
            }
            if (beStore != null) {
                icBeStore.setImageBitmap(beStore);
            }
            mDialogView.findViewById(R.id.layout_store).setVisibility(View.VISIBLE);
        }
    }

    private void showOnBeBoundServicesTokenChangedDialog(boolean beBoundServicesFound) {
        if (beBoundServicesFound) {
            DialogUtil.getAlertBuilderMaterial(this, R.style.AppCompatAlertDialogStyle)
                    .setTitle(R.string.dialog_services_uninstalled_title)
                    .setMessage(R.string.dialog_services_uninstalled_content)
                    .setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //BeAppUtils.clearData(activity);
                            finish();
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            //BeAppUtils.clearData(activity);
                            finish();
                        }
                    }).show();
//            DialogUtils.newDialog(this)
//                    .title(R.string.dialog_services_uninstalled_title)
//                    .content(R.string.dialog_services_uninstalled_content)
//                    .positiveText(R.string.dialog_ok)
//                    .cancelListener(new DialogInterface.OnCancelListener() {
//                        @Override
//                        public void onCancel(DialogInterface dialog) {
//                            //BeAppUtils.clearData(activity);
//                            finish();
//                        }
//                    })
//                    .onPositive(new MaterialDialog.SingleButtonCallback() {
//                        @Override
//                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
//                            //BeAppUtils.clearData(activity);
//                            finish();
//                        }
//                    }).show();
        } else {
            DialogUtil.getAlertBuilderMaterial(this, R.style.AppCompatAlertDialogStyle)
                    .setTitle(R.string.dialog_services_uninstalled_title)
                    .setMessage(R.string.dialog_services_uninstalled_content)
                    .setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //BeAppUtils.clearData(activity);
                            finish();
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            //BeAppUtils.clearData(activity);
                            finish();
                        }
                    }).show();
//            DialogUtils.newDialog(this)
//                    .title(R.string.dialog_services_uninstalled_title)
//                    .content(R.string.dialog_services_uninstalled_content)
//                    .positiveText(R.string.dialog_ok)
//                    .cancelListener(new DialogInterface.OnCancelListener() {
//                        @Override
//                        public void onCancel(DialogInterface dialog) {
//                            //BeAppUtils.clearData(activity);
//                            finish();
//                        }
//                    })
//                    .onPositive(new MaterialDialog.SingleButtonCallback() {
//                        @Override
//                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
//                            //BeAppUtils.clearData(activity);
//                            finish();
//                        }
//                    }).show();
        }
    }

    public final void openPlayStore(String packageName) {
        try {
            this.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("market://details?id=" + packageName)));
        } catch (ActivityNotFoundException var5) {
            try {
                this.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://play.google.com/store/apps/details?id=" +
                        packageName)));
            } catch (ActivityNotFoundException var4) {

            }
        }

    }

    @Nullable
    private Bitmap getNoBeStoreDialogIcon(SharedPreferences pref, boolean hasStoreInfo) {
        if (hasStoreInfo) {
            byte[] imageAsBytes = Base64.decode(pref.getString(KEY_STORE_ICON_PATH, "").getBytes(), Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
        }
        return null;
    }

    @NonNull
    private String getNoBeStoreDialogContent(SharedPreferences pref, boolean hasStoreInfo) {
        return hasStoreInfo ? getString(R.string.dialog_no_bestore_content_specific, getString(R.string.app_name), pref.getString
                (KEY_STORE_NAME, null))
                : getString(R.string.dialog_no_bestore_content, getString(R.string.app_name));
    }

    @NonNull
    private String getNoBeStoreDialogTitle(SharedPreferences pref, boolean hasStoreInfo) {
        return hasStoreInfo ? getString(R.string.dialog_no_bestore_title_specific,
                pref.getString(KEY_STORE_NAME, null))
                : getString(R.string.dialog_no_bestore_title);
    }

    private void dismissDialog() {
        // Hide dialog if needed
        if (dialog != null && dialog.isShowing()) {
            Log.e(TAG, "Hiding dialog to stop activity");
            dialog.dismiss();
            dialog = null;
            mDialogView = null;
        }
    }

    private boolean hasStoreInfo(SharedPreferences pref) {
        return pref.contains(KEY_STORE_NAME) && pref.contains(KEY_STORE_PACKAGE) && pref.contains(KEY_STORE_ICON_PATH);
    }

//    public Realm getRealm() {
//        return mRealm;
//    }
    /*    public AppConfig getAppConfig() {
        return appConfig;
    }*/
}
