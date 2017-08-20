package com.peterjosling.scroball;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

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

  public static final int ERROR_NO_ERROR = 0;
  public static final int ERROR_UNKNOWN = 1;
  public static final int ERROR_AUTH_FAILED = 4;
  public static final int ERROR_OPERATION_FAILED = 8;
  public static final int ERROR_INVALID_SESSION = 9;
  public static final int ERROR_SERVICE_OFFLINE = 11;
  public static final int ERROR_UNAUTHORIZED_TOKEN = 14;
  public static final int ERROR_SERVICE_TEMPORARILY_UNAVAILABLE = 16;
  public static final ImmutableSet<Integer> TRANSIENT_ERROR_CODES =
      ImmutableSet.of(
          ERROR_NO_ERROR,
          ERROR_UNKNOWN,
          ERROR_OPERATION_FAILED,
          ERROR_SERVICE_OFFLINE,
          ERROR_SERVICE_TEMPORARILY_UNAVAILABLE);

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

  public void authenticate(String username, String password, Handler.Callback callback) {
    new AuthenticateTask(
            message -> {
              AuthResult result = (AuthResult) message.obj;
              if (result.sessionKey().isPresent()) {
                setSession(result.sessionKey().get());
              }
              callback.handleMessage(message);
              return true;
            })
        .execute(AuthRequest.create(username, password));
  }

  public void updateNowPlaying(com.peterjosling.scroball.Track track, Handler.Callback callback) {
    new UpdateNowPlayingTask(session, callback).execute(track);
  }

  public void scrobbleTracks(List<Scrobble> scrobbles, Handler.Callback callback) {
    final ScrobbleData[] scrobbleData = new ScrobbleData[scrobbles.size()];

    for (int i = 0; i < scrobbles.size(); i++) {
      Scrobble scrobble = scrobbles.get(i);
      com.peterjosling.scroball.Track track = scrobble.track();
      scrobbleData[i] = new ScrobbleData(track.artist(), track.track(), scrobble.timestamp());
    }

    new ScrobbleTracksTask(session, callback).execute(scrobbleData);
  }

  public void getTrackInfo(com.peterjosling.scroball.Track track, Handler.Callback callback) {
    new GetTrackInfoTask(session, callback).execute(track);
  }

  public void clearSession() {
    session = null;
  }

  private void setSession(String sessionKey) {
    session = Session.createSession(API_KEY, API_SECRET, sessionKey);
  }

  /**
   * Returns {@code true} if the specified {@code errorCode} is transient, and the request should be
   * retried.
   */
  public static boolean isTransientError(int errorCode) {
    return TRANSIENT_ERROR_CODES.contains(errorCode);
  }

  /**
   * Returns {@code true} if the specified {@code errorCode} indicates an authentication error,
   * and the user must reauthenticate.
   */
  public static boolean isAuthenticationError(int errorCode) {
    return errorCode == ERROR_INVALID_SESSION || errorCode == ERROR_UNAUTHORIZED_TOKEN;
  }

  private static class AuthenticateTask extends AsyncTask<AuthRequest, Void, AuthResult> {
    private final Handler.Callback callback;

    public AuthenticateTask(Handler.Callback callback) {
      this.callback = callback;
    }

    @Override
    protected AuthResult doInBackground(AuthRequest... params) {
      AuthRequest request = params[0];
      Session session =
          Authenticator.getMobileSession(
              request.username(), request.password(), API_KEY, API_SECRET);

      if (session != null) {
        return AuthResult.builder().sessionKey(session.getKey()).build();
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
  }

  private static class UpdateNowPlayingTask
      extends AsyncTask<com.peterjosling.scroball.Track, Object, ScrobbleResult> {
    private final Session session;
    private final Handler.Callback callback;

    public UpdateNowPlayingTask(Session session, Handler.Callback callback) {
      this.session = session;
      this.callback = callback;
    }

    @Override
    protected ScrobbleResult doInBackground(com.peterjosling.scroball.Track... params) {
      com.peterjosling.scroball.Track track = params[0];
      try {
        return Track.updateNowPlaying(track.artist(), track.track(), session);
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

      Message message = Message.obtain();
      message.obj = scrobbleResult;
      callback.handleMessage(message);
    }
  }

  private static class ScrobbleTracksTask
      extends AsyncTask<ScrobbleData, Object, List<ScrobbleResult>> {
    private final Session session;
    private final Handler.Callback callback;

    ScrobbleTracksTask(Session session, Handler.Callback callback) {
      this.callback = callback;
      this.session = session;
    }

    @Override
    protected List<ScrobbleResult> doInBackground(ScrobbleData... params) {
      try {
        return Track.scrobble(ImmutableList.copyOf(params), session);
      } catch (CallException e) {
        Log.e(TAG, "Failed to submit scrobbles", e);
      }

      ArrayList<ScrobbleResult> output = new ArrayList<>();
      for (int i = 0; i < params.length; i++) {
        output.add(null);
      }
      return ImmutableList.copyOf(output);
    }

    @Override
    protected void onPostExecute(List<ScrobbleResult> results) {
      Message message = Message.obtain();
      message.obj = results;
      callback.handleMessage(message);
      Log.i(TAG, String.format("Scrobbles submitted: %s", Arrays.toString(results.toArray())));
    }
  }

  private static class GetTrackInfoTask
      extends AsyncTask<com.peterjosling.scroball.Track, Object, Track> {
    private final Session session;
    private final Handler.Callback callback;
    private com.peterjosling.scroball.Track track;

    public GetTrackInfoTask(Session session, Handler.Callback callback) {
      this.session = session;
      this.callback = callback;
    }

    @Override
    protected Track doInBackground(com.peterjosling.scroball.Track... params) {
      track = params[0];
      try {
        return Track.getInfo(track.artist(), track.track(), session.getApiKey());
      } catch (CallException e) {
        Log.e(TAG, "Failed to fetch track info", e);
      }
      return null;
    }

    @Override
    protected void onPostExecute(Track updatedTrack) {
      Message message = Message.obtain();

      if (updatedTrack != null) {
        com.peterjosling.scroball.Track.Builder builder =
            com.peterjosling.scroball.Track.builder()
                .artist(track.artist())
                .track(track.track())
                .duration(updatedTrack.getDuration() * 1000);

        if (track.album().isPresent()) {
          builder.album(track.album().get());
        } else {
          builder.album(updatedTrack.getAlbum());
        }
        message.obj = builder.build();
      }

      callback.handleMessage(message);
    }
  }
}
