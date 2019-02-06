package com.example.connectbase;

import android.Manifest;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
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
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    Users user;
    String id, currentId;
    DatabaseReference mChatIdReference, mChatReference;
    final int REQUEST_CODE_GALLERY = 1031;
    EditText etMessage;
    String chatId = null;
    static final int REQUEST_CODE_CAMERA = 101;
    static final int REQUEST_CODE_FILE = 102;
    StorageReference mChatImageReference, mChatFileReference;
    static final int REQUEST_CODE_STORAGE = 104;
    static final int REQUEST_CODE_IMAGE_EDITING = 105;
    static final int REQUEST_CODE_VIEW_IMAGES = 106;
    static final int REQUEST_CODE_VIEW_FILES = 107;
    Uri cameraUri;
    File cameraFile;
    RecyclerView chatList;
    CommonFunctions commonFunctions = new CommonFunctions();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Toolbar toolbar = findViewById(R.id.toolbar_chat);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();

        actionBar.setDisplayShowCustomEnabled(true);

        etMessage = findViewById(R.id.et_chat_message);
        chatList = findViewById(R.id.list_chat);

        View view = getLayoutInflater().inflate(R.layout.layout_toolbar_chat_activity, null, false);
        actionBar.setCustomView(view, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        actionBar.setDisplayHomeAsUpEnabled(true);

        mChatIdReference = FirebaseDatabase.getInstance().getReference().child("ChatId");
        mChatReference = FirebaseDatabase.getInstance().getReference().child("Chats");
        mChatImageReference = FirebaseStorage.getInstance().getReference().child("ChatImage");
        mChatFileReference = FirebaseStorage.getInstance().getReference().child("ChatFiles");

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
                    chatId = dataSnapshot.child("value").getValue().toString();
                else {
                    chatId = mChatIdReference.child(currentId).child(id).push().getKey();
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

        if (!commonFunctions.checkInternetConnection(this)) {

            Snackbar.make(chatList, "No Internet Connection!!", Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (chatId == null) {
            Snackbar.make(view, "No Internet Connection!!", Snackbar.LENGTH_SHORT).show();
            generateChatId();
            return;
        }

        String message = etMessage.getText().toString().trim();
        etMessage.setText(null);
        if (message.isEmpty())
            return;
        HashMap map = new HashMap();
        map.put("messageType", "text");
        map.put("message", message);
        map.put("sender", currentId);
        map.put("time", ServerValue.TIMESTAMP);
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
            File parentFile = new File(Environment.getExternalStorageDirectory() + "/ConnectBase/temp/image");
            parentFile.mkdirs();
            File file = new File(parentFile, "IMG_" + new Date().getTime() + ".jpg");
            cameraFile = file;
            cameraUri = commonFunctions.getUriFromFile(getApplicationContext(), file);

            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_CODE_CAMERA);
            dialog.hide();
        });

        ivGallery.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            //Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            //intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(Intent.createChooser(intent, "Select Images"), REQUEST_CODE_GALLERY);
            dialog.hide();
        });

        ivFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.setType("*/*");
            startActivityForResult(intent, REQUEST_CODE_FILE);
            dialog.hide();
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_CAMERA:
                if (resultCode == RESULT_OK) {

                    Intent intent = new Intent(this, ImageEditingActivity.class);
                    intent.putExtra("path", cameraFile.toString());
                    // intent.putExtra("imageCode", REQUEST_CODE_CAMERA);

                    startActivityForResult(intent, REQUEST_CODE_IMAGE_EDITING);

                } else if (resultCode == RESULT_CANCELED) {
                    Snackbar.make(chatList, "Oops, Action Cancelled!!", Snackbar.LENGTH_SHORT).show();
                }
                break;

            case REQUEST_CODE_GALLERY:
                if (resultCode == RESULT_OK && data != null) {
                    if (data.getData() != null) {
                        Intent intent = new Intent(this, ImageEditingActivity.class);
                        File file = createFileFromUri(data.getData(), "image");
                        intent.putExtra("path", file.getPath());
                        //   intent.putExtra("imageCode", REQUEST_CODE_GALLERY);
                        Log.i("ConnectBase Data", data.getData().toString());
                        startActivityForResult(intent, REQUEST_CODE_IMAGE_EDITING);


                    } else if (data.getClipData() != null) {

                        Log.i("ConnectBase Clip Data", data.getClipData().toString());

                        ClipData clipData = data.getClipData();
                        ArrayList<Uri> uriList = new ArrayList<>();
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            ClipData.Item item = clipData.getItemAt(i);
                            uriList.add(item.getUri());

                        }

                        if (uriList.size() == 1) {

                            Intent intent = new Intent(this, ImageEditingActivity.class);
                            File file = createFileFromUri(uriList.get(0), "image");
                            intent.putExtra("path", file.getPath());
                            startActivityForResult(intent, REQUEST_CODE_IMAGE_EDITING);

                        } else {
                            Bundle bundle = new Bundle();
                            ArrayList<String> list = new ArrayList<>();
                            for (int i = 0; i < Math.min(20, uriList.size()); i++)
                                list.add(createFileFromUri(uriList.get(i), "image").getPath());
                            bundle.putStringArrayList("pathList", list);
                            if (uriList.size() > 20) {
                                Snackbar.make(chatList, "Loading first 20 images..", Snackbar.LENGTH_SHORT).show();
                            }

                            Intent intent = new Intent(this, ViewImagesActivity.class);

                            intent.putExtra("bundle", bundle);
                            startActivityForResult(intent, REQUEST_CODE_VIEW_IMAGES);

                        }

                    }
                } else if (resultCode == RESULT_CANCELED) {
                    Snackbar.make(chatList, "Oops, Action Cancelled!!", Snackbar.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_CODE_IMAGE_EDITING:
                if (resultCode == RESULT_OK) {

                    Bundle bundle = data.getBundleExtra("uriBundle");

                    Log.i("BundleData", bundle.getString("desc"));
                    Log.i("BundleData", bundle.getString("path"));

                    sendImageMessage(bundle.getString("desc"), bundle.getString("path"));

                } else if (resultCode == RESULT_CANCELED) {
                    Snackbar.make(chatList, "Oops, Action Cancelled!!", Snackbar.LENGTH_SHORT).show();
                }

                break;
            case REQUEST_CODE_VIEW_IMAGES:
                if (resultCode == RESULT_OK) {

                    Bundle bundle = data.getBundleExtra("bundle");
                    ArrayList<String> pathList, descList;
                    pathList = bundle.getStringArrayList("pathList");
                    descList = bundle.getStringArrayList("descList");
                    for (int i = 0; i < pathList.size(); i++)
                        sendImageMessage(descList.get(i), pathList.get(i));


                } else if (resultCode == RESULT_CANCELED) {
                    Snackbar.make(chatList, "Oops, Action Cancelled!!", Snackbar.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_CODE_FILE:
                if (resultCode == RESULT_OK) {

                    Bundle bundle = new Bundle();
                    ArrayList<String> pathList = new ArrayList<>();
                    if (data != null) {
                        if (data.getData() != null) {
                            pathList.add(createFileFromUri(data.getData(), "file").getPath());
                            Log.i("ConnectBase FileUri", data.getData().toString());
                            //sendFileMessage(data.getData());
                        } else if (data.getClipData() != null) {
                            ClipData clipData = data.getClipData();
                            for (int i = 0; i < clipData.getItemCount(); i++)
                                pathList.add(createFileFromUri(clipData.getItemAt(i).getUri(), "file").getPath());
                            //  sendFileMessage(clipData.getItemAt(i).getUri());
                        }
                        Intent intent = new Intent(this, ViewFilesActivity.class);
                        bundle.putStringArrayList("pathList", pathList);
                        intent.putExtra("bundle", bundle);
                        startActivityForResult(intent, REQUEST_CODE_VIEW_FILES);
                    } else {
                        Snackbar.make(chatList, "Oops!!, There was some problem retrieving files.", Snackbar.LENGTH_SHORT).show();
                    }


                } else if (resultCode == RESULT_CANCELED) {

                    Snackbar.make(chatList, "Oops, Action Cancelled!!", Snackbar.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_CODE_VIEW_FILES:

                if (resultCode == RESULT_OK) {

                    Bundle bundle = data.getBundleExtra("bundle");
                    ArrayList<String> pathList, descList;
                    pathList = bundle.getStringArrayList("pathList");
                    descList = bundle.getStringArrayList("descList");
                    for (int i = 0; i < pathList.size(); i++)
                        sendFileMessage(pathList.get(i), descList.get(i));


                } else if (resultCode == RESULT_CANCELED) {
                    Snackbar.make(chatList, "Oops, Action Cancelled!!", Snackbar.LENGTH_SHORT).show();
                }
                break;

        }
    }

    private void sendFileMessage(String path, String desc) {

        File file = new File(path);
        Uri fileUri = commonFunctions.getUriFromFile(getApplicationContext(), file);
        Log.i("SendFile", fileUri.toString());
        if (!commonFunctions.checkInternetConnection(this)) {
            Snackbar.make(chatList, "No Internet Connection!!", Snackbar.LENGTH_SHORT).show();
            //TODO: Add queue of messages to send in future
            return;
        }

        if (chatId == null) {
            Snackbar.make(chatList, "No Internet Connection!!", Snackbar.LENGTH_SHORT).show();
            generateChatId();
            return;
        }

        if (!file.exists()) {
            Snackbar.make(chatList, "File Not Found", Snackbar.LENGTH_SHORT).show();
            return;
        }
        Log.i("ConnectBase Path", file.getPath());

        HashMap hashMap = new HashMap();

        hashMap.put("sender", currentId);
        hashMap.put("messageType", "file");
        hashMap.put("description", desc);
        hashMap.put("fileUrl", "");
        hashMap.put("status", "");
        hashMap.put("fileName", file.getName());
        hashMap.put("time", ServerValue.TIMESTAMP);
        hashMap.put("seen", "false");


        String pushKey = mChatReference.child(chatId).push().getKey();

        StorageReference fileReference = mChatFileReference.child(chatId).child(pushKey);

        //TODO: add notification for progress of file uploading

        UploadTask uploadTask = fileReference.putFile(fileUri);


        //Notification for uploading files

        int id = (int) new Date().getTime();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, ApplicationClass.NOTIFICATION_CHANNEL__UPLOAD);
        notificationBuilder.setOngoing(true)
                .setContentTitle("Uploading file...")
                // .addAction()
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setAutoCancel(false)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_upload))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setProgress(100, 0, false);

        uploadTask.addOnProgressListener(taskSnapshot -> {

            int percent = (int) ((taskSnapshot.getBytesTransferred() * 1.0 / taskSnapshot.getTotalByteCount()) * 100);
            Log.i("Progress", String.valueOf(percent));
            notificationBuilder.setProgress(100, percent, false)
                    .setContentText("Progress: " + percent + "%");
            notificationManager.notify(id, notificationBuilder.build());

        });


        uploadTask.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                fileReference.getDownloadUrl().addOnSuccessListener(uri1 -> {
                    hashMap.put("fileUrl", uri1.toString());
                    mChatReference.child(chatId).child(pushKey).setValue(hashMap);
                    sendFileToSentFolder(file, "file");
                    Snackbar.make(chatList, "Message Sent Successfully", Snackbar.LENGTH_SHORT).show();
                    notificationManager.cancel(id);
                });
            } else {
                Snackbar.make(chatList, task.getException().getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });


    }

    private File createFileFromUri(Uri uri, String type) {

        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File parentFile;
            if (type.equals("file")) {
                parentFile = new File(Environment.getExternalStorageDirectory() + "/ConnectBase/temp/Files/");
            } else
                parentFile = new File(Environment.getExternalStorageDirectory() + "/ConnectBase/temp/image/");
            parentFile.mkdirs();

            String name = "";//= uri.toString();
            Cursor cursor = getContentResolver().query(uri, null, null, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                name = cursor.getString(cursor.getColumnIndex("_display_name"));

                if (!name.contains(".")) {
                    int typeidx = cursor.getColumnIndex("mime_type");
                    if (typeidx > 0) {
                        String extension = ".file";
                        String mime_type = cursor.getString(typeidx);
                        if (mime_type.contains("audio"))
                            extension = ".mp3";
                        else if (mime_type.contains("pdf"))
                            extension = ".pdf";
                        else if (mime_type.contains("video"))
                            extension = ".mp4";
                        name += extension;
                    }
                }

              /*  for(int i=0;i<cursor.getColumnCount();i++)
                {
                    Log.i("ConnectBase Col",cursor.getColumnName(i));
                    Log.i("ConnectBase Data",cursor.getString(i));
                }*/
                cursor.close();
            } else {

                name = uri.toString();
                name = name.replace("%20", "-");
                name = name.replace("%2F", "/");
                name = name.replace("%3A", "/");
                name = name.replace("%2C", "");
                name = name.substring(name.lastIndexOf("/") + 1);
                if (!name.contains(".") && uri.toString().contains("photo") || uri.toString().contains("image"))
                    name += ".jpg";

            }

            File file = new File(parentFile, name);
            //f.setWritable(true, false);
            OutputStream outputStream = new FileOutputStream(file);

            commonFunctions.copyStream(inputStream, outputStream);
            return file;
        } catch (IOException e) {
            System.out.println("error in creating a file");
            e.printStackTrace();
        }

        return null;

    }

    private void sendImageMessage(String desc, String path) {

        Log.i("SendImageFile", path);
        if (!commonFunctions.checkInternetConnection(this)) {
            Snackbar.make(chatList, "No Internet Connection!!", Snackbar.LENGTH_SHORT).show();
            new File(path).delete();
            //TODO: Add queue of messages to send in future
            return;
        }


        if (chatId == null) {
            Snackbar.make(chatList, "No Internet Connection!!", Snackbar.LENGTH_SHORT).show();
            generateChatId();
            return;
        }

        HashMap hashMap = new HashMap();
        File imageFile = new File(path);

        if (!imageFile.exists()) {
            return;
        }


        hashMap.put("sender", currentId);
        hashMap.put("messageType", "image");
        hashMap.put("description", desc);
        hashMap.put("imageName", imageFile.getName());
        hashMap.put("imageUrl", "");
        hashMap.put("thumbImage", "");
        hashMap.put("status", "");
        hashMap.put("time", ServerValue.TIMESTAMP);
        hashMap.put("seen", "false");

        String pushKey = mChatReference.child(chatId).push().getKey();
        File thumbFile = commonFunctions.compressImage(this, imageFile, "/ConnectBase/temp/thumbImage", 250, 250, 25);


        StorageReference imageReference = mChatImageReference.child(chatId).child(pushKey + ".jpg");
        StorageReference thumbImageReference = mChatImageReference.child(chatId).child("ThumbImage").child(pushKey + ".jpg");

        Uri thumbImageUri = commonFunctions.getUriFromFile(getApplicationContext(), thumbFile);


        //TODO: add notification for progress of image uploading


        imageReference.putFile(commonFunctions.getUriFromFile(getApplicationContext(), imageFile)).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                imageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    hashMap.put("imageUrl", uri.toString());
                    thumbImageReference.putFile(thumbImageUri).addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            thumbImageReference.getDownloadUrl().addOnSuccessListener(uri1 -> {
                                hashMap.put("thumbImage", uri1.toString());
                                mChatReference.child(chatId).child(pushKey).setValue(hashMap);
                                Snackbar.make(chatList, "Message Sent Successfully", Snackbar.LENGTH_SHORT).show();
                                thumbFile.delete();
                                sendFileToSentFolder(imageFile, "image");

                                //TODO: Notify Adapter about dataset change
                            });
                        } else
                            commonFunctions.showErrorDialog(this, task1.getException().getMessage());
                    });
                });
            } else commonFunctions.showErrorDialog(this, task.getException().getMessage());

        });


    }


    private void sendFileToSentFolder(File inputFile, String type) {

        String path;
        if (type.equals("image"))
            path = "/ConnectBase/Media/Images/" + user.getName() + "\t\t" + id + "/sent";
        else
            path = "/ConnectBase/Media/Files/" + user.getName() + "\t\t" + id + "/sent";
        File parentOutput = new File(Environment.getExternalStorageDirectory() + path);
        parentOutput.mkdirs();
        File outputFile = new File(parentOutput, inputFile.getName());
        try {

            InputStream in = new FileInputStream(inputFile);
            OutputStream out = new FileOutputStream(outputFile);

            commonFunctions.copyStream(in, out);
            inputFile.delete();
        } catch (Exception e) {
            commonFunctions.showErrorDialog(this, e.getMessage());
        }

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

    private void openUserProfile() {

        Intent intent = new Intent(this, ViewUserProfile.class);
        intent.putExtra("user", user);
        intent.putExtra("id", id);
        startActivity(intent);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_STORAGE:
                if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    String message = "Reading and writing External Storage is required for Sending attachments";
                    commonFunctions.showErrorDialog(this, message);
                }
                break;
        }
    }

}