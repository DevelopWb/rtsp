package org.easydarwin.mine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.orhanobut.hawk.Hawk;

import org.easydarwin.homepage.SplashActivity;
import org.easydarwin.util.HawkProperty;


/**
 * @Author: tobato
 * @Description: 作用描述
 * @CreateDate: 2020/3/27 20:11
 * @UpdateUser: 更新者
 * @UpdateDate: 2020/3/27 20:11
 */

public class MyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
         boolean autoRun =    Hawk.get(HawkProperty.AUTO_RUN,true);
            if (autoRun) {
                Intent i = new Intent(context, SplashActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            }

        }
    }
}
