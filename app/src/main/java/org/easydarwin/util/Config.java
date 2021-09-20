/*
	Copyright (c) 2013-2016 EasyDarwin.ORG.  All rights reserved.
	Github: https://github.com/EasyDarwin
	WEChat: EasyDarwin
	Website: http://www.easydarwin.org
*/

package org.easydarwin.util;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.orhanobut.hawk.Hawk;

/**
 * 推流地址的常量类
 */
public class Config {

    public static UsbDevice usbDevice = null;

    public static String getServerURL(Context context) {
        return String.format("rtsp://%s:%s/%s",getIp(context),getPort(context),(String) Hawk.get(HawkProperty.REG_CODE));
    }


    public static String getIp(Context context) {
        return SPUtil.getScreenPushingIP(context);
    }

    public static String getPort(Context context) {
        return SPUtil.getScreenPushingPort(context);
    }

    public static String getId(Context context) {
        return (String) Hawk.get(HawkProperty.REG_CODE);
    }

    public static String recordPath() {
        return Environment.getExternalStorageDirectory() +"/智能取证 S";
    }
}
