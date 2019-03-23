package com.example.connectbase;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
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
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPopupHelper;
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
import android.widget.Button;
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
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    Users user;
    String id, currentId;
    DatabaseReference mChatReference, mFriendReference, mUserReference;
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

    String lastKey;

    ArrayList<Pair> chatArray = new ArrayList<>();
    HashMap<String, Object> chatMap = new HashMap<>();
    ChatAdapter adapter;
    ImageView ivSend;
    SharedPreferences sharedPreferences;
    Query chatQuery;

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
        mUserReference = FirebaseDatabase.getInstance().getReference().child("Users");
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
        sharedPreferences = getSharedPreferences("chatData", MODE_PRIVATE);
        if (checkFriends(id))
            generateChatId();
        else {
            etMessage.setHint("You can no longer reply to this conversation");
            etMessage.setEnabled(false);
        }

        chatList = findViewById(R.id.list_chat);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        layoutManager.setSmoothScrollbarEnabled(true);
        chatList.setLayoutManager(layoutManager);

        ImageView scrollUp, scrollDown;
        scrollDown = findViewById(R.id.btn_chat_gotoDown);
        scrollUp = findViewById(R.id.btn_chat_gotoUp);


        scrollDown.setOnClickListener(v -> {
            if (chatArray.size() > 0)
                chatList.smoothScrollToPosition(chatArray.size() - 1);
        });
        scrollUp.setOnClickListener(v -> {
            if (chatArray.size() > 0)
                chatList.smoothScrollToPosition(0);
        });

        adapter = new ChatAdapter();
        chatList.setAdapter(adapter);

        new LoadChatsFromDatabase().execute();

        TextView tvName = view.findViewById(R.id.tv_lTCA_name);
        TextView tvOnline = view.findViewById(R.id.tv_lTCA_online);
        CircleImageView ivProfilePic = view.findViewById(R.id.iv_lTCA_ivProfilePic);
        tvOnline.setSelected(true);

        mUserReference.child(id).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild("online")) {
                    String online = dataSnapshot.child("online").getValue().toString();
                    if (online.equals("true")) {
                        tvOnline.setText("Online. Tap for more Info...");
                    } else
                        tvOnline.setText(commonFunctions.getlastOnline(Long.parseLong(online)) + " . Tap for more Info...");
                }
                user = dataSnapshot.getValue(Users.class);
                tvName.setText(user.getName());
                if (!user.getThumbImage().isEmpty()) {
                    Picasso.get()
                            .load(user.getThumbImage())
                            .placeholder(R.drawable.avatar)
                            .into(ivProfilePic);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        mUserReference.child(id).keepSynced(true);


        tvName.setText(user.getName());

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

    private boolean checkFriends(String id) {

        try {
            SQLiteDatabase userDatabase = openOrCreateDatabase("users", MODE_PRIVATE, null);
            Cursor cursor = userDatabase.rawQuery("Select * from friends where id='" + id + "'", null, null);

            if (cursor.getCount() == 0)
                return false;

            cursor.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void addUserTableToDatabase() {
        chatDatabase.execSQL("create table if not exists user_list('user_id' varchar not null primary key)");
        try {
            chatDatabase.execSQL("insert into user_list values('" + id + "')");
        } catch (SQLiteConstraintException e) {
            e.printStackTrace();
        }
    }

    public void addAttachment(View view) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE);
            return;
        }

        InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (getCurrentFocus() != null && getCurrentFocus().getWindowToken() != null)
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
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                File file = createFileFromUri(clipData.getItemAt(i).getUri(), "file");
                                if (file != null) {
                                    pathList.add(file.getPath());
                                }
                            }
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


        HashMap<String, Object> hashMap = new HashMap<>();

        hashMap.put("sender", currentId);
        hashMap.put("messageType", "file");
        hashMap.put("description", desc);
        hashMap.put("fileUrl", "");
        hashMap.put("size", file.length());
        hashMap.put("fileName", file.getName());
        hashMap.put("time", ServerValue.TIMESTAMP);
        hashMap.put("seen", "false");


        String pushKey = mChatReference.child(chatId).push().getKey();
        mChatReference.child(chatId).child(pushKey).setValue(hashMap);
        sendFileToSentFolder(file, "file");

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
        hashMap.put("time", ServerValue.TIMESTAMP);
        hashMap.put("seen", "false");

        String pushKey = mChatReference.child(chatId).push().getKey();
        mChatReference.child(chatId).child(pushKey).setValue(hashMap);
        sendFileToSentFolder(imageFile, "image");

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_chat_clear:
                new ClearChats().execute();
                break;
            case R.id.menu_chat_generatetext:
                for (int i = 1; i <= 50; i++)
                    sendMessage("Message (" + i + ")");
                break;
            case R.id.menu_chat_clearSharedpref:
                sharedPreferences.edit().clear().apply();
                break;
            case R.id.menu_chat_save:
                new SaveChats().execute();
                break;

        }
        return true;
    }

    void generateChatId() {

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
                values.put("time", chatFile.getTime());
                values.put("size", chatFile.getSize());
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

                if (type.equals("text")) {
                    new Handler().post(() -> {
                        if (sent == 1)
                            playSound(true);
                        else if (sent == -1)
                            playSound(false);
                    });
                }
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

                    Cursor urlCursor = chatDatabase.rawQuery("select imageUrl from message_image where message_id='" + msgId + "'", null, null);
                    urlCursor.moveToFirst();
                    String imageUrl1 = urlCursor.getString(0);
                    urlCursor.close();
                    if (imageUrl1.equals(imageUrl))
                        return;

                    chatDatabase.execSQL("update message_image set imageUrl='" + imageUrl + "',thumbImage='" + thumbUrl + "' where message_id='" + msgId + "'");

                    chatMap.put(msgId, object);
                    int index = -1;
                    for (int i = 0; i < chatArray.size(); i++)
                        if (chatArray.get(i).getId().equals(msgId))
                            index = i;
                    adapter.notifyItemChanged(index);

                    new Handler().post(() -> {
                        if (sent == 1)
                            playSound(true);
                        else if (sent == -1)
                            playSound(false);
                    });

                } else if (type.equals("file")) {
                    ChatFile chatFile = (ChatFile) object;

                    if (chatFile.getFileUrl().isEmpty())
                        return;
                    String fileUrl = chatFile.getFileUrl();

                    Cursor urlCursor = chatDatabase.rawQuery("select fileUrl from message_file where message_id='" + msgId + "'", null, null);
                    urlCursor.moveToFirst();
                    String fileUrl1 = urlCursor.getString(0);
                    urlCursor.close();
                    if (fileUrl1.equals(fileUrl))
                        return;

                    chatDatabase.execSQL("update message_file set fileUrl='" + fileUrl + "'where message_id='" + msgId + "'");

                    chatMap.put(msgId, object);
                    int index = -1;
                    for (int i = 0; i < chatArray.size(); i++)
                        if (chatArray.get(i).getId().equals(msgId))
                            index = i;
                    adapter.notifyItemChanged(index);
                    new Handler().post(() -> {
                        if (sent == 1)
                            playSound(true);
                        else if (sent == -1)
                            playSound(false);
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    void playSound(boolean sender) {

        MediaPlayer mediaPlayer;

        if (sender) {
            mediaPlayer = MediaPlayer.create(this, R.raw.tick_message);
            mediaPlayer.start();
        } else {
            mediaPlayer = MediaPlayer.create(this, R.raw.piunn);
            mediaPlayer.start();
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

        chatDatabase.execSQL("CREATE TABLE if not exists 'message_image' ('message_id' VARCHAR NOT NULL,'sender' VARCHAR NOT NULL,'description' VARCHAR NOT NULL,'imageName' VARCHAR NOT NULL,'imageUrl' VARCHAR NOT NULL,'thumbImage' VARCHAR NOT NULL,'time' varchar NOT NULL,'seen' VARCHAR NOT NULL,PRIMARY KEY ('message_id'))");

        chatDatabase.execSQL("CREATE TABLE if not exists 'message_file' ('message_id' VARCHAR NOT NULL,'sender' VARCHAR NOT NULL,'description' VARCHAR NOT NULL,'fileName' VARCHAR NOT NULL,'fileUrl' VARCHAR NOT NULL,'size' VARCHAR NOT NULL,'time' varchar NOT NULL,'seen' VARCHAR NOT NULL,PRIMARY KEY ('message_id'))");

    }

    void loadOnlineMessages() {

        lastKey = sharedPreferences.getString("user_" + id + "_message_id", "");

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
        addUserTableToDatabase();
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
        for (int i = chatArray.size() - 1; i >= 0; i--) {
            String key = chatArray.get(i).getId();
            String type = chatArray.get(i).getType();
            switch (type) {
                case "text":
                    String seen = ((ChatMessage) chatMap.get(key)).getSeen();
                    if (seen.equals("true"))
                        return key;
                    break;
                case "image":
                    seen = ((ChatImage) chatMap.get(key)).getSeen();
                    if (seen.equals("true"))
                        return key;
                    break;
                case "file":
                    seen = ((ChatFile) chatMap.get(key)).getSeen();
                    if (seen.equals("true"))
                        return key;
                    break;
            }
        }
        return null;

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

            String lastSeen = getLastSeenMessage();
            if (lastSeen != null)
                updateSharedPreference(lastSeen);

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
            int size = chatArray.size();
            chatArray.clear();
            chatMap.clear();
            adapter.notifyItemRangeRemoved(0, size);
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
                    View view2 = inflater.inflate(R.layout.layout_row_chat_file, viewGroup, false);
                    return new ViewHolderFile(view2);
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

                    viewHolderMessage.ivMore.setOnClickListener(v -> {
                        showPopupMenu(v, 0, i);
                    });

                    viewHolderMessage.tvMessage.setText(chatMessage.getMessage());
                    break;

                case 1:
                    ChatImage chatImage = ((ChatImage) object);
                    ViewHolderImage viewHolderImage = ((ViewHolderImage) viewHolder);
                    String sender1 = chatImage.getSender().equals(currentId) ? "You" : user.getName();

                    viewHolderImage.tvName.setText(sender1);
                    viewHolderImage.ivPic.setClickable(false);

                    String description = chatImage.getDescription().trim();
                    viewHolderImage.tvDesc.setText(description);
                    viewHolderImage.tvTime.setText(commonFunctions.convertTime(chatImage.getTime(), true));

                    if (sender1.equals("You")) {

                        viewHolderImage.ivPic.setClickable(true);
                        viewHolderImage.layout.setGravity(Gravity.END);
                        viewHolderImage.tvName.setGravity(Gravity.END);
                        viewHolderImage.ivDownload.setVisibility(View.GONE);
                        viewHolderImage.ivSeen.setVisibility(View.VISIBLE);

                        if (chatImage.getImageUrl().isEmpty()) {

                            viewHolderImage.ivSeen.setVisibility(View.GONE);

                            File imageFile = new File(Environment.getExternalStorageDirectory() + "/ConnectBase/Media/Images/sent/" + chatImage.getImageName());

                            Uri imageUri = commonFunctions.getUriFromFile(getApplicationContext(), imageFile);
                            viewHolderImage.ivPic.setImageURI(imageUri);

                            if (ApplicationClass.uploadTaskHashMap.get(msgId) == null) {


                                File thumbFile = commonFunctions.compressImage(ChatActivity.this, imageFile, "/ConnectBase/temp/thumbImage", 180, 180, 10);

                                StorageReference imageReference = mChatImageReference.child(chatId).child(msgId + ".jpg");
                                StorageReference thumbImageReference = mChatImageReference.child(chatId).child("ThumbImage").child(msgId + ".jpg");
                                Uri thumbImageUri = commonFunctions.getUriFromFile(getApplicationContext(), thumbFile);

                                HashMap<String, Object> hashMap = new HashMap<>();
                                thumbImageReference.putFile(thumbImageUri).addOnSuccessListener(taskSnapshot -> {
                                    thumbImageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                                        hashMap.put("thumbImage", uri.toString());
                                        mChatReference.child(chatId).child(msgId).updateChildren(hashMap);
                                    });
                                });
                                UploadTask uploadTask = imageReference.putFile(imageUri);
                                ApplicationClass.uploadTaskHashMap.put(msgId, uploadTask);

                                uploadTask.addOnSuccessListener(taskSnapshot -> {
                                    imageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                                        hashMap.put("imageUrl", uri.toString());
                                        mChatReference.child(chatId).child(msgId).updateChildren(hashMap);
                                        ApplicationClass.uploadTaskHashMap.remove(msgId);
                                    });
                                });

                                uploadTask.addOnProgressListener(taskSnapshot -> {
                                    int progress = (int) (taskSnapshot.getBytesTransferred() * 1.0 / taskSnapshot.getTotalByteCount()) * 100;
                                    viewHolderImage.progressBar.setProgress(progress);
                                });

                            } else {
                                ApplicationClass.uploadTaskHashMap.get(msgId).addOnProgressListener(taskSnapshot -> {
                                    int progress = (int) (taskSnapshot.getBytesTransferred() * 1.0 / taskSnapshot.getTotalByteCount()) * 100;
                                    viewHolderImage.progressBar.setProgress(progress);
                                });
                            }


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
                                File thumbFile = new File(Environment.getExternalStorageDirectory() + "/ConnectBase/temp/thumbImage/" + chatImage.getImageName());

                                if (thumbFile.exists()) {
                                    viewHolderImage.ivPic.setImageURI(commonFunctions.getUriFromFile(getApplicationContext(), thumbFile));
                                }
                            }
                        }
                    } else {
                        viewHolderImage.layout.setGravity(Gravity.START);
                        viewHolderImage.tvName.setGravity(Gravity.START);
                        viewHolderImage.ivSeen.setVisibility(View.GONE);
                        viewHolderImage.ivDownload.setVisibility(View.GONE);

                        if (description.isEmpty())
                            viewHolderImage.tvDesc.setVisibility(View.GONE);
                        else viewHolderImage.tvDesc.setVisibility(View.VISIBLE);


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

                    viewHolderImage.ivMore.setOnClickListener(v -> {
                        showPopupMenu(v, 1, i);
                    });

                    break;

                case 2:
                    ChatFile chatFile = ((ChatFile) object);
                    ViewHolderFile viewHolderFile = ((ViewHolderFile) viewHolder);
                    viewHolderFile.tvTime.setText(commonFunctions.convertTime(chatFile.getTime(), true));

                    String sender2 = chatFile.getSender().equals(currentId) ? "You" : user.getName();
                    viewHolderFile.tvName.setText(sender2);
                    viewHolderFile.tvTime.setText(commonFunctions.convertTime(chatFile.getTime(), true));
                    String name = chatFile.getFileName().substring(0, chatFile.getFileName().lastIndexOf("."));
                    String extension = chatFile.getFileName().substring(chatFile.getFileName().lastIndexOf(".") + 1);
                    if (name.length() > 15)
                        name = name.substring(0, 15) + "...";

                    String type = extension;
                    switch (extension) {
                        case "jpg":
                            type = "Image";
                            break;
                        case "mp3":
                            type = "Audio";
                            break;
                        case "mp4":
                            type = "Video";
                            break;
                        case "pdf":
                            type = "Document/PDF";
                            break;
                    }
                    double size = chatFile.getSize() / (1000 * 1000.0);

                    String desc = chatFile.getDescription();
                    String details = "Name: " + name
                            + "\nType: " + type
                            + "\nSize: " + Math.round(size * 100) / 100.0 + " MB";
                    if (!desc.isEmpty())
                        details += "\nDesc: " + desc;

                    viewHolderFile.tvFileDetails.setText(details);


                    if (sender2.equals("You")) {
                        viewHolderFile.ivDownload.setVisibility(View.GONE);
                        viewHolderFile.layout.setGravity(Gravity.END);
                        viewHolderFile.tvName.setGravity(Gravity.END);


                        if (chatFile.getFileUrl().isEmpty()) {
                            viewHolderFile.ivSeen.setVisibility(View.GONE);

                            if (!ApplicationClass.uploadTaskHashMap.containsKey(msgId)) {

                                File file = new File(Environment.getExternalStorageDirectory() + "/ConnectBase/Media/Files/sent/" + chatFile.getFileName());

                                UploadTask uploadTask = mChatFileReference.child(chatId).child(msgId).putFile(commonFunctions.getUriFromFile(getApplicationContext(), file));
                                ApplicationClass.uploadTaskHashMap.put(msgId, uploadTask);

                                uploadTask.addOnProgressListener(taskSnapshot -> {
                                    int progress = (int) ((taskSnapshot.getBytesTransferred() * 1.0 / taskSnapshot.getTotalByteCount()) * 100);
                                    viewHolderFile.progressBar.setProgress(progress);
                                });

                                uploadTask.addOnSuccessListener(taskSnapshot -> {
                                    HashMap<String, Object> hashMap = new HashMap<>();
                                    mChatFileReference.child(chatId).child(msgId).getDownloadUrl().addOnSuccessListener(uri -> {
                                        hashMap.put("fileUrl", uri.toString());
                                        mChatReference.child(chatId).child(msgId).updateChildren(hashMap);
                                        notifyItemChanged(i);
                                        ApplicationClass.uploadTaskHashMap.remove(msgId);
                                    });
                                });

                            } else {
                                ApplicationClass.uploadTaskHashMap.get(msgId).addOnProgressListener(taskSnapshot -> {
                                    int progress = (int) ((taskSnapshot.getBytesTransferred() * 1.0 / taskSnapshot.getTotalByteCount()) * 100);
                                    viewHolderFile.progressBar.setProgress(progress);
                                });
                            }

                        } else {
                            viewHolderFile.progressBar.setProgress(100);
                            if (chatFile.getSeen().equals("true")) {
                                viewHolderFile.ivSeen.setVisibility(View.VISIBLE);
                                viewHolderFile.ivSeen.setImageResource(R.drawable.ic_circle_seen_blue);
                            } else if (chatFile.getSeen().equals("false")) {
                                viewHolderFile.ivSeen.setVisibility(View.VISIBLE);
                                viewHolderFile.ivSeen.setImageResource(R.drawable.ic_circle_seen);
                            }
                        }

                    } else {
                        viewHolderFile.layout.setGravity(Gravity.START);
                        viewHolderFile.tvName.setGravity(Gravity.START);
                        if (chatFile.getSeen().equals("false"))
                            mChatReference.child(chatId).child(msgId).child("seen").setValue("true");
                        viewHolderFile.ivSeen.setVisibility(View.GONE);

                        File parentFile = new File(Environment.getExternalStorageDirectory() + "/ConnectBase/Media/Files/received/");
                        parentFile.mkdirs();
                        File file = new File(parentFile, chatFile.getFileName());

                        if (!file.exists() || ApplicationClass.downloadTaskHashMap.containsKey(msgId) || file.length() != chatFile.getSize()) {
                            viewHolderFile.ivDownload.setVisibility(View.VISIBLE);
                            if (!ApplicationClass.downloadTaskHashMap.containsKey(msgId))
                                viewHolderFile.ivDownload.setImageResource(R.drawable.ic_download);
                            else {
                                if (ApplicationClass.downloadTaskHashMap.get(msgId).isPaused())
                                    viewHolderFile.ivDownload.setImageResource(R.drawable.ic_download);
                                else
                                    viewHolderFile.ivDownload.setImageResource(R.drawable.ic_pause);
                            }
                            viewHolderFile.progressBar.setProgress(0);

                            viewHolderFile.ivDownload.setOnClickListener(v -> {

                                if (!ApplicationClass.downloadTaskHashMap.containsKey(msgId)) {
                                    FileDownloadTask downloadTask = mChatFileReference.child(chatId).child(msgId).getFile(file);
                                    viewHolderFile.ivDownload.setImageResource(R.drawable.ic_pause);

                                    ApplicationClass.downloadTaskHashMap.put(msgId, downloadTask);

                                    ApplicationClass.downloadTaskHashMap.get(msgId).addOnSuccessListener(taskSnapshot -> {
                                        ApplicationClass.downloadTaskHashMap.remove(msgId);
                                        viewHolderFile.ivDownload.setVisibility(View.GONE);
                                        notifyItemChanged(i);
                                    });

                                    ApplicationClass.downloadTaskHashMap.get(msgId).addOnProgressListener(taskSnapshot -> {
                                        int progress = (int) ((taskSnapshot.getBytesTransferred() * 1.0 / taskSnapshot.getTotalByteCount()) * 100);
                                        viewHolderFile.progressBar.setProgress(progress);
                                    });
                                } else if (ApplicationClass.downloadTaskHashMap.get(msgId).isPaused()) {
                                    viewHolderFile.ivDownload.setImageResource(R.drawable.ic_pause);
                                    ApplicationClass.downloadTaskHashMap.get(msgId).resume();

                                } else if (ApplicationClass.downloadTaskHashMap.get(msgId).isInProgress()) {
                                    viewHolderFile.ivDownload.setImageResource(R.drawable.ic_download);
                                    ApplicationClass.downloadTaskHashMap.get(msgId).pause();
                                }

                            });


                        } else {
                            viewHolderFile.ivDownload.setVisibility(View.GONE);
                            viewHolderFile.progressBar.setProgress(100);
                        }

                    }

                    viewHolderFile.layout.setOnClickListener(v -> {
                        String path = Environment.getExternalStorageDirectory() + "/ConnectBase/Media/Files/";
                        if (sender2.equals("You")) {
                            path += "sent/";
                        } else path += "received/";
                        path += chatFile.fileName;
                        File file = new File(path);
                        if (file.exists()) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(commonFunctions.getUriFromFile(getApplicationContext(), file), "*/*");
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(Intent.createChooser(intent, "Choose an Application to open with.."));
                        }
                    });

                    viewHolderFile.ivMore.setOnClickListener(v -> {
                        showPopupMenu(v, 2, i);
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
            ImageView ivMore, ivDownload, ivSeen, ivPic;

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

        class ViewHolderFile extends RecyclerView.ViewHolder {

            TextView tvName, tvFileDetails, tvTime;
            LinearLayout layout;
            ImageView ivSeen, ivMore, ivDownload;
            ProgressBar progressBar;

            ViewHolderFile(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_lRCF_name);
                tvFileDetails = itemView.findViewById(R.id.tv_lRCF_file);
                tvTime = itemView.findViewById(R.id.tv_lRCF_time);

                layout = itemView.findViewById(R.id.linLay_lRCF);
                ivSeen = itemView.findViewById(R.id.iv_lRCF_seen);
                ivDownload = itemView.findViewById(R.id.iv_lRCF_download);
                ivMore = itemView.findViewById(R.id.iv_lRCF_more);
                progressBar = itemView.findViewById(R.id.pb_lRCF_progress);

            }

        }
    }

    public class Pair {


        private String id, type;

        Pair(String id, String type) {
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
                            cursor1.moveToFirst();
                            ChatImage chatImage = new ChatImage(cursor1.getString(senderIdx),
                                    "image",
                                    cursor1.getString(descIdx),
                                    cursor1.getString(imgNameIdx),
                                    cursor1.getString(imgUrlIdx),
                                    cursor1.getString(thumbIdx),
                                    Long.parseLong(cursor1.getString(timeIdx)),
                                    cursor1.getString(seenIdx)
                            );
                            chatArray.add(new Pair(message_id, type));
                            chatMap.put(message_id, chatImage);
                            publishProgress();
                            cursor1.close();
                            break;

                        case "file":
                            cursor1 = chatDatabase.rawQuery("Select * from message_file where message_id='" + message_id + "'", null, null);
                            descIdx = cursor1.getColumnIndex("description");
                            senderIdx = cursor1.getColumnIndex("sender");
                            timeIdx = cursor1.getColumnIndex("time");
                            seenIdx = cursor1.getColumnIndex("seen");
                            int fileNameIdx = cursor1.getColumnIndex("fileName");
                            int fileUrlIdx = cursor1.getColumnIndex("fileUrl");
                            int sizeIdx = cursor1.getColumnIndex("size");
                            cursor1.moveToFirst();
                            ChatFile chatFile = new ChatFile(cursor1.getString(senderIdx),
                                    "file",
                                    cursor1.getString(descIdx),
                                    cursor1.getString(fileUrlIdx),
                                    Long.parseLong(cursor1.getString(timeIdx)),
                                    cursor1.getString(fileNameIdx),
                                    cursor1.getString(seenIdx),
                                    Long.parseLong(cursor1.getString(sizeIdx)));
                            chatArray.add(new Pair(message_id, type));
                            chatMap.put(message_id, chatFile);
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
                        ChatImage chatImage = dataSnapshot.getValue(ChatImage.class);
                        if (sender.equals(currentId)) {
                            if (chatImage.getImageUrl().isEmpty())
                                addMessageToDatabase(key, chatImage, 0);
                            else addMessageToDatabase(key, chatImage, 1);
                        } else {
                            String image = dataSnapshot.child("imageUrl").getValue().toString();
                            if (!image.isEmpty()) {
                                addMessageToDatabase(key, chatImage, -1);
                            }
                        }
                        break;

                    case "file":
                        ChatFile chatFile = dataSnapshot.getValue(ChatFile.class);
                        if (sender.equals(currentId)) {
                            if (chatFile.getFileUrl().isEmpty())
                                addMessageToDatabase(key, chatFile, 0);
                            else addMessageToDatabase(key, chatFile, 1);
                        } else {
                            if (!chatFile.getFileUrl().isEmpty()) {
                                addMessageToDatabase(key, chatFile, -1);
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
                    case "file":
                        if (!dataSnapshot.child("fileUrl").getValue().toString().isEmpty())
                            addMessageToDatabase(key, dataSnapshot.getValue(ChatFile.class), 1);
                        break;
                }
            } else {
                switch (type) {
                    case "text":
                        ChatMessage chatMessage = dataSnapshot.getValue(ChatMessage.class);
                        addMessageToDatabase(key, chatMessage, 1);
                        break;
                    case "image":
                        String image = dataSnapshot.child("imageUrl").getValue().toString();
                        String thumbImage = dataSnapshot.child("thumbImage").getValue().toString();
                        if (!image.isEmpty() && !thumbImage.isEmpty()) {
                            File parentFile = new File(Environment.getExternalStorageDirectory() + "/ConnectBase/temp/thumbImage/");
                            parentFile.mkdirs();
                            File thumbFile = new File(parentFile, key + ".jpg");

                            mChatImageReference.child(chatId).child("ThumbImage").child(key + ".jpg").getFile(thumbFile).addOnSuccessListener(taskSnapshot -> {
                                ChatImage chatImage = dataSnapshot.getValue(ChatImage.class);
                                addMessageToDatabase(key, chatImage, -1);
                            });
                        }
                        break;

                    case "file":
                        if (!dataSnapshot.child("fileUrl").getValue().toString().isEmpty())
                            addMessageToDatabase(key, dataSnapshot.getValue(ChatFile.class), 1);
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

    @SuppressLint("RestrictedApi")
    void showPopupMenu(View view, int choice, int position) {


        MenuBuilder menuBuilder = new MenuBuilder(this);
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        MenuPopupHelper menuPopupHelper;

        switch (choice) {
            case 0:

                getMenuInflater().inflate(R.menu.menu_popup_chat_message, menuBuilder);

                menuPopupHelper = new MenuPopupHelper(this, menuBuilder, view);
                menuPopupHelper.setForceShowIcon(true);
                menuPopupHelper.show();

                menuBuilder.setCallback(new MenuBuilder.Callback() {
                    @Override
                    public boolean onMenuItemSelected(MenuBuilder menuBuilder, MenuItem menuItem) {
                        ChatMessage chatMessage = (ChatMessage) chatMap.get(chatArray.get(position).getId());
                        String message = chatMessage.getMessage();
                        switch (menuItem.getItemId()) {
                            case R.id.menu_pCM_copy:
                                ClipData clipData = ClipData.newPlainText("message", message);
                                clipboardManager.setPrimaryClip(clipData);
                                Snackbar.make(chatList, "Copied", Snackbar.LENGTH_SHORT).show();
                                return true;

                            case R.id.menu_pCM_forward:
                                return true;
                            case R.id.menu_pCM_info:
                                showMessageInfoDialog(0, chatMessage, null);
                                return true;
                            case R.id.menu_pCM_share:
                                Intent intent = new Intent(Intent.ACTION_SEND);
                                intent.putExtra(Intent.EXTRA_TEXT, message);
                                intent.setType("text/plain");
                                startActivity(Intent.createChooser(intent, "Share via..."));
                                return true;

                        }
                        return false;
                    }

                    @Override
                    public void onMenuModeChange(MenuBuilder menuBuilder) {

                    }
                });

                break;

            case 1:

                getMenuInflater().inflate(R.menu.menu_popup_chat_image, menuBuilder);
                menuPopupHelper = new MenuPopupHelper(this, menuBuilder, view);
                menuPopupHelper.setForceShowIcon(true);
                menuPopupHelper.show();

                menuBuilder.setCallback(new MenuBuilder.Callback() {
                    @Override
                    public boolean onMenuItemSelected(MenuBuilder menuBuilder, MenuItem menuItem) {

                        ChatImage chatImage = ((ChatImage) chatMap.get(chatArray.get(position).getId()));
                        String path = Environment.getExternalStorageDirectory() + "/ConnectBase/Media/Images/";
                        if (chatImage.getSender().equals(currentId))
                            path += "sent/";
                        else path += "received/";
                        path += chatImage.getImageName();
                        if (!new File(path).exists()) {
                            Snackbar.make(chatList, "File doesn't exist in storage", Snackbar.LENGTH_SHORT).show();
                            menuPopupHelper.dismiss();
                            return true;
                        }
                        Uri imageUri = commonFunctions.getUriFromFile(getApplicationContext(), new File(path));

                        switch (menuItem.getItemId()) {
                            case R.id.menu_pCI_gallery:
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setDataAndType(imageUri, "image/*");
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                startActivity(intent);
                                return true;
                            case R.id.menu_pCI_copy:
                                ClipData clipData = ClipData.newPlainText("desc", chatImage.getDescription());
                                clipboardManager.setPrimaryClip(clipData);
                                Snackbar.make(chatList, "Copied", Snackbar.LENGTH_SHORT).show();
                                return true;
                            case R.id.menu_pCI_forward:
                                return true;
                            case R.id.menu_pCI_info:
                                showMessageInfoDialog(1, chatImage, path);
                                return true;
                            case R.id.menu_pCI_share:
                                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                                shareIntent.setType("image/jpeg");
                                startActivity(Intent.createChooser(shareIntent, "Share via..."));
                                return true;
                        }
                        return false;
                    }

                    @Override
                    public void onMenuModeChange(MenuBuilder menuBuilder) {

                    }
                });
                break;

            case 2:

                getMenuInflater().inflate(R.menu.menu_popup_chat_file, menuBuilder);
                menuPopupHelper = new MenuPopupHelper(this, menuBuilder, view);
                menuPopupHelper.setForceShowIcon(true);
                menuPopupHelper.show();

                menuBuilder.setCallback(new MenuBuilder.Callback() {
                    @Override
                    public boolean onMenuItemSelected(MenuBuilder menuBuilder, MenuItem menuItem) {

                        ChatFile chatFile = ((ChatFile) chatMap.get(chatArray.get(position).getId()));

                        String path = Environment.getExternalStorageDirectory() + "/ConnectBase/Media/Files/";
                        if (chatFile.getSender().equals(currentId))
                            path += "sent/";
                        else path += "received/";
                        path += chatFile.getFileName();
                        if (!new File(path).exists()) {
                            Snackbar.make(chatList, "File doesn't exist in storage", Snackbar.LENGTH_SHORT).show();
                            return true;
                        }
                        Uri fileUri = commonFunctions.getUriFromFile(getApplicationContext(), new File(path));

                        switch (menuItem.getItemId()) {

                            case R.id.menu_pCF_copy:
                                ClipData clipData = ClipData.newPlainText("desc", chatFile.getDescription());
                                clipboardManager.setPrimaryClip(clipData);
                                Snackbar.make(chatList, "Copied", Snackbar.LENGTH_SHORT).show();
                                return true;
                            case R.id.menu_pCF_forward:
                                return true;
                            case R.id.menu_pCF_info:
                                showMessageInfoDialog(2, chatFile, path);
                                return true;
                            case R.id.menu_pCF_share:
                                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                                shareIntent.setType("*/*");
                                startActivity(Intent.createChooser(shareIntent, "Share via..."));
                                return true;
                            default:
                                int id = menuItem.getItemId();
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_GRANT_READ_URI_PERMISSION);

                                switch (id) {
                                    case R.id.menu_pcF_open_audio:
                                        intent.setDataAndType(fileUri, "audio/*");
                                        startActivity(Intent.createChooser(intent, "Open with"));
                                        break;
                                    case R.id.menu_pcF_open_document:
                                        intent.setDataAndType(fileUri, "application/*");
                                        startActivity(Intent.createChooser(intent, "Open with"));
                                        break;
                                    case R.id.menu_pcF_open_image:
                                        intent.setDataAndType(fileUri, "image/*");
                                        startActivity(Intent.createChooser(intent, "Open with"));
                                        break;
                                    case R.id.menu_pcF_open_text:
                                        intent.setDataAndType(fileUri, "text/*");
                                        startActivity(Intent.createChooser(intent, "Open with"));
                                        break;
                                    case R.id.menu_pcF_open_video:
                                        intent.setDataAndType(fileUri, "video/*");
                                        startActivity(Intent.createChooser(intent, "Open with"));
                                        break;
                                }
                                return true;
                        }
                    }

                    @Override
                    public void onMenuModeChange(MenuBuilder menuBuilder) {

                    }
                });
                break;
        }

    }

    void showMessageInfoDialog(int type, Object object, String path) {

        Dialog dialog = new Dialog(this);
        View view;
        TextView tvSender, tvTime, tvStatus;
        Button btnOk;

        if (type == 0) {
            ChatMessage chatMessage = (ChatMessage) object;
            view = getLayoutInflater().inflate(R.layout.layout_dialog_message_info_text, null, false);
            TextView tvMessage;

            tvSender = view.findViewById(R.id.tv_lDMIT_sender);
            tvMessage = view.findViewById(R.id.tv_lDMIT_message);
            tvTime = view.findViewById(R.id.tv_lDMIT_time);
            tvStatus = view.findViewById(R.id.tv_lDMIT_status);
            btnOk = view.findViewById(R.id.btn_lDMIT_ok);

            tvSender.setText(chatMessage.getSender().equals(currentId) ? "You" : user.getName());
            tvMessage.setText(chatMessage.getMessage());
            tvStatus.setText(chatMessage.getSeen().equals("false") ? "Delivered" : "Seen");
            tvTime.setText(commonFunctions.convertTime(chatMessage.getTime(), false));
            btnOk.setOnClickListener(v -> dialog.dismiss());

            dialog.setContentView(view);
            dialog.setCanceledOnTouchOutside(true);
            dialog.setCancelable(false);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();

        } else {
            TextView tvFileName, tvFileLoc, tvDesc, tvType;
            view = getLayoutInflater().inflate(R.layout.layout_dialog_message_info_image_file, null, false);

            tvDesc = view.findViewById(R.id.tv_lDMIIF_desc);
            tvFileLoc = view.findViewById(R.id.tv_lDMIIF_file_location);
            tvFileName = view.findViewById(R.id.tv_lDMIIF_file_name);
            tvSender = view.findViewById(R.id.tv_lDMIIF_sender);
            tvStatus = view.findViewById(R.id.tv_lDMIIF_status);
            tvTime = view.findViewById(R.id.tv_lDMIIF_time);
            btnOk = view.findViewById(R.id.btn_lDMIIF_ok);
            tvType = view.findViewById(R.id.tv_lDMIIF_type);

            if (type == 1) {

                ChatImage chatImage = (ChatImage) object;
                tvSender.setText(chatImage.getSender().equals(currentId) ? "You" : user.getName());
                tvDesc.setText(chatImage.getDescription());
                tvStatus.setText(chatImage.getSeen().equals("false") ? "Delivered" : "Seen");
                tvTime.setText(commonFunctions.convertTime(chatImage.getTime(), false));
                tvFileName.setText(chatImage.getImageName());
                tvFileLoc.setText(path);
            } else {
                tvType.setText("File");
                ChatFile chatFile = (ChatFile) object;
                tvSender.setText(chatFile.getSender().equals(currentId) ? "You" : user.getName());
                tvDesc.setText(chatFile.getDescription());
                tvStatus.setText(chatFile.getSeen().equals("false") ? "Delivered" : "Seen");
                tvTime.setText(commonFunctions.convertTime(chatFile.getTime(), false));
                tvFileName.setText(chatFile.getFileName());
                tvFileLoc.setText(path);
            }

            btnOk.setOnClickListener(v -> dialog.dismiss());
            dialog.setContentView(view);
            dialog.setCanceledOnTouchOutside(true);
            dialog.setCancelable(false);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();
        }
    }

    @SuppressLint("StaticFieldLeak")
    public class SaveChats extends AsyncTask<Void, Integer, Void> {

        ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(ChatActivity.this);
            dialog.setCancelable(false);
            dialog.setProgress(0);
            dialog.setMax(chatArray.size());
            dialog.setTitle("Loading!!");
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setMessage("Please wait while Saving Chats...");
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Document document = new Document();
                String path = Environment.getExternalStorageDirectory() + "/ConnectBase/Saved Chats/";
                new File(path).mkdirs();
                PdfWriter.getInstance(document, new FileOutputStream(path + user.getName() + "\t" + commonFunctions.convertTime(new Date().getTime(), false) + ".pdf"));
                document.open();

                Font redHeading = new Font(Font.FontFamily.COURIER, 18, Font.NORMAL, BaseColor.RED);
                Font heading = new Font(Font.FontFamily.COURIER, 20.0f, Font.UNDERLINE);

                Paragraph headingPara = new Paragraph("Chats with " + user.getName(), heading);
                document.add(headingPara);
                String date = "";

                for (int i = 0; i < chatArray.size(); i++) {
                    switch (chatArray.get(i).getType()) {
                        case "text":
                            ChatMessage chatMessage = ((ChatMessage) chatMap.get(chatArray.get(i).getId()));
                            String newDate = calcDate(chatMessage.getTime());
                            if (!date.equals(newDate)) {
                                date = newDate;
                                document.add(new Paragraph("\n" + date + "\n", redHeading));
                            }
                            String name = (chatMessage.getSender().equals(currentId) ? "You" : user.getName().trim()) + " -";
                            String s = commonFunctions.convertTime(chatMessage.getTime(), true) + " :" + lPadding(name) + lPadding(chatMessage.getMessage());
                            Paragraph paragraph = new Paragraph(s);
                            document.add(paragraph);
                            publishProgress(i);
                            break;

                        case "image":
                            ChatImage chatImage = (ChatImage) chatMap.get(chatArray.get(i).getId());
                            newDate = calcDate(chatImage.getTime());
                            if (!date.equals(newDate)) {
                                date = newDate;
                                document.add(new Paragraph("\n" + date + "\n", redHeading));
                            }

                            name = (chatImage.getSender().equals(currentId) ? "You" : user.getName().trim()) + " -";
                            s = commonFunctions.convertTime(chatImage.getTime(), true) + " :" + lPadding(name) + lPadding("(sends Image)");
                            paragraph = new Paragraph("\n" + s + "\n");
                            document.add(paragraph);

                            String imagePath = Environment.getExternalStorageDirectory() + "/ConnectBase/Media/Images/";
                            if (chatImage.getSender().equals(currentId)) {
                                imagePath += "sent/";
                            } else imagePath += "received/";
                            imagePath += chatImage.imageName;

                            if (!new File(imagePath).exists()) {
                                document.add(new Paragraph("(File doesn't exists in storage)"));
                                break;
                            }

                            Image image = Image.getInstance(imagePath);

                            image.setAlignment(Image.ALIGN_CENTER | Image.TEXTWRAP);
                            image.setBorder(Image.BOX);
                            image.scaleToFit(200, 200);
                            image.setBorderWidth(5);
                            document.add(image);
                            publishProgress(i);
                            break;

                        case "file":
                            ChatFile chatFile = (ChatFile) chatMap.get(chatArray.get(i).getId());
                            newDate = calcDate(chatFile.getTime());
                            if (!date.equals(newDate)) {
                                date = newDate;
                                document.add(new Paragraph("\n" + date + "\n", redHeading));
                            }
                            name = (chatFile.getSender().equals(currentId) ? "You" : user.getName().trim()) + " -";
                            s = commonFunctions.convertTime(chatFile.getTime(), true) + " :" + lPadding(name) + lPadding("(sends File '" + chatFile.getFileName() + "')");
                            paragraph = new Paragraph("\n" + s + "\n");
                            document.add(paragraph);
                            publishProgress(i);
                            break;
                    }
                }

                document.close();
            } catch (Exception e) {
                Snackbar.make(chatList, e.getMessage(), Snackbar.LENGTH_SHORT).show();
                Log.i("ConnectBaseEx", e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            dialog.dismiss();
            Snackbar.make(chatList, "Chats Successfully Saved!!", Snackbar.LENGTH_SHORT).show();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            dialog.setProgress(values[0]);
        }

    }

    String lPadding(String s) {
        String s1 = "^!^!^!^!^!^!^!^!^!^!" + s;
        return s1.replace("^!", " ");
    }

    String calcDate(long time) {
        String date;
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yy");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        date = dateFormat.format(calendar.getTime());
        return date;
    }


}
