package com.example.connectbase;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.content.Context.MODE_PRIVATE;

public class FragChat extends Fragment {

    View view;
    RecyclerView chatList;
    EditText etSearch;
    SQLiteDatabase chatDatabase, userDatabase;
    DatabaseReference mFriendReference, mChatReference;
    static ChatAdapter adapter;
    static ArrayList<String> chatArrayList;
    static HashMap<String, UserHolder> chatHashMap;
    ArrayList<Query> referenceArrayList;
    String currentId;
    SharedPreferences sharedPreferences;
    StorageReference mThumbImagesReference;

    public FragChat() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_bookmark, container, false);
        initialiseVariables();
        Log.i("MethodCalled", "OnCreateView");
        return view;
    }

    private void initialiseVariables() {

        currentId = FirebaseAuth.getInstance().getUid();
        sharedPreferences = view.getContext().getSharedPreferences("chatData", MODE_PRIVATE);

        mChatReference = FirebaseDatabase.getInstance().getReference().child("Chats");
        mFriendReference = FirebaseDatabase.getInstance().getReference().child("Friends").child(currentId);
        mThumbImagesReference = FirebaseStorage.getInstance().getReference().child("ProfileImage").child("ThumbImage");

        chatList = view.findViewById(R.id.list_fragBookmark);
        etSearch = view.findViewById(R.id.et_fragBookmark_search);
        etSearch.setVisibility(View.GONE);
        chatDatabase = view.getContext().openOrCreateDatabase("chats", MODE_PRIVATE, null);
        userDatabase = view.getContext().openOrCreateDatabase("users", MODE_PRIVATE, null);

        chatArrayList = new ArrayList<>();
        chatHashMap = new HashMap<>();
        referenceArrayList = new ArrayList<>();

        chatList.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ChatAdapter();
        chatList.setAdapter(adapter);
    }

    class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_row_chat_fragment, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {

            UserHolder userHolder = chatHashMap.get(chatArrayList.get(i));
            userHolder.lastMessage = getLastMessage(chatArrayList.get(i));

            viewHolder.tvLastMessage.setText(userHolder.lastMessage);
            viewHolder.tvName.setText(userHolder.name);

            Log.i("CBLis last message", userHolder.lastMessage + "");
            Log.i("CBLis count", userHolder.messageCount + "");

            userHolder.messageCount = getLastUnreadMessageCount(chatArrayList.get(i));


            switch (userHolder.messageCount) {
                case -1:
                    break;
                case 0:
                    viewHolder.btnMessageCount.setVisibility(View.INVISIBLE);
                    break;
                default:
                    viewHolder.btnMessageCount.setVisibility(View.VISIBLE);
                    viewHolder.btnMessageCount.setText(userHolder.messageCount + "");
            }

            boolean thumbImage = userHolder.pic.isEmpty();
            if (!thumbImage) {
                String path = Environment.getExternalStorageDirectory() + "/ConnectBase/ProfilePics/thumbImage/" + chatArrayList.get(i) + ".jpg";
                File thumbFile = new File(path);

                if (thumbFile.exists()) {
                    viewHolder.ivPic.setImageBitmap(BitmapFactory.decodeFile(path));

                } else {
                    mThumbImagesReference.child(chatArrayList.get(i) + ".jpg").getFile(thumbFile).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            viewHolder.ivPic.setImageBitmap(BitmapFactory.decodeFile(path));
                        } else {
                            Picasso.get()
                                    .load(userHolder.pic)
                                    .placeholder(R.drawable.avatar)
                                    .error(R.drawable.avatar)
                                    .into(viewHolder.ivPic);
                        }
                    });

                }
            } else viewHolder.ivPic.setImageResource(R.drawable.avatar);
            viewHolder.itemView.setOnClickListener(v -> openChatActivity(i));


        }

        @Override
        public int getItemCount() {
            return chatArrayList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            TextView tvName, tvLastMessage;
            CircleImageView ivPic;
            Button btnMessageCount;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_lRCFr_name);
                tvLastMessage = itemView.findViewById(R.id.tv_lRCFr__position);
                ivPic = itemView.findViewById(R.id.iv_lRCFr_profilePic);
                btnMessageCount = itemView.findViewById(R.id.btn_lRCFr_messageCount);
            }
        }
    }

    private void openChatActivity(int i) {
        String id = chatArrayList.get(i);
        Users user = getFriendFromDatabase(id);
        if (user == null) {
            user = new Users();
            user.setName("Anonymous");
            user.setThumbImage("");
        }
        Intent intent = new Intent(view.getContext(), ChatActivity.class);
        intent.putExtra("user", user);
        intent.putExtra("id", id);
        startActivity(intent);

    }

    private Users getFriendFromDatabase(String id) {
        Users user = new Users();

        Cursor cursor = userDatabase.rawQuery("Select * from friends where id='" + id + "'", null, null);

        if (cursor == null || cursor.getCount() == 0)
            return null;
        cursor.moveToFirst();

        user.setAge(cursor.getString(2));
        user.setCity(cursor.getString(3));
        user.setEmail(cursor.getString(4));
        user.setExperience(cursor.getString(5));
        user.setImage(cursor.getString(6));
        user.setMobile(cursor.getString(7));
        user.setName(cursor.getString(8));
        user.setOrganisation(cursor.getString(9));
        user.setPosition(cursor.getString(10));
        user.setQualification(cursor.getString(11));
        user.setResume(cursor.getString(12));
        user.setSkills(cursor.getString(13));
        user.setState(cursor.getString(14));
        user.setThumbImage(cursor.getString(15));

        cursor.close();
        return user;
    }

    private String getLastMessage(String id) {

        String lastMessage = "";
        Cursor cursor = chatDatabase.rawQuery("select * from user_" + id, null, null);

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToLast();
            String messageId = cursor.getString(0);
            Log.i("CBLis messageId", messageId);
            String type = cursor.getString(1);
            Cursor cursor1 = null;
            switch (type) {
                case "text":
                    cursor1 = chatDatabase.rawQuery("select message from message_text where message_id='" + messageId + "'", null, null);
                    cursor1.moveToFirst();
                    lastMessage = cursor1.getString(0);
                    break;
                case "image":
                    cursor1 = chatDatabase.rawQuery("select description from message_image where message_id='" + messageId + "'", null, null);
                    cursor1.moveToFirst();
                    lastMessage = "(Image) " + cursor1.getString(0);
                    break;
                case "file":
                    cursor1 = chatDatabase.rawQuery("select description from message_file where message_id='" + messageId + "'", null, null);
                    cursor1.moveToFirst();
                    lastMessage = "(File) " + cursor1.getString(0);
                    break;
            }
            if (cursor1 != null)
                cursor1.close();
            cursor.close();
        }
        return lastMessage;

    }

    private int getLastUnreadMessageCount(String id) {

        int count = 0;

        Cursor cursor = chatDatabase.rawQuery("select message_id,message_type from user_" + id + " where sent='-1'", null, null);

        if (cursor == null || cursor.getCount() == 0)
            return count;
        cursor.moveToLast();
        do {
            String type = cursor.getString(1);
            String messageId = cursor.getString(0);
            Cursor cursor1 = chatDatabase.rawQuery("select seen from message_" + type + " where message_id='" + messageId + "'", null, null);
            cursor1.moveToFirst();
            String seen = cursor1.getString(0);
            if (seen.equals("true"))
                return count;
            count++;
            cursor1.close();
        }
        while (cursor.moveToPrevious());
        cursor.close();

        Log.i("CBLis count", count + "");
        return count;

    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i("MethodCalled", "OnStart");
        new LoadChats().execute();
        mFriendReference.addChildEventListener(friendEventListener);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.i("MethodCalled", "OnActivityCreated");
    }

    @SuppressLint("StaticFieldLeak")
    private class LoadChats extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            chatArrayList.clear();
            chatHashMap.clear();

            Cursor cursor = chatDatabase.rawQuery("select * from user_list", null, null);

            if (cursor == null || cursor.getCount() < 1)
                return null;
            cursor.moveToFirst();
            do {
                String id = cursor.getString(0);
                Cursor cursor1 = userDatabase.rawQuery("select * from friends where id='" + id + "'", null, null);

                String name, image;
                if (cursor1.getCount() > 0) {
                    cursor1.moveToFirst();
                    name = cursor1.getString(8);
                    image = cursor1.getString(15);
                } else {
                    name = "Friend Removed";
                    image = "";
                }

                cursor1.close();

                chatArrayList.add(id);
                chatHashMap.put(id, new UserHolder(name, null, image, -1));
                publishProgress();
            }
            while (cursor.moveToNext());
            cursor.close();
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            adapter.notifyDataSetChanged();
        }
    }

    class UserHolder {
        String name, lastMessage;
        String pic;
        int messageCount;

        UserHolder(String name, String lastMessage, String pic, int messageCount) {
            this.name = name;
            this.lastMessage = lastMessage;
            this.pic = pic;
            this.messageCount = messageCount;
        }

    }

    ChildEventListener friendEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            if (dataSnapshot.hasChild("chatId")) {
                String chatId = dataSnapshot.child("chatId").getValue().toString();
                String id = dataSnapshot.getKey();
                String cid = sharedPreferences.getString("user_" + id + "_chat_id", "");

                Query query;

                if (cid.equals(chatId)) {
                    String lastKey = sharedPreferences.getString("user_" + id + "_message_id", "");
                    if (!lastKey.equals(""))
                        query = mChatReference.child(chatId).orderByKey().startAt(lastKey);
                    else query = mChatReference.child(chatId).orderByKey();
                } else {
                    sharedPreferences.edit().putString("user_" + id + "_chat_id", chatId).apply();
                    query = mChatReference.child(chatId).orderByKey();
                }

                query.addChildEventListener(chatEventListener);
                referenceArrayList.add(query);
            }

        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            if (dataSnapshot.hasChild("chatId")) {
                String chatId = dataSnapshot.child("chatId").getValue().toString();
                String id = dataSnapshot.getKey();
                String cid = sharedPreferences.getString("user_" + id + "_chat_id", "");

                Query query;

                if (cid.equals(chatId)) {
                    String lastKey = sharedPreferences.getString("user_" + id + "_message_id", "");
                    if (!lastKey.equals(""))
                        query = mChatReference.child(chatId).orderByKey().startAt(lastKey);
                    else query = mChatReference.child(chatId).orderByKey();
                } else {
                    sharedPreferences.edit().putString("user_" + id + "_chat_id", chatId).apply();
                    query = mChatReference.child(chatId).orderByKey();
                }

                query.addChildEventListener(chatEventListener);
                referenceArrayList.add(query);
            }
        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            if (dataSnapshot.hasChild("chatId")) {
                String id = dataSnapshot.getKey();
                String chatId = dataSnapshot.child("chatId").getValue().toString();
                mChatReference.child(chatId).removeEventListener(chatEventListener);
                referenceArrayList.remove(mChatReference.child(chatId));
                sharedPreferences.edit().remove("user_" + id + "_chat_id").remove("user_" + id + "_message_id").apply();
                adapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };

    ChildEventListener chatEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            if (dataSnapshot.hasChild("messageType")) {
                String sender = dataSnapshot.child("sender").getValue().toString();
                String type = dataSnapshot.child("messageType").getValue().toString();
                Log.i("CBLis", dataSnapshot.getKey());
                Object object = null;
                switch (type) {
                    case "text":
                        object = dataSnapshot.getValue(ChatMessage.class);
                        break;
                    case "image":
                        object = dataSnapshot.getValue(ChatImage.class);
                        break;
                    case "file":
                        object = dataSnapshot.getValue(ChatFile.class);
                        break;
                }
                addMessageToDatabase(sender, type, dataSnapshot.getKey(), object);
                adapter.notifyDataSetChanged();
            }


        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

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

    void addMessageToDatabase(String sender, String type, String msgId, Object object) {

        ContentValues values = new ContentValues();

        try {
            ContentValues messageMetaData = new ContentValues();
            messageMetaData.put("message_id", msgId);
            messageMetaData.put("sent", -1);
            values.put("message_id", msgId);

            switch (type) {
                case "text":

                    ChatMessage chatMessage = (ChatMessage) object;
                    values.put("sender", chatMessage.getSender());
                    values.put("message", chatMessage.getMessage());
                    values.put("time", chatMessage.getTime());
                    values.put("seen", chatMessage.getSeen());

                    break;
                case "image":

                    ChatImage chatImage = (ChatImage) object;
                    values.put("sender", chatImage.getSender());
                    values.put("description", chatImage.getDescription());
                    values.put("imageUrl", chatImage.getImageUrl());
                    values.put("imageName", chatImage.getImageName());
                    values.put("thumbImage", chatImage.getThumbImage());
                    values.put("time", chatImage.getTime());
                    values.put("seen", chatImage.getSeen());

                    break;
                case "file":

                    ChatFile chatFile = (ChatFile) object;
                    values.put("sender", chatFile.getSender());
                    values.put("description", chatFile.getDescription());
                    values.put("fileUrl", chatFile.getFileUrl());
                    values.put("fileName", chatFile.getFileName());
                    values.put("time", chatFile.getTime());
                    values.put("size", chatFile.getSize());
                    values.put("seen", chatFile.getSeen());

                    break;
            }

            messageMetaData.put("message_type", type);
            long v1 = chatDatabase.insert("user_" + sender, null, messageMetaData);
            long v2 = chatDatabase.insert("message_" + type, null, values);
            adapter.notifyDataSetChanged();

            Log.i("CBLis Database", v1 + "\t\t" + v2);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i("MethodCalled", "OnStop");
        mFriendReference.removeEventListener(friendEventListener);
        for (Query query : referenceArrayList) {
            query.removeEventListener(chatEventListener);
        }

    }

}
