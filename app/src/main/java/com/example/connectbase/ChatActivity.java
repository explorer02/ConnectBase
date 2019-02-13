package com.example.connectbase;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
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
    DatabaseReference mChatReference, mFriendReference;
    final int REQUEST_CODE_GALLERY = 1031;
    EditText etMessage;
    String chatId = "";
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
    SQLiteDatabase chatDatabase;

    ArrayList<Pair> chatArray = new ArrayList<>();
    HashMap<String, Object> chatMap = new HashMap<>();
    ChatAdapter adapter;
    ImageView ivSend;
    SharedPreferences sharedPreferences;
    Query chatQuery;
    HashMap<String, Boolean> uploadStarted = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Toolbar toolbar = findViewById(R.id.toolbar_chat);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();

        actionBar.setDisplayShowCustomEnabled(true);

        mFriendReference = FirebaseDatabase.getInstance().getReference().child("Friends");
        mChatReference = FirebaseDatabase.getInstance().getReference().child("Chats");
        mChatImageReference = FirebaseStorage.getInstance().getReference().child("ChatImage");
        mChatFileReference = FirebaseStorage.getInstance().getReference().child("ChatFiles");

        ivSend = findViewById(R.id.iv_chat_send);
        etMessage = findViewById(R.id.et_chat_message);

        View view = getLayoutInflater().inflate(R.layout.layout_toolbar_chat_activity, null, false);
        actionBar.setCustomView(view, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        actionBar.setDisplayHomeAsUpEnabled(true);


        user = (Users) getIntent().getSerializableExtra("user");
        id = getIntent().getStringExtra("id");
        currentId = FirebaseAuth.getInstance().getUid();
        generateChatId();

        chatList = findViewById(R.id.list_chat);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        layoutManager.setSmoothScrollbarEnabled(true);
        chatList.setLayoutManager(layoutManager);

        adapter = new ChatAdapter();
        chatList.setAdapter(adapter);

        new LoadChatsFromDatabase().execute();

        TextView tvName = view.findViewById(R.id.tv_lTCA_name);
        tvName.setText(user.getName());
        CircleImageView ivProfilePic = view.findViewById(R.id.iv_lTCA_ivProfilePic);
        if (!user.getThumbImage().isEmpty()) {
            Picasso.get()
                    .load(user.getThumbImage())
                    .placeholder(R.drawable.avatar)
                    .into(ivProfilePic);
        }
        View linLay = view.findViewById(R.id.linLay_lTCA_view);
        linLay.setOnClickListener(v -> openUserProfile());
        ivSend.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            etMessage.setText(null);
            if (!message.isEmpty())
                sendMessage(message);
        });

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

        HashMap<String, Object> hashMap = new HashMap<>();

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

                    mChatReference.child(chatId).child(pushKey).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            ChatFile chatFile = dataSnapshot.getValue(ChatFile.class);
                            addMessageToDatabase(pushKey, chatFile, 1);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
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

        HashMap<String, Object> hashMap = new HashMap<>();
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
        mChatReference.child(chatId).child(pushKey).setValue(hashMap);
        sendFileToSentFolder(imageFile, "image");

