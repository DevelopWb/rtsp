package com.juntai.wisdom.basecomponent.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.juntai.wisdom.basecomponent.app.BaseApplication;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by Ma
 * on 2019/5/8
 */
public class NotificationTool {
    /**
     *
     * @param context
     * @param content:通知显示内容
     * @param imageRes:图标
     * @param ongo:是否  设置为一个正在进行的通知，此时用户无法清除通知
     */
    public static Notification sendNotifLocService(Context context,String title,String content,int imageRes,boolean ongo){
        NotificationManager manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        String channelId = "notifi";
        String channelName = "消息";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            createNotificationChannel(context,channelId, channelName, importance);
        }
        // 创建PendingIntent
        PendingIntent notifyPendingIntent = PendingIntent.getActivity(
                context, 0,
                new Intent(BaseApplication.app,BaseApplication.app.getNowActivity().getClass()),
                PendingIntent.FLAG_UPDATE_CURRENT);
        //创建通知
        Notification notification = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(content)
                .setWhen(System.currentTimeMillis())
                .setOngoing(ongo)//设置为一个正在进行的通知，此时用户无法清除通知
                .setSmallIcon(imageRes)
                .setContentIntent(notifyPendingIntent)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), imageRes))
                .setAutoCancel(true)
                .build();
//        manager.notify(id, notification);
        return notification;
    }
    /**
     *
     * @param context
     * @param id:通知显示id
     * @param content:通知显示内容
     * @param imageRes:图标
     * @param ongo:是否  设置为一个正在进行的通知，此时用户无法清除通知
     * @param intent
     */
    public static void sendNotifMessage(Context context,int id,String title,String content,int imageRes,boolean ongo,Intent intent){
        NotificationManager manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        String channelId = "notifi";
        String channelName = "消息";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            createNotificationChannel(context,channelId, channelName, importance);
        }
        // 创建PendingIntent
        PendingIntent notifyPendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //手机设置的默认提示音
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        Notification notification = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(content)
                .setSound(uri)
                .setWhen(System.currentTimeMillis())
                .setOngoing(ongo)//设置为一个正在进行的通知，此时用户无法清除通知
                .setSmallIcon(imageRes)
                .setContentIntent(notifyPendingIntent)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), imageRes))
                .setAutoCancel(true)
                .build();
        manager.notify(id, notification);
    }

    public static void startNotifBg(Context context, int id, String title, String content, int imageRes){
        NotificationManager manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        String channelId = "notifi";
        String channelName = "消息";
        Intent intent = new Intent();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            createNotificationChannel(context,channelId, channelName, importance);
        }
        // 创建PendingIntent
        PendingIntent notifyPendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(content)
                .setWhen(System.currentTimeMillis())
                .setOngoing(true)//设置为一个正在进行的通知，此时用户无法清除通知
                .setSmallIcon(imageRes)
                .setContentIntent(notifyPendingIntent)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), imageRes))
                .setAutoCancel(true)
                .build();
        manager.notify(id, notification);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static void createNotificationChannel(Context context,String channelId, String channelName, int importance) {
        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(
                NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);
    }
    /**
     * 获取notification
     * @param context
     * @return
     */
    public static Notification getNotification(Context context){
        String channelId = "notifi";
        String channelName = "消息";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            createNotificationChannel(context,channelId, channelName, importance);

        }
        return new NotificationCompat.Builder(context, channelId).build();
    }
}
