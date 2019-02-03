package com.example.connectbase;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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

    ArrayList<String> pathList = new ArrayList<>();
    ArrayList<String> compressedPathList = new ArrayList<>();
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
        pathList = bundle.getStringArrayList("pathList");
        descList = new ArrayList<>(20);
        for (int i = 0; i < pathList.size(); i++) {
            descList.add("");
        }


        adapter = new Adapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setHasFixedSize(true);

        new LoadImages().execute();

    }

    public void doneEditing(View view) {

        ArrayList<String> pathList2 = new ArrayList<>(compressedPathList);

        Intent intent = new Intent(this, ChatActivity.class);
        Bundle bundle = new Bundle();
        bundle.putStringArrayList("pathList", pathList2);
        bundle.putStringArrayList("descList", descList);
        intent.putExtra("bundle", bundle);
        setResult(RESULT_OK, intent);
        finish();

    }

    File compressImage(File file) {

        try {

            String path;
            path = "/ConnectBase/temp/image/compress/";

            return new Compressor(this)
                    .setCompressFormat(Bitmap.CompressFormat.JPEG)
                    .setMaxHeight(600)
                    .setMaxWidth(600)
                    .setQuality(40)
                    .setDestinationDirectoryPath(Environment.getExternalStorageDirectory() + path)
                    .compressToFile(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Go Back??")
                .setMessage("Are you sure you do not want to send these images?")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", (dialog, which) -> {
                    for (int i = 0; i < compressedPathList.size(); i++)
                        new File(compressedPathList.get(i)).delete();
                    dialog.dismiss();
                    Intent intent = new Intent(ViewImagesActivity.this, ChatActivity.class);
                    setResult(RESULT_CANCELED, intent);
                    finish();
                });
        builder.show();
    }

    private Uri getUriFromFile(File file) {

        if (Build.VERSION.SDK_INT >= 24)
            return FileProvider.getUriForFile(getApplicationContext(), getApplicationContext()
                    .getPackageName() + ".provider", file);
        else return Uri.fromFile(file);

    }

    public class LoadImages extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            for (int i = 0; i < pathList.size(); i++) {
                Log.i("ConnectBase Path", pathList.get(i));
                compressedPathList.add(compressImage(new File(pathList.get(i))).getPath());
                new File(pathList.get(i)).delete();
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
            viewHolder.ivPic.setImageURI(getUriFromFile(new File(compressedPathList.get(i))));
            int pos = viewHolder.getAdapterPosition();

            viewHolder.ivDelete.setOnClickListener(v -> {
                File file = new File(compressedPathList.get(i));
                if (file.exists())
                    file.delete();
                compressedPathList.remove(pos);
                descList.remove(i);
                notifyItemRemoved(pos);
                notifyItemRangeChanged(pos, getItemCount());
            });

            viewHolder.ivAdd.setOnClickListener(v -> {

                AlertDialog.Builder builder = new AlertDialog.Builder(ViewImagesActivity.this);

                builder.setTitle("Add Description");

                ViewGroup.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);


                LinearLayout linearLayout = new LinearLayout(ViewImagesActivity.this);
                linearLayout.setLayoutParams(params);
                linearLayout.setPadding(20, 20, 20, 10);
                EditText editText = new EditText(ViewImagesActivity.this);
                editText.setText(descList.get(pos).trim());
                linearLayout.addView(editText, params);
                builder.setView(linearLayout);
                editText.setHint("Type Something..");
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
            return compressedPathList.size();
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
