/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package de.dennisguse.opentracks.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import androidx.core.content.pm.PackageInfoCompat;

import java.lang.reflect.Method;

import de.dennisguse.opentracks.BuildConfig;

/**
 * Utility class for accessing basic Android functionality.
 *
 * @author Rodrigo Damazio
 */
public class SystemUtils {

    private static final String TAG = SystemUtils.class.getSimpleName();

    private SystemUtils() {
    }

    /**
     * Get the app version from the manifest.
     *
     * @return the version, or an empty string in case of failure.
     */
    public static String getAppVersionName(Context context) {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(BuildConfig.APPLICATION_ID, PackageManager.GET_META_DATA);
            return pi.versionName + "/" + BuildConfig.VERSION_NAME_FULL;
        } catch (NameNotFoundException e) {
            Log.w(TAG, "Failed to get version info.", e);
            return "";
        }
    }

    /**
     * Check if the device vendor is xiaomi
     * @return true or false
     */

    public static boolean isVendorXiaomi(Context context) {
        Log.d(TAG, Build.MANUFACTURER);
        if (Build.MANUFACTURER.equals("Xiaomi")) {
            return Settings.Global.getInt(context.getContentResolver(), "force_fsg_nav_bar", 0) != 0;
        }
        return false;
    }

    public static int getNavBarHeight(Context context)  {
        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();

        try {
            @SuppressWarnings("rawtypes")
                    Class c = Class.forName("android.view.Display");
            @SuppressWarnings("unchecked")
            Method method = c.getMethod("getRealMetrics", DisplayMetrics.class);
            method.invoke(display, dm);
            return dm.heightPixels - display.getHeight();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static Long getAppVersionCode(Context context) {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(BuildConfig.APPLICATION_ID, PackageManager.GET_META_DATA);
            return PackageInfoCompat.getLongVersionCode(pi);
        } catch (NameNotFoundException e) {
            Log.w(TAG, "Failed to get version info.", e);
            return -1L;
        }
    }

    /**
     * Acquire a wake lock if not already acquired.
     *
     * @param context  the context
     * @param wakeLock wake lock or null
     */
    @SuppressLint("WakelockTimeout")
    public static WakeLock acquireWakeLock(Context context, WakeLock wakeLock) {
        Log.i(TAG, "Acquiring wake lock.");
        try {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerManager == null) {
                Log.e(TAG, "Power manager null.");
                return wakeLock;
            }
            if (wakeLock == null) {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
                if (wakeLock == null) {
                    Log.e(TAG, "Cannot create a new wake lock.");
                    return null;
                }
            }
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
                if (!wakeLock.isHeld()) {
                    Log.e(TAG, "Cannot acquire wake lock.");
                }
            }
        } catch (RuntimeException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return wakeLock;
    }

    /**
     * Releases the wake lock if it is held.
     *
     * @return null
     */
    public static WakeLock releaseWakeLock(WakeLock wakeLock) {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        return null;
    }
}