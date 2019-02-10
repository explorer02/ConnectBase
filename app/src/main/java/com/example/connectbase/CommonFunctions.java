package com.example.connectbase;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.widget.ImageView;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import id.zelory.compressor.Compressor;

import static android.content.Context.CONNECTIVITY_SERVICE;

class CommonFunctions {

    StorageReference mProfilePicsReference;

    CommonFunctions() {
        mProfilePicsReference = FirebaseStorage.getInstance().getReference().child("ProfileImage");


    }

    void downloadProfilePic(Context context, String userId, ImageView imageView, String imageUrl) {


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

    boolean checkInternetConnection(Context context) {

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        return networkInfo != null;
    }

    Uri getUriFromFile(Context context, File file) {

        if (Build.VERSION.SDK_INT >= 24)
            return FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
        else return Uri.fromFile(file);

    }

    void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024 * 16];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        in.close();
        out.close();

    }

    File compressImage(Context context, File file, String outputPath, int h, int w, int q) {

        try {

            /*String path;
            path = "/ConnectBase/temp/image/compress";
*/
            return new Compressor(context)
                    .setCompressFormat(Bitmap.CompressFormat.JPEG)
                    .setMaxHeight(h)
                    .setMaxWidth(w)
                    .setQuality(q)
                    .setDestinationDirectoryPath(Environment.getExternalStorageDirectory() + outputPath)
                    .compressToFile(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    void showErrorDialog(Context context, String message) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle("Oops!!");
        dialog.setMessage(message);
        dialog.setPositiveButton("Ok", null);
        dialog.setCancelable(false);
        dialog.show();

    }

    String convertTime(long milliSec, boolean small) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSec);
        /*calendar.add(Calendar.HOUR,5);
        calendar.add(Calendar.MINUTE,30);
*/
        SimpleDateFormat dateFormat;
        if (small)
            dateFormat = new SimpleDateFormat("hh:mm a");
        else dateFormat = new SimpleDateFormat("dd-MMM-yy HH:mm a");

        return dateFormat.format(calendar.getTime());
    }

}
