package com.peterjosling.scroball;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

public class SplashScreen extends Activity {

  private AlertDialog alertDialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_splash_screen);
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

    if (!ListenerService.isNotificationAccessEnabled) {
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
      // TODO
    } else {
      Intent intent = new Intent(this, LoginActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(intent);
      finish();
    }
  }
}
