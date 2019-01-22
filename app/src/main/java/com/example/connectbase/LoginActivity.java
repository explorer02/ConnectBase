package com.example.connectbase;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import in.shadowfax.proswipebutton.ProSwipeButton;

public class LoginActivity extends AppCompatActivity {

    TextInputLayout tilEmail,tilPass;
    private DatabaseReference mUserReference;
    ProSwipeButton btnSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        android.support.v7.widget.Toolbar toolbar=findViewById(R.id.toolbar_login);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Sign In");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mUserReference=FirebaseDatabase.getInstance().getReference().child("Users");
        tilEmail=findViewById(R.id.til_login_email);
        tilPass=findViewById(R.id.til_login_pass);

        btnSignIn=findViewById(R.id.btn_login_signIn);
        btnSignIn.setOnSwipeListener(new ProSwipeButton.OnSwipeListener() {
            @Override
            public void onSwipeConfirm() {
                signIn();
            }
        });

    }

    public void signIn(){
        tilEmail.setError(null);
        tilPass.setError(null);

        String email=tilEmail.getEditText().getText().toString().trim();
        String pass=tilPass.getEditText().getText().toString();
        boolean b1=!email.isEmpty();
        boolean b2=email.contains("@")&&email.contains(".");
        boolean b3=!pass.isEmpty();

        if(!b1){
            tilEmail.setError("Email cannot be empty!!");
        }
        if(!b2){
            tilEmail.setError("Invalid Email format!!");
        }
        if(!b3){
            tilPass.setError("Password cannot be empty!!");
        }
        if(b1&&b2&&b3){
            signInFirebase(email, pass);
        }
        else {
            setButton(false);
        }

    }
    void setButton(final boolean value){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                btnSignIn.showResultIcon(value);
            }
        },100);
    }


    private void signInFirebase(final String email, final String pass){

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email,pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    setButton(true);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startActivity(new Intent(getApplicationContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
                            finish();

                        }
                    },500);


                } else {
                    setButton(false);
                    showErrorDialog(task.getException().getMessage());
                }
            }
        });

    }

    private void showErrorDialog(String message) {
        AlertDialog.Builder dialog=new AlertDialog.Builder(this);
        dialog.setTitle("Oops!!");
        dialog.setMessage(message);
        dialog.setPositiveButton("Ok",null);
        dialog.show();
    }
}
