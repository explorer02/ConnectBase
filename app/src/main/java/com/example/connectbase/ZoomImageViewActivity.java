package com.example.connectbase;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.jsibbold.zoomage.ZoomageView;

import java.io.File;

public class ZoomImageViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zoom_image_view);
        ZoomageView zoomageView=findViewById(R.id.iv_zoomImage_profilePic);
        String imagePath = getIntent().getStringExtra("path");
        File imageFile = new File(imagePath);
        if (imageFile.exists())
            zoomageView.setImageURI(new CommonFunctions().getUriFromFile(getApplicationContext(), imageFile));
        else zoomageView.setImageResource(R.drawable.avatar);

    }
}
