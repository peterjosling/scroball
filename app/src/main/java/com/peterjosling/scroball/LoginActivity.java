package com.peterjosling.scroball;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;

/** A login screen that offers login via email/password. */
public class LoginActivity extends AppCompatActivity {

  private ScroballApplication application;

  /** Keep track of the login task to ensure we can cancel it if requested. */
  private Object mAuthTask = null;

  // UI references.
  private EditText mUsernameView;
  private EditText mPasswordView;
  private View mProgressView;
  private View mLoginFormView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);
    // Set up the login form.
    mUsernameView = findViewById(R.id.username);

    mPasswordView = findViewById(R.id.password);
    mPasswordView.setOnEditorActionListener(
        (textView, id, keyEvent) -> {
          if (id == R.id.login || id == EditorInfo.IME_NULL) {
            attemptLogin();
            return true;
          }

          return false;
        });

    Button mEmailSignInButton = findViewById(R.id.email_sign_in_button);
    mEmailSignInButton.setOnClickListener(view -> attemptLogin());

    mLoginFormView = findViewById(R.id.login_form);
    mProgressView = findViewById(R.id.login_progress);

    application = (ScroballApplication) getApplication();
  }

  /**
   * Attempts to sign in or register the account specified by the login form. If there are form
   * errors (invalid email, missing fields, etc.), the errors are presented and no actual login
   * attempt is made.
   */
  private void attemptLogin() {
    // TODO this stops multiple attempts. Fix it.
    if (mAuthTask != null) {
      return;
    }

    // Reset errors.
    mUsernameView.setError(null);
    mPasswordView.setError(null);

    // Store values at the time of the login attempt.
    String username = mUsernameView.getText().toString();
    String password = mPasswordView.getText().toString();

    boolean cancel = false;
    View focusView = null;

    // Check for a valid password, if the user entered one.
    if (TextUtils.isEmpty(password)) {
      mPasswordView.setError(getString(R.string.error_field_required));
      focusView = mPasswordView;
      cancel = true;
    }

    // Check for a valid email address.
    if (TextUtils.isEmpty(username)) {
      mUsernameView.setError(getString(R.string.error_field_required));
      focusView = mUsernameView;
      cancel = true;
    }

    // Check for network connectivity.
    ConnectivityManager connectivityManager =
        (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
    boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

    if (!isConnected) {
      cancel = true;
      focusView = mPasswordView;
      showErrorDialog(getString(R.string.error_internet));
    }

    if (cancel) {
      // There was an error; don't attempt login and focus the first
      // form field with an error.
      focusView.requestFocus();
    } else {
      // Show a progress spinner, and kick off a background task to
      // perform the user login attempt.
      showProgress(true);
      LastfmClient lastfmClient = application.getLastfmClient();
      lastfmClient.authenticate(
          username,
          password,
          message -> {
            mAuthTask = null;
            showProgress(false);

            AuthResult result = (AuthResult) message.obj;

            if (result.sessionKey().isPresent()) {
              SharedPreferences preferences = application.getSharedPreferences();
              SharedPreferences.Editor editor = preferences.edit();
              editor.putString(getString(R.string.saved_session_key), result.sessionKey().get());
              editor.apply();

              Intent intent = new Intent(getApplicationContext(), MainActivity.class);
              intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
              startActivity(intent);
              finish();
            } else if (result.errorCode().or(0) == LastfmClient.ERROR_CODE_AUTH) {
              mPasswordView.setError(getString(R.string.error_incorrect_password));
            } else {
              String errorMessage = result.error().or(getString(R.string.error_unknown));
              showErrorDialog(errorMessage);
            }

            mPasswordView.requestFocus();
            return true;
          });
    }
  }

  /** Shows the progress UI and hides the login form. */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
  private void showProgress(final boolean show) {
    int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
    mLoginFormView
        .animate()
        .setDuration(shortAnimTime)
        .alpha(show ? 0 : 1)
        .setListener(
            new AnimatorListenerAdapter() {
              @Override
              public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
              }
            });

    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
    mProgressView
        .animate()
        .setDuration(shortAnimTime)
        .alpha(show ? 1 : 0)
        .setListener(
            new AnimatorListenerAdapter() {
              @Override
              public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
              }
            });
  }

  private void showErrorDialog(String message) {
    new AlertDialog.Builder(this)
        .setTitle(R.string.error)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok, null)
        .show();
  }
}
