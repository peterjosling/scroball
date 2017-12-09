package com.peterjosling.scroball.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

import com.peterjosling.scroball.AuthResult;
import com.peterjosling.scroball.LastfmClient;
import com.peterjosling.scroball.R;
import com.peterjosling.scroball.ScroballApplication;

/** A login screen that offers login via email/password. */
public class LoginActivity extends AppCompatActivity {

  private ScroballApplication application;

  private ProgressDialog loadingDialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);

    Button mLoginButton = findViewById(R.id.email_sign_in_button);
    mLoginButton.setOnClickListener(view -> getAuthenticationToken());

    application = (ScroballApplication) getApplication();

    Intent intent = getIntent();
    if (Intent.ACTION_VIEW.equals(intent.getAction())) {
      Uri uri = intent.getData();
      if (uri != null && uri.getHost().equals("authenticate")) {
        String token = uri.getQueryParameter("token");
        getSessionFromToken(token);
      }
    }
  }

  /**
   * Returns true if there is an active internet connection. An error dialog will also be displayed
   * if the device is not connected.
   */
  private boolean isConnected() {
    NetworkInfo activeNetwork = null;
    ConnectivityManager connectivityManager =
        (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

    if (connectivityManager != null) {
      activeNetwork = connectivityManager.getActiveNetworkInfo();
    }

    boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    if (!isConnected) {
      showErrorDialog(getString(R.string.error_internet));
    }
    return isConnected;
  }

  /** Kicks off the browser-based login flow. */
  private void getAuthenticationToken() {
    if (!isConnected()) {
      return;
    }

    Intent browserIntent =
        new Intent(Intent.ACTION_VIEW, application.getLastfmClient().getAuthUrl());
    startActivity(browserIntent);
  }

  /** Takes an authentication token from the API and requests a session. */
  private void getSessionFromToken(String token) {
    if (!isConnected()) {
      return;
    }

    showProgress();
    LastfmClient lastfmClient = application.getLastfmClient();
    lastfmClient.getSession(
        token,
        message -> {
          hideProgress();
          AuthResult result = (AuthResult) message.obj;

          if (result.sessionKey().isPresent()) {
            SharedPreferences preferences = application.getSharedPreferences();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(getString(R.string.saved_session_key), result.sessionKey().get());
            editor.apply();
            goToApp();
          } else {
            String errorMessage = result.error().or(getString(R.string.error_unknown));
            showErrorDialog(errorMessage);
          }

          return true;
        });
  }

  /** Shows the modal progress dialog. */
  private void showProgress() {
    loadingDialog = ProgressDialog.show(this, null, getText(R.string.progress_logging_in));
  }

  /** Hides the modal progress dialog. */
  private void hideProgress() {
    loadingDialog.cancel();
  }

  private void showErrorDialog(String message) {
    new AlertDialog.Builder(this)
        .setTitle(R.string.error)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok, null)
        .show();
  }

  private void goToApp() {
    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    startActivity(intent);
    finish();
  }
}
