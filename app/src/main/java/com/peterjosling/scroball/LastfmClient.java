package com.peterjosling.scroball;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.CallException;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Result;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleData;
import de.umass.lastfm.scrobble.ScrobbleResult;

public class LastfmClient {

  public static final int ERROR_CODE_AUTH = 4;

  private static final String TAG = LastfmClient.class.getName();
  private static final String API_KEY = "17f6f4f55152871370780cd9c0761509";
  private static final String API_SECRET = "99eafa4c2412543f3141505121184b8a";

  private Session session;

  public LastfmClient(String userAgent, String sessionKey) {
    this(userAgent);
    setSession(sessionKey);
  }

  public LastfmClient(String userAgent) {
    Caller caller = Caller.getInstance();
    caller.setUserAgent(userAgent);
    caller.setCache(null);
  }

  public boolean isAuthenticated() {
    return session != null;
  }

  public void authenticate(final String username, final String password, final Handler.Callback callback) {
    new AsyncTask<Void, Void, AuthResult>() {
      @Override
      protected AuthResult doInBackground(Void... voids) {
        Session session = Authenticator.getMobileSession(username, password, API_KEY, API_SECRET);

        if (session != null) {
          String sessionKey = session.getKey();
          setSession(sessionKey);

          return AuthResult.builder()
              .sessionKey(sessionKey)
              .build();
        }

        Result result = Caller.getInstance().getLastResult();
        AuthResult.Builder authResultBuilder = AuthResult.builder();
        int httpErrorCode = result.getHttpErrorCode();
        int errorCode = result.getErrorCode();
        String errorMessage = result.getErrorMessage();

        if (httpErrorCode > -1) {
          authResultBuilder.httpErrorCode(httpErrorCode);
        }

        if (errorCode > -1) {
          authResultBuilder.errorCode(errorCode);
        }

        if (errorMessage != null) {
          authResultBuilder.error(errorMessage);
        }

        return authResultBuilder.build();
      }

      @Override
      protected void onPostExecute(AuthResult authResult) {
        Message message = Message.obtain();
        message.obj = authResult;
        callback.handleMessage(message);
      }
    }.execute();
  }

  public void updateNowPlaying(final String artist, final String track) {
    new AsyncTask<Object, Object, ScrobbleResult>() {
      @Override
      protected ScrobbleResult doInBackground(Object... params) {
        try {
          return Track.updateNowPlaying(artist, track, session);
        } catch (CallException e) {
          Log.e(TAG, "Failed to update now playing status", e);
        }
        return null;
      }

      @Override
      protected void onPostExecute(ScrobbleResult scrobbleResult) {
        if (scrobbleResult != null && scrobbleResult.isSuccessful()) {
          Log.i(TAG, "Now playing status updated");
        } else {
          Log.e(TAG, String.format("Failed to update now playing status: %s", scrobbleResult));
        }
      }
    }.execute();
  }

  public void scrobbleTracks(final List<Scrobble> scrobbles, final Handler.Callback callback) {
    final List<ScrobbleData> scrobbleData = new ArrayList<>();

    for (Scrobble scrobble : scrobbles) {
      com.peterjosling.scroball.Track track = scrobble.track();
      scrobbleData.add(new ScrobbleData(track.artist(), track.track(), scrobble.timestamp()));
    }

    new AsyncTask<Object, Object, List<ScrobbleResult>>() {
      @Override
      protected List<ScrobbleResult> doInBackground(Object... params) {
        try {
          return Track.scrobble(scrobbleData, session);
        } catch (CallException e) {
          Log.e(TAG, "Failed to submit scrobbles", e);
        }
        return ImmutableList.of();
      }

      @Override
      protected void onPostExecute(List<ScrobbleResult> results) {
        Message message = Message.obtain();
        message.obj = results;
        callback.handleMessage(message);
        Log.i(TAG, String.format("Scrobbles submitted: %s", Arrays.toString(results.toArray())));
      }
    }.execute();
  }

  public void getTrackInfo(final String artist, final String track, final Handler.Callback callback) {
    new AsyncTask<Object, Object, Track>() {
      @Override
      protected Track doInBackground(Object... params) {
        try {
          return Track.getInfo(artist, track, session.getApiKey());
        } catch (CallException e) {
          Log.e(TAG, "Failed to fetch track info", e);
        }
        return null;
      }

      @Override
      protected void onPostExecute(Track updatedTrack) {
        Message message = Message.obtain();

        if (updatedTrack != null) {
          message.obj = com.peterjosling.scroball.Track.builder()
              .artist(artist)
              .track(track)
              .duration(updatedTrack.getDuration() * 1000)
              .build();
        }

        callback.handleMessage(message);
      }
    }.execute();
  }

  public void clearSession() {
    session = null;
  }

  private void setSession(String sessionKey) {
    session = Session.createSession(API_KEY, API_SECRET, sessionKey);
  }
}
