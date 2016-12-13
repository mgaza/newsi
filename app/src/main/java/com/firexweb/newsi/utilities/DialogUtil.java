package com.firexweb.newsi.utilities;

import android.app.Activity;
import android.support.v7.app.AlertDialog;

/**
 * Created by MINH NGUYEN on 12/1/2016.
 */

public class DialogUtil {

    /**
     *
     * @param activity activity
     * @param resourceStyle AppCompatAlertDialogStyle in style xml
     * @return alert dialog builder
     */
    public static AlertDialog.Builder getAlertBuilderMaterial(Activity activity, int resourceStyle){
        return new AlertDialog.Builder(activity, resourceStyle);
    }
}
