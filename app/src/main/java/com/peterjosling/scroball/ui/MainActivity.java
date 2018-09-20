package com.peterjosling.scroball.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
import android.widget.RelativeLayout;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.BillingResponse;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.common.collect.ImmutableList;
import com.peterjosling.scroball.R;
import com.peterjosling.scroball.ScroballApplication;

import java.util.List;

public class MainActivity extends AppCompatActivity implements PurchasesUpdatedListener {

  public static final String EXTRA_INITIAL_TAB = "initial_tab";
  public static final int TAB_NOW_PLAYING = 0;
  public static final int TAB_SCROBBLE_HISTORY = 1;

  private static final String REMOVE_ADS_SKU = "remove_ads";

  private ScroballApplication application;
  private BillingClient billingClient;
  private AdView adView;
  private boolean adsRemoved = false;

  /**
   * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for each of the
   * sections. We use a {@link FragmentPagerAdapter} derivative, which will keep every loaded
   * fragment in memory. If this becomes too memory intensive, it may be best to switch to a {@link
   * android.support.v4.app.FragmentStatePagerAdapter}.
   */
  private SectionsPagerAdapter mSectionsPagerAdapter;

  /** The {@link ViewPager} that will host the section contents. */
  private ViewPager mViewPager;

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

    // Initial tab may have been specified in the intent.
    int initialTab = getIntent().getIntExtra(EXTRA_INITIAL_TAB, TAB_NOW_PLAYING);
    mViewPager.setCurrentItem(initialTab);

    this.adsRemoved = application.getSharedPreferences().getBoolean(REMOVE_ADS_SKU, false);

    adView = findViewById(R.id.adView);
    if (this.adsRemoved) {
      RelativeLayout parent = (RelativeLayout) adView.getParent();
      if (parent != null) {
        parent.removeView(adView);
      }
    } else {
      AdRequest adRequest =
          new AdRequest.Builder().addTestDevice("86193DC9EBC8E1C3873178900C9FCCFC").build();
      adView.loadAd(adRequest);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    billingClient = new BillingClient.Builder(this).setListener(this).build();
    billingClient.startConnection(
        new BillingClientStateListener() {
          @Override
          public void onBillingSetupFinished(@BillingResponse int billingResponseCode) {
            if (billingResponseCode == BillingResponse.OK) {
              Purchase.PurchasesResult purchasesResult =
                  billingClient.queryPurchases(BillingClient.SkuType.INAPP);
              onPurchasesUpdated(
                  purchasesResult.getResponseCode(), purchasesResult.getPurchasesList());
            }
          }

          @Override
          public void onBillingServiceDisconnected() {}
        });
  }

  @Override
  protected void onPause() {
    super.onPause();
    billingClient.endConnection();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);

    // Hide menu item if the IAP has been purchased.
    if (this.adsRemoved) {
      menu.findItem(R.id.remove_ads_item).setVisible(false);
    }
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.settings_item:
        Intent intent = new Intent(getBaseContext(), SettingsActivity.class);
        startActivityForResult(intent, 1);
        return true;
      case R.id.remove_ads_item:
        BillingFlowParams.Builder builder =
            new BillingFlowParams.Builder()
                .setSku(REMOVE_ADS_SKU)
                .setType(BillingClient.SkuType.INAPP);
        int responseCode = billingClient.launchBillingFlow(this, builder.build());
        if (responseCode != BillingResponse.OK) {
          purchaseFailed();
        }
        return true;
      case R.id.privacy_policy_item:
        Intent browserIntent =
            new Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://peterjosling.com/scroball/privacy_policy.html"));
        startActivity(browserIntent);
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
              application.logout();

              Intent intent = new Intent(this, SplashScreen.class);
              intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
              startActivity(intent);
              finish();
            })
        .setNegativeButton(android.R.string.no, null)
        .show();
  }

  @Override
  public void onPurchasesUpdated(int responseCode, List<Purchase> purchases) {
    if (responseCode != BillingResponse.OK) {
      purchaseFailed();
    } else if (purchases != null) {
      for (Purchase purchase : purchases) {
        if (purchase.getSku().equals(REMOVE_ADS_SKU)) {
          RelativeLayout parent = (RelativeLayout) adView.getParent();
          if (parent != null) {
            parent.removeView(adView);
          }
          this.invalidateOptionsMenu();
          this.adsRemoved = true;
          SharedPreferences.Editor editor = application.getSharedPreferences().edit();
          editor.putBoolean(REMOVE_ADS_SKU, true);
          editor.apply();
        }
      }
    }
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
          return getString(R.string.tab_now_playing);
        case 1:
          return getString(R.string.tab_history);
      }
      return null;
    }
  }

  private void purchaseFailed() {
    new AlertDialog.Builder(this)
        .setMessage(R.string.purchase_failed)
        .setPositiveButton(android.R.string.ok, null)
        .create()
        .show();
  }
}
