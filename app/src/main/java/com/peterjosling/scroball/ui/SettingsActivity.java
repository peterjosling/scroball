package com.peterjosling.scroball.ui;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import com.peterjosling.scroball.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
  /**
   * A preference value change listener that updates the preference's summary
   * to reflect its new value.
   */
  private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
      String stringValue = value.toString();

      if (preference instanceof ListPreference) {
        // For list preferences, look up the correct display value in
        // the preference's 'entries' list.
        ListPreference listPreference = (ListPreference) preference;
        int index = listPreference.findIndexOfValue(stringValue);

        // Set the summary to reflect the new value.
        preference.setSummary(
            index >= 0
                ? listPreference.getEntries()[index]
                : null);
      } else {
        // For all other preferences, set the summary to the value's
        // simple string representation.
        preference.setSummary(stringValue);
      }
      return true;
    }
  };

  /**
   * Helper method to determine if the device has an extra-large screen. For
   * example, 10" tablets are extra-large.
   */
  private static boolean isXLargeTablet(Context context) {
    return (context.getResources().getConfiguration().screenLayout
        & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setupActionBar();
  }

  /**
   * Set up the {@link android.app.ActionBar}, if the API is available.
   */
  private void setupActionBar() {
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      // Show the Up button in the action bar.
      actionBar.setDisplayHomeAsUpEnabled(true);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      super.onBackPressed();
    }

    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean onIsMultiPane() {
    return isXLargeTablet(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public void onBuildHeaders(List<Header> target) {
    loadHeadersFromResource(R.xml.pref_headers, target);
  }

  /**
   * This method stops fragment injection in malicious applications.
   * Make sure to deny any unknown fragments here.
   */
  protected boolean isValidFragment(String fragmentName) {
    return PreferenceFragment.class.getName().equals(fragmentName)
        || PlayersPreferenceFragment.class.getName().equals(fragmentName)
        || NotificationPreferenceFragment.class.getName().equals(fragmentName);
  }

  /**
   * This fragment shows general preferences only. It is used when the
   * activity is showing a two-pane settings UI.
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public static class PlayersPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.pref_players);
      setHasOptionsMenu(true);

      SharedPreferences sharedPreferences = this.getPreferenceManager().getSharedPreferences();
      Set<String> prefKeys = sharedPreferences.getAll().keySet();
      List<String> playerPrefKeys = new ArrayList<>();

      for (String key : prefKeys) {
        if (key.startsWith("player.")) {
          playerPrefKeys.add(key);
        }
      }

      PreferenceScreen root = getPreferenceScreen();
      PackageManager packageManager = root.getContext().getApplicationContext().getPackageManager();

      for (String key : playerPrefKeys) {
        Preference preference = new SwitchPreference(root.getContext());
        String packageName = key.substring(7);
        ApplicationInfo applicationInfo;

        try {
          applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
          SharedPreferences.Editor editor = sharedPreferences.edit();
          editor.remove(key);
          editor.apply();

          continue;
        }

        preference.setTitle(packageManager.getApplicationLabel(applicationInfo));
        preference.setKey(key);
        preference.setIcon(packageManager.getApplicationIcon(applicationInfo));
        root.addPreference(preference);
      }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      int id = item.getItemId();
      if (id == android.R.id.home) {
        startActivity(new Intent(getActivity(), SettingsActivity.class));
        return true;
      }
      return super.onOptionsItemSelected(item);
    }
  }

  /**
   * This fragment shows notification preferences only. It is used when the
   * activity is showing a two-pane settings UI.
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public static class NotificationPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.pref_notification);
      setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      int id = item.getItemId();
      if (id == android.R.id.home) {
        startActivity(new Intent(getActivity(), SettingsActivity.class));
        return true;
      }
      return super.onOptionsItemSelected(item);
    }
  }
}
