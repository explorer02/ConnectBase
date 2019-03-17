package com.example.connectbase;


import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class FragChat extends Fragment {

    View view;
    RecyclerView chatList;
    EditText etSearch;
    SQLiteDatabase chatDatabase, userDatabase;
    static ChatAdapter adapter;
    static ArrayList<String> chatArrayList;
    static HashMap<String, UserHolder> chatHashmap;

    public FragChat() {
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
        chatList = view.findViewById(R.id.list_fragBookmark);
        etSearch = view.findViewById(R.id.et_fragBookmark_search);
        etSearch.setVisibility(View.GONE);
        chatDatabase = view.getContext().openOrCreateDatabase("chats", Context.MODE_PRIVATE, null);
        userDatabase = view.getContext().openOrCreateDatabase("users", Context.MODE_PRIVATE, null);

        chatArrayList = new ArrayList<>();
        chatHashmap = new HashMap<>();

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

            UserHolder userHolder = chatHashmap.get(chatArrayList.get(i));
            viewHolder.tvLastMessage.setText(userHolder.lastMessage);
            viewHolder.tvName.setText(userHolder.name);
            //viewHolder.tvLastMessage.setText(userHolder.lastMessage);


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

    @Override
    public void onStart() {
        super.onStart();
        new LoadChats().execute();
    }

    @SuppressLint("StaticFieldLeak")
    private class LoadChats extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            chatArrayList.clear();
            chatHashmap.clear();

            Cursor cursor = chatDatabase.rawQuery("select * from user_list", null, null);

            if (cursor.getCount() < 1)
                return null;
            cursor.moveToFirst();
            do {
                String id = cursor.getString(0);
                Cursor cursor1 = userDatabase.rawQuery("select * from friends where id='" + id + "'", null, null);
                Cursor cursor2 = chatDatabase.rawQuery("select * from user_" + id, null, null);

                String name, lastMessage = "", image;
                if (cursor1.getCount() > 0) {
                    cursor1.moveToFirst();
                    name = cursor1.getString(8);
                    image = cursor1.getString(15);
                } else {
                    name = "Friend Removed";
                    image = "";
                }
                if (cursor2.getCount() > 0) {
                    cursor2.moveToLast();
                    String messageId = cursor2.getString(0);
                    String type = cursor2.getString(1);
                    Cursor cursor3 = null;
                    switch (type) {
                        case "text":
                            cursor3 = chatDatabase.rawQuery("select message from message_text where message_id='" + messageId + "'", null, null);
                            cursor3.moveToFirst();
                            lastMessage = cursor3.getString(0);
                            break;
                        case "image":
                            cursor3 = chatDatabase.rawQuery("select description from message_image where message_id='" + messageId + "'", null, null);
                            cursor3.moveToFirst();
                            lastMessage = "(Image) " + cursor3.getString(0);
                            break;
                        case "file":
                            cursor3 = chatDatabase.rawQuery("select description from message_file where message_id='" + messageId + "'", null, null);
                            cursor3.moveToFirst();
                            lastMessage = "(File) " + cursor3.getString(0);
                            break;
                    }
                    if (cursor3 != null)
                        cursor3.close();

                }

                cursor1.close();
                cursor2.close();

                chatArrayList.add(id);
                chatHashmap.put(id, new UserHolder(name, lastMessage, image));
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

        public UserHolder(String name, String lastMessage, String pic) {
            this.name = name;
            this.lastMessage = lastMessage;
            this.pic = pic;
        }

    }
}
