package org.easydarwin;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.juntai.wisdom.basecomponent.utils.ActivityManagerTool;
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import org.easydarwin.push.UVCCameraService;
import org.easydarwin.util.SPUtil;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * @Author: tobato
 * @Description: 作用描述
 * @CreateDate: 2020/5/5 20:47
 * @UpdateUser: 更新者
 * @UpdateDate: 2020/5/5 20:47
 */
public abstract class BaseProjectActivity extends RxAppCompatActivity {

    public abstract void onUvcCameraConnected();

    public abstract void onUvcCameraAttached();

    public abstract void onUvcCameraDisConnected();
    protected Context mContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        ActivityManagerTool.getInstance().addActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mContext = null;
        EventBus.getDefault().unregister(this);
        ActivityManagerTool.getInstance().removeActivity(this);
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receivedStringMsg(String msg) {
        switch (msg) {
            case "onAttach":
                //                Toast.makeText(getApplicationContext(),"Attached",Toast.LENGTH_SHORT).show();
                onUvcCameraAttached();
                Log.d(UVCCameraService.TAG, "BaseProjectActivity======onAttach:");
                break;
            case "onConnect":
                if (!UVCCameraService.uvcConnected) {
                   return;
                }
                Log.d(UVCCameraService.TAG, "BaseProjectActivity======onConnect+++:"+UVCCameraService.uvcConnected);

                //                Toast.makeText(getApplicationContext(),"connect",Toast.LENGTH_SHORT).show();
                SPUtil.setBitrateKbps(this,5000000);
                onUvcCameraConnected();
                break;
            case "onDisconnect":
                Log.d(UVCCameraService.TAG, "BaseProjectActivity======onDisconnect+++:"+UVCCameraService.uvcConnected);
                SPUtil.setBitrateKbps(this,SPUtil.BITRATEKBPS);
                //                Toast.makeText(getApplicationContext(),"disconnect",Toast.LENGTH_SHORT).show();

                onUvcCameraDisConnected();
                break;
            default:
                break;
        }
    }

    /**
     * 隐藏控件  Invisible  gone
     *
     * @param isGone gone
     * @param views
     */
    protected void setViewsInvisible(boolean isGone, View... views) {
        if (views != null && views.length > 0) {
            for (View view : views) {
                if (view != null) {
                    if (isGone) {
                        view.setVisibility(View.GONE);
                    } else {
                        view.setVisibility(View.INVISIBLE);
                    }
                }
            }
        }
    }

    /**
     * 显示控件  Invisible  gone
     *
     * @param views
     */
    protected void setViewsVisible(View... views) {
        if (views != null && views.length > 0) {
            for (View view : views) {
                if (view != null) {
                    view.setVisibility(View.VISIBLE);
                }
            }
        }
    }
}
