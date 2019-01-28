package com.example.connectbase;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.File;
import java.util.Date;

import id.zelory.compressor.Compressor;
import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;

public class ImageEditingActivity extends AppCompatActivity {

    PhotoEditor mPhotoEditor;
    PhotoEditorView mPhotoEditorView;
    EditText etDesc;
    Uri imageUri;
    int imageCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.FullScreenTheme);
        setContentView(R.layout.activity_image_editing);
        String path = getIntent().getStringExtra("path");
        imageUri = Uri.parse(path);
        imageCode = getIntent().getIntExtra("imageCode", -1);
        etDesc = findViewById(R.id.et_imageEdit_desc);


        mPhotoEditorView = findViewById(R.id.photoEditor_ImageEditing);
        mPhotoEditor = new PhotoEditor.Builder(this, mPhotoEditorView)
                .setPinchTextScalable(true)
                .setDefaultTextTypeface(ResourcesCompat.getFont(this, R.font.dancing_script))
                .build();
        mPhotoEditorView.getSource().setImageURI(imageUri);
        mPhotoEditorView.getSource().setScaleType(ImageView.ScaleType.CENTER_CROP);


    }

    public void editingToolClick(View view) {

        String tag = (String) view.getTag();
        if (tag.equals("brush")) {
            mPhotoEditor.setBrushDrawingMode(true);
            mPhotoEditor.setBrushColor(getResources().getColor(R.color.colorWhite));

        } else if (tag.equals("eraser")) {
            mPhotoEditor.brushEraser();

        } else if (tag.equals("text")) {

            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle("Add Text");
            TextInputLayout til = new TextInputLayout(this);
            ViewGroup.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            til.setLayoutParams(params);
            dialog.setView(til);
            TextInputEditText editText = new TextInputEditText(this);
            til.setPadding(20, 20, 20, 5);
            editText.setHint("Type Something..");
            til.addView(editText, params);
            dialog.setNegativeButton("Cancel", null);
            dialog.setPositiveButton("Ok", (dialog1, which) -> {
                String text = til.getEditText().getText().toString().trim();
                if (text.isEmpty()) {
                    Snackbar.make(mPhotoEditorView, "Please Add Non Empty Text", Snackbar.LENGTH_SHORT).show();
                } else {
                    mPhotoEditor.addText(text, getResources().getColor(R.color.colorWhite));
                }
            });
            dialog.show();

        } else if (tag.equals("undo")) {
            mPhotoEditor.undo();

        } else if (tag.equals("redo")) {
            mPhotoEditor.redo();

        }

    }

    public void doneEditing(View view) {

        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setTitle("Loading...");
        dialog.setMessage("Please Wait while the image is Saving...");
        dialog.show();

        File parentFile = new File(Environment.getExternalStorageDirectory() + "/ConnectBase/temp");
        parentFile.mkdirs();
        File file = new File(parentFile, "IMG_" + new Date().getTime() + ".jpg");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mPhotoEditor.saveAsFile(file.getPath(), new PhotoEditor.OnSaveListener() {
            @Override
            public void onSuccess(@NonNull String imagePath) {
                dialog.dismiss();
                if (imageCode == ChatActivity.REQUEST_CODE_CAMERA) {
                    Log.i("Uri Camera", imageUri.toString());
                    new File(getRealPathFromUri(imageUri)).delete();
                }
                File compressedFile = compressImage(file);
                file.delete();
                Uri uri;
                if (Build.VERSION.SDK_INT >= 24)
                    uri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext()
                            .getPackageName() + ".provider", compressedFile);
                else uri = Uri.fromFile(compressedFile);

                Intent intent = new Intent(ImageEditingActivity.this, ChatActivity.class);
                setResult(RESULT_OK, intent);
                Log.i("ConnectBase SaveAsFile", uri.toString());
                Bundle bundle = new Bundle();
                bundle.putString("desc", etDesc.getText().toString().trim());
                bundle.putString("path", uri.toString());
                intent.putExtra("uriBundle", bundle);

                finish();
            }

            @Override
            public void onFailure(@NonNull Exception exception) {

            }
        });

    }

    File compressImage(File file) {

        try {

            String path;
            path = "/ConnectBase/temp/image/compress";

            return new Compressor(this)
                    .setCompressFormat(Bitmap.CompressFormat.JPEG)
                    .setMaxHeight(600)
                    .setMaxWidth(600)
                    .setQuality(35)
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
            Log.i("GetReal", cursor.getColumnName(idx));
            result = cursor.getString(idx);
            Log.i("GetReal", result);

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
                    if (imageCode == ChatActivity.REQUEST_CODE_CAMERA)
                        new File(getRealPathFromUri(imageUri)).delete();
                    Intent intent = new Intent(ImageEditingActivity.this, ChatActivity.class);
                    setResult(RESULT_CANCELED, intent);
                    finish();
                });
        builder.show();
    }

}
