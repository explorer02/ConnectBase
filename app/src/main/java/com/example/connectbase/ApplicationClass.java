package com.example.connectbase;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;


public class ApplicationClass extends Application {

    public static final String NOTIFICATION_CHANNEL__UPLOAD = "channel_upload";
    public static final String NOTIFICATION_CHANNEL__DOWNLOAD = "channel_download";
    FirebaseAuth mAuth;
    DatabaseReference mUserReference;
    static HashMap<String, UploadTask> uploadTaskHashMap;
    static HashMap<String, FileDownloadTask> downloadTaskHashMap;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        uploadTaskHashMap = new HashMap<>();
        downloadTaskHashMap = new HashMap<>();

        mAuth = FirebaseAuth.getInstance();
        mUserReference = FirebaseDatabase.getInstance().getReference().child("Users");
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getUid();
            mUserReference.child(uid).child("online").setValue("true");
            mUserReference.child(uid).child("online").onDisconnect().setValue(ServerValue.TIMESTAMP);

        }


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
