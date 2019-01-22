package com.example.connectbase;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.like.LikeButton;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;


public class FragFriends extends Fragment {

    DatabaseReference mFriendReference,mUserReference;
    RecyclerView friendList;
    String currentId=MainActivity.currentId;
    View view;
    EditText etSearch;
    FirebaseRecyclerAdapter<UserId,FragFriends.ViewHolder> adapter;
    HashMap<String,Users> usersHashMap=new HashMap<>();
    FirebaseRecyclerOptions friendOptions;
    ArrayList<String> arrayId=new ArrayList<>();

    public FragFriends() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view=inflater.inflate(R.layout.fragment_bookmark, container, false);
        initialiseVariables();
        return view;
    }

    private void initialiseVariables() {

        mFriendReference= FirebaseDatabase.getInstance().getReference().child("Friends");
        currentId= FirebaseAuth.getInstance().getUid();
        mUserReference = FirebaseDatabase.getInstance().getReference().child("Users");
        friendList=view.findViewById(R.id.list_fragBookmark);
        LinearLayoutManager manager=new LinearLayoutManager(view.getContext());
        friendList.setLayoutManager(manager);
        etSearch=view.findViewById(R.id.et_fragBookmark_search);
        friendOptions=new FirebaseRecyclerOptions.Builder<UserId>()
                .setQuery(mFriendReference.child(currentId).orderByChild("time"),UserId.class)
                .build();

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                if (!s.toString().trim().isEmpty())
                    loadIndices(s.toString());

            }
        });

        adapter=new FirebaseRecyclerAdapter<UserId, FragFriends.ViewHolder>(friendOptions) {
            @Override
            protected void onBindViewHolder(@NonNull FragFriends.ViewHolder holder, int position, @NonNull UserId model) {

                String id=getRef(position).getKey();
                arrayId.add(position,id);

                mUserReference.child(id).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        holder.tvName.setText(dataSnapshot.child("name").getValue().toString());
                        holder.tvPosition.setText(dataSnapshot.child("position").getValue().toString());
                        String image=dataSnapshot.child("image").getValue().toString();

                        Users user=dataSnapshot.getValue(Users.class);
                        usersHashMap.put(id,user);
                        etSearch.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
                        if(!image.isEmpty())
                            Picasso.get()
                                    .load(image)
                                    .placeholder(R.drawable.avatar)
                                    .into(holder.ivPic);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


                holder.itemView.setOnClickListener(v -> showMenu(id, holder.itemView.getContext()));
    }

            @NonNull
            @Override
            public FragFriends.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

                View view=LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_row_bookmark,viewGroup,false);

                return new FragFriends.ViewHolder(view);
            }
        };
        friendList.setAdapter(adapter);
        adapter.startListening();

    }


    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView tvName,tvPosition;
        CircleImageView ivPic;
        LikeButton star;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName=itemView.findViewById(R.id.tv_layoutRowBookmark_name);
            tvPosition=itemView.findViewById(R.id.tv_layoutRowBookmark_position);
            ivPic=itemView.findViewById(R.id.iv_layoutRowBookmark_profilePic);
            star=itemView.findViewById(R.id.star_layoutRowBookmark_like);
            star.setVisibility(View.GONE);

        }
    }


    private void showMenu(String id, Context context) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        dialog.setCancelable(true);
        View view = getLayoutInflater().inflate(R.layout.layout_friend_bottomsheetdialog, null, false);

        View sendMessage = view.findViewById(R.id.linLay_LFBSD_SendMessage);
        View viewProfile = view.findViewById(R.id.linLay_LFBSD_ViewProfile);

        sendMessage.setOnClickListener(v -> {
            FragFriends.this.openChatActivity(id, context);
            dialog.dismiss();
        });

        viewProfile.setOnClickListener(v -> {
            openUserProfile(id, context);
            dialog.dismiss();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void openChatActivity(String id, Context context) {

        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra("user", usersHashMap.get(id));
        intent.putExtra("id", id);
        startActivity(intent);
    }

    private void openUserProfile(String id, Context context){

        Intent intent=new Intent(context,ViewUserProfile.class);
        intent.putExtra("user",usersHashMap.get(id));
        intent.putExtra("id",id);
        startActivity(intent);
    }

    private void loadIndices(String text) {
        for (int i = 0; i < arrayId.size(); i++) {
            if (usersHashMap.get(arrayId.get(i)).getName().toLowerCase().contains(text.toLowerCase())){
                friendList.smoothScrollToPosition(i);
                break;
            }
        }
    }
}
