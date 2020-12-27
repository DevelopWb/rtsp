package org.easydarwin;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.juntai.wisdom.basecomponent.utils.ActivityManagerTool;
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

/**
 * @Author: tobato
 * @Description: 作用描述
 * @CreateDate: 2020/5/5 20:47
 * @UpdateUser: 更新者
 * @UpdateDate: 2020/5/5 20:47
 */
public abstract class BaseProjectActivity extends RxAppCompatActivity {


    protected Context mContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        ActivityManagerTool.getInstance().addActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mContext = null;
        ActivityManagerTool.getInstance().removeActivity(this);
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
