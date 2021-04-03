/*
	Copyright (c) 2013-2016 EasyDarwin.ORG.  All rights reserved.
	Github: https://github.com/EasyDarwin
	WEChat: EasyDarwin
	Website: http://www.easydarwin.org
*/
package org.easydarwin.homepage;

import android.Manifest;
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
import com.regmode.RegLatestContact;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.trello.rxlifecycle2.android.ActivityEvent;

import org.easydarwin.BaseProjectActivity;
import org.easydarwin.easypusher.R;

import java.util.concurrent.TimeUnit;

import io.reactivex.functions.Consumer;

/**
 * 启动页
 * */
public class SplashActivity extends BaseProjectActivity {
    String[] permissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    @Override
    public void onUvcCameraConnected() {

    }

    @Override
    public void onUvcCameraAttached() {

    }

    @Override
    public void onUvcCameraDisConnected() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); //隐藏状态栏
//        if (!NetWorkUtil.isNetworkAvailable()) {
//            new AlertDialog.Builder(mContext)
//                    .setCancelable(false)
//                    .setMessage("网络连接异常，请检查手机网络或系统时间！")
//                    .setPositiveButton("知道了", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            ActivityManagerTool.getInstance().finishApp();
//                        }
//                    }).show();
//            return;
//        }

        new RxPermissions(this)
                .request(permissions)
                .delay(1, TimeUnit.SECONDS)
                .compose(this.bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            startActivity(new Intent(SplashActivity.this, StreamActivity.class));
                           finish();

                        } else {
                            //有一个权限没通过
                            finish();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                    }
                });


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
