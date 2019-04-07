package com.example.connectbase;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ForwardActivity extends AppCompatActivity {

    RecyclerView friendList;
    ArrayList<String> friendArrayList;
    HashMap<String, Friend> friendsHashMap;
    SQLiteDatabase userDatabase;
    FriendsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forward);
        Toolbar toolbar = findViewById(R.id.toolbar_forward);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Forward To...");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        friendList = findViewById(R.id.list_forwardActivity);
        friendArrayList = new ArrayList<>();
        friendsHashMap = new HashMap<>();
        adapter = new FriendsAdapter();
        friendList.setAdapter(adapter);
        friendList.setHasFixedSize(true);
        friendList.setLayoutManager(new LinearLayoutManager(this));
        userDatabase = openOrCreateDatabase("users", Context.MODE_PRIVATE, null);
        new LoadFriends().execute();

    }


    class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_row_forward_activity, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {

            String id = friendArrayList.get(i);
            Friend friend = friendsHashMap.get(id);
            viewHolder.tvName.setText(friend.name);

        }

        @Override
        public int getItemCount() {
            return friendArrayList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            TextView tvName;
            CircleImageView ivPic;
            CheckBox checkBox;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_lRFA_name);
                ivPic = itemView.findViewById(R.id.iv_lRFA_pic);
                checkBox = itemView.findViewById(R.id.cb_lRFA_check);
            }
        }
    }


    @SuppressLint("StaticFieldLeak")
    private class LoadFriends extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {

            Cursor cursor = userDatabase.rawQuery("Select id,chatId,name,thumbImage from friends", null, null);

            if (cursor == null || cursor.getCount() == 0)
                return null;

            cursor.moveToFirst();
            do {
                String id = cursor.getString(0);
                String chatId = cursor.getString(1);
                String name = cursor.getString(2);
                String thumbImage = cursor.getString(3);
                friendArrayList.add(id);
                friendsHashMap.put(id, new Friend(chatId, name, thumbImage));
                publishProgress();
            } while (cursor.moveToNext());

            cursor.close();
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            adapter.notifyDataSetChanged();
        }
    }

    class Friend {
        String chatId, name, thumbImage;

        Friend(String chatId, String name, String thumbImage) {
            this.chatId = chatId;
            this.name = name;
            this.thumbImage = thumbImage;
        }
    }

}
