package com.example.connectbase;

import android.app.ProgressDialog;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;

import id.zelory.compressor.Compressor;

public class ViewImagesActivity extends AppCompatActivity {

    ArrayList<Uri> uriList = new ArrayList<>();
    ArrayList<Uri> compressedUriList = new ArrayList<>();
    RecyclerView recyclerView;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.FullScreenTheme);
        setContentView(R.layout.activity_view_images);

        dialog = new ProgressDialog(this);
        dialog.setTitle("Loading...");
        dialog.setMessage("Please wait while Loading (0/" + uriList.size() + "+) images");
        dialog.show();

        recyclerView = findViewById(R.id.list_viewImages);
        Bundle bundle = getIntent().getBundleExtra("bundle");
        ArrayList<String> list = bundle.getStringArrayList("uriList");
        for (int i = 0; i < list.size(); i++)
            uriList.add(Uri.parse(list.get(i)));
        for (int i = 0; i < uriList.size(); i++) {
            compressedUriList.add(getUriFromFile(compressImage(new File(getRealPathFromUri(uriList.get(i))), 700, 700, 40)));
            dialog.setMessage("Please wait while Loading (" + i + "/" + uriList.size() + "+) images");
            if (i == uriList.size() - 1)
                dialog.dismiss();
        }

        Adapter adapter = new Adapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setHasFixedSize(true);

    }

    public void doneEditing(View view) {

    }

    private Uri getUriFromFile(File file) {
        Uri uri;

        if (Build.VERSION.SDK_INT >= 24)
            uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        else uri = Uri.fromFile(file);

        return uri;
    }


    File compressImage(File file, int h, int w, int q) {

        try {

            String path;
            path = "/ConnectBase/temp/image/compress";

            return new Compressor(this)
                    .setCompressFormat(Bitmap.CompressFormat.JPEG)
                    .setMaxHeight(h)
                    .setMaxWidth(w)
                    .setQuality(q)
                    .setDestinationDirectoryPath(Environment.getExternalStorageDirectory() + path)
                    .compressToFile(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getRealPathFromUri(Uri uri) {
        String result;
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            result = uri.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }


    public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

        public Adapter() {
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_row_view_image, viewGroup, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
            viewHolder.ivPic.setImageURI(compressedUriList.get(i));
            //TODO image Editing and deleting code

        }

        @Override
        public int getItemCount() {
            return compressedUriList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            ImageView ivPic, ivEdit, ivDelete;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivPic = itemView.findViewById(R.id.iv_lRVI_pic);
                ivEdit = itemView.findViewById(R.id.iv_lRVI_edit);
                ivDelete = itemView.findViewById(R.id.iv_lRVI_delete);
            }
        }
    }
}
