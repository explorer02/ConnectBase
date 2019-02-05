package com.example.connectbase;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.widget.ImageView;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.File;

import static android.content.Context.CONNECTIVITY_SERVICE;

public class CommonFunctions {

    StorageReference mProfilePicsReference;

    public CommonFunctions() {
        mProfilePicsReference = FirebaseStorage.getInstance().getReference().child("ProfileImage");


    }

    public void downloadProfilePic(Context context, String userId, ImageView imageView, String imageUrl) {

        File parentFile = new File(Environment.getExternalStorageDirectory() + "/ConnectBase/ProfilePics/");
        parentFile.mkdirs();
        File imageFile = new File(parentFile, userId + ".jpg");

        if (imageFile.exists())
            imageView.setImageURI(getUriFromFile(context, imageFile));
        else imageView.setImageResource(R.drawable.avatar);

        if (checkInternetConnection(context)) {

            StorageReference imageReference = mProfilePicsReference.child(userId + ".jpg");

            imageReference.getFile(imageFile).addOnCompleteListener(task -> {

                if (task.isSuccessful()) {
                    imageView.setImageURI(getUriFromFile(context, imageFile));
                } else {
                    Picasso.get()
                            .load(imageUrl)
                            .placeholder(R.drawable.avatar)
                            .into(imageView);
                }

            });
        }


    }

    private boolean checkInternetConnection(Context context) {

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        return networkInfo != null;
    }

    private Uri getUriFromFile(Context context, File file) {

        if (Build.VERSION.SDK_INT >= 24)
            return FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
        else return Uri.fromFile(file);

    }

}
