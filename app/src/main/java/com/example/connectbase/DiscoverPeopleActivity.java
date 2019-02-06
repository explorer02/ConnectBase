package com.example.connectbase;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.like.LikeButton;
import com.like.OnLikeListener;
import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;


public class DiscoverPeopleActivity extends AppCompatActivity implements UserAdapter.onclickItem{

    RecyclerView recyclerView;
    UserAdapter adapter;
    ArrayList<Users>userList,subUserList;
    ArrayList<String>tags=new ArrayList<>();
    ArrayList<String>userKeyList,subUserkeyList;
    DatabaseReference mUserReference,mBookmarkReference,mInviteReference,mFriendReference,mTagsReference;
    BottomSheetBehavior bottomSheetBehavior;
    String currentId=FirebaseAuth.getInstance().getUid();
    MaterialSearchView searchView;
    int listSelected=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover_people);
        Toolbar toolbar = findViewById(R.id.toolbar_discover);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Discover people");
        searchView = findViewById(R.id.search_view_discover);
        View bottomSheet=findViewById(R.id.bottom_sheet_discover);
        bottomSheetBehavior=BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        userList=new ArrayList<>();
        subUserList=new ArrayList<>();
        userKeyList=new ArrayList<>();
        subUserkeyList=new ArrayList<>();

        recyclerView = findViewById(R.id.rv_discover);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        adapter = new UserAdapter(this, userList);
        recyclerView.setAdapter(adapter);

        mUserReference = FirebaseDatabase.getInstance().getReference().child("Users");
        mBookmarkReference=FirebaseDatabase.getInstance().getReference().child("Bookmark");
        mFriendReference=FirebaseDatabase.getInstance().getReference().child("Friends");
        mInviteReference=FirebaseDatabase.getInstance().getReference().child("Invites");
        mTagsReference=FirebaseDatabase.getInstance().getReference().child("Tags");
        searchView.setHint("Search....");
        searchView.setCursorDrawable(R.drawable.search_background);


        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                InputMethodManager methodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                methodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                loadData(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return true;
            }
        });


        mUserReference.orderByChild("name").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Users users=dataSnapshot.getValue(Users.class);
                userList.add(users);
                userKeyList.add(dataSnapshot.getKey());
                adapter.notifyDataSetChanged();
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
        });
        mTagsReference.orderByChild("value").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                tags.add(dataSnapshot.child("value").getValue().toString());
                searchView.setSuggestions(tags.toArray(new String[]{}));
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
        });

    }

    private void loadData(String keyword) {
        if (!keyword.isEmpty()) {
            subUserkeyList.clear();
            subUserList.clear();
            for (int i=0;i<userKeyList.size();i++){
                keyword=keyword.toLowerCase();
                if(userList.get(i).getName().toLowerCase().contains(keyword)||userList.get(i).getSkills().toLowerCase().contains(keyword)||userList.get(i).getPosition().toLowerCase().contains(keyword)){
                    subUserList.add(userList.get(i));
                    subUserkeyList.add(userKeyList.get(i));
                }
            }
            adapter = new UserAdapter(this, subUserList);
            recyclerView.setAdapter(adapter);
            listSelected=1;
        }
        else {
            listSelected = 0;
            adapter = new UserAdapter(this, userList);
            recyclerView.setAdapter(adapter);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_discover, menu);

        MenuItem item = menu.findItem(R.id.menu_discover_search);
        searchView.setMenuItem(item);

        return true;
    }

    @Override
    public void onBackPressed() {
        if (bottomSheetBehavior.getState()==BottomSheetBehavior.STATE_EXPANDED||bottomSheetBehavior.getState()==BottomSheetBehavior.STATE_HALF_EXPANDED)
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        else if(searchView.isSearchOpen())
            searchView.closeSearch();
        else if(listSelected==1&&subUserList.size()==0)
            loadData("");
        else
            super.onBackPressed();
    }


    @Override
    public void onItemClicked(final int i) {

        ArrayList<String>keyArray=(listSelected>0)?subUserkeyList:userKeyList;
        ArrayList<Users>userArray=(listSelected>0)?subUserList:userList;

        final String uid=keyArray.get(i);


        if(uid.equals(currentId)){
            startActivity(new Intent(this,UpdateProfileActivity.class).putExtra("id",currentId));
            return;
        }

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        Toolbar toolbar=findViewById(R.id.toolbar_fragSheet);

        toolbar.setTitle(userArray.get(i).getName());

        ArrayList<TextView>arrayView=new ArrayList<>();
        arrayView.add(findViewById(R.id.tv_fragSheet_age));
        arrayView.add(findViewById(R.id.tv_fragSheet_qualification));
        arrayView.add(findViewById(R.id.tv_fragSheet_organisation));
        arrayView.add(findViewById(R.id.tv_fragSheet_position));
        arrayView.add(findViewById(R.id.tv_fragSheet_skills));
        arrayView.add(findViewById(R.id.tv_fragSheet_experience));
        arrayView.add(findViewById(R.id.tv_fragSheet_city));
        arrayView.add(findViewById(R.id.tv_fragSheet_state));
        CircleImageView ivProfilePic=findViewById(R.id.iv_fragSheet_profilePic);
        ImageView ivResume=findViewById(R.id.iv_fragSheet_resume);
        final LikeButton star=findViewById(R.id.star_fragSheet_bookmark);
        final Button btnSend,btnReject;
        btnSend=findViewById(R.id.btn_fragSheet_sendInviteRequest);
        btnReject=findViewById(R.id.btn_fragSheet_rejectInviteRequest);
        btnReject.setVisibility(View.GONE);

        btnSend.setTag("not_friend");
        btnSend.setClickable(false);



        mBookmarkReference.child(currentId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild(uid)){
                    star.setLiked(true);
                }
                else star.setLiked(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        mInviteReference.child(currentId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild(uid)){
                    String type=dataSnapshot.child(keyArray.get(i)).child("request_type").getValue().toString();
                    btnSend.setTag(type);
                    if(type.equals("request_sent")){
                        btnSend.setText("Cancel Invite");
                        btnReject.setVisibility(View.GONE);

                    }
                    else {
                        btnSend.setText("Accept Invite");
                        btnReject.setVisibility(View.VISIBLE);
                    }
                    btnSend.setClickable(true);

                }
                else{
                    mFriendReference.child(currentId).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.hasChild(keyArray.get(i))){

                                btnSend.setTag("friend");
                                btnSend.setText("Remove from FriendList");

                                btnReject.setVisibility(View.GONE);
                                btnSend.setClickable(true);
                            }
                            else {
                                btnSend.setText("Send Invite");
                                btnSend.setClickable(true);
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        
        star.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton likeButton) {
                HashMap map=new HashMap();
                map.put("star","true");
                map.put("time",ServerValue.TIMESTAMP);
                mBookmarkReference.child(currentId).child(keyArray.get(i)).setValue(map);
            }

            @Override
            public void unLiked(LikeButton likeButton) {
                mBookmarkReference.child(currentId).child(keyArray.get(i)).removeValue();
            }
        });

        btnSend.setOnClickListener(v -> {

            btnSend.setClickable(false);

            String type = btnSend.getTag().toString();
            if (type.equals("not_friend")) {

                mInviteReference.child(currentId).child(uid).child("request_type").setValue("request_sent").addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        mInviteReference.child(uid).child(currentId).child("request_type").setValue("request_received").addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                btnSend.setTag("request_sent");
                                btnSend.setClickable(true);
                                btnSend.setText("Cancel Invite");
                            } else {
                                Toast.makeText(DiscoverPeopleActivity.this, task1.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                btnSend.setClickable(true);
                            }
                        });
                    } else {
                        Toast.makeText(DiscoverPeopleActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        btnSend.setClickable(true);
                    }
                });
            } else if (type.equals("friend")) {

                mFriendReference.child(currentId).child(uid).removeValue().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        mFriendReference.child(uid).child(currentId).removeValue().addOnCompleteListener(task12 -> {
                            if (task12.isSuccessful()) {
                                btnSend.setTag("not_friend");
                                btnSend.setText("Send Invite");
                                btnSend.setClickable(true);
                            } else {
                                Toast.makeText(DiscoverPeopleActivity.this, task12.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                btnSend.setClickable(true);
                            }
                        });
                    } else {
                        Toast.makeText(DiscoverPeopleActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        btnSend.setClickable(true);
                    }
                });

            } else if (type.equals("request_sent")) {

                mInviteReference.child(currentId).child(uid).removeValue().addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        mInviteReference.child(uid).child(currentId).removeValue().addOnCompleteListener(task13 -> {

                            if (task13.isSuccessful()) {
                                btnSend.setTag("not_friend");
                                btnSend.setText("Send Invite");
                                btnSend.setClickable(true);
                            } else {
                                Toast.makeText(DiscoverPeopleActivity.this, task13.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                btnSend.setClickable(true);
                            }
                        });
                    } else {
                        Toast.makeText(DiscoverPeopleActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        btnSend.setClickable(true);
                    }
                });

            } else if (type.equals("request_received")) {

                mFriendReference.child(currentId).child(uid).child("time").setValue(ServerValue.TIMESTAMP).addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        mFriendReference.child(uid).child(currentId).child("since").setValue(ServerValue.TIMESTAMP).addOnCompleteListener(task14 -> {

                            if (task14.isSuccessful()) {
                                btnSend.setTag("friend");
                                btnSend.setText("Remove from FriendList");
                                btnSend.setClickable(true);
                                btnReject.setVisibility(View.GONE);
                                mInviteReference.child(currentId).child(uid).removeValue();
                                mInviteReference.child(uid).child(currentId).removeValue();
                            } else {
                                Toast.makeText(DiscoverPeopleActivity.this, task14.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                btnSend.setClickable(true);
                            }

                        });
                    } else {
                        Toast.makeText(DiscoverPeopleActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        btnSend.setClickable(true);
                    }
                });

            }

        });

        btnReject.setOnClickListener(v -> {
            btnReject.setClickable(false);
            mInviteReference.child(currentId).child(uid).removeValue().addOnCompleteListener(task -> {

                if (task.isSuccessful()) {
                    mInviteReference.child(uid).child(currentId).removeValue().addOnCompleteListener(task15 -> {

                        if (task15.isSuccessful()) {
                            btnReject.setClickable(true);
                            btnReject.setVisibility(View.GONE);
                            btnSend.setTag("not_friend");
                            btnSend.setText("Send Invite");
                        } else {
                            Toast.makeText(DiscoverPeopleActivity.this, task15.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            btnReject.setClickable(true);
                        }
                    });
                } else {
                    Toast.makeText(DiscoverPeopleActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    btnReject.setClickable(true);
                }
            });

        });

            ivResume.setVisibility(View.GONE);
        if(!userArray.get(i).getImage().isEmpty())
            Picasso.get()
            .load(userArray.get(i).getImage())
            .placeholder(R.drawable.avatar)
            .into(ivProfilePic);
        else ivProfilePic.setImageResource(R.drawable.avatar);

        arrayView.get(0).setText("Age:\t\t"+userArray.get(i).getAge());
        arrayView.get(1).setText("Qualifications:\t\t"+userArray.get(i).getQualification());
        arrayView.get(2).setText("Organisation:\t\t"+userArray.get(i).getOrganisation());
        arrayView.get(3).setText("Position:\t\t"+userArray.get(i).getPosition());
        arrayView.get(4).setText("Skills:\t\t"+userArray.get(i).getSkills());
        arrayView.get(5).setText("Experience:\t\t"+userArray.get(i).getExperience());
        arrayView.get(6).setText("City:\t\t"+userArray.get(i).getCity());
        arrayView.get(7).setText("State:\t\t"+userArray.get(i).getState());

    }
}

