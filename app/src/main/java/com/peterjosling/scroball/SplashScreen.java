package com.peterjosling.scroball;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class SplashScreen extends Activity {

  private AlertDialog alertDialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_splash_screen);
    PreferenceManager.setDefaultValues(this, R.xml.pref_notification, false);
    PreferenceManager.setDefaultValues(this, R.xml.pref_players, false);
    startService();
  }

  @Override
  protected void onResume() {
    super.onResume();
    startService();
  }

  private void startService() {
    enableNotificationAccess();

    if (ListenerService.isNotificationAccessEnabled(this)) {
      startService(new Intent(this, ListenerService.class));
    }
  }

  private void enableNotificationAccess() {
    if (alertDialog != null) {
      alertDialog.dismiss();
    }

    if (!ListenerService.isNotificationAccessEnabled(this)) {
      alertDialog = new AlertDialog.Builder(this)
          .setTitle(R.string.splash_notification_access)
          .setMessage(R.string.splash_notification_access_text)
          .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
              startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            }
          })
          .show();

      return;
    }

    ScroballApplication application = (ScroballApplication) getApplication();

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
