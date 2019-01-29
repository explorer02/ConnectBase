package com.example.connectbase;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import id.zelory.compressor.Compressor;

public class UpdateProfileActivity extends AppCompatActivity {

    ArrayList<TextInputLayout>arrayLayout;
    Button btnUpdate;
    ProgressDialog dialog;
    ImageView ivResume,ivUpload;
    String currentId;
    DatabaseReference mUserReference;
    StorageReference mProfileImageReference,mResumeReference;
    final int REQUEST_CODE_PICK_RESUME=101,REQUEST_CODE_STORAGE_READ=201,REQUEST_CODE_STORAGE_WRITE=202;
    SharedPreferences sharedPreferences;

    ImageView ivCamera,ivProfilePic;
    Uri resumeUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile);

        Toolbar toolbar=findViewById(R.id.toolbar_updateProfile);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Update Profile");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        sharedPreferences=getSharedPreferences("data",MODE_PRIVATE);


        currentId=getIntent().getStringExtra("id");
        arrayLayout=new ArrayList<>();
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
        btnUpdate=findViewById(R.id.btn_updateProfile_update);


        ivCamera=findViewById(R.id.iv_updateProfile_camera);
        ivProfilePic=findViewById(R.id.iv_updateProfile_profilePic);
        ivResume=findViewById(R.id.iv_updateProfile_resume);
        ivResume.setVisibility(View.GONE);
        ivUpload=findViewById(R.id.iv_updateProfile_upload);
        mUserReference=FirebaseDatabase.getInstance().getReference().child("Users");
        mProfileImageReference=FirebaseStorage.getInstance().getReference().child("ProfileImage");
        mResumeReference=FirebaseStorage.getInstance().getReference().child("Resume");

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        loadProfile();

        ivCamera.setOnClickListener(v -> CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setMinCropWindowSize(300, 300)
                .setOutputCompressQuality(80)
                .setOutputCompressFormat(Bitmap.CompressFormat.JPEG)
                .setAspectRatio(1, 1)
                .setMaxZoom(5)
                .start(UpdateProfileActivity.this));


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

        ivResume.setOnClickListener(v -> {
            File parentFile = new File(Environment.getExternalStorageDirectory() + "/ConnectBase/Resume/");
            File resumeFile = new File(parentFile, "resume.pdf");

            Log.i("ConnectBase", resumeFile.getPath() + "\t\t\t" + resumeFile.exists());

            if (resumeFile.exists()) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(getUriFromFile(resumeFile), "application/pdf");
                        startActivity(intent);
                    }

                else {
                    Toast.makeText(UpdateProfileActivity.this, "File Not found on your device", Toast.LENGTH_SHORT).show();
                    downloadResume();
                    ivResume.setClickable(false);
                }
        });

    }

    private void showDialog(String message,int style){
        dialog=new ProgressDialog(this);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setProgressStyle(style);
        dialog.show();
    }

    private void loadProfile() {

        showDialog("Please wait while we are loading your profile",ProgressDialog.STYLE_SPINNER);


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

                if(!resume.trim().isEmpty())
                    ivResume.setVisibility(View.VISIBLE);
                else ivResume.setVisibility(View.GONE);

                if (!image.isEmpty()) {
                    File file = new File(Environment.getExternalStorageDirectory() + "ConnectBase/temp/ProfilePics/" + currentId + ".jpg");
                    if (file.exists()) {
                        Log.i("ConnectBase Uri", getUriFromFile(file).toString());
                        ivProfilePic.setImageURI(getUriFromFile(file));
                    } else
                        Picasso.get()
                                .load(image)
                                .placeholder(R.drawable.avatar)
                                .into(ivProfilePic);
                }
                dialog.dismiss();
                for (int i=0;i<arrayLayout.size();i++)
                    arrayLayout.get(i).clearFocus();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                dialog.dismiss();
                showErrorDialog(databaseError.getMessage());

            }
        });


    }

    private void checkProfile() {
        arrayLayout.get(0).getEditText().setError(null);
        arrayLayout.get(3).getEditText().setError(null);
        String name=arrayLayout.get(0).getEditText().getText().toString().trim();
        boolean b1=!name.isEmpty();
        if(!b1)
            arrayLayout.get(0).getEditText().setError("Name cannot be empty!!");
        String mobile=arrayLayout.get(3).getEditText().getText().toString().trim();
        boolean b2=mobile.length()==0||mobile.length()==10;
        if(!b2)
            arrayLayout.get(3).getEditText().setError("Invalid Mobile Number!!");
        if(b1&&b2){
            updateProfile();
        }
    }

    private void updateProfile(){

        Map hashmap=new HashMap();
        hashmap.put("name",arrayLayout.get(0).getEditText().getText().toString().trim());
        hashmap.put("age",arrayLayout.get(1).getEditText().getText().toString().trim());
        hashmap.put("qualification",arrayLayout.get(2).getEditText().getText().toString().trim());
        hashmap.put("mobile",arrayLayout.get(3).getEditText().getText().toString().trim());
        hashmap.put("organisation",arrayLayout.get(4).getEditText().getText().toString().trim());
        hashmap.put("position",arrayLayout.get(5).getEditText().getText().toString().trim());
        hashmap.put("skills",arrayLayout.get(6).getEditText().getText().toString().trim());
        hashmap.put("experience",arrayLayout.get(7).getEditText().getText().toString().trim());
        hashmap.put("city",arrayLayout.get(8).getEditText().getText().toString().trim());
        hashmap.put("state",arrayLayout.get(9).getEditText().getText().toString().trim());

        mUserReference.child(currentId).updateChildren(hashmap).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(UpdateProfileActivity.this, "Profile Updated", Toast.LENGTH_SHORT).show();
            } else {
                showErrorDialog(task.getException().getMessage());
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
                    btnUpdate.setPressed(true);
                    final File file = new File(resultUri.getPath());
                    final Bitmap bitmap = new Compressor(this)
                            .setCompressFormat(Bitmap.CompressFormat.JPEG)
                            .setMaxHeight(200)
                            .setMaxWidth(200)
                            .setQuality(50)
                            .compressToBitmap(file);

                    final ByteArrayOutputStream byteOutputStream=new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG,90,byteOutputStream);
                    final byte[]thumbByte=byteOutputStream.toByteArray();


                    final StorageReference profileImageReference=mProfileImageReference.child(currentId+".jpg");
                    final StorageReference bitmapImageReference=mProfileImageReference.child("ThumbImage").child(currentId+".jpg");
                    showDialog("Uploading Image", ProgressDialog.STYLE_HORIZONTAL);


                    profileImageReference.putFile(resultUri).addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {
                            dialog.dismiss();
                            profileImageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                                final String downloadLink = uri.toString();
                                mUserReference.child(currentId).child("image").setValue(downloadLink).addOnSuccessListener(aVoid -> bitmapImageReference.putBytes(thumbByte).addOnSuccessListener(taskSnapshot -> bitmapImageReference.getDownloadUrl().addOnSuccessListener(uri1 -> mUserReference.child(currentId).child("thumbImage").setValue(uri1.toString()).addOnSuccessListener(aVoid1 -> {
                                    sendFileToProfilePicFolder(file);

                                    File imageFile = new File(Environment.getExternalStorageDirectory() + "ConnectBase/temp/ProfilePics/" + currentId + ".jpg");
                                    if (imageFile.exists())
                                        ivProfilePic.setImageURI(getUriFromFile(imageFile));

                                    /*else
                                    Picasso.get()
                                            .load(downloadLink)
                                            .placeholder(R.drawable.avatar)
                                    .error(R.drawable.avatar)
                                    .into(ivProfilePic);
                                    */

                                    Toast.makeText(UpdateProfileActivity.this, "Profile pic updated", Toast.LENGTH_SHORT).show();
                                }))));
                            });
                        } else {
                            dialog.dismiss();
                            showErrorDialog(task.getException().getMessage());
                        }
                    }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            dialog.setMax((int)(taskSnapshot.getTotalByteCount()/100));
                            dialog.setProgress((int)(taskSnapshot.getBytesTransferred()/100));
                        }
                    });

                }
                catch (Exception e) {
                    showErrorDialog(e.getMessage());
                }
            }
            else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    showErrorDialog(result.getError().getMessage());
                }
            }


        else if(requestCode==REQUEST_CODE_PICK_RESUME&&resultCode==RESULT_OK) {

            if (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},REQUEST_CODE_STORAGE_READ);

            } else {
                Uri uri = data.getData();
                resumeUri=uri;
                uploadResume(uri);

            }
        }

    }

    private void sendFileToProfilePicFolder(File imageFile) {
        String path = "/ConnectBase/temp/ProfilePics/";
        File parentOutput = new File(Environment.getExternalStorageDirectory() + path);
        parentOutput.mkdirs();
        File outputFile = new File(parentOutput, currentId + ".jpg");
        if (outputFile.exists())
            outputFile.delete();
        Log.i("ConnectBase Uri", "Sending file");
        try {

            InputStream in = new FileInputStream(imageFile);
            OutputStream out = new FileOutputStream(outputFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.close();
            imageFile.delete();
            Log.i("ConnectBase Uri", getUriFromFile(outputFile).toString());
        } catch (Exception e) {
            showErrorDialog(e.getMessage());
            Log.i("ConnectBase Uri", "Exception");
        }
    }


    public void showErrorDialog(String message){
        AlertDialog.Builder builder= new AlertDialog.Builder(this);
                builder.setTitle("Oops!!")
                .setMessage(message)
                .setPositiveButton("Ok",null)
                .show();
    }

    void uploadResume(final Uri mainUri){

        Log.i("ConnectBase Uri", mainUri.toString());
        showDialog("Uploading Resume", ProgressDialog.STYLE_HORIZONTAL);

        final StorageReference myResumeReference=mResumeReference.child(currentId+".pdf");

        myResumeReference.putFile(mainUri).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                dialog.dismiss();
                myResumeReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadLink = uri.toString();
                    mUserReference.child(currentId).child("resume").setValue(downloadLink).addOnSuccessListener(aVoid -> {
                        sendFileToResumeFolder(mainUri);
                        Toast.makeText(UpdateProfileActivity.this, "Resume Uploaded Successfully", Toast.LENGTH_SHORT).show();
                    });
                });
            } else {
                dialog.dismiss();
                showErrorDialog(task.getException().getMessage());
            }

        }).addOnProgressListener(taskSnapshot -> {
            dialog.setMax((int) (taskSnapshot.getTotalByteCount() / 1000));
            dialog.setProgress((int) (taskSnapshot.getBytesTransferred() / 1000));
        });

    }

    private void sendFileToResumeFolder(Uri mainUri) {
        File parentFile = new File(Environment.getExternalStorageDirectory() + "/ConnectBase/Resume/");
        parentFile.mkdirs();
        final File outputFile = new File(parentFile, "resume.pdf");
        String path = getRealPathFromUri(mainUri);
        if (path == null) {
            if (outputFile.exists())
                outputFile.delete();
            return;
        }
        File inputFile = new File(path);
        if (outputFile.exists())
            outputFile.delete();

        try {

            InputStream in = new FileInputStream(inputFile);
            OutputStream out = new FileOutputStream(outputFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.close();
            Log.i("ConnectBase Uri", getUriFromFile(outputFile).toString());
        } catch (Exception e) {
            showErrorDialog(e.getMessage());
            Log.i("ConnectBase Uri", "Exception");
        }



    }

    void downloadResume(){
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_CODE_STORAGE_WRITE);
        }
        else {
            Toast.makeText(this, "Downloading file from Server...", Toast.LENGTH_SHORT).show();

            File parentFile = new File(Environment.getExternalStorageDirectory() + "/ConnectBase/Resume/");
            parentFile.mkdirs();
                final File file=new File(parentFile,"resume.pdf");
                showDialog("Downloading Resume", ProgressDialog.STYLE_HORIZONTAL);
            mResumeReference.child(currentId + ".pdf").getFile(file).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    ivResume.setClickable(true);
                    dialog.dismiss();
                    Toast.makeText(UpdateProfileActivity.this, "File downloaded Successfully!!", Toast.LENGTH_SHORT).show();
                } else {
                    ivResume.setClickable(true);
                    dialog.dismiss();
                    showErrorDialog(task.getException().getMessage());
                }

            }).addOnProgressListener(taskSnapshot -> {
                dialog.setMax((int) (taskSnapshot.getTotalByteCount() / 100));
                dialog.setProgress((int) (taskSnapshot.getBytesTransferred() / 100));
                });


        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case REQUEST_CODE_STORAGE_READ:
                if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)
                    uploadResume(resumeUri);
                else
                Toast.makeText(this, "This functionality requires reading exernal storage permission", Toast.LENGTH_SHORT).show();

                break;
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

    private Uri getUriFromFile(File file) {

        if (Build.VERSION.SDK_INT >= 24)
            return FileProvider.getUriForFile(getApplicationContext(), getApplicationContext()
                    .getPackageName() + ".provider", file);
        else return Uri.fromFile(file);

    }

    private String getRealPathFromUri(Uri uri) {

        Cursor cursor = getContentResolver().query(uri, null, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex("_data");
            if (idx < 0)
                return null;
            else {
                return cursor.getString(idx);
            }
        }
        return uri.getPath();

    }
}


