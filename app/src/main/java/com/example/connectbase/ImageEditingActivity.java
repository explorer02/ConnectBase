package com.example.connectbase;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
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

    static Bundle myBundle = new Bundle();
    PhotoEditor mPhotoEditor;
    PhotoEditorView mPhotoEditorView;
    EditText etDesc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.FullScreenTheme);
        setContentView(R.layout.activity_image_editing);
        String path = getIntent().getStringExtra("path");
        Uri uri = Uri.parse(path);
        etDesc = findViewById(R.id.et_imageEdit_desc);


        mPhotoEditorView = findViewById(R.id.photoEditor_ImageEditing);
        mPhotoEditor = new PhotoEditor.Builder(this, mPhotoEditorView)
                .setPinchTextScalable(true)
                .setDefaultTextTypeface(ResourcesCompat.getFont(this, R.font.dancing_script))
                .build();
        mPhotoEditorView.getSource().setImageURI(uri);
        mPhotoEditorView.getSource().setScaleType(ImageView.ScaleType.CENTER);


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
                Intent intent = new Intent(ImageEditingActivity.this, ChatActivity.class);
                setResult(RESULT_OK, intent);
                myBundle.putString("desc", etDesc.getText().toString().trim());
                myBundle.putString("path", imagePath);

                finish();
            }

            @Override
            public void onFailure(@NonNull Exception exception) {

            }
        });

    }

}