/*
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

                                mChatReference.child(chatId).child(pushKey).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        ChatImage chatImage = dataSnapshot.getValue(ChatImage.class);
                                        addMessageToDatabase(pushKey, chatImage, 1);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });

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
*/


    }

    private void sendFileToSentFolder(File inputFile, String type) {

        String path;
        if (type.equals("image"))
            path = "/ConnectBase/Media/Images/sent";
        else
            path = "/ConnectBase/Media/Files/sent";
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

    void generateChatId() {

        sharedPreferences = getSharedPreferences("chatData", MODE_PRIVATE);
        String cid = sharedPreferences.getString("user_" + id + "_chat_id", "");

        if (!cid.isEmpty()) {
            chatId = cid;
            return;
        }

        mFriendReference.child(currentId).child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild("chatId"))
                    chatId = dataSnapshot.child("chatId").getValue().toString();

                else {
                    chatId = mChatReference.child(currentId).child(id).push().getKey();
                    mFriendReference.child(currentId).child(id).child("chatId").setValue(chatId);
                    mFriendReference.child(id).child(currentId).child("chatId").setValue(chatId);
                }
                sharedPreferences.edit()
                        .putString("user_" + id + "_chat_id", chatId)
                        .apply();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    void sendMessage(String message) {
        if (!commonFunctions.checkInternetConnection(this)) {

            Snackbar.make(chatList, "No Internet Connection!!", Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (chatId == null) {
            Snackbar.make(chatList, "No Internet Connection!!", Snackbar.LENGTH_SHORT).show();
            generateChatId();
            return;
        }

        HashMap<String, Object> map = new HashMap<>();
        map.put("messageType", "text");
        map.put("message", message);
        map.put("sender", currentId);
        map.put("time", ServerValue.TIMESTAMP);
        map.put("seen", "false");

        String pushKey = mChatReference.child(chatId).push().getKey();
        mChatReference.child(chatId).child(pushKey).setValue(map);


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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_chat_clear:
                new ClearChats().execute();
                break;
            case R.id.menu_chat_generatetext:
                for (int i = 1; i <= 20; i++)
                    sendMessage("Message (" + i + ")");
                break;
            case R.id.menu_chat_clearSharedpref:
                sharedPreferences.edit().clear().apply();
                break;
        }
        return true;
    }

    void addMessageToDatabase(String msgId, Object object, int sent) {

        ContentValues values = new ContentValues();
        String type = "";
        String seen = "";

        try {
            ContentValues messageMetaData = new ContentValues();
            messageMetaData.put("message_id", msgId);
            messageMetaData.put("sent", sent);
            values.put("message_id", msgId);


            if (object instanceof ChatMessage) {

                ChatMessage chatMessage = (ChatMessage) object;
                values.put("sender", chatMessage.getSender());
                values.put("message", chatMessage.getMessage());
                values.put("time", chatMessage.getTime());
                values.put("seen", chatMessage.getSeen());
                seen = chatMessage.getSeen();
                type = "text";


            } else if (object instanceof ChatImage) {

                ChatImage chatImage = (ChatImage) object;
                values.put("sender", chatImage.getSender());
                values.put("description", chatImage.getDescription());
                values.put("imageUrl", chatImage.getImageUrl());
                values.put("imageName", chatImage.getImageName());
                values.put("thumbImage", chatImage.getThumbImage());
                values.put("status", chatImage.getStatus());
                values.put("time", chatImage.getTime());
                values.put("seen", chatImage.getSeen());
                seen = chatImage.getSeen();
                type = "image";

            } else if (object instanceof ChatFile) {

                ChatFile chatFile = (ChatFile) object;
                values.put("sender", chatFile.getSender());
                values.put("description", chatFile.getDescription());
                values.put("fileUrl", chatFile.getFileUrl());
                values.put("fileName", chatFile.getFileName());
                values.put("status", chatFile.getStatus());
                values.put("time", chatFile.getTime());
                values.put("seen", chatFile.getSeen());
                seen = chatFile.getSeen();
                type = "file";

            }

            messageMetaData.put("message_type", type);
            long val1 = chatDatabase.insert("user_" + id, null, messageMetaData);
            long val2 = chatDatabase.insert("message_" + type, null, values);

            Log.i("ConnectBase", val1 + "\t\t" + val2);

            if (val1 != -1 && val2 != -1) {

                chatArray.add(new Pair(msgId, type));
                chatMap.put(msgId, object);
                adapter.notifyItemInserted(chatArray.size() - 1);
                chatList.scrollToPosition(chatArray.size() - 1);
            } else {
                if (seen.equals("true")) {
                    chatDatabase.execSQL("update message_" + type + " set seen='true' where message_id='" + msgId + "'");

                    switch (type) {
                        case "text":
                            ((ChatMessage) object).setSeen("true");
                            break;
                        case "image":
                            ((ChatImage) object).setSeen("true");
                            break;
                        case "file":
                            ((ChatFile) object).setSeen("true");
                            break;
                    }
                    chatMap.put(msgId, object);
                    int index = -1;
                    for (int i = 0; i < chatArray.size(); i++)
                        if (chatArray.get(i).getId().equals(msgId))
                            index = i;

                    adapter.notifyItemChanged(index);
                    updateSharedPreference(msgId);
                } else if (type.equals("image")) {
                    ChatImage chatImage = (ChatImage) object;
                    if (chatImage.getImageUrl().isEmpty())
                        return;
                    String imageUrl = chatImage.getImageUrl();
                    String thumbUrl = chatImage.getThumbImage();

                    chatDatabase.execSQL("update message_image set imageUrl='" + imageUrl + "',thumbImage='" + thumbUrl + "' where message_id='" + msgId + "'");

                    chatMap.put(msgId, object);
                    int index = -1;
                    for (int i = 0; i < chatArray.size(); i++)
                        if (chatArray.get(i).getId().equals(msgId))
                            index = i;
                    adapter.notifyItemChanged(index);

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void updateSharedPreference(String msgId) {
        sharedPreferences.edit()
                .putString("user_" + id + "_message_id", msgId)
                .apply();
    }

    void createTables() {

        chatDatabase = openOrCreateDatabase("chats", MODE_PRIVATE, null);

        chatDatabase.execSQL("create table if not exists user_" + id + "('message_id' varchar not null primary key,'message_type' varchar not null,sent int)");

        chatDatabase.execSQL("CREATE TABLE if not exists message_text('message_id' varchar NOT NULL ,'message' varchar NOT NULL,'sender' varchar NOT NULL,'time' varchar NOT NULL,'seen' varchar NOT NULL,PRIMARY KEY ('message_id'))");

        chatDatabase.execSQL("CREATE TABLE if not exists 'message_image' ('message_id' VARCHAR NOT NULL,'sender' VARCHAR NOT NULL,'description' VARCHAR NOT NULL,'imageName' VARCHAR NOT NULL,'imageUrl' VARCHAR NOT NULL,'thumbImage' VARCHAR NOT NULL,'status' VARCHAR NOT NULL,'time' varchar NOT NULL,'seen' VARCHAR NOT NULL,PRIMARY KEY ('message_id'))");

        chatDatabase.execSQL("CREATE TABLE if not exists 'message_file' ('message_id' VARCHAR NOT NULL,'sender' VARCHAR NOT NULL,'description' VARCHAR NOT NULL,'fileName' VARCHAR NOT NULL,'fileUrl' VARCHAR NOT NULL,'status' VARCHAR NOT NULL,'time' varchar NOT NULL,'seen' VARCHAR NOT NULL,PRIMARY KEY ('message_id'))");

        //message_id,sender,description,imageName,imageUrl,thumbImage,status,time,seen
    }

    void loadOnlineMessages() {

        String lastKey = sharedPreferences.getString("user_" + id + "_message_id", "");

        if (lastKey.isEmpty()) {
            chatQuery = mChatReference.child(chatId).orderByKey();
        } else chatQuery = mChatReference.child(chatId).orderByKey().startAt(lastKey);

        chatQuery.addChildEventListener(chatListener);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatQuery != null && chatListener != null)
            chatQuery.removeEventListener(chatListener);
        finish();

    }

    @Override
    protected void onStop() {
        super.onStop();
        String msgId = getLastSeenMessage();
        if (msgId != null)
            updateSharedPreference(msgId);
        if (chatQuery != null && chatListener != null)
            chatQuery.removeEventListener(chatListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        new Handler().postDelayed(this::loadOnlineMessages, 1000);

    }

    private String getLastSeenMessage() {
        String msgid = null;
        for (int i = 0; i < chatArray.size(); i++) {
            String key = chatArray.get(i).getId();
            String type = chatArray.get(i).getType();
            switch (type) {
                case "text":
                    String seen = ((ChatMessage) chatMap.get(key)).getSeen();
                    if (seen.equals("true"))
                        msgid = key;
                    break;
                case "image":
                    seen = ((ChatImage) chatMap.get(key)).getSeen();
                    if (seen.equals("true"))
                        msgid = key;
                    break;
                case "file":
                    seen = ((ChatFile) chatMap.get(key)).getSeen();
                    if (seen.equals("true"))
                        msgid = key;
                    break;
            }
        }
        return msgid;

    }

    @SuppressLint("StaticFieldLeak")
    class ClearChats extends AsyncTask<Void, Integer, Void> {


        ProgressDialog dialog = new ProgressDialog(ChatActivity.this);

        Cursor cursor;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            dialog.setTitle("Deleting Messages");
            dialog.setMessage("Please wait while deleting messages...");
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setProgress(0);
            dialog.show();
            dialog.setCancelable(false);
            cursor = chatDatabase.rawQuery("Select * from user_" + id, null, null);
            int max = cursor.getCount();
            dialog.setMax(max);

        }

        @Override
        protected Void doInBackground(Void... voids) {

            int curr = 0;
            try {
                cursor.moveToFirst();
                do {
                    curr++;
                    if (curr % 10 == 0)
                        publishProgress(curr);

                    String type = cursor.getString(1);
                    String msgid = cursor.getString(0);
                    chatDatabase.execSQL("delete from message_" + type + " where message_id='" + msgid + "'");
                }
                while (cursor.moveToNext());
                cursor.close();
                chatDatabase.execSQL("delete from user_" + id);
                chatMap.clear();
                chatArray.clear();
                adapter.notifyDataSetChanged();

            } catch (Exception e) {
                Snackbar.make(chatList, e.getMessage(), Snackbar.LENGTH_SHORT).show();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            chatArray.clear();
            chatMap.clear();
            adapter.notifyDataSetChanged();
            dialog.dismiss();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            dialog.setProgress(values[0]);
        }

    }

    public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        ChatAdapter() {

        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());

            switch (i) {
                case 0:
                    View view0 = inflater.inflate(R.layout.layout_row_chat_message, viewGroup, false);
                    return new ViewHolderMessage(view0);
                case 1:
                    View view1 = inflater.inflate(R.layout.layout_row_chat_image, viewGroup, false);
                    return new ViewHolderImage(view1);
                case 2:
                    return null;
            }
            return null;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {

            String msgId = chatArray.get(i).getId();
            Object object = chatMap.get(msgId);

            switch (getItemViewType(i)) {

                case 0:
                    ChatMessage chatMessage = ((ChatMessage) object);
                    ViewHolderMessage viewHolderMessage = ((ViewHolderMessage) viewHolder);
                    viewHolderMessage.tvTime.setText(commonFunctions.convertTime(chatMessage.getTime(), true));

                    String sender0 = chatMessage.getSender().equals(currentId) ? "You" : user.getName();
                    viewHolderMessage.tvName.setText(sender0);

                    if (chatMessage.getSender().equals(currentId)) {
                        viewHolderMessage.layout.setGravity(Gravity.END);
                        viewHolderMessage.tvName.setGravity(Gravity.END);
                        viewHolderMessage.ivSeen.setVisibility(View.VISIBLE);
                        switch (chatMessage.getSeen()) {
                            case "true":
                                viewHolderMessage.ivSeen.setImageResource(R.drawable.ic_circle_seen_blue);
                                break;
                            case "false":
                                viewHolderMessage.ivSeen.setImageResource(R.drawable.ic_circle_seen);
                                break;
                        }
                    } else {

                        viewHolderMessage.layout.setGravity(Gravity.START);
                        viewHolderMessage.tvName.setGravity(Gravity.START);
                        viewHolderMessage.ivSeen.setVisibility(View.GONE);
                        if (chatMessage.getSeen().equals("false"))
                            mChatReference.child(chatId).child(chatArray.get(i).id).child("seen").setValue("true");
                    }

                    viewHolderMessage.tvMessage.setText(chatMessage.getMessage());
                    break;

                case 1:
                    Log.i("ImageItem", "true");
                    ChatImage chatImage = ((ChatImage) object);
                    ViewHolderImage viewHolderImage = ((ViewHolderImage) viewHolder);
                    String sender1 = chatImage.getSender().equals(currentId) ? "You" : user.getName();

                    viewHolderImage.tvName.setText(sender1);
                    viewHolderImage.ivPic.setClickable(false);

                    viewHolderImage.tvDesc.setText(chatImage.getDescription());

                    viewHolderImage.tvTime.setText(commonFunctions.convertTime(chatImage.getTime(), true));

                    if (sender1.equals("You")) {

                        viewHolderImage.ivPic.setClickable(true);
                        viewHolderImage.layout.setGravity(Gravity.END);
                        viewHolderImage.tvName.setGravity(Gravity.END);
                        viewHolderImage.ivDownload.setVisibility(View.GONE);
                        viewHolderImage.ivSeen.setVisibility(View.VISIBLE);

                        if (chatImage.getImageUrl().isEmpty()) {

                            if (uploadStarted.get(msgId) != null && uploadStarted.get(msgId)) {
                                viewHolderImage.ivSeen.setVisibility(View.GONE);
                                return;
                            }

                            viewHolderImage.ivSeen.setVisibility(View.GONE);

                            uploadStarted.put(msgId, true);

                            File imageFile = new File(Environment.getExternalStorageDirectory() + "/ConnectBase/Media/Images/sent/" + chatImage.getImageName());
                            File thumbFile = commonFunctions.compressImage(ChatActivity.this, imageFile, "/ConnectBase/temp/thumbImage", 200, 200, 15);


                            StorageReference imageReference = mChatImageReference.child(chatId).child(msgId + ".jpg");
                            StorageReference thumbImageReference = mChatImageReference.child(chatId).child("ThumbImage").child(msgId + ".jpg");
                            Uri thumbImageUri = commonFunctions.getUriFromFile(getApplicationContext(), thumbFile);
                            Uri imageUri = commonFunctions.getUriFromFile(getApplicationContext(), imageFile);
                            viewHolderImage.ivPic.setImageURI(imageUri);
                            HashMap<String, Object> hashMap = new HashMap<>();
                            thumbImageReference.putFile(thumbImageUri).addOnSuccessListener(taskSnapshot -> {
                                thumbImageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                                    hashMap.put("thumbImage", uri.toString());
                                    mChatReference.child(chatId).child(msgId).updateChildren(hashMap);
                                });
                            });
                            UploadTask uploadTask = imageReference.putFile(imageUri);

                            uploadTask.addOnSuccessListener(taskSnapshot -> {
                                imageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                                    hashMap.put("imageUrl", uri.toString());
                                    mChatReference.child(chatId).child(msgId).updateChildren(hashMap);
                                });
                            });
                            uploadTask.addOnProgressListener(taskSnapshot -> {
                                int progress = (int) (taskSnapshot.getBytesTransferred() * 1.0 / taskSnapshot.getTotalByteCount()) * 100;
                                viewHolderImage.progressBar.setProgress(progress);
                            });

                        } else {
                            if (chatImage.getSeen().equals("true"))
                                viewHolderImage.ivSeen.setImageResource(R.drawable.ic_circle_seen_blue);
                            else viewHolderImage.ivSeen.setImageResource(R.drawable.ic_circle_seen);

                            viewHolderImage.progressBar.setProgress(100);
                            File imageFile = new File(Environment.getExternalStorageDirectory() + "/ConnectBase/Media/Images/sent/" + chatImage.getImageName());
                            if (imageFile.exists()) {
                                Uri imageUri = commonFunctions.getUriFromFile(getApplicationContext(), imageFile);
                                viewHolderImage.ivPic.setImageURI(imageUri);
                            } else {
                                //TODO: image sent but doesnt exist in storage;
                            }
                        }
                    } else {
                        viewHolderImage.layout.setGravity(Gravity.START);
                        viewHolderImage.tvName.setGravity(Gravity.START);
                        viewHolderImage.ivSeen.setVisibility(View.GONE);
                        viewHolderImage.ivDownload.setVisibility(View.GONE);


                        if (chatImage.getSeen().equals("false"))
                            mChatReference.child(chatId).child(chatArray.get(i).id).child("seen").setValue("true");

                        File parentFile = new File(Environment.getExternalStorageDirectory() + "/ConnectBase/Media/Images/received/");
                        parentFile.mkdirs();
                        File imageFile = new File(parentFile, chatImage.getImageName());

                        if (imageFile.exists()) {
                            Uri imageUri = commonFunctions.getUriFromFile(getApplicationContext(), imageFile);
                            viewHolderImage.ivPic.setImageURI(imageUri);
                            viewHolderImage.progressBar.setProgress(100);
                            viewHolderImage.ivPic.setClickable(true);

                        } else {
                            viewHolderImage.progressBar.setProgress(0);
                            File thumbFile = new File(Environment.getExternalStorageDirectory() + "/ConnectBase/temp/thumbImage/" + msgId + ".jpg");
                            if (thumbFile.exists()) {
                                viewHolderImage.ivPic.setImageURI(commonFunctions.getUriFromFile(getApplicationContext(), thumbFile));
                            }

                            viewHolderImage.ivDownload.setVisibility(View.VISIBLE);
                            viewHolderImage.ivDownload.setOnClickListener(v -> {

                                FileDownloadTask downloadTask = mChatImageReference.child(chatId).child(msgId + ".jpg").getFile(imageFile);
                                viewHolderImage.ivDownload.setVisibility(View.GONE);

                                downloadTask.addOnSuccessListener(taskSnapshot -> {

                                    viewHolderImage.ivDownload.setVisibility(View.GONE);
                                    notifyItemChanged(i);
                                });

                                downloadTask.addOnProgressListener(taskSnapshot -> {
                                    int progress = (int) (taskSnapshot.getBytesTransferred() * 1.0 / taskSnapshot.getTotalByteCount()) * 100;
                                    viewHolderImage.progressBar.setProgress(progress);
                                });

                            });
                        }
                    }

                    viewHolderImage.ivPic.setOnClickListener(v -> {
                        File parent = new File(Environment.getExternalStorageDirectory() + "/ConnectBase/Media/Images/");
                        File imageFile;
                        if (sender1.equals("You"))
                            imageFile = new File(parent, "/sent/" + chatImage.getImageName());
                        else imageFile = new File(parent, "/received/" + chatImage.getImageName());
                        startActivity(new Intent(ChatActivity.this, ZoomImageViewActivity.class).putExtra("path", imageFile.getPath()));
                    });

                    break;

            }

        }

        @Override
        public int getItemCount() {
            return chatArray.size();
        }

        @Override
        public int getItemViewType(int position) {

            switch (chatArray.get(position).getType()) {

                case "text":
                    return 0;
                case "image":
                    return 1;
                case "file":
                    return 2;
            }
            return -1;
        }

        class ViewHolderMessage extends RecyclerView.ViewHolder {

            TextView tvName, tvMessage, tvTime;
            ImageView ivMore, ivSeen;
            LinearLayout layout;

            ViewHolderMessage(@NonNull View itemView) {
                super(itemView);

                tvMessage = itemView.findViewById(R.id.tv_lRCM_message);
                tvName = itemView.findViewById(R.id.tv_lRCM_name);
                tvTime = itemView.findViewById(R.id.tv_lRCM_time);
                ivMore = itemView.findViewById(R.id.iv_LRCM_more);
                ivSeen = itemView.findViewById(R.id.iv_LRCM_seen);
                layout = itemView.findViewById(R.id.linLay_lRCM);
            }
        }

        class ViewHolderImage extends RecyclerView.ViewHolder {

            TextView tvName, tvDesc, tvTime;
            ImageView ivMore, ivPic, ivDownload, ivSeen;
            LinearLayout layout;
            ProgressBar progressBar;

            ViewHolderImage(@NonNull View itemView) {
                super(itemView);

                tvDesc = itemView.findViewById(R.id.tv_lRCI_desc);
                tvName = itemView.findViewById(R.id.tv_lRCI_name);
                tvTime = itemView.findViewById(R.id.tv_lRCI_time);
                ivMore = itemView.findViewById(R.id.iv_lRCI_more);
                ivPic = itemView.findViewById(R.id.iv_lRCI_image);
                ivDownload = itemView.findViewById(R.id.iv_lRCI_download);
                ivSeen = itemView.findViewById(R.id.iv_lRCI_seen);
                layout = itemView.findViewById(R.id.linLay_lRCI);
                progressBar = itemView.findViewById(R.id.pb_lRCI_progress);
            }
        }
    }

    public class Pair {


        private String id, type;
        public Pair(String id, String type) {
            this.id = id;
            this.type = type;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }


    }

    @SuppressLint("StaticFieldLeak")
    public class LoadChatsFromDatabase extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                chatArray.clear();
                chatMap.clear();

                createTables();
                chatDatabase = openOrCreateDatabase("chats", MODE_PRIVATE, null);


                Cursor cursor = chatDatabase.rawQuery("Select * from user_" + id, null, null);

                if (cursor == null || cursor.getCount() == 0) {
                    return null;
                }
                cursor.moveToFirst();
                int idIdx = cursor.getColumnIndex("message_id");
                int typeIdx = cursor.getColumnIndex("message_type");
                do {
                    String message_id = cursor.getString(idIdx);
                    String type = cursor.getString(typeIdx);
                    Cursor cursor1;
                    switch (type) {
                        case "text":
                            cursor1 = chatDatabase.rawQuery("Select * from message_text where message_id='" + message_id + "'", null, null);
                            int msgIdx = cursor1.getColumnIndex("message");
                            int senderIdx = cursor1.getColumnIndex("sender");
                            int timeIdx = cursor1.getColumnIndex("time");
                            int seenIdx = cursor1.getColumnIndex("seen");
                            cursor1.moveToFirst();
                            String message = cursor1.getString(msgIdx);
                            String sender = cursor1.getString(senderIdx);
                            Long time = Long.parseLong(cursor1.getString(timeIdx));
                            String seen = cursor1.getString(seenIdx);
                            ChatMessage chatMessage = new ChatMessage("text", message, sender, time, seen);
                            chatArray.add(new Pair(message_id, type));
                            chatMap.put(message_id, chatMessage);
                            publishProgress();
                            cursor1.close();
                            break;

                        case "image":
                            cursor1 = chatDatabase.rawQuery("Select * from message_image where message_id='" + message_id + "'", null, null);
                            int descIdx = cursor1.getColumnIndex("description");
                            senderIdx = cursor1.getColumnIndex("sender");
                            timeIdx = cursor1.getColumnIndex("time");
                            seenIdx = cursor1.getColumnIndex("seen");
                            int imgNameIdx = cursor1.getColumnIndex("imageName");
                            int imgUrlIdx = cursor1.getColumnIndex("imageUrl");
                            int thumbIdx = cursor1.getColumnIndex("thumbImage");
                            int statusIdx = cursor1.getColumnIndex("status");
                            cursor1.moveToFirst();
                            ChatImage chatImage = new ChatImage(cursor1.getString(senderIdx),
                                    "image",
                                    cursor1.getString(descIdx),
                                    cursor1.getString(imgNameIdx),
                                    cursor1.getString(imgUrlIdx),
                                    cursor1.getString(thumbIdx),
                                    Long.parseLong(cursor1.getString(timeIdx)),
                                    cursor1.getString(statusIdx),
                                    cursor1.getString(seenIdx)
                            );
                            chatArray.add(new Pair(message_id, type));
                            chatMap.put(message_id, chatImage);
                            publishProgress();
                            cursor1.close();
                            break;
                    }

                }
                while (cursor.moveToNext());

                cursor.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            adapter.notifyDataSetChanged();
            chatList.scrollToPosition(chatArray.size() - 1);
        }

    }

    ChildEventListener chatListener = new ChildEventListener() {
        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            if (dataSnapshot.hasChild("messageType")) {

                String type = dataSnapshot.child("messageType").getValue().toString();
                String sender = dataSnapshot.child("sender").getValue().toString();
                String key = dataSnapshot.getKey();

                switch (type) {
                    case "text":
                        ChatMessage chatMessage = dataSnapshot.getValue(ChatMessage.class);
                        Log.i("DataSnap", chatMessage.getMessage());
                        int sentKey = chatMessage.getSender().equals(currentId) ? 1 : -1;
                        addMessageToDatabase(key, chatMessage, sentKey);
                        break;
                    case "image":
                        if (sender.equals(currentId)) {
                            ChatImage chatImage = dataSnapshot.getValue(ChatImage.class);
                            addMessageToDatabase(key, chatImage, 0);
                        } else {
                            String image = dataSnapshot.child("imageUrl").getValue().toString();
                            if (!image.isEmpty()) {
                                ChatImage chatImage = dataSnapshot.getValue(ChatImage.class);
                                addMessageToDatabase(key, chatImage, -1);
                            }
                        }
                        break;

                }
            }

        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            if (!dataSnapshot.hasChild("messageType"))
                return;

            String type = dataSnapshot.child("messageType").getValue().toString();
            String sender = dataSnapshot.child("sender").getValue().toString();
            String key = dataSnapshot.getKey();

            if (sender.equals(currentId)) {

                switch (type) {
                    case "text":
                        ChatMessage chatMessage = dataSnapshot.getValue(ChatMessage.class);
                        addMessageToDatabase(key, chatMessage, 1);
                        break;
                    case "image":
                        String image = dataSnapshot.child("imageUrl").getValue().toString();
                        if (!image.isEmpty())
                            addMessageToDatabase(key, dataSnapshot.getValue(ChatImage.class), 1);
                        break;
                }
            } else {
                switch (type) {
                    case "image":
                        String image = dataSnapshot.child("imageUrl").getValue().toString();
                        String thumbImage = dataSnapshot.child("thumbImage").getValue().toString();
                        if (!image.isEmpty() && !thumbImage.isEmpty()) {
                            downloadThumbImage(dataSnapshot.getKey());
                            ChatImage chatImage = dataSnapshot.getValue(ChatImage.class);
                            new Handler().postDelayed(() -> addMessageToDatabase(key, chatImage, -1), 1000);
                        }
                        break;
                }
            }

        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };

    private void downloadThumbImage(String key) {

        File parentFile = new File(Environment.getExternalStorageDirectory() + "/ConnectBase/temp/thumbImage/");
        parentFile.mkdirs();
        File thumbFile = new File(parentFile, key + ".jpg");
        Log.i("DownloadStarted", "true");
        mChatImageReference.child(chatId).child("ThumbImage").child(key + ".jpg").getFile(thumbFile);

    }
}

/*if(ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},121212);
            return;
        }*/