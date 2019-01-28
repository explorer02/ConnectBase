package com.example.connectbase;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.File;
import java.util.ArrayList;

import id.zelory.compressor.Compressor;

public class ViewImagesActivity extends AppCompatActivity {

    ArrayList<Uri> uriList = new ArrayList<>();
    ArrayList<Uri> compressedUriList = new ArrayList<>();
    ArrayList<String> descList;
    RecyclerView recyclerView;
    Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.FullScreenTheme);
        setContentView(R.layout.activity_view_images);

        recyclerView = findViewById(R.id.list_viewImages);
        Bundle bundle = getIntent().getBundleExtra("bundle");
        ArrayList<String> list = bundle.getStringArrayList("uriList");
        descList = new ArrayList<>(20);
        for (int i = 0; i < list.size(); i++) {
            uriList.add(Uri.parse(list.get(i)));
            descList.add("");
        }


        adapter = new Adapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setHasFixedSize(true);

        new LoadImages().execute();

    }

    public void doneEditing(View view) {

        ArrayList<String> uriList = new ArrayList<>();

        for (int i = 0; i < compressedUriList.size(); i++) {
            uriList.add(compressedUriList.get(i).toString());
        }
        Intent intent = new Intent(this, ChatActivity.class);
        Bundle bundle = new Bundle();
        bundle.putStringArrayList("uriList", uriList);
        bundle.putStringArrayList("descList", descList);
        intent.putExtra("bundle", bundle);
        setResult(RESULT_OK, intent);
        finish();

    }

    private Uri getUriFromFile(File file) {

        if (Build.VERSION.SDK_INT >= 24)
            return FileProvider.getUriForFile(getApplicationContext(), getApplicationContext()
                    .getPackageName() + ".provider", file);
        else return Uri.fromFile(file);

    }


    File compressImage(File file) {

        try {

            String path;
            path = "/ConnectBase/temp/image/compress";

            return new Compressor(this)
                    .setCompressFormat(Bitmap.CompressFormat.JPEG)
                    .setMaxHeight(600)
                    .setMaxWidth(600)
                    .setQuality(30)
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
            Log.i("GetReal", "null Cursor");
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            Log.i("GetReal", result);
            Log.i("GetReal", cursor.getColumnName(idx));
            cursor.close();
        }
        return result;
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Go Back??")
                .setMessage("Are you sure you do not want to send these images?")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", (dialog, which) -> {
                    for (int i = 0; i < compressedUriList.size(); i++)
                        new File(getRealPathFromUri(compressedUriList.get(i))).delete();
                    dialog.dismiss();
                    Intent intent = new Intent(ViewImagesActivity.this, ChatActivity.class);
                    setResult(RESULT_CANCELED, intent);
                    finish();
                });
        builder.show();
    }

    public class LoadImages extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            for (int i = 0; i < uriList.size(); i++) {
                compressedUriList.add(getUriFromFile(compressImage(new File(getRealPathFromUri(uriList.get(i))))));
                publishProgress();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... voids) {
            super.onProgressUpdate(voids);

            adapter.notifyDataSetChanged();


        }
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
            int pos = viewHolder.getAdapterPosition();
            viewHolder.ivDelete.setOnClickListener(v -> {
                File file = new File(getRealPathFromUri(compressedUriList.get(pos)));
                if (file.exists())
                    file.delete();
                compressedUriList.remove(pos);
                descList.remove(i);
                notifyItemRemoved(pos);
                notifyItemRangeChanged(pos, getItemCount());
            });

            viewHolder.ivAdd.setOnClickListener(v -> {

                AlertDialog.Builder builder = new AlertDialog.Builder(ViewImagesActivity.this);

                builder.setTitle("Add Description");

                ViewGroup.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                EditText editText = new EditText(ViewImagesActivity.this);
                editText.setLayoutParams(params);
                builder.setView(editText);
                editText.setHint("Type Something..");
                editText.setPadding(10, 0, 10, 0);
                editText.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                builder.setCancelable(false);
                builder.setNegativeButton("Cancel", null);
                builder.setPositiveButton("Ok", (dialog1, which) -> {
                    String text = editText.getText().toString().trim();
                    descList.add(i, text);
                });
                builder.show();

            });

        }

        @Override
        public int getItemCount() {
            return compressedUriList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            ImageView ivPic, ivDelete, ivAdd;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivPic = itemView.findViewById(R.id.iv_lRVI_pic);
                ivDelete = itemView.findViewById(R.id.iv_lRVI_delete);
                ivAdd = itemView.findViewById(R.id.iv_lRVI_add);
            }
        }
    }
}
