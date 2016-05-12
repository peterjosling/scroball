package com.peterjosling.scroball;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

public class ScroballApplication extends Application {

  private LastfmClient lastfmClient;
  private SharedPreferences sharedPreferences;

  @Override
  public void onCreate() {
    super.onCreate();

    sharedPreferences = getSharedPreferences(
        getString(R.string.preference_file_key), Context.MODE_PRIVATE);

    String userAgent = getString(R.string.user_agent);
    String sessionKeyKey = getString(R.string.saved_session_key);

    if (sharedPreferences.contains(sessionKeyKey)) {
      String sessionKey = sharedPreferences.getString(sessionKeyKey, null);
      lastfmClient = new LastfmClient(userAgent, sessionKey);
    } else {
      lastfmClient = new LastfmClient(userAgent);
    }
  }

  public LastfmClient getLastfmClient() {
    return lastfmClient;
  }

  public SharedPreferences getSharedPreferences() {
    return sharedPreferences;
  }
}
