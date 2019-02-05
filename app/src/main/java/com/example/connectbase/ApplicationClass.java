package com.example.connectbase;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;


public class ApplicationClass extends Application {

    public static final String NOTIFICATION_CHANNEL__UPLOAD = "channel_upload";
    public static final String NOTIFICATION_CHANNEL__DOWNLOAD = "channel_download";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    private void createNotificationChannels() {

        if (Build.VERSION.SDK_INT < 26)
            return;
        NotificationChannel channel_upload = new NotificationChannel(NOTIFICATION_CHANNEL__UPLOAD, "Channel_Upload", NotificationManager.IMPORTANCE_DEFAULT);
        channel_upload.setDescription("This is upload channel!!");
        NotificationChannel channel_download = new NotificationChannel(NOTIFICATION_CHANNEL__DOWNLOAD, "Channel_Download", NotificationManager.IMPORTANCE_DEFAULT);
        channel_upload.setDescription("This is download channel!!");

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel_upload);
        manager.createNotificationChannel(channel_download);

    }
}
