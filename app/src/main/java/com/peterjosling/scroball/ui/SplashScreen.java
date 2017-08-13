package com.peterjosling.scroball.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;

import com.peterjosling.scroball.ListenerService;
import com.peterjosling.scroball.R;
import com.peterjosling.scroball.ScroballApplication;

public class SplashScreen extends Activity {

  private AlertDialog alertDialog;
  private ScroballApplication application;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_splash_screen);
    PreferenceManager.setDefaultValues(this, R.xml.pref_notification, false);
    PreferenceManager.setDefaultValues(this, R.xml.pref_players, false);
    application = (ScroballApplication) getApplication();
    enableNotificationAccess();
  }

  @Override
  protected void onResume() {
    super.onResume();
    enableNotificationAccess();
  }

  private void enableNotificationAccess() {
    if (alertDialog != null) {
      alertDialog.dismiss();
    }

    if (!ListenerService.isNotificationAccessEnabled(this)) {
      alertDialog = new AlertDialog.Builder(this)
          .setTitle(R.string.splash_notification_access)
          .setMessage(R.string.splash_notification_access_text)
          .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
            String action;
            if (Build.VERSION.SDK_INT >= 22) {
              action = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS;
            } else {
              action = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
            }
            startActivity(new Intent(action));
          })
          .show();

      return;
    }

    if (application.getLastfmClient().isAuthenticated()) {
      Intent intent = new Intent(this, MainActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(intent);
      finish();
    } else {
      Intent intent = new Intent(this, LoginActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(intent);
      finish();
    }
  }
}
