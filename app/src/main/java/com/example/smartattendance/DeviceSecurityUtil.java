package com.example.smartattendance;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;

public class DeviceSecurityUtil {

    @SuppressLint("HardwareIds")
    public static String getDeviceId(Context context) {
        if (context == null) return "UNKNOWN_DEVICE";
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return (androidId != null && !androidId.isEmpty()) ? androidId : "EMULATOR_DEVICE";
    }
}