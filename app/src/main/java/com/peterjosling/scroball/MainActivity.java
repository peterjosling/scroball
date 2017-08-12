package com.peterjosling.scroball;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.common.collect.ImmutableList;

import java.util.List;

public class MainActivity extends AppCompatActivity {

  private ScroballApplication application;

  /**
   * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for each of the
   * sections. We use a {@link FragmentPagerAdapter} derivative, which will keep every loaded
   * fragment in memory. If this becomes too memory intensive, it may be best to switch to a {@link
   * android.support.v4.app.FragmentStatePagerAdapter}.
   */
  private SectionsPagerAdapter mSectionsPagerAdapter;

  /** The {@link ViewPager} that will host the section contents. */
  private ViewPager mViewPager;

  private GoogleApiClient mGoogleApiClient;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    application = (ScroballApplication) getApplication();
    application.startListenerService();

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    // Create the adapter that will return a fragment for each of the three
    // primary sections of the activity.
    mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

    // Set up the ViewPager with the sections adapter.
    mViewPager = findViewById(R.id.container);
    mViewPager.setAdapter(mSectionsPagerAdapter);

    TabLayout tabLayout = findViewById(R.id.tabs);
    tabLayout.setupWithViewPager(mViewPager);

    mGoogleApiClient =
        new GoogleApiClient.Builder(this)
            .enableAutoManage(this, 0, null)
            .addApi(Auth.CREDENTIALS_API)
            .build();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.settings_item:
        Intent intent = new Intent(getBaseContext(), SettingsActivity.class);
        startActivityForResult(intent, 1);
        return true;
      case R.id.logout_item:
        logout();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  public void logout() {
    new AlertDialog.Builder(this)
        .setTitle(R.string.are_you_sure)
        .setMessage(R.string.logout_confirm)
        .setPositiveButton(
            android.R.string.yes,
            (dialog, whichButton) -> {
              SharedPreferences preferences = application.getSharedPreferences();
              SharedPreferences.Editor editor = preferences.edit();
              editor.remove(getString(R.string.saved_session_key));
              editor.apply();

              application.getScroballDB().clear();
              application.getLastfmClient().clearSession();
              Auth.CredentialsApi.disableAutoSignIn(mGoogleApiClient);

              Intent intent = new Intent(this, SplashScreen.class);
              intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
              startActivity(intent);
              finish();
            })
        .setNegativeButton(android.R.string.no, null)
        .show();
  }

  /**
   * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the
   * sections/tabs/pages.
   */
  public class SectionsPagerAdapter extends FragmentPagerAdapter {

    List<Fragment> fragments =
        ImmutableList.of(new NowPlayingFragment(), new ScrobbleHistoryFragment());

    public SectionsPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int position) {
      // getItem is called to instantiate the fragment for the given page.
      // Return a PlaceholderFragment (defined as a static inner class below).
      return fragments.get(position);
    }

    @Override
    public int getCount() {
      return fragments.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
      switch (position) {
        case 0:
          return "Now Playing";
        case 1:
          return "History";
      }
      return null;
    }
  }
}
