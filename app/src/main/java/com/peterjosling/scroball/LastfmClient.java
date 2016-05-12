package com.peterjosling.scroball;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.umass.lastfm.*;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleData;
import de.umass.lastfm.scrobble.ScrobbleResult;

public class LastfmClient {

  public static final int ERROR_CODE_AUTH = 4;

  private static final String API_KEY = "e0189dd89bed85023712c63544325558";
  private static final String API_SECRET = "747ea338a0e071b7d3d14c1a64e13567";

  private Session session;

  public LastfmClient(String userAgent, String sessionKey) {
    this(userAgent);
    setSession(sessionKey);
  }

  public LastfmClient(String userAgent) {
    Caller.getInstance().setUserAgent(userAgent);
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

          return ImmutableAuthResult.builder()
              .sessionKey(sessionKey)
              .build();
        }

        Result result = Caller.getInstance().getLastResult();
        ImmutableAuthResult.Builder authResultBuilder = ImmutableAuthResult.builder();
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
        return Track.updateNowPlaying(artist, track, session);
      }

      @Override
      protected void onPostExecute(ScrobbleResult scrobbleResult) {
        // TODO remove
        System.out.println(scrobbleResult);
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
        return Track.scrobble(scrobbleData, session);
      }

      @Override
      protected void onPostExecute(List<ScrobbleResult> results) {
        Message message = Message.obtain();
        message.obj = results;
        callback.handleMessage(message);
        System.out.println(Arrays.toString(results.toArray()));
      }
    }.execute();
  }

  public void getTrackInfo(final String artist, final String track, final Handler.Callback callback) {
    new AsyncTask<Object, Object, Track>() {
      @Override
      protected Track doInBackground(Object... params) {
        return Track.getInfo(artist, track, session.getApiKey());
      }

      @Override
      protected void onPostExecute(Track updatedTrack) {
        Message message = Message.obtain();

        if (updatedTrack != null) {
          message.obj = ImmutableTrack.builder()
              .artist(artist)
              .track(track)
              .duration(updatedTrack.getDuration() * 1000)
              .build();
        }

        callback.handleMessage(message);
      }
    }.execute();
  }

  private void setSession(String sessionKey) {
    session = Session.createSession(API_KEY, API_SECRET, sessionKey);
  }
}
