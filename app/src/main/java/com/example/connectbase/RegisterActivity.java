package com.example.connectbase;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

import in.shadowfax.proswipebutton.ProSwipeButton;

public class RegisterActivity extends AppCompatActivity {

    TextInputLayout tilName, tilEmail, tilPass, tilConfPass;
    ProSwipeButton swipeRegister;
    FirebaseAuth mAuth;
    DatabaseReference mUserReference;
    CommonFunctions commonFunctions = new CommonFunctions();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Toolbar toolbar = findViewById(R.id.toolbar_register);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Create a new Account");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mAuth = FirebaseAuth.getInstance();
        tilEmail = findViewById(R.id.til_reg_email);
        tilConfPass = findViewById(R.id.til_reg_cpass);
        tilPass = findViewById(R.id.til_reg_pass);
        tilName = findViewById(R.id.til_reg_name);
        swipeRegister = findViewById(R.id.swipe_reg_register);
        swipeRegister.setOnSwipeListener(() -> createAccount());


    }

    public void createAccount() {

        tilName.setError(null);
        tilEmail.setError(null);
        tilPass.setError(null);
        tilConfPass.setError(null);

        String name = tilName.getEditText().getText().toString().trim();
        String email = tilEmail.getEditText().getText().toString().trim();
        String pass = tilPass.getEditText().getText().toString();
        String cpass = tilConfPass.getEditText().getText().toString();

        boolean b1 = !name.isEmpty();
        boolean b2 = !email.isEmpty();
        boolean b3 = email.contains("@") && email.contains(".");
        boolean b4 = pass.length() > 5;
        boolean b5 = cpass.equals(pass);

        if (!b1)
            tilName.setError("Name cannot be empty!!");
        if (!b2)
            tilEmail.setError("Email cannot be empty!!");
        else if (!b3)
            tilEmail.setError("Invalid Email!!");
        if (!b4)
            tilPass.setError("Password length should be atleast 6 characters");
        if (!b5)
            tilConfPass.setError("Password should match with above entered password!!");

        if (b1 && b2 && b3 && b4 && b5) {
            createUserOnFirebase(email, pass);
        } else setButton(false);
    }

    void setButton(final boolean value) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                swipeRegister.showResultIcon(value);
            }
        }, 100);
    }

    private void createUserOnFirebase(final String email, String pass) {

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {

                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                        String uid = currentUser.getUid();

                        mUserReference = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);
                        Map hashMap = new HashMap<>();
                        hashMap.put("name", tilName.getEditText().getText().toString().trim());
                        hashMap.put("age", "");
                        hashMap.put("organisation", "");
                        hashMap.put("position", "");
                        hashMap.put("skills", "");
                        hashMap.put("experience", "");
                        hashMap.put("qualification", "");
                        hashMap.put("image", "");
                        hashMap.put("thumbImage", "");
                        hashMap.put("mobile", "");
                        hashMap.put("email", email);
                        hashMap.put("city", "");
                        hashMap.put("state", "");
                        hashMap.put("resume", "");


                        mUserReference.setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    setButton(true);

                                    startActivity(new Intent(getApplicationContext(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                    finish();

                                } else {

                                    commonFunctions.showErrorDialog(RegisterActivity.this, task.getException().getMessage());

                                }
                            }
                        });
                    } else {
                        commonFunctions.showErrorDialog(RegisterActivity.this, task.getException().getMessage());
                        setButton(false);
                    }
                });
    }

}
