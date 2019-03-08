package com.example.connectbase;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class UpdateProfileActivity extends AppCompatActivity {

    ArrayList<TextInputLayout> arrayLayout;
    Button btnUpdate;
    ProgressDialog dialog;
    ImageView ivResume, ivUpload;
    String currentId;
    DatabaseReference mUserReference;
    StorageReference mProfileImageReference, mResumeReference;
    final int REQUEST_CODE_PICK_RESUME = 101, REQUEST_CODE_STORAGE_READ = 201, REQUEST_CODE_STORAGE_WRITE = 202;
    View relativeLayout;
    ImageView ivCamera;
    CircleImageView ivProfilePic;
    Uri resumeUri;
    CommonFunctions commonFunctions = new CommonFunctions();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile);

        Toolbar toolbar = findViewById(R.id.toolbar_updateProfile);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Update Profile");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        currentId = getIntent().getStringExtra("id");
        arrayLayout = new ArrayList<>();
        arrayLayout.add(findViewById(R.id.til_updateProfile_name));
        arrayLayout.add(findViewById(R.id.til_updateProfile_age));
        arrayLayout.add(findViewById(R.id.til_updateProfile_qualification));
        arrayLayout.add(findViewById(R.id.til_updateProfile_mobile));
        arrayLayout.add(findViewById(R.id.til_updateProfile_organisation));
        arrayLayout.add(findViewById(R.id.til_updateProfile_position));
        arrayLayout.add(findViewById(R.id.til_updateProfile_skills));
        arrayLayout.add(findViewById(R.id.til_updateProfile_experience));
        arrayLayout.add(findViewById(R.id.til_updateProfile_city));
        arrayLayout.add(findViewById(R.id.til_updateProfile_state));
        btnUpdate = findViewById(R.id.btn_updateProfile_update);

        relativeLayout = findViewById(R.id.relativeLayout_update_profile);

        ivCamera = findViewById(R.id.iv_updateProfile_camera);
        ivProfilePic = findViewById(R.id.iv_updateProfile_profilePic);
        ivResume = findViewById(R.id.iv_updateProfile_resume);
        ivResume.setVisibility(View.GONE);
        ivUpload = findViewById(R.id.iv_updateProfile_upload);
        mUserReference = FirebaseDatabase.getInstance().getReference().child("Users");
        mProfileImageReference = FirebaseStorage.getInstance().getReference().child("ProfileImage");
        mResumeReference = FirebaseStorage.getInstance().getReference().child("Resume");

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        loadProfile();

        ivCamera.setOnClickListener(v -> {
            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setMinCropWindowSize(300, 300)
                    .setOutputCompressQuality(20)
                    .setOutputCompressFormat(Bitmap.CompressFormat.JPEG)
                    .setAspectRatio(1, 1)
                    .setMaxZoom(5)
                    .start(UpdateProfileActivity.this);
        });


        btnUpdate.setOnClickListener(v -> {
            checkProfile();
            for (int i = 0; i < arrayLayout.size(); i++)
                arrayLayout.get(i).clearFocus();
        });

        ivUpload.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            startActivityForResult(intent, REQUEST_CODE_PICK_RESUME);
        });

        ivProfilePic.setOnClickListener(v -> {
            String path = Environment.getExternalStorageDirectory() + "/ConnectBase/ProfilePics/" + currentId + ".jpg";
            startActivity(new Intent(this, ZoomImageViewActivity.class).putExtra("path", path));
        });

        ivResume.setOnClickListener(v -> {
            File parentFile = new File(Environment.getExternalStorageDirectory() + "/ConnectBase/Resume/");
            File resumeFile = new File(parentFile, "resume.pdf");

            Log.i("ConnectBase", resumeFile.getPath() + "\t\t\t" + resumeFile.exists());

            if (resumeFile.exists()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(commonFunctions.getUriFromFile(getApplicationContext(), resumeFile), "application/pdf");
                startActivity(intent);
            } else {
                Snackbar.make(relativeLayout, "File Not found on your device", Snackbar.LENGTH_SHORT).show();
                downloadResume();
                ivResume.setClickable(false);
            }
        });

    }

    private void showDialog(String message, int style) {
        dialog = new ProgressDialog(this);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setProgressStyle(style);
        dialog.show();
    }

    private void loadProfile() {

        showDialog("Please wait while we are loading your profile", ProgressDialog.STYLE_SPINNER);


        mUserReference.child(currentId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                final String name, age, qualification, mobile, organisation, position, skills, experience, city, state, image, resume;
                name = dataSnapshot.child("name").getValue().toString().trim();
                arrayLayout.get(0).getEditText().setText(name);
                age = dataSnapshot.child("age").getValue().toString().trim();
                arrayLayout.get(1).getEditText().setText(age);
                qualification = dataSnapshot.child("qualification").getValue().toString().trim();
                arrayLayout.get(2).getEditText().setText(qualification);
                mobile = dataSnapshot.child("mobile").getValue().toString().trim();
                arrayLayout.get(3).getEditText().setText(mobile);
                organisation = dataSnapshot.child("organisation").getValue().toString().trim();
                arrayLayout.get(4).getEditText().setText(organisation);
                position = dataSnapshot.child("position").getValue().toString().trim();
                arrayLayout.get(5).getEditText().setText(position);
                skills = dataSnapshot.child("skills").getValue().toString().trim();
                arrayLayout.get(6).getEditText().setText(skills);
                experience = dataSnapshot.child("experience").getValue().toString().trim();
                arrayLayout.get(7).getEditText().setText(experience);
                city = dataSnapshot.child("city").getValue().toString().trim();
                arrayLayout.get(8).getEditText().setText(city);
                state = dataSnapshot.child("state").getValue().toString().trim();
                arrayLayout.get(9).getEditText().setText(state);
                image = dataSnapshot.child("image").getValue().toString();
                resume = dataSnapshot.child("resume").getValue().toString();

                if (!resume.trim().isEmpty())
                    ivResume.setVisibility(View.VISIBLE);
                else ivResume.setVisibility(View.GONE);

                if (!image.isEmpty()) {
                    String path = Environment.getExternalStorageDirectory() + "/ConnectBase/ProfilePics/";
                    File parentFile = new File(path);
                    parentFile.mkdirs();
                    File imageFile = new File(parentFile, currentId + ".jpg");
                    Uri imageUri = commonFunctions.getUriFromFile(getApplicationContext(), imageFile);

                    if (imageFile.exists()) {
                        ivProfilePic.setImageURI(imageUri);
                    } else {

                        Log.i("ConnectBase Else", "Here");
                        mProfileImageReference.child(currentId + ".jpg").getFile(imageFile).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                ivProfilePic.setImageURI(imageUri);
                            } else {
                                Snackbar.make(ivProfilePic, "Failed to load Image", Snackbar.LENGTH_SHORT).show();
                                Log.i("ConnectBase", task.getException().getMessage());
                                Picasso.get()
                                        .load(image)
                                        .placeholder(R.drawable.avatar)
                                        .into(ivProfilePic);
                            }
                        });
                    }
                }
                dialog.dismiss();
                for (int i = 0; i < arrayLayout.size(); i++)
                    arrayLayout.get(i).clearFocus();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                dialog.dismiss();
                commonFunctions.showErrorDialog(UpdateProfileActivity.this, databaseError.getMessage());

            }
        });

        mUserReference.keepSynced(true);

    }

    private void checkProfile() {
        arrayLayout.get(0).getEditText().setError(null);
        arrayLayout.get(3).getEditText().setError(null);
        String name = arrayLayout.get(0).getEditText().getText().toString().trim();
        boolean b1 = !name.isEmpty();
        if (!b1)
            arrayLayout.get(0).getEditText().setError("Name cannot be empty!!");
        String mobile = arrayLayout.get(3).getEditText().getText().toString().trim();
        boolean b2 = mobile.length() == 0 || mobile.length() == 10;
        if (!b2)
            arrayLayout.get(3).getEditText().setError("Invalid Mobile Number!!");
        if (b1 && b2) {
            updateProfile();
        }
    }

    private void updateProfile() {

        Map hashmap = new HashMap();
        hashmap.put("name", arrayLayout.get(0).getEditText().getText().toString().trim());
        hashmap.put("age", arrayLayout.get(1).getEditText().getText().toString().trim());
        hashmap.put("qualification", arrayLayout.get(2).getEditText().getText().toString().trim());
        hashmap.put("mobile", arrayLayout.get(3).getEditText().getText().toString().trim());
        hashmap.put("organisation", arrayLayout.get(4).getEditText().getText().toString().trim());
        hashmap.put("position", arrayLayout.get(5).getEditText().getText().toString().trim());
        hashmap.put("skills", arrayLayout.get(6).getEditText().getText().toString().trim());
        hashmap.put("experience", arrayLayout.get(7).getEditText().getText().toString().trim());
        hashmap.put("city", arrayLayout.get(8).getEditText().getText().toString().trim());
        hashmap.put("state", arrayLayout.get(9).getEditText().getText().toString().trim());

        mUserReference.child(currentId).updateChildren(hashmap).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Snackbar.make(relativeLayout, "Profile Updated", Snackbar.LENGTH_SHORT).show();
            } else {
                commonFunctions.showErrorDialog(this, task.getException().getMessage());
            }
        });


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                try {
                    final Uri resultUri = result.getUri();

                    uploadProfilePic(resultUri);

                } catch (Exception e) {
                    commonFunctions.showErrorDialog(this, e.getMessage());
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                commonFunctions.showErrorDialog(this, result.getError().getMessage());
            }
        } else if (requestCode == REQUEST_CODE_PICK_RESUME && resultCode == RESULT_OK) {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE_READ);

            } else {
                Uri uri = data.getData();
                resumeUri = uri;
                uploadResume(uri);

            }
        }
    }

    private void uploadProfilePic(Uri resultUri) throws IOException {

        File file = new File(resultUri.getPath());

        Bitmap bitmap = new Compressor(this)
                .setCompressFormat(Bitmap.CompressFormat.JPEG)
                .setMaxHeight(200)
                .setMaxWidth(200)
                .setQuality(50)
                .compressToBitmap(file);

        final ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteOutputStream);
        final byte[] thumbByte = byteOutputStream.toByteArray();


        final StorageReference profileImageReference = mProfileImageReference.child(currentId + ".jpg");
        final StorageReference bitmapImageReference = mProfileImageReference.child("ThumbImage").child(currentId + ".jpg");
        showDialog("Uploading Image", ProgressDialog.STYLE_HORIZONTAL);

        profileImageReference.putFile(resultUri).addOnCompleteListener(task -> {

            if (task.isSuccessful()) {
                dialog.dismiss();
                profileImageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    final String downloadLink = uri.toString();
                    mUserReference.child(currentId).child("image").setValue(downloadLink).addOnSuccessListener(aVoid -> bitmapImageReference.putBytes(thumbByte).addOnSuccessListener(taskSnapshot -> bitmapImageReference.getDownloadUrl().addOnSuccessListener(uri1 -> mUserReference.child(currentId).child("thumbImage").setValue(uri1.toString()).addOnSuccessListener(aVoid1 -> {
                        sendFileToProfilePicFolder(file);
                        String imagePath = Environment.getExternalStorageDirectory() + "/ConnectBase/ProfilePics/" + currentId + ".jpg";
                        ivProfilePic.setImageBitmap(BitmapFactory.decodeFile(imagePath));

                        Snackbar.make(relativeLayout, "Profile picture Updated", Snackbar.LENGTH_SHORT).show();
                    }))));
                });
            } else {
                dialog.dismiss();
                commonFunctions.showErrorDialog(this, task.getException().getMessage());
            }
        }).addOnProgressListener(taskSnapshot -> {
            dialog.setMax((int) (taskSnapshot.getTotalByteCount() / 1024));
            dialog.setProgress((int) (taskSnapshot.getBytesTransferred() / 1024));
        });
    }

    private void sendFileToProfilePicFolder(File imageFile) {
        String path = "/ConnectBase/ProfilePics/";
        File parentOutput = new File(Environment.getExternalStorageDirectory() + path);
        parentOutput.mkdirs();
        File outputFile = new File(parentOutput, currentId + ".jpg");
        if (outputFile.exists())
            outputFile.delete();

        try {
            InputStream in = new FileInputStream(imageFile);
            OutputStream out = new FileOutputStream(outputFile);
            commonFunctions.copyStream(in, out);
            imageFile.delete();
        } catch (Exception e) {
            commonFunctions.showErrorDialog(this, e.getMessage());
            Log.i("ConnectBase Uri", "Exception");
        }
    }

    void uploadResume(final Uri mainUri) {

        Log.i("ConnectBase Uri", mainUri.toString());
        showDialog("Uploading Resume", ProgressDialog.STYLE_HORIZONTAL);

        final StorageReference myResumeReference = mResumeReference.child(currentId + ".pdf");

        myResumeReference.putFile(mainUri).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                dialog.dismiss();
                myResumeReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadLink = uri.toString();
                    mUserReference.child(currentId).child("resume").setValue(downloadLink).addOnSuccessListener(aVoid -> {
                        sendFileToResumeFolder(mainUri);
                        Snackbar.make(relativeLayout, "Resume Uploaded Successfully!!", Snackbar.LENGTH_SHORT).show();
                    });
                });
            } else {
                dialog.dismiss();
                commonFunctions.showErrorDialog(this, task.getException().getMessage());
            }

        }).addOnProgressListener(taskSnapshot -> {
            dialog.setMax((int) (taskSnapshot.getTotalByteCount() / 1024));
            dialog.setProgress((int) (taskSnapshot.getBytesTransferred() / 1024));
        });

    }

    private void sendFileToResumeFolder(Uri mainUri) {

        File parentFile = new File(Environment.getExternalStorageDirectory() + "/ConnectBase/Resume/");
        parentFile.mkdirs();
        final File outputFile = new File(parentFile, "resume.pdf");

        if (outputFile.exists())
            outputFile.delete();

        try {

            InputStream in = getContentResolver().openInputStream(mainUri);
            OutputStream out = new FileOutputStream(outputFile);
            commonFunctions.copyStream(in, out);
            Log.i("ConnectBase ResumePath", outputFile.getPath());
        } catch (Exception e) {
            commonFunctions.showErrorDialog(this, e.getMessage());
            Log.i("ConnectBase Uri", "Exception");
        }
    }

    void downloadResume() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE_WRITE);
        } else {
            Snackbar.make(relativeLayout, "Downloading File from Server", Snackbar.LENGTH_SHORT).show();

            File parentFile = new File(Environment.getExternalStorageDirectory() + "/ConnectBase/Resume/");
            parentFile.mkdirs();
            final File file = new File(parentFile, "resume.pdf");
            showDialog("Downloading Resume", ProgressDialog.STYLE_HORIZONTAL);
            mResumeReference.child(currentId + ".pdf").getFile(file).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    ivResume.setClickable(true);
                    dialog.dismiss();
                    Snackbar.make(relativeLayout, "File Downloaded Successfully", Snackbar.LENGTH_SHORT).show();
                } else {
                    ivResume.setClickable(true);
                    dialog.dismiss();
                    commonFunctions.showErrorDialog(this, task.getException().getMessage());
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

        switch (requestCode) {
            case REQUEST_CODE_STORAGE_READ:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    uploadResume(resumeUri);
                else

                    Snackbar.make(relativeLayout, "This functionality requires reading external storage permission", Snackbar.LENGTH_SHORT).show();

                break;
            case REQUEST_CODE_STORAGE_WRITE:

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    downloadResume();
                else {
                    ivResume.setClickable(true);
                    Snackbar.make(relativeLayout, "This functionality requires writing external storage permission", Snackbar.LENGTH_SHORT).show();
                }

                break;
        }
    }
}


