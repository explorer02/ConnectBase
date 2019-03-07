package com.example.connectbase;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.like.LikeButton;
import com.like.OnLikeListener;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ViewUserProfile extends AppCompatActivity {

    String currentId=MainActivity.currentId;
    String uid;
    ArrayList<TextView> arrayView=new ArrayList<>();
    DatabaseReference mBookmarkReference,mFriendReference,mInviteReference;
    final int REQUEST_CODE_STORAGE_WRITE=202;
    ImageView ivResume;
    TextView tvResume;
    StorageReference mResumeReference;
    ProgressDialog dialog;
    CircleImageView ivProfilePic;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_user_profile);

        Toolbar toolbar=findViewById(R.id.toolbar_ViewUserProfile);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Users user=(Users) getIntent().getSerializableExtra("user");
        uid=getIntent().getStringExtra("id");

        getSupportActionBar().setTitle(user.getName());

        mBookmarkReference=FirebaseDatabase.getInstance().getReference().child("Bookmark");
        mFriendReference=FirebaseDatabase.getInstance().getReference().child("Friends");
        mInviteReference=FirebaseDatabase.getInstance().getReference().child("Invites");
        mResumeReference= FirebaseStorage.getInstance().getReference().child("Resume");


        arrayView.add( findViewById(R.id.tv_ViewUserProfile_age));
        arrayView.add(findViewById(R.id.tv_ViewUserProfile_qualification));
        arrayView.add(findViewById(R.id.tv_ViewUserProfile_organisation));
        arrayView.add(findViewById(R.id.tv_ViewUserProfile_position));
        arrayView.add( findViewById(R.id.tv_ViewUserProfile_skills));
        arrayView.add( findViewById(R.id.tv_ViewUserProfile_experience));
        arrayView.add( findViewById(R.id.tv_ViewUserProfile_city));
        arrayView.add( findViewById(R.id.tv_ViewUserProfile_state));
        ivProfilePic = findViewById(R.id.iv_ViewUserProfile_profilePic);
        ivResume=findViewById(R.id.iv_ViewUserProfile_resume);
        tvResume=findViewById(R.id.tv_ViewUserProfile_resume);

        tvResume.setVisibility(View.GONE);
        ivResume.setVisibility(View.GONE);

        final LikeButton star=findViewById(R.id.star_viewUserProfile_bookmark);
        star.setLiked(true);
        final Button btnSend,btnReject;
        btnSend=findViewById(R.id.btn_ViewUserProfile_sendInviteRequest);
        btnReject=findViewById(R.id.btn_ViewUserProfile_rejectInviteRequest);
        btnReject.setVisibility(View.GONE);

        btnSend.setTag("not_friend");
        btnSend.setClickable(false);

        star.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton likeButton) {

                HashMap map=new HashMap();
                map.put("star","true");
                map.put("time",ServerValue.TIMESTAMP);
                mBookmarkReference.child(currentId).child(uid).setValue(map);

            }

            @Override
            public void unLiked(LikeButton likeButton) {
                AlertDialog.Builder dialog=new AlertDialog.Builder(ViewUserProfile.this);
                dialog.setTitle("Remove from Bookmarks")
                        .setMessage("Are you sure you want to remove "+user.getName()+" from Bookmarks??")
                        .setNegativeButton("Cancel", (dialog12, which) -> star.setLiked(true))
                        .setPositiveButton("Ok", (dialog1, which) -> {
                            mBookmarkReference.child(currentId).child(uid).removeValue();
                        });

                dialog.setCancelable(false)
                        .show();
            }
        });

        ivResume.setOnClickListener(v -> {
            String location = Environment.getExternalStorageDirectory() + "/ConnectBase/Resume/" + uid + ".pdf";
            File file=new File(location);

            if (file.exists()) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setTitle("Select your Choice!!");
                dialog.setMessage("This file exist in your device");
                dialog.setNegativeButton("Re-Download", (dialog13, which) -> {
                    downloadResume();
                    ivResume.setClickable(false);
                });
                dialog.setPositiveButton("View", (dialog14, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    Uri uri;
                    if (Build.VERSION.SDK_INT >= 24)
                        uri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext()
                                .getPackageName() + ".provider", file);
                    else uri = Uri.fromFile(file);
                    intent.setDataAndType(uri, "application/pdf");
                    startActivity(intent);

                });
                dialog.show();
            }

            else {
                downloadResume();
                ivResume.setClickable(false);
            }
        });

        mInviteReference.child(currentId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                btnSend.setClickable(true);
                if(dataSnapshot.hasChild(uid)){
                    String type=dataSnapshot.child(uid).child("request_type").getValue().toString();
                    if(type.equals("request_sent")){
                        btnSend.setText("Cancel Invite");
                        btnReject.setVisibility(View.GONE);
                    }
                    else {
                        btnSend.setText("Accept Invite");
                        btnReject.setVisibility(View.VISIBLE);
                    }
                    btnSend.setTag(type);
                    ivResume.setVisibility(View.GONE);
                    tvResume.setVisibility(View.GONE);
                }
                else {
                    mFriendReference.child(currentId).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.hasChild(uid)){
                                btnSend.setTag("friend");
                                if (!user.getResume().trim().isEmpty())
                                    ivResume.setVisibility(View.VISIBLE);
                                tvResume.setVisibility(View.VISIBLE);
                                btnSend.setText("Remove from FriendList");
                                btnSend.setClickable(true);
                                btnReject.setVisibility(View.GONE);
                            }
                            else {
                                btnSend.setTag("not_friend");
                                ivResume.setVisibility(View.GONE);
                                tvResume.setVisibility(View.GONE);
                                btnSend.setText("Send Invite");
                                btnSend.setClickable(true);
                                btnReject.setVisibility(View.GONE);
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

        mInviteReference.keepSynced(true);
        mFriendReference.keepSynced(true);
        mBookmarkReference.keepSynced(true);


        btnSend.setOnClickListener(v -> {

            btnSend.setClickable(false);

            String type=btnSend.getTag().toString();
            switch (type) {
                case "not_friend":

                    mInviteReference.child(currentId).child(uid).child("request_type").setValue("request_sent").addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            mInviteReference.child(uid).child(currentId).child("request_type").setValue("request_received").addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    btnSend.setTag("request_sent");
                                    btnSend.setClickable(true);
                                    btnSend.setText("Cancel Invite");
                                } else {
                                    Toast.makeText(ViewUserProfile.this, task1.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    btnSend.setClickable(true);
                                }
                            });
                        } else {
                            Toast.makeText(ViewUserProfile.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            btnSend.setClickable(true);
                        }
                    });
                    break;
                case "friend":

                    mFriendReference.child(currentId).child(uid).removeValue().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            mFriendReference.child(uid).child(currentId).removeValue().addOnCompleteListener(task12 -> {
                                if (task12.isSuccessful()) {
                                    btnSend.setTag("not_friend");
                                    btnSend.setText("Send Invite");
                                    ivResume.setVisibility(View.GONE);
                                    btnSend.setClickable(true);
                                } else {
                                    Toast.makeText(ViewUserProfile.this, task12.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    btnSend.setClickable(true);
                                }
                            });
                        } else {
                            Toast.makeText(ViewUserProfile.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            btnSend.setClickable(true);
                        }
                    });

                    break;
                case "request_sent":

                    mInviteReference.child(currentId).child(uid).removeValue().addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {
                            mInviteReference.child(uid).child(currentId).removeValue().addOnCompleteListener(task13 -> {

                                if (task13.isSuccessful()) {
                                    btnSend.setTag("not_friend");
                                    btnSend.setText("Send Invite");
                                    btnSend.setClickable(true);
                                } else {
                                    Toast.makeText(ViewUserProfile.this, task13.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    btnSend.setClickable(true);
                                }
                            });
                        } else {
                            Toast.makeText(ViewUserProfile.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            btnSend.setClickable(true);
                        }
                    });

                    break;
                case "request_received":

                    mFriendReference.child(currentId).child(uid).child("time").setValue(ServerValue.TIMESTAMP).addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {
                            mFriendReference.child(uid).child(currentId).child("since").setValue(ServerValue.TIMESTAMP).addOnCompleteListener(task15 -> {

                                if (task15.isSuccessful()) {
                                    btnSend.setTag("friend");
                                    btnSend.setText("Remove from FriendList");
                                    btnSend.setClickable(true);
                                    btnReject.setVisibility(View.GONE);
                                    mInviteReference.child(currentId).child(uid).removeValue();
                                    mInviteReference.child(uid).child(currentId).removeValue();
                                } else {
                                    Toast.makeText(ViewUserProfile.this, task15.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    btnSend.setClickable(true);
                                }

                            });
                        } else {
                            Toast.makeText(ViewUserProfile.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            btnSend.setClickable(true);
                        }
                    });

                    break;
            }

        });

        btnReject.setOnClickListener(v -> {
            btnReject.setClickable(false);
            mInviteReference.child(currentId).child(uid).removeValue().addOnCompleteListener(task -> {

                if (task.isSuccessful()) {
                    mInviteReference.child(uid).child(currentId).removeValue().addOnCompleteListener(task14 -> {

                        if (task14.isSuccessful()) {
                            btnReject.setClickable(true);
                            btnReject.setVisibility(View.GONE);
                            btnSend.setTag("not_friend");
                            btnSend.setText("Send Invite");
                        } else {
                            Toast.makeText(ViewUserProfile.this, task14.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            btnReject.setClickable(true);
                        }
                    });
                } else {
                    Toast.makeText(ViewUserProfile.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    btnReject.setClickable(true);
                }
            });

        });

        if (!user.getImage().trim().isEmpty()) {

            new CommonFunctions().downloadProfilePic(getApplicationContext(), uid, ivProfilePic, user.getImage());


        }
        else ivProfilePic.setImageResource(R.drawable.avatar);

        arrayView.get(0).setText("Age:\t\t"+user.getAge());
        arrayView.get(1).setText("Qualifications:\t\t"+user.getQualification());
        arrayView.get(2).setText("Organisation:\t\t"+user.getOrganisation());
        arrayView.get(3).setText("Position:\t\t"+user.getPosition());
        arrayView.get(4).setText("Skills:\t\t"+user.getSkills());
        arrayView.get(5).setText("Experience:\t\t"+user.getExperience());
        arrayView.get(6).setText("City:\t\t"+user.getCity());
        arrayView.get(7).setText("State:\t\t"+user.getState());

        ivProfilePic.setOnClickListener(v -> {
            String path = Environment.getExternalStorageDirectory() + "/ConnectBase/ProfilePics/" + uid + ".jpg";
            Intent intent = new Intent(this, ZoomImageViewActivity.class);
            intent.putExtra("path", path);
            startActivity(intent);
        });

    }

    void downloadResume(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_CODE_STORAGE_WRITE);
        }
        else {
            Toast.makeText(this, "Downloading file from Server...", Toast.LENGTH_SHORT).show();

            File parentFile = new File(Environment.getExternalStorageDirectory() + "/ConnectBase/Resume/");
            parentFile.mkdirs();
            final File file=new File(parentFile,uid+".pdf");
            if (file.exists())
                file.delete();
            showDialog("Downloading Resume", ProgressDialog.STYLE_HORIZONTAL);
            mResumeReference.child(uid + ".pdf").getFile(file).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    ivResume.setClickable(true);
                    dialog.dismiss();
                    Toast.makeText(ViewUserProfile.this, "File downloaded Successfully!!", Toast.LENGTH_SHORT).show();
                } else {
                    ivResume.setClickable(true);
                    dialog.dismiss();
                    Toast.makeText(ViewUserProfile.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }

            }).addOnProgressListener(taskSnapshot -> {
                dialog.setMax((int) (taskSnapshot.getTotalByteCount() / 1024));
                dialog.setProgress((int) (taskSnapshot.getBytesTransferred() / 1024));
            });


        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case REQUEST_CODE_STORAGE_WRITE:

                if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)
                    downloadResume();
                else {
                    ivResume.setClickable(true);
                    Toast.makeText(this, "This functionality requires writing exernal storage permission", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void showDialog(String message,int style){
        dialog=new ProgressDialog(this);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setProgressStyle(style);
        dialog.show();
    }

}
