package com.example.connectbase;

import android.Manifest;
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
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class ChatActivity extends AppCompatActivity {

    Users user;
    String id, currentId;
    DatabaseReference mChatIdReference, mChatReference;
    final int REQUEST_CODE_GALLERY_SINGLE = 1031;
    EditText etMessage;
    String chatId=null;
    final int REQUEST_CODE_CAMERA = 101;
    final int REQUEST_CODE_FILE = 102;
    final int REQUEST_CODE_GALLERY_BATCH = 1032;
    StorageReference mChatImageReference;
    final int REQUEST_CODE_STORAGE = 104;
    Uri cameraUri, fileUri, galleryUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Toolbar toolbar = findViewById(R.id.toolbar_chat);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();

        actionBar.setDisplayShowCustomEnabled(true);

        etMessage = findViewById(R.id.et_chat_message);

        View view = getLayoutInflater().inflate(R.layout.layout_toolbar_chat_activity, null, false);
        actionBar.setCustomView(view, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        actionBar.setDisplayHomeAsUpEnabled(true);

        mChatIdReference = FirebaseDatabase.getInstance().getReference().child("ChatId");
        mChatReference = FirebaseDatabase.getInstance().getReference().child("Chats");
        mChatImageReference = FirebaseStorage.getInstance().getReference().child("ChatImage");

        user = (Users) getIntent().getSerializableExtra("user");
        id = getIntent().getStringExtra("id");
        currentId = FirebaseAuth.getInstance().getUid();


        TextView tvname = view.findViewById(R.id.tv_lTCA_name);
        tvname.setText(user.getName());
        CircleImageView ivProfilePic = view.findViewById(R.id.iv_lTCA_ivProfilePic);
        if (!user.getThumbImage().isEmpty())
            Picasso.get()
                    .load(user.getThumbImage())
                    .placeholder(R.drawable.avatar)
                    .into(ivProfilePic);
        View linLay = view.findViewById(R.id.linLay_lTCA_view);
        linLay.setOnClickListener(v -> openUserProfile());
        generateChatId();

    }

    void generateChatId() {
        mChatIdReference.child(currentId).child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild("value"))
                    chatId=dataSnapshot.child("value").getValue().toString();
                else {
                    chatId=mChatIdReference.child(currentId).child(id).push().getKey();
                    mChatIdReference.child(currentId).child(id).child("value").setValue(chatId);
                    mChatIdReference.child(id).child(currentId).child("value").setValue(chatId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    public void sendMessage(View view) {

        if(chatId==null) {
            Snackbar.make(view,"No Internet Connection!!",Snackbar.LENGTH_SHORT).show();
            generateChatId();
            return;
        }

        String message = etMessage.getText().toString().trim();
        etMessage.setText(null);
        if (message.isEmpty())
            return;
        HashMap map=new HashMap();
        map.put("messageType","text");
        map.put("message",message);
        map.put("sender",currentId);
        map.put("time",ServerValue.TIMESTAMP);
        map.put("seen", "false");

        mChatReference.child(chatId).push().setValue(map);
        //TODO: Notify Adapter about dataset change


    }

    public void addAttachment(View view) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE);
            return;
        }

        InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        manager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0, null);

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.layout_add_attachment, null, false);
        dialog.setContentView(dialogView);
        dialog.show();

        ImageView ivFile, ivCamera, ivGallery;
        ivFile = dialogView.findViewById(R.id.iv_lAA_file);
        ivGallery = dialogView.findViewById(R.id.iv_lAA_gallery);
        ivCamera = dialogView.findViewById(R.id.iv_lAA_camera);

        ivCamera.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File parentFile = new File(Environment.getExternalStorageDirectory() + "/ConnectBase/temp");
            parentFile.mkdirs();
            File file = new File(parentFile, "IMG_" + new Date().getTime() + ".jpg");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                cameraUri = FileProvider.getUriForFile(ChatActivity.this, getPackageName() + ".provider", file);
            else
                cameraUri = Uri.fromFile(file);

            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_CODE_CAMERA);
            dialog.hide();
        });

        ivGallery.setOnClickListener(v -> new AlertDialog.Builder(ChatActivity.this)
                .setItems(new String[]{"Single Image Mode", "Batch Image Mode"}, (dialog1, which) -> {
                    if (which == 0) singleImageModeGallery();
                    else batchImageModeGallery();
                    dialog.hide();
                })
                .show());


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_STORAGE:
                if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    String message = "Reading and writing External Storage is required for Sending attachments";
                    showErrorDialog(message);
                }
                break;
        }
    }

    private void showErrorDialog(String message) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Oops!!");
        dialog.setMessage(message);
        dialog.setPositiveButton("Ok", null);
        dialog.setCancelable(false);
        dialog.show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_CAMERA:
                if (resultCode == RESULT_OK) {
                    showAddAttachmentDescriptionToImage(cameraUri, REQUEST_CODE_CAMERA);
                } else if (resultCode == RESULT_CANCELED) {
                    Snackbar.make(findViewById(R.id.list_chat), "Oops, Action Cancelled!!", Snackbar.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_CODE_GALLERY_SINGLE:
                if (resultCode == RESULT_OK) {
                    showAddAttachmentDescriptionToImage(data.getData(), REQUEST_CODE_GALLERY_SINGLE);
                } else if (resultCode == RESULT_CANCELED) {
                    Snackbar.make(findViewById(R.id.list_chat), "Oops, Action Cancelled!!", Snackbar.LENGTH_SHORT).show();
                }

                break;
            case REQUEST_CODE_GALLERY_BATCH:
                break;
        }
    }

    private void showAddAttachmentDescriptionToImage(Uri uri, int requestCode) {

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.layout_add_attachment_description, null, false);
        dialog.setView(view);
        ImageView ivPic = view.findViewById(R.id.iv_lAAD_pic);
        ivPic.setImageURI(uri);
        TextInputLayout tilDesc = view.findViewById(R.id.til_lAAD_desc);

        dialog.setCancelable(false);

        dialog.setNegativeButton("Cancel", (dialog1, which) -> {
            if (requestCode == REQUEST_CODE_CAMERA)
                new File(getRealPathFromUri(uri)).delete();
        });
        dialog.setPositiveButton("Send", (dialog1, which) -> sendImageMessage(tilDesc.getEditText().getText().toString().trim(), uri, requestCode));

        dialog.show();

    }

    private void sendImageMessage(String desc, Uri imageUri, int code) {

        if (chatId == null) {
            Snackbar.make(findViewById(R.id.list_chat), "No Internet Connection!!", Snackbar.LENGTH_SHORT).show();
            generateChatId();
            return;
        }

        Log.i("ConnectBase", imageUri.getPath());

        HashMap hashMap = new HashMap();
        File file = new File(getRealPathFromUri(imageUri));
        if (!file.exists()) {
            return;
        }


        hashMap.put("sender", currentId);
        hashMap.put("messageType", "image");
        hashMap.put("description", desc);
        hashMap.put("imageName", file.getName());
        hashMap.put("imageUrl", "");
        hashMap.put("thumbImage", "");
        hashMap.put("status", "");
        hashMap.put("time", ServerValue.TIMESTAMP);
        hashMap.put("seen", "false");

        File thumbFile, imageFile;

        String pushKey = mChatReference.child(chatId).push().getKey();
        try {

            int q = 80;
            if (code == REQUEST_CODE_GALLERY_SINGLE)
                q = 40;

            imageFile = compressImage(file, 700, 700, q, true);

            thumbFile = compressImage(file, 250, 250, q - 10, false);


        } catch (IOException e) {
            showErrorDialog(e.getMessage());
            return;
        }


        StorageReference imageReference = mChatImageReference.child(pushKey + ".jpg");
        StorageReference thumbImageReference = mChatImageReference.child("ThumbImage").child(pushKey + ".jpg");

        Uri thumbImageUri, imageFileUri;
        if (Build.VERSION.SDK_INT < 24) {
            thumbImageUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", thumbFile);
            imageFileUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", imageFile);
        } else {
            thumbImageUri = Uri.fromFile(thumbFile);
            imageFileUri = Uri.fromFile(imageFile);
        }

        //TODO: add notification for progress of image uploading

        imageReference.putFile(imageFileUri).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                imageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    hashMap.put("imageUrl", uri.toString());
                    thumbImageReference.putFile(thumbImageUri).addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            thumbImageReference.getDownloadUrl().addOnSuccessListener(uri1 -> {
                                hashMap.put("thumbImage", uri1.toString());
                                mChatReference.child(chatId).child(pushKey).setValue(hashMap);
                                file.delete();
                                thumbFile.delete();
                                sendFileToSentFolder(imageFile);

                                //TODO: Notify Adapter about dataset change
                            });
                        } else showErrorDialog(task1.getException().getMessage());
                    });
                });
            } else showErrorDialog(task.getException().getMessage());

        });


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

    private void sendFileToSentFolder(File imageFile) {

        String path = "/ConnectBase/Media/Images/" + user.getName() + "\t\t" + id + "/sent";
        File parentOutput = new File(Environment.getExternalStorageDirectory() + path);
        File outputFile = new File(parentOutput, imageFile.getName());
        parentOutput.mkdirs();
        try {

            InputStream in = new FileInputStream(imageFile);
            OutputStream out = new FileOutputStream(outputFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.close();
            imageFile.delete();
        } catch (Exception e) {
            showErrorDialog(e.getMessage());
        }

    }

    File compressImage(File file, int h, int w, int q, boolean image) throws IOException {

        String path;
        if (image) path = "/ConnectBase/temp/image";
        else path = "/ConnectBase/temp/thumbImage";

        return new Compressor(this)
                .setCompressFormat(Bitmap.CompressFormat.JPEG)
                .setMaxHeight(h)
                .setMaxWidth(w)
                .setQuality(q)
                .setDestinationDirectoryPath(Environment.getExternalStorageDirectory() + path)
                .compressToFile(file);
    }

    private void openUserProfile() {

        Intent intent = new Intent(this, ViewUserProfile.class);
        intent.putExtra("user", user);
        intent.putExtra("id", id);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_chat_clear:
                break;
        }
        return true;
    }

    private void batchImageModeGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select Images"), REQUEST_CODE_GALLERY_SINGLE);

    }

    private void singleImageModeGallery() {

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), REQUEST_CODE_GALLERY_SINGLE);

    }

}
