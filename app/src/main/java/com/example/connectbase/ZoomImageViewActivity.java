package com.example.connectbase;

import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.jsibbold.zoomage.ZoomageView;

import java.io.File;

public class ZoomImageViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zoom_image_view);
        ZoomageView zoomageView=findViewById(R.id.iv_zoomImage_profilePic);
        File file=new File(Environment.getExternalStorageDirectory()+"/ConnectBase/profilePic.jpg");
        if(file.exists()){
            zoomageView.setImageURI(Uri.fromFile(file));
        }

    }
}
