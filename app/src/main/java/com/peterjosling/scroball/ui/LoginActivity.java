package com.peterjosling.scroball.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.peterjosling.scroball.AuthResult;
import com.peterjosling.scroball.LastfmClient;
import com.peterjosling.scroball.R;
import com.peterjosling.scroball.ScroballApplication;

/** A login screen that offers login via email/password. */
public class LoginActivity extends AppCompatActivity
    implements GoogleApiClient.ConnectionCallbacks {

  private static final String TAG = LoginActivity.class.getName();
  private static final int RC_SAVE = 1;
  private static final int RC_READ = 3;

  private ScroballApplication application;
  private GoogleApiClient mCredentialsApiClient;
  private Credential mReceivedCredential;
  private boolean mIsResolving;

  /** Keep track of the login task to ensure we can cancel it if requested. */
  private Object mAuthTask = null;

  // UI references.
  private EditText mUsernameView;
  private EditText mPasswordView;
  private Button mLoginButton;
  private ProgressDialog loadingDialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);
    // Set up the login form.
    mUsernameView = findViewById(R.id.username);

    mPasswordView = findViewById(R.id.password);
    mPasswordView.setOnEditorActionListener(
        (textView, id, keyEvent) -> {
          if (id == EditorInfo.IME_ACTION_GO) {
            attemptLogin();
            return true;
          }
          return false;
        });

    mLoginButton = findViewById(R.id.email_sign_in_button);
    mLoginButton.setOnClickListener(view -> attemptLogin());

    application = (ScroballApplication) getApplication();

    mCredentialsApiClient =
        new GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .enableAutoManage(this, null)
            .addApi(Auth.CREDENTIALS_API)
            .build();
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
      doLogin(username, password);
    }
  }

  private void doLogin(String username, String password) {
    // Show a progress spinner, and kick off a background task to
    // perform the user login attempt.
    showProgress();
    LastfmClient lastfmClient = application.getLastfmClient();
    lastfmClient.authenticate(
        username,
        password,
        message -> {
          mAuthTask = null;
          hideProgress();
          disableForm();

          AuthResult result = (AuthResult) message.obj;

          if (result.sessionKey().isPresent()) {
            SharedPreferences preferences = application.getSharedPreferences();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(getString(R.string.saved_session_key), result.sessionKey().get());
            editor.apply();

            Credential credential = new Credential.Builder(username).setPassword(password).build();
            saveCredential(credential);
          } else {
            enableForm();
            if (result.errorCode().or(0) == LastfmClient.ERROR_CODE_AUTH) {
              mPasswordView.setError(getString(R.string.error_incorrect_password));
            } else {
              String errorMessage = result.error().or(getString(R.string.error_unknown));
              showErrorDialog(errorMessage);
            }

            if (mReceivedCredential != null) {
              deleteCredential(mReceivedCredential);
              mReceivedCredential = null;
              requestCredentials();
            }
          }

          mPasswordView.requestFocus();
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

  private void disableForm() {
    mUsernameView.setEnabled(false);
    mPasswordView.setEnabled(false);
    mLoginButton.setEnabled(false);
  }

  private void enableForm() {
    mUsernameView.setEnabled(true);
    mPasswordView.setEnabled(true);
    mLoginButton.setEnabled(true);
  }

  private void goToApp() {
    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    startActivity(intent);
    finish();
  }

  private void saveCredential(Credential credential) {
    Auth.CredentialsApi.save(mCredentialsApiClient, credential)
        .setResultCallback(
            status -> {
              if (status.isSuccess()) {
                Log.d(TAG, "Credential saved");
                goToApp();
              } else {
                Log.d(
                    TAG,
                    "Attempt to save credential failed "
                        + status.getStatusMessage()
                        + " "
                        + status.getStatusCode());
                resolveSmartLockResult(status, RC_SAVE);
              }
            });
  }

  private void deleteCredential(Credential credential) {
    Auth.CredentialsApi.delete(mCredentialsApiClient, credential);
  }

  private void requestCredentials() {
    CredentialRequest request =
        new CredentialRequest.Builder().setSupportsPasswordLogin(true).build();

    Auth.CredentialsApi.request(mCredentialsApiClient, request)
        .setResultCallback(
            credentialRequestResult -> {
              Status status = credentialRequestResult.getStatus();
              if (credentialRequestResult.getStatus().isSuccess()) {
                mReceivedCredential = credentialRequestResult.getCredential();
                doLogin(mReceivedCredential.getId(), mReceivedCredential.getPassword());
              } else if (status.getStatusCode() == CommonStatusCodes.RESOLUTION_REQUIRED) {
                resolveSmartLockResult(status, RC_READ);
              } else if (status.getStatusCode() == CommonStatusCodes.SIGN_IN_REQUIRED) {
                Log.d(TAG, "Sign in required");
              } else {
                Log.w(TAG, "Unrecognized status code: " + status.getStatusCode());
              }
            });
  }

  private void resolveSmartLockResult(Status status, int requestCode) {
    if (mIsResolving) {
      return;
    }

    if (status.hasResolution()) {
      try {
        status.startResolutionForResult(this, requestCode);
        mIsResolving = true;
      } catch (IntentSender.SendIntentException e) {
        Log.e(TAG, "STATUS: Failed to send resolution.", e);
      }
    } else {
      goToApp();
    }
  }

  @Override
  public void onConnected(@Nullable Bundle bundle) {
    Log.d(TAG, "API client connected");
    requestCredentials();
  }

  @Override
  public void onConnectionSuspended(int i) {
    Log.d(TAG, "API client connection suspended");
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    Log.d(TAG, "onActivityResult:" + requestCode + ":" + resultCode + ":" + data);

    if (requestCode == RC_READ) {
      if (resultCode == RESULT_OK) {
        mReceivedCredential = data.getParcelableExtra(Credential.EXTRA_KEY);
        doLogin(mReceivedCredential.getId(), mReceivedCredential.getPassword());
      } else {
        Log.e(TAG, "Credential Read: NOT OK");
      }
    } else if (requestCode == RC_SAVE) {
      Log.d(TAG, "Result code: " + resultCode);
      if (resultCode == RESULT_OK) {
        Log.d(TAG, "Credential Save: OK");
      } else {
        Log.e(TAG, "Credential Save Failed");
      }
      goToApp();
    }
    mIsResolving = false;
  }
}
