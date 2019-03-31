package com.example.connectbase;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class FragFriends extends Fragment {

    RecyclerView friendList;
    DatabaseReference mFriendsReference, mUserReference;
    StorageReference mThumbImagesReference;
    View view;
    ArrayList<String> friendsArrayList;
    ArrayList<DatabaseReference> referenceArrayList;
    HashMap<String, Users> friendHashMap;
    HashMap<String, Boolean> isOnline;
    SQLiteDatabase userDatabase;
    String currentId;
    FriendsAdapter adapter;
    EditText etSearch;

    ChildEventListener childEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            String id = dataSnapshot.getKey();
            addVEL(id, false);
        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
            String id = dataSnapshot.getKey();
            int index1 = friendsArrayList.indexOf(id);
            int index2 = referenceArrayList.indexOf(mUserReference.child(id));

            if (index1 >= 0) {
                deleteFriend(id);
            }
            if (index2 >= 0) {
                referenceArrayList.get(index2).removeEventListener(valueEventListener);
                referenceArrayList.remove(index2);
            }

        }

        @Override
        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };

    ValueEventListener valueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            Log.i("CBVEL", dataSnapshot.toString());
            String id = dataSnapshot.getKey();
            Users friend = dataSnapshot.getValue(Users.class);

            isOnline.put(id, dataSnapshot.hasChild("online") && dataSnapshot.child("online").getValue().toString().equals("true"));
            mFriendsReference.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot1) {

                    if (dataSnapshot1.hasChild("chatId")) {
                        String chatId = dataSnapshot1.child("chatId").getValue().toString();
                        addFriendToDatabase(id, friend, chatId);
                    } else addFriendToDatabase(id, friend, "");
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }

            });


        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };


    public FragFriends() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_bookmark, container, false);
        initialiseVariables();

        return view;
    }

    private void initialiseVariables() {

        friendList = view.findViewById(R.id.list_fragBookmark);
        friendsArrayList = new ArrayList<>();
        friendHashMap = new HashMap<>();
        isOnline = new HashMap<>();
        referenceArrayList = new ArrayList<>();
        adapter = new FriendsAdapter();
        currentId = FirebaseAuth.getInstance().getUid();
        mFriendsReference = FirebaseDatabase.getInstance().getReference().child("Friends").child(currentId);
        mUserReference = FirebaseDatabase.getInstance().getReference().child("Users");
        mThumbImagesReference = FirebaseStorage.getInstance().getReference().child("ProfileImage").child("ThumbImage");
        etSearch = view.findViewById(R.id.et_fragBookmark_search);

        friendList.setLayoutManager(new LinearLayoutManager(getActivity()));
        friendList.setAdapter(adapter);

        userDatabase = view.getContext().openOrCreateDatabase("users", Context.MODE_PRIVATE, null);
        userDatabase.execSQL("create table if not exists friends(" +
                "id varchar primary key not null," +
                "chatId varchar," +
                "age varchar," +
                "city varchar," +
                "email varchar not null," +
                "experience text," +
                "image varchar," +
                "mobile varchar," +
                "name varchar not null," +
                "organisation varchar," +
                "position varchar," +
                "qualification varchar," +
                "resume varchar," +
                "skills varchar," +
                "state varchar," +
                "thumbImage varchar)");

        createFolders();
        loadFromDatabase();


    }

    private void createFolders() {
        String path = Environment.getExternalStorageDirectory() + "/ConnectBase/ProfilePics/thumbImage";
        File folder = new File(path);
        folder.mkdirs();
    }

    private void loadFromDatabase() {
        friendsArrayList.clear();
        referenceArrayList.clear();
        friendHashMap.clear();
        isOnline.clear();

        Cursor cursor = userDatabase.rawQuery("Select id,name,position,thumbImage from friends", null, null);

        if (cursor.getCount() == 0)
            return;
        cursor.moveToFirst();
        do {
            String id = cursor.getString(0);
            String name = cursor.getString(1);
            String position = cursor.getString(2);
            String thumbImage = cursor.getString(3);

            Users friend = new Users();
            friend.setName(name);
            friend.setPosition(position);
            friend.setThumbImage(thumbImage);

            friendsArrayList.add(id);
            friendHashMap.put(id, friend);
            addVEL(id, true);
        }
        while (cursor.moveToNext());

        cursor.close();
    }

    private void addFriendToDatabase(String id, Users friend, String chatId) {

        ContentValues values = new ContentValues();
        values.put("age", friend.getAge());
        values.put("id", id);
        values.put("city", friend.getCity());
        values.put("email", friend.getEmail());
        values.put("experience", friend.getExperience());
        values.put("image", friend.getImage());
        values.put("mobile", friend.getMobile());
        values.put("name", friend.getName());
        values.put("organisation", friend.getOrganisation());
        values.put("position", friend.getPosition());
        values.put("qualification", friend.getQualification());
        values.put("resume", friend.getResume());
        values.put("skills", friend.getSkills());
        values.put("state", friend.getState());
        values.put("thumbImage", friend.getThumbImage());
        values.put("chatId", chatId);

        long num = userDatabase.insert("friends", null, values);

        friendHashMap.put(id, friend);

        if (num < 0) {
            updateFriend(id, values);
            adapter.notifyItemChanged(friendsArrayList.indexOf(id));
        } else {
            friendsArrayList.add(id);
            adapter.notifyItemInserted(friendsArrayList.size() - 1);
        }


    }

    private void deleteFriend(String id) {

        userDatabase.delete("friends", "id=?", new String[]{id});
        friendHashMap.remove(id);
        friendsArrayList.remove(id);
        adapter.notifyDataSetChanged();


        FragChat.sharedPreferences.edit()
                .remove("user_" + id + "_chat_id")
                .remove("user_" + id + "_message_id")
                .apply();

    }

    private void addVEL(String id, boolean database) {
        if (database || referenceArrayList.indexOf(mUserReference.child(id)) < 0) {
            mUserReference.child(id).addValueEventListener(valueEventListener);
            referenceArrayList.add(mUserReference.child(id));
        }

    }

    private void updateFriend(String id, ContentValues values) {

        userDatabase.update("friends", values, "id=?", new String[]{id});

    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.i("CalledOnFriend", " On ActivityCreated");
        mFriendsReference.addChildEventListener(childEventListener);
        checkFriends();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                findAndScroll(s.toString());

            }
        });
    }

    void findAndScroll(String s) {

        if (s.equals(""))
            return;
        for (int i = 0; i < friendsArrayList.size(); i++) {
            if (friendHashMap.get(friendsArrayList.get(i)).getName().toLowerCase().contains(s.toLowerCase())) {
                friendList.smoothScrollToPosition(i);
                break;
            }
        }
    }

    private void checkFriends() {

        ArrayList<Boolean> arrayList = new ArrayList<>();
        for (int i = 0; i < friendsArrayList.size(); i++)
            arrayList.add(i, false);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.i("CBvel", dataSnapshot.toString());
                Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                for (DataSnapshot child : children) {
                    int index = friendsArrayList.indexOf(child.getKey());
                    if (index != -1) {
                        arrayList.remove(index);
                        arrayList.add(index, true);
                    }
                }
                updateFriendList(arrayList);
                mFriendsReference.removeEventListener(valueEventListener);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        mFriendsReference.addListenerForSingleValueEvent(listener);

    }

    private void updateFriendList(ArrayList<Boolean> arrayList) {
        ArrayList<String> deletedFriend = new ArrayList<>();
        for (int i = 0; i < arrayList.size(); i++) {
            if (!arrayList.get(i)) {
                deletedFriend.add(friendsArrayList.get(i));
            }
        }
        for (int i = 0; i < deletedFriend.size(); i++) {
            deleteFriend(deletedFriend.get(i));
        }
    }

    public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            View rowView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_row_people, viewGroup, false);

            return new ViewHolder(rowView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {

            Users friend = friendHashMap.get(friendsArrayList.get(i));
            viewHolder.tvName.setText(friend.getName());
            viewHolder.tvPosition.setText(friend.getPosition());

            if (isOnline.containsKey(friendsArrayList.get(i))) {
                if (isOnline.get(friendsArrayList.get(i)))
                    viewHolder.ivSeen.setVisibility(View.VISIBLE);
            } else {
                viewHolder.ivSeen.setVisibility(View.INVISIBLE);
            }
            boolean thumbImage = friend.getThumbImage().isEmpty();
            if (!thumbImage) {
                String path = Environment.getExternalStorageDirectory() + "/ConnectBase/ProfilePics/thumbImage/" + friendsArrayList.get(i) + ".jpg";
                File thumbFile = new File(path);

                if (thumbFile.exists()) {
                    viewHolder.ivPic.setImageBitmap(BitmapFactory.decodeFile(path));

                } else {
                    mThumbImagesReference.child(friendsArrayList.get(i) + ".jpg").getFile(thumbFile).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            viewHolder.ivPic.setImageBitmap(BitmapFactory.decodeFile(path));
                        } else {
                            Picasso.get()
                                    .load(friend.getThumbImage())
                                    .placeholder(R.drawable.avatar)
                                    .error(R.drawable.avatar)
                                    .into(viewHolder.ivPic);
                        }
                    });

                }
            } else viewHolder.ivPic.setImageResource(R.drawable.avatar);

            viewHolder.layout.setOnClickListener(v -> showMenu(i, viewHolder.itemView.getContext()));

        }

        @Override
        public int getItemCount() {
            return friendsArrayList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            TextView tvName, tvPosition;
            CircleImageView ivPic;
            View layout;
            ImageView ivSeen;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_layoutRowPeople_name);
                tvPosition = itemView.findViewById(R.id.tv_layoutRowPeople_position);
                ivPic = itemView.findViewById(R.id.iv_layoutRowPeople_profilePic);
                layout = itemView.findViewById(R.id.linlay_lRP);
                ivSeen = itemView.findViewById(R.id.iv_lRP_online);
            }

        }

    }

    private void showMenu(int position, Context context) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        dialog.setCancelable(true);
        View view = getLayoutInflater().inflate(R.layout.layout_friend_bottomsheetdialog, null, false);

        View sendMessage = view.findViewById(R.id.tv_lFBSD_sendMessage);
        View viewProfile = view.findViewById(R.id.tv_lFBSD_viewProfile);

        String id = friendsArrayList.get(position);
        Users user = getFriendFromDatabase(id);

        sendMessage.setOnClickListener(v -> {
            openChatActivity(id, user, context);
            dialog.dismiss();
        });

        viewProfile.setOnClickListener(v -> {
            openUserProfile(id, user, context);
            dialog.dismiss();
        });


        dialog.setContentView(view);
        dialog.show();
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

    private void openChatActivity(String id, Users user, Context context) {

        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra("user", user);
        intent.putExtra("id", id);
        startActivity(intent);
    }

    private void openUserProfile(String id, Users user, Context context) {

        Intent intent = new Intent(context, ViewUserProfile.class);
        intent.putExtra("user", user);
        intent.putExtra("id", id);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFriendsReference.removeEventListener(childEventListener);
        for (int i = 0; i < referenceArrayList.size(); i++) {
            referenceArrayList.get(i).removeEventListener(valueEventListener);
        }
    }
}
