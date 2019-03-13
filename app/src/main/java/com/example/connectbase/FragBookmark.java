package com.example.connectbase;

import android.app.AlertDialog;
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
import com.like.LikeButton;
import com.like.OnLikeListener;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class FragBookmark extends Fragment {

    View view;
    RecyclerView bookmarkList;
    EditText etSearch;
    DatabaseReference mBookmarkReference, mUserReference;
    ArrayList<String> bookmarkArrayList;
    HashMap<String, Users> bookmarkHashMap;
    SQLiteDatabase userDatabase;
    String currentId;
    StorageReference mThumbImagesReference;
    BookmarkAdapter adapter;
    ArrayList<DatabaseReference> referenceArrayList;

    ChildEventListener childEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            String id = dataSnapshot.getKey();
            mUserReference.child(id).addValueEventListener(valueEventListener);
        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            String id = dataSnapshot.getKey();
            Log.i("ConnectBase", "Bookmark deleted");
            int index1 = bookmarkArrayList.indexOf(id);
            int index2 = referenceArrayList.indexOf(mUserReference.child(id));

            if (index1 >= 0) {
                deleteBookmark(id);
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

            String id = dataSnapshot.getKey();
            Users bookmark = dataSnapshot.getValue(Users.class);
            addBookmarkToDatabase(id, bookmark);

        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };

    private void addBookmarkToDatabase(String id, Users bookmark) {

        ContentValues values = new ContentValues();
        values.put("age", bookmark.getAge());
        values.put("id", id);
        values.put("city", bookmark.getCity());
        values.put("email", bookmark.getEmail());
        values.put("experience", bookmark.getExperience());
        values.put("image", bookmark.getImage());
        values.put("mobile", bookmark.getMobile());
        values.put("name", bookmark.getName());
        values.put("organisation", bookmark.getOrganisation());
        values.put("position", bookmark.getPosition());
        values.put("qualification", bookmark.getQualification());
        values.put("resume", bookmark.getResume());
        values.put("skills", bookmark.getSkills());
        values.put("state", bookmark.getState());
        values.put("thumbImage", bookmark.getThumbImage());

        long num = userDatabase.insert("bookmarks", null, values);

        bookmarkHashMap.put(id, bookmark);

        if (num < 0) {
            updateBookmark(id, values);
            adapter.notifyItemChanged(bookmarkArrayList.indexOf(id));
        } else {
            bookmarkArrayList.add(id);
            adapter.notifyItemInserted(bookmarkArrayList.size() - 1);
        }


    }

    private void deleteBookmark(String id) {

        userDatabase.delete("bookmarks", "id=?", new String[]{id});
        bookmarkHashMap.remove(id);
        bookmarkArrayList.remove(id);
        adapter.notifyDataSetChanged();

    }

    private void updateBookmark(String id, ContentValues values) {

        userDatabase.update("bookmarks", values, "id=?", new String[]{id});

    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_bookmark, container, false);
        initialiseVariables();
        return view;
    }

    private void initialiseVariables() {

        bookmarkList = view.findViewById(R.id.list_fragBookmark);
        etSearch = view.findViewById(R.id.et_fragBookmark_search);
        currentId = FirebaseAuth.getInstance().getUid();
        mBookmarkReference = FirebaseDatabase.getInstance().getReference().child("Bookmark").child(currentId);
        mUserReference = FirebaseDatabase.getInstance().getReference().child("Users");
        mThumbImagesReference = FirebaseStorage.getInstance().getReference().child("ProfileImage").child("ThumbImage");
        bookmarkArrayList = new ArrayList<>();
        referenceArrayList = new ArrayList<>();
        bookmarkHashMap = new HashMap<>();
        userDatabase = view.getContext().openOrCreateDatabase("users", Context.MODE_PRIVATE, null);
        adapter = new BookmarkAdapter();

        userDatabase.execSQL("create table if not exists bookmarks(" +
                "id varchar primary key not null," +
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

        bookmarkList.setLayoutManager(new LinearLayoutManager(getActivity()));
        bookmarkList.setAdapter(adapter);

        Log.i("CalledOn", "OnCreateView");


    }

    private void createFolders() {
        String path = Environment.getExternalStorageDirectory() + "/ConnectBase/ProfilePics/thumbImage";
        File folder = new File(path);
        folder.mkdirs();
    }

    private void loadFromDatabase() {
        bookmarkArrayList.clear();
        bookmarkHashMap.clear();

        Cursor cursor = userDatabase.rawQuery("Select id,name,position,thumbImage from bookmarks", null, null);

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

            bookmarkArrayList.add(id);
            bookmarkHashMap.put(id, friend);
        }
        while (cursor.moveToNext());
        cursor.close();
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mBookmarkReference.addChildEventListener(childEventListener);
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
        for (int i = 0; i < bookmarkArrayList.size(); i++) {
            if (bookmarkHashMap.get(bookmarkArrayList.get(i)).getName().toLowerCase().contains(s.toLowerCase())) {
                bookmarkList.smoothScrollToPosition(i);
                break;
            }
        }
    }


    public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            View rowView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_row_people, viewGroup, false);
            return new ViewHolder(rowView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {

            Users bookmark = bookmarkHashMap.get(bookmarkArrayList.get(i));
            viewHolder.tvName.setText(bookmark.getName());
            viewHolder.tvPosition.setText(bookmark.getPosition());
            viewHolder.star.setLiked(true);

            boolean thumbImage = bookmark.getThumbImage().isEmpty();
            if (!thumbImage) {
                String path = Environment.getExternalStorageDirectory() + "/ConnectBase/ProfilePics/thumbImage/" + bookmarkArrayList.get(i) + ".jpg";
                File thumbFile = new File(path);

                if (thumbFile.exists()) {
                    viewHolder.ivPic.setImageBitmap(BitmapFactory.decodeFile(path));

                } else {
                    mThumbImagesReference.child(bookmarkArrayList.get(i) + ".jpg").getFile(thumbFile).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            viewHolder.ivPic.setImageBitmap(BitmapFactory.decodeFile(path));
                        } else {
                            Picasso.get()
                                    .load(bookmark.getThumbImage())
                                    .placeholder(R.drawable.avatar)
                                    .error(R.drawable.avatar)
                                    .into(viewHolder.ivPic);
                        }
                    });

                }
            } else viewHolder.ivPic.setImageResource(R.drawable.avatar);

            viewHolder.layout.setOnClickListener(v -> openProfile(i, viewHolder.itemView.getContext()));

            viewHolder.star.setOnLikeListener(new OnLikeListener() {
                @Override
                public void liked(LikeButton likeButton) {

                }

                @Override
                public void unLiked(LikeButton likeButton) {
                    new AlertDialog.Builder(view.getContext())
                            .setTitle("Remove From Bookmark!!")
                            .setMessage("Are you sure you want to remove " + bookmark.getName() + " from bookmarks??")
                            .setPositiveButton("Yes", (dialog, which) -> mBookmarkReference.child(bookmarkArrayList.get(i)).removeValue())
                            .setNegativeButton("No", (dialog, which) -> viewHolder.star.setLiked(true))
                            .show();
                }
            });


        }

        @Override
        public int getItemCount() {
            return bookmarkArrayList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            TextView tvName, tvPosition;
            CircleImageView ivPic;
            View layout;
            LikeButton star;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_layoutRowPeople_name);
                tvPosition = itemView.findViewById(R.id.tv_layoutRowPeople_position);
                ivPic = itemView.findViewById(R.id.iv_layoutRowPeople_profilePic);
                layout = itemView.findViewById(R.id.linlay_lRP);
                star = itemView.findViewById(R.id.star_lRP_like);
                star.setVisibility(View.VISIBLE);
                star.setLiked(true);
            }
        }
    }

    private void openProfile(int i, Context context) {
        String id = bookmarkArrayList.get(i);

        Users bookmark = getBookmarkFromDatabase(id);
        Intent intent = new Intent(context, ViewUserProfile.class);
        intent.putExtra("user", bookmark);
        intent.putExtra("id", id);
        startActivity(intent);
    }

    private Users getBookmarkFromDatabase(String id) {
        Users user = new Users();

        Cursor cursor = userDatabase.rawQuery("Select * from bookmarks where id='" + id + "'", null, null);

        if (cursor == null || cursor.getCount() == 0)
            return null;
        cursor.moveToFirst();

        user.setAge(cursor.getString(1));
        user.setCity(cursor.getString(2));
        user.setEmail(cursor.getString(3));
        user.setExperience(cursor.getString(4));
        user.setImage(cursor.getString(5));
        user.setMobile(cursor.getString(6));
        user.setName(cursor.getString(7));
        user.setOrganisation(cursor.getString(8));
        user.setPosition(cursor.getString(9));
        user.setQualification(cursor.getString(10));
        user.setResume(cursor.getString(11));
        user.setSkills(cursor.getString(12));
        user.setState(cursor.getString(13));
        user.setThumbImage(cursor.getString(14));

        cursor.close();
        return user;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBookmarkReference.removeEventListener(childEventListener);
        for (int i = 0; i < referenceArrayList.size(); i++) {
            referenceArrayList.get(i).removeEventListener(valueEventListener);
        }

    }
}
