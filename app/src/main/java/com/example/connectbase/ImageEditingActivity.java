package com.example.connectbase;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.File;
import java.util.Date;

import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;

public class ImageEditingActivity extends AppCompatActivity {

    PhotoEditor mPhotoEditor;
    PhotoEditorView mPhotoEditorView;
    EditText etDesc;
    int imageCode;
    String imagePath;
    File imageFile;
    int rotation = 0;
    CommonFunctions commonFunctions = new CommonFunctions();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.FullScreenTheme);
        setContentView(R.layout.activity_image_editing);


        imageCode = getIntent().getIntExtra("imageCode", -1);
        imagePath = getIntent().getStringExtra("path");
        imageFile = new File(imagePath);
        etDesc = findViewById(R.id.et_imageEdit_desc);

        if (imageFile.length() > 2 * 1024 * 1024L) {
            File file = commonFunctions.compressImage(this, imageFile, "/ConnectBase/temp/image/compress", 1280, 720, 100);

            imageFile.delete();
            imageFile = file;
        }


        mPhotoEditorView = findViewById(R.id.photoEditor_ImageEditing);
        mPhotoEditor = new PhotoEditor.Builder(this, mPhotoEditorView)
                .setPinchTextScalable(true)
                .setDefaultTextTypeface(ResourcesCompat.getFont(this, R.font.dancing_script))
                .build();
        mPhotoEditorView.getSource().setImageURI(commonFunctions.getUriFromFile(getApplicationContext(), imageFile));
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
            TextInputEditText editText = new TextInputEditText(this);
            til.setPadding(20, 20, 20, 5);
            editText.setHint("Type Something..");
            til.addView(editText, params);
            dialog.setView(til);
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

        } else if (tag.equals("rotleft")) {
            rotation = (rotation - 90) % 360;
            mPhotoEditorView.getSource().setRotation(rotation);

        } else if (tag.equals("rotright")) {
            rotation = (rotation + 90) % 360;
            mPhotoEditorView.getSource().setRotation(rotation);
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
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 110);
            return;
        }


        mPhotoEditor.saveAsFile(file.getPath(), new PhotoEditor.OnSaveListener() {
            @Override
            public void onSuccess(@NonNull String savedImagePath) {
                dialog.dismiss();

                imageFile.delete();
                File compressedFile = commonFunctions.compressImage(ImageEditingActivity.this, file, "/ConnectBase/temp/image/compress", 600, 600, 35);
                file.delete();

                Intent intent = new Intent(ImageEditingActivity.this, ChatActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("desc", etDesc.getText().toString().trim());
                bundle.putString("path", compressedFile.getPath());
                intent.putExtra("uriBundle", bundle);
                setResult(RESULT_OK, intent);
                finish();
            }

            @Override
            public void onFailure(@NonNull Exception exception) {

            }
        });

    }


    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Go Back??")
                .setMessage("Are you sure you do not want to send these images?")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (imageCode == ChatActivity.REQUEST_CODE_CAMERA)
                        new File(imagePath).delete();
                    Intent intent = new Intent(ImageEditingActivity.this, ChatActivity.class);
                    setResult(RESULT_CANCELED, intent);
                    finish();
                });
        builder.show();
    }

}
