/*
	Copyright (c) 2013-2016 EasyDarwin.ORG.  All rights reserved.
	Github: https://github.com/EasyDarwin
	WEChat: EasyDarwin
	Website: http://www.easydarwin.org
*/

package org.easydarwin.mine;

import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.orhanobut.hawk.Hawk;

import org.easydarwin.easypusher.BuildConfig;
import org.easydarwin.easypusher.R;
import org.easydarwin.easypusher.databinding.ActivitySettingBinding;
import org.easydarwin.util.Config;
import org.easydarwin.util.HawkProperty;
import org.easydarwin.util.SPUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 设置页
 * */
public class SettingActivity extends AppCompatActivity implements Toolbar.OnMenuItemClickListener,View.OnClickListener {

    public static final int REQUEST_OVERLAY_PERMISSION = 1004;  // 悬浮框
    private static final int REQUEST_SCAN_TEXT_URL = 1003;      // 扫描二维码

    private ActivitySettingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_setting);

        setSupportActionBar(binding.mainToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.mainToolbar.setOnMenuItemClickListener(this);
        // 左边的小箭头（注意需要在setSupportActionBar(toolbar)之后才有效果）
        binding.mainToolbar.setNavigationIcon(R.drawable.com_back);

        binding.registCodeValue.setText((String) Hawk.get(HawkProperty.REG_CODE));
        binding.pushServerIpEt.setText(SPUtil.getScreenPushingIP(this));
        binding.pushServerPortEt.setText(SPUtil.getScreenPushingPort(this));
        binding.quitAppBt.setOnClickListener(this);
        binding.autoPushWhenRunCb.setChecked(Hawk.get(HawkProperty.AUTO_RUN,true));
        // 使能摄像头后台采集
        CheckBox backgroundPushing = (CheckBox) findViewById(R.id.enable_background_camera_pushing);
        backgroundPushing.setChecked(SPUtil.getEnableBackgroundCamera(this));
        backgroundPushing.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (Settings.canDrawOverlays(SettingActivity.this)) {
                            SPUtil.setEnableBackgroundCamera(SettingActivity.this, true);
                        } else {
                            new AlertDialog
                                    .Builder(SettingActivity.this)
                                    .setTitle("后台上传视频")
                                    .setMessage("后台上传视频需要APP出现在顶部.是否确定?")
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            // 在Android 6.0后，Android需要动态获取权限，若没有权限，提示获取.
                                            final Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                                            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
                                        }
                                    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    SPUtil.setEnableBackgroundCamera(SettingActivity.this, false);
                                    buttonView.toggle();
                                }
                            })
                                    .setCancelable(false)
                                    .show();
                        }
                    } else {
                        SPUtil.setEnableBackgroundCamera(SettingActivity.this, true);
                    }
                } else {
                    SPUtil.setEnableBackgroundCamera(SettingActivity.this, false);
                }
            }
        });

        //        // 是否使用软编码
        //        CheckBox x264enc = findViewById(R.id.use_x264_encode);
        //        x264enc.setChecked(SPUtil.getswCodec(this));
        //        x264enc.setOnCheckedChangeListener(
        //                (buttonView, isChecked) -> SPUtil.setswCodec(this, isChecked)
        //        );

        //        // 叠加水印
        //        CheckBox enable_video_overlay = findViewById(R.id.enable_video_overlay);
        //        enable_video_overlay.setChecked(SPUtil.getEnableVideoOverlay(this));
        //        enable_video_overlay.setOnCheckedChangeListener(
        //                (buttonView, isChecked) -> SPUtil.setEnableVideoOverlay(this, isChecked)
        //        );

        // 推送内容
        RadioGroup push_content = findViewById(R.id.push_content);

        boolean videoEnable = SPUtil.getEnableVideo(this);
        if (videoEnable) {
            boolean audioEnable = SPUtil.getEnableAudio(this);

            if (audioEnable) {
                RadioButton push_av = findViewById(R.id.push_av);
                push_av.setChecked(true);
            } else {
                RadioButton push_v = findViewById(R.id.push_v);
                push_v.setChecked(true);
            }
        } else {
            RadioButton push_a = findViewById(R.id.push_a);
            push_a.setChecked(true);
        }

        push_content.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.push_av) {
                    SPUtil.setEnableVideo(SettingActivity.this, true);
                    SPUtil.setEnableAudio(SettingActivity.this, true);
                } else if (checkedId == R.id.push_a) {
                    SPUtil.setEnableVideo(SettingActivity.this, false);
                    SPUtil.setEnableAudio(SettingActivity.this, true);
                } else if (checkedId == R.id.push_v) {
                    SPUtil.setEnableVideo(SettingActivity.this, true);
                    SPUtil.setEnableAudio(SettingActivity.this, false);
                }
            }
        });
        onAutoRun();
    }
    /**
     * 自启动
     */
    private void onAutoRun() {
        //        initBitrateData();
        binding.autoPushWhenRunCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    Hawk.put(HawkProperty.AUTO_RUN, true);
                    AlertDialog.Builder builder = new AlertDialog.Builder(SettingActivity.this);
                    builder.setMessage("开启后需要手动开启软件自启动权限").setPositiveButton("知道了", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
                    builder.create().show();
                } else {
                    Hawk.put(HawkProperty.AUTO_RUN, false);
                }
            }
        });
    }
    @Override
    protected void onPause() {
        super.onPause();

        String text = binding.pushServerIpEt.getText().toString().trim();
        if (isIP(text)) {
            SPUtil.setScreenPushingIP(this, text);
        }

        String textPort = binding.pushServerPortEt.getText().toString().trim();
        SPUtil.setScreenPushingPort(this, textPort);


    }


    /*
     * 二维码扫码
     * */
    public void onScanQRCode(View view) {
        Intent intent = new Intent(this, ScanQRActivity.class);
        startActivityForResult(intent, REQUEST_SCAN_TEXT_URL);
        overridePendingTransition(R.anim.slide_bottom_in, R.anim.slide_top_out);
    }

    /*
     * 本地录像
     * */
    public void onOpenLocalRecord(View view) {
        Intent intent = new Intent(this, MediaFilesActivity.class);
        startActivityForResult(intent, 0);
        overridePendingTransition(R.anim.slide_right_in, R.anim.slide_left_out);
    }

    /*
     * 屏幕分辨率
     * */
    public void onScreenPushResolution(View view) {
        int defaultIdx = SPUtil.getScreenPushingResIndex(this);
        new AlertDialog.Builder(this).setTitle("推送屏幕分辨率").setSingleChoiceItems(
                new CharSequence[]{"1倍屏幕大小", "0.75倍屏幕大小", "0.5倍屏幕大小", "0.3倍屏幕大小", "0.25倍屏幕大小", "0.2倍屏幕大小"}, defaultIdx, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SPUtil.setScreenPushingResIndex(SettingActivity.this, which);

                        Toast.makeText(SettingActivity.this,"配置更改将在下次启动屏幕推送时生效", Toast.LENGTH_SHORT).show();

                        dialog.dismiss();
                    }
                }).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean canDraw = Settings.canDrawOverlays(this);
                SPUtil.setEnableBackgroundCamera(SettingActivity.this, canDraw);

                if (!canDraw) {
                    CheckBox backgroundPushing = (CheckBox) findViewById(R.id.enable_background_camera_pushing);
                    backgroundPushing.setChecked(false);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        return false;
    }

    // 返回的功能
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public boolean isIP(String addr) {
        if (addr.length() < 7 || addr.length() > 15 || "".equals(addr)) {
            return false;
        }
        /**
         * 判断IP格式和范围
         */
        String rexp = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";

        Pattern pat = Pattern.compile(rexp);

        Matcher mat = pat.matcher(addr);

        boolean ipAddress = mat.find();

        //============对之前的ip判断的bug在进行判断
        if (ipAddress == true) {
            String ips[] = addr.split("\\.");

            if (ips.length == 4) {
                try {
                    for (String ip : ips) {
                        if (Integer.parseInt(ip) < 0 || Integer.parseInt(ip) > 255) {
                            return false;
                        }

                    }
                } catch (Exception e) {
                    return false;
                }

                return true;
            } else {
                return false;
            }
        }

        return ipAddress;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.quit_app_bt:
                //                new AlertDialog.Builder(this).setCancelable(false)
                //                        .setTitle("是否退出App")
                //                        .setNegativeButton("否", new DialogInterface.OnClickListener() {
                //                            @Override
                //                            public void onClick(DialogInterface dialog, int which) {
                //                                dialog.dismiss();
                //
                //                            }
                //                        })
                //                        .setPositiveButton("是", new DialogInterface.OnClickListener() {
                //                            @Override
                //                            public void onClick(DialogInterface dialog, int which) {
                //                                dialog.dismiss();
                //                                ActivityManagerTool.getInstance().finishApp();
                //                            }
                //                        }).show();
                break;
            default:
                break;
        }
    }
}