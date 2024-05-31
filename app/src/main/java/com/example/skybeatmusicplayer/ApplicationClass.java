package com.example.skybeatmusicplayer;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import java.util.ArrayList;

public class ApplicationClass extends Application {

    public static ArrayList<Audio> audioList;
    public static String CHANNEL_2= "Channel_2";

    @Override
    public void onCreate() {
        super.onCreate();

        audioList = new ArrayList<Audio>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_2, CHANNEL_2, NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            channel.setSound(null, null);
            manager.createNotificationChannel(channel);



        }
    }


}


