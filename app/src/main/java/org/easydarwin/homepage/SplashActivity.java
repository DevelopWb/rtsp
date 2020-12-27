/*
	Copyright (c) 2013-2016 EasyDarwin.ORG.  All rights reserved.
	Github: https://github.com/EasyDarwin
	WEChat: EasyDarwin
	Website: http://www.easydarwin.org
*/
package org.easydarwin.homepage;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.view.WindowManager;
import android.widget.TextView;

import com.basenetlib.util.NetWorkUtil;
import com.juntai.wisdom.basecomponent.utils.ActivityManagerTool;

import org.easydarwin.BaseProjectActivity;
import org.easydarwin.easypusher.R;

/**
 * 启动页
 * */
public class SplashActivity extends BaseProjectActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); //隐藏状态栏
        if (!NetWorkUtil.isNetworkAvailable()) {
            new AlertDialog.Builder(mContext)
                    .setCancelable(false)
                    .setMessage("网络连接异常，请检查手机网络或系统时间！")
                    .setPositiveButton("知道了", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityManagerTool.getInstance().finishApp();
                        }
                    }).show();
            return;
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashActivity.this, StreamActivity.class));
                SplashActivity.this.finish();
            }
        }, 2000);

        String versionName;

        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = getResources().getString(R.string.version);
        }

        TextView txtVersion = (TextView) findViewById(R.id.txt_version);
        txtVersion.setText(String.format("v%s", versionName));
    }
}
