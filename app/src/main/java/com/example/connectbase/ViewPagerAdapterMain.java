package com.example.connectbase;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class ViewPagerAdapterMain extends FragmentPagerAdapter {

    private final int noOfTabs = 4;

    ViewPagerAdapterMain(FragmentManager fm) {
        super(fm);
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return "Invites";
            case 1:
                return "Chats";
            case 2:
                return "Contacts";
            case 3:
                return "Bookmarks";
            default:
                return null;
        }
    }

    @Override
    public Fragment getItem(int i) {
        switch (i) {
            case 0:
                return new FragInviteRequest();
            case 1:
                return new FragChat();
            case 2:
                return new FragFriends();
            case 3:
                return new FragBookmark();
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return noOfTabs;
    }
}
