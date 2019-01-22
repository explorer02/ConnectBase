package com.example.connectbase;


import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class FragInviteRequest extends Fragment {

    View view;
    RecyclerView inviteRequestList;
    FirebaseRecyclerOptions iROptions;
    FirebaseRecyclerAdapter<Invite, ViewHolder> adapter;
    ArrayList<String> keyList = new ArrayList<>();
    HashMap<String, Users> usersHashMap = new HashMap<>();
    DatabaseReference mInviteReference, mUserReference, mFriendReference;
    String currentId;


    public interface CallBacks {
        void changeTabText(String text);
    }

    CallBacks callBacks;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        callBacks = (CallBacks) context;
    }

    @Override
    public void onResume() {
        super.onResume();
        onActivityCreated(null);
        callBacks.changeTabText("Invites (" + usersHashMap.size() + ")");

    }

    public FragInviteRequest() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_invite_request, container, false);
        initialiseVariables();
        return view;
    }

    private void initialiseVariables() {

        mInviteReference = FirebaseDatabase.getInstance().getReference().child("Invites");
        mUserReference = FirebaseDatabase.getInstance().getReference().child("Users");
        mFriendReference = FirebaseDatabase.getInstance().getReference().child("Friends");
        inviteRequestList = view.findViewById(R.id.list_fragInviteRequest);
        inviteRequestList.setLayoutManager(new LinearLayoutManager(view.getContext()));
        currentId = FirebaseAuth.getInstance().getUid();
        iROptions = new FirebaseRecyclerOptions.Builder<Invite>()
                .setQuery(mInviteReference.child(currentId).orderByChild("request_type").equalTo("request_received"), Invite.class)
                .build();

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        keyList.clear();
        usersHashMap.clear();

        adapter = new FirebaseRecyclerAdapter<Invite, ViewHolder>(iROptions) {
            @Override
            protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull Invite model) {

                String id = getRef(position).getKey();
                keyList.add(position, id);
                mUserReference.child(keyList.get(position)).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Users user = dataSnapshot.getValue(Users.class);
                        usersHashMap.put(id, user);

                        callBacks.changeTabText("Invites (" + usersHashMap.size() + ")");

                        holder.tvName.setText(user.getName());
                        holder.tvPosition.setText(user.getPosition());
                        if (!user.getThumbImage().isEmpty())
                            Picasso.get()
                                    .load(user.getThumbImage())
                                    .into(holder.ivPic);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                holder.itemView.setOnClickListener(v -> openUserProfile(id, holder.itemView.getContext()));
                holder.ivAccept.setOnClickListener(v -> acceptRequest(id));
                holder.ivReject.setOnClickListener(v -> rejectRequest(id, holder.ivReject.getContext()));

            }

            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_row_invite_request, viewGroup, false);
                return new ViewHolder(view);
            }
        };

        inviteRequestList.setAdapter(adapter);
        adapter.startListening();

    }

    private void rejectRequest(String s, Context context) {

        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle("Delete Invite");
        dialog.setMessage("Are you sure you want to delete Invite from " + usersHashMap.get(s).getName() + "??");
        dialog.setPositiveButton("Ok", (dialog1, which) -> {
            Snackbar.make(view, usersHashMap.get(s).getName() + " Invite Removed", Snackbar.LENGTH_SHORT).show();
            deleteRequestOnFirebase(s);
        });
        dialog.setNegativeButton("Cancel", null);
        dialog.show();
    }

    private void acceptRequest(String s) {

        mFriendReference.child(currentId).child(s).child("since").setValue(ServerValue.TIMESTAMP).addOnCompleteListener(task -> {

            if (task.isSuccessful()) {
                mFriendReference.child(s).child(currentId).child("since").setValue(ServerValue.TIMESTAMP).addOnCompleteListener(task1 -> {

                    if (task1.isSuccessful()) {
                        Snackbar.make(view, usersHashMap.get(s).getName() + " added to your Contacts", Snackbar.LENGTH_SHORT).show();
                        deleteRequestOnFirebase(s);
                    } else
                        Snackbar.make(view, task1.getException().getMessage(), Snackbar.LENGTH_SHORT).show();
                });
            } else
                Snackbar.make(view, task.getException().getMessage(), Snackbar.LENGTH_SHORT).show();
        });

    }


    private void deleteRequestOnFirebase(String id) {

        usersHashMap.remove(id);
        keyList.remove(id);
        callBacks.changeTabText("Invites (" + usersHashMap.size() + ")");

        mInviteReference.child(currentId).child(id).removeValue();
        mInviteReference.child(id).child(currentId).removeValue();
    }

    void openUserProfile(String id, Context context) {
        Intent intent = new Intent(context, ViewUserProfile.class);
        intent.putExtra("user", usersHashMap.get(id));
        intent.putExtra("id", id);
        startActivity(intent);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvPosition;
        CircleImageView ivPic;
        ImageView ivAccept, ivReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvName = itemView.findViewById(R.id.tv_layoutIR_name);
            tvPosition = itemView.findViewById(R.id.tv_layoutIR_position);
            ivPic = itemView.findViewById(R.id.iv_layoutRowIR_profilePic);
            ivAccept = itemView.findViewById(R.id.iv_layout_IR_accept);
            ivReject = itemView.findViewById(R.id.iv_layout_IR_reject);

        }
    }


}
