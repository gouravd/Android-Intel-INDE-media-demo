package com.johnlotito.intelvideotest;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

public class RecentMediaActivity extends AppCompatActivity {

    /**********************************************************
     * Activity Lifecycle Methods
     *********************************************************/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_recent_media);

        configureActionBar();

        if (savedInstanceState == null) {
            loadFragment();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**********************************************************
     * Private Methods
     *********************************************************/

    private void configureActionBar() {
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setTitle(R.string.select_image);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
    }

    private void loadFragment() {
        RecentMediaFragment fragment = (RecentMediaFragment)
                getSupportFragmentManager().findFragmentByTag(RecentMediaFragment.TAG);

        if (fragment == null) {
            fragment = new RecentMediaFragment();
        }

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, fragment, RecentMediaFragment.TAG);
        ft.commit();
    }
}
