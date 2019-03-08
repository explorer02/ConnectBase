package com.example.connectbase;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.like.LikeButton;
import com.like.OnLikeListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;


public class FragBookmark extends Fragment {

    EditText etSearch;
    DatabaseReference mBookmarkReference, mUserReference;
    FirebaseRecyclerOptions bookmarkOptions;
    RecyclerView bookmarkList;
    String currentId;
    View view;
    FirebaseRecyclerAdapter<UserId, FragBookmark.ViewHolder> adapter;
    HashMap<String, Users> usersHashMap = new HashMap<>();
    ArrayList<String> arrayId = new ArrayList<>();


    public FragBookmark() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_bookmark, container, false);
        initialiseVariables();
        return view;

    }

    void initialiseVariables() {
        mBookmarkReference = FirebaseDatabase.getInstance().getReference().child("Bookmark");
        currentId = FirebaseAuth.getInstance().getUid();
        mUserReference = FirebaseDatabase.getInstance().getReference().child("Users");
        bookmarkList = view.findViewById(R.id.list_fragBookmark);
        LinearLayoutManager manager = new LinearLayoutManager(view.getContext());
        bookmarkList.setLayoutManager(manager);
        bookmarkOptions = new FirebaseRecyclerOptions.Builder<UserId>()
                .setQuery(mBookmarkReference.child(currentId).orderByChild("time"), UserId.class)
                .build();
        etSearch = view.findViewById(R.id.et_fragBookmark_search);
        etSearch.setInputType(InputType.TYPE_NULL);


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

        adapter = new FirebaseRecyclerAdapter<UserId, ViewHolder>(bookmarkOptions) {
            @Override
            protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull UserId model) {

                String id = getRef(position).getKey();
                arrayId.add(position, id);

                mUserReference.child(id).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        holder.star.setLiked(true);
                        holder.tvName.setText(dataSnapshot.child("name").getValue().toString());
                        holder.tvPosition.setText(dataSnapshot.child("position").getValue().toString());
                        String image = dataSnapshot.child("image").getValue().toString();

                        Users user = dataSnapshot.getValue(Users.class);
                        usersHashMap.put(id, user);
                        etSearch.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
                        if (!image.isEmpty())
                            Picasso.get()
                                    .load(image)
                                    .placeholder(R.drawable.avatar)
                                    .into(holder.ivPic);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
                mUserReference.keepSynced(true);
                holder.star.setOnLikeListener(new OnLikeListener() {
                    @Override
                    public void liked(LikeButton likeButton) {

                    }

                    @Override
                    public void unLiked(LikeButton likeButton) {
                        showUnBookmarkDialog(id, holder.star);
                    }
                });

                holder.itemView.setOnClickListener(v -> openUserProfile(id, holder.itemView.getContext()));

            }

            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_row_bookmark, viewGroup, false);

                return new ViewHolder(view);
            }
        };
        mBookmarkReference.keepSynced(true);
        bookmarkList.setAdapter(adapter);
        adapter.startListening();


    }


    private void loadIndices(String text) {

        for (int i = 0; i < arrayId.size(); i++) {

            if (usersHashMap.get(arrayId.get(i)).getName().toLowerCase().contains(text.toLowerCase())) {

                bookmarkList.smoothScrollToPosition(i);
                break;
            }
        }
    }

    private void showUnBookmarkDialog(String id, final View star) {


        AlertDialog.Builder dialog = new AlertDialog.Builder(view.getContext());
        dialog.setTitle("Remove from Bookmarks")
                .setMessage("Are you sure you want to remove " + usersHashMap.get(id).getName() + " from Bookmarks??")
                .setNegativeButton("Cancel", (dialog12, which) -> ((LikeButton) star).setLiked(true))

                .setPositiveButton("Ok", (dialog1, which) -> mBookmarkReference.child(currentId).child(id).removeValue().addOnCompleteListener(task -> {
                    if (!task.isComplete()) {
                        Toast.makeText(view.getContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }));

        dialog.setCancelable(false)
                .show();

    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvPosition;
        CircleImageView ivPic;
        LikeButton star;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_layoutRowBookmark_name);
            tvPosition = itemView.findViewById(R.id.tv_layoutRowBookmark_position);
            ivPic = itemView.findViewById(R.id.iv_layoutRowBookmark_profilePic);
            star = itemView.findViewById(R.id.star_layoutRowBookmark_like);
        }
    }

    private void openUserProfile(String id, Context context) {

        Intent intent = new Intent(context, ViewUserProfile.class);
        intent.putExtra("user", usersHashMap.get(id));
        intent.putExtra("id", id);
        startActivity(intent);

    }

}
