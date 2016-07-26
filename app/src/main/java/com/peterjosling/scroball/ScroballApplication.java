package com.peterjosling.scroball;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ScroballApplication extends Application {

  private LastfmClient lastfmClient;
  private ScroballDB scroballDB;
  private SharedPreferences sharedPreferences;

  @Override
  public void onCreate() {
    super.onCreate();

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    String userAgent = getString(R.string.user_agent);
    String sessionKeyKey = getString(R.string.saved_session_key);

    if (sharedPreferences.contains(sessionKeyKey)) {
      String sessionKey = sharedPreferences.getString(sessionKeyKey, null);
      lastfmClient = new LastfmClient(userAgent, sessionKey);
    } else {
      lastfmClient = new LastfmClient(userAgent);
    }

    scroballDB = new ScroballDB(new ScroballDBHelper(this));
    scroballDB.open();
  }

  public LastfmClient getLastfmClient() {
    return lastfmClient;
  }

  public ScroballDB getScroballDB() {
    return scroballDB;
  }

  public SharedPreferences getSharedPreferences() {
    return sharedPreferences;
  }
}
