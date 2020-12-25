/*
	Copyright (c) 2013-2016 EasyDarwin.ORG.  All rights reserved.
	Github: https://github.com/EasyDarwin
	WEChat: EasyDarwin
	Website: http://www.easydarwin.org
*/

package org.easydarwin.util;

import android.content.Context;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.orhanobut.hawk.Hawk;

/**
 * 推流地址的常量类
 */
public class Config {

    private static final String SERVER_URL = "serverUrl";
    //    private static final String DEFAULT_SERVER_URL = "rtsp://cloud.easydarwin.org:554/" + String.valueOf((int) (Math.random() * 1000000 + 100000));
    private static final String DEFAULT_SERVER_URL = "rtsp://58.49.46.179:554/X6fIDGY9";

    public static String getServerURL(Context context) {
        return String.format("rtsp://%s:%s/%s",getIp(context),getPort(context),(String) Hawk.get(HawkProperty.REG_CODE,"X6fIDGY9"));
    }

    public static void setServerURL(Context context, String value) {
        if (value == null || TextUtils.isEmpty(value)) {
            value = DEFAULT_SERVER_URL;
        }

        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(SERVER_URL, value)
                .apply();
    }

    public static String getIp(Context context) {
        return SPUtil.getScreenPushingIP(context);
    }

    public static String getPort(Context context) {
        return SPUtil.getScreenPushingPort(context);
    }

    public static String getId(Context context) {
        return (String) Hawk.get(HawkProperty.REG_CODE,"X6fIDGY9");
    }

    public static String recordPath() {
        return Environment.getExternalStorageDirectory() +"/取证终端";
    }
}
