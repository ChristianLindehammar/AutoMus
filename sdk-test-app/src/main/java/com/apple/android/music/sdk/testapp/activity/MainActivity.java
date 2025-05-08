package com.apple.android.music.sdk.testapp.activity;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.RemoteException;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.view.MenuItem;

import com.apple.android.music.sdk.testapp.R;
import com.apple.android.music.sdk.testapp.fragment.BaseFragment;
import com.apple.android.music.sdk.testapp.fragment.BrowseFragment;
import com.apple.android.music.sdk.testapp.fragment.NowPlayingFragment;
import com.apple.android.music.sdk.testapp.fragment.SettingsFragment;
import com.apple.android.music.sdk.testapp.fragment.UpNextFragment;
import com.apple.android.music.sdk.testapp.util.MediaBrowserHelper;
import com.google.android.material.navigation.NavigationView;

/**
 * Copyright (C) 2017 Apple, Inc. All rights reserved.
 */
public final class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, MediaBrowserHelper.Listener {

    private MediaBrowserHelper mediaBrowserHelper;
    private MediaBrowserCompat mediaBrowser;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationView navigationView;


    public void browseMedia(String title, String parentId) {
        navigateToBrowse(parentId, title, true);
    }


    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        super.onBackPressed();
        final Fragment fragment = getFragmentManager().findFragmentById(R.id.main_container);
        if (fragment != null) {
            final String tag = fragment.getTag();
            if (tag.startsWith(BrowseFragment.TAG)) {
                //bottomNavigationView.getMenu().findItem(R.id.navitem_browse).setChecked(true);
            } else if (NowPlayingFragment.TAG.equals(tag)) {
                //bottomNavigationView.getMenu().findItem(R.id.navitem_now_playing).setChecked(true);
            } else if (SettingsFragment.TAG.equals(tag)) {
                //bottomNavigationView.getMenu().findItem(R.id.navitem_settings).setChecked(true);
            }
        }
    }


    @Override
    public void onMediaBrowserConnected(@NonNull MediaBrowserCompat mediaBrowser) {
        try {
            final MediaControllerCompat mediaController = new MediaControllerCompat(this, mediaBrowser.getSessionToken());
            MediaControllerCompat.setMediaController(this, mediaController);
            BaseFragment fragment = (BaseFragment) getFragmentManager().findFragmentById(R.id.main_container);
            if (fragment != null) {
                fragment.onMediaBrowserConnected(mediaBrowser);
            }
            this.mediaBrowser = mediaBrowser;
        } catch (RemoteException e) {
        }
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.navitem_browse:
                navigateToBrowse(null, null, true);
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            case R.id.navitem_now_playing:
                navigateToNowPlaying(true);
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            case R.id.navitem_up_next:
                navigateToUpNext(true);
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            case R.id.navitem_settings:
                navigateToSettings(true);
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
        }
        return false;
    }


    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        configureNavigation();
        configureAppBar();
        navigateToBrowse(null, null, false);
        mediaBrowserHelper = new MediaBrowserHelper(this, this);
    }


    @Override
    protected void onStart() {
        super.onStart();
        mediaBrowserHelper.connect();
    }


    @Override
    protected void onStop() {
        super.onStop();
        mediaBrowserHelper.disconnect();
    }


    private void configureAppBar() {
        final Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getTitle());
        }

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, 0, 0);
        drawerToggle.setDrawerIndicatorEnabled(true);
        drawerToggle.syncState();
        drawerLayout.addDrawerListener(drawerToggle);
    }


    private void configureNavigation() {
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationView = (NavigationView) drawerLayout.findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);
    }


    private void navigateToBrowse(@Nullable String parentMediaId, String title, boolean addToBackStack) {
        final String tag = BrowseFragment.TAG + parentMediaId;
        BrowseFragment fragment = (BrowseFragment) getFragmentManager().findFragmentByTag(tag);
        if (fragment == null) {
            fragment = BrowseFragment.newInstance(title, parentMediaId);
            if (mediaBrowser != null && mediaBrowser.isConnected()) {
                fragment.onMediaBrowserConnected(mediaBrowser);
            }
        }
        replaceMainFragment(fragment, tag, addToBackStack);
    }


    private void navigateToNowPlaying(boolean addToBackStack) {
        NowPlayingFragment fragment = (NowPlayingFragment) getFragmentManager().findFragmentByTag(NowPlayingFragment.TAG);
        if (fragment == null) {
            fragment = NowPlayingFragment.newInstance();
        }
        replaceMainFragment(fragment, NowPlayingFragment.TAG, addToBackStack);
    }


    private void navigateToSettings(boolean addToBackStack) {
        SettingsFragment fragment = (SettingsFragment) getFragmentManager().findFragmentByTag(SettingsFragment.TAG);
        if (fragment == null) {
            fragment = SettingsFragment.newInstance();
        }
        replaceMainFragment(fragment, SettingsFragment.TAG, addToBackStack);
    }


    private void navigateToUpNext(boolean addToBackStack) {
        UpNextFragment fragment = (UpNextFragment) getFragmentManager().findFragmentByTag(UpNextFragment.TAG);
        if (fragment == null) {
            fragment = UpNextFragment.newInstance();
        }
        replaceMainFragment(fragment, UpNextFragment.TAG, addToBackStack);
    }


    private void replaceMainFragment(BaseFragment fragment, String tag, boolean addToBackStack) {
        final FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.main_container, fragment, tag);
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commit();
    }


}
