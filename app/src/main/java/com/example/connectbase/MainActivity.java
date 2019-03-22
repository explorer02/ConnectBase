package com.example.connectbase;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity implements FragInviteRequest.CallBacks {

    FirebaseAuth mAuth;
    static String currentId;
    ViewPager viewPager;
    ViewPagerAdapterMain viewPagerAdapter;
    TabLayout tabLayout;
    int REQUEST_CODE_EXTERNAL_STORAGE = 1221;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Connect Base");

        viewPager = findViewById(R.id.viewPager_main);
        viewPagerAdapter = new ViewPagerAdapterMain(getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setCurrentItem(1, true);
        tabLayout = findViewById(R.id.tablayout_main);
        tabLayout.setupWithViewPager(viewPager);
        createDatabase();

    }

    private void createDatabase() {
        SQLiteDatabase chatDatabase = openOrCreateDatabase("chats", MODE_PRIVATE, null);
        chatDatabase.execSQL("create table if not exists user_list('user_id' varchar not null primary key)");
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, StartActivity.class));
            finish();
        } else {
            currentId = mAuth.getUid();
            askPermissions();
            FirebaseDatabase.getInstance().getReference().child("Users").child(currentId).child("online").setValue("true");
        }
    }

    private void askPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_EXTERNAL_STORAGE);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_main_logout:
                logout();
                break;
            case R.id.menu_main_update:
                startActivity(new Intent(this, UpdateProfileActivity.class).putExtra("id", currentId));
                break;
            case R.id.menu_main_discover:
                startActivity(new Intent(this, DiscoverPeopleActivity.class).putExtra("id", currentId));
                break;
        }
        return true;
    }

    private void logout() {
        mAuth.signOut();
        finish();
        //TODO: delete database
        startActivity(new Intent(this, StartActivity.class));
    }

    @Override
    public void changeTabText(String text) {
        tabLayout.getTabAt(0).setText(text);
    }

}
