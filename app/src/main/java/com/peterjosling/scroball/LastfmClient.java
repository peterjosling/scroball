package com.peterjosling.scroball;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.Arrays;
import java.util.List;

import de.umass.lastfm.CallException;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleData;
import de.umass.lastfm.scrobble.ScrobbleResult;

/**
 * Client for accessing the Last.fm API.
 *
 * <p>Acts as a wrapper around the client in {@link de.umass.lastfm}, providing asynchronous methods
 * and transforming between the Scroball and internal API data types.
 */
public class LastfmClient {

  public static final int ERROR_NO_ERROR = 0;
  public static final int ERROR_UNKNOWN = 1;
  public static final int ERROR_OPERATION_FAILED = 8;
  public static final int ERROR_INVALID_SESSION = 9;
  public static final int ERROR_SERVICE_OFFLINE = 11;
  public static final int ERROR_UNAUTHORIZED_TOKEN = 14;
  public static final int ERROR_SERVICE_TEMPORARILY_UNAVAILABLE = 16;

  /**
   * The set of error codes which indicate transient errors, for which requests should be retried.
   */
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

  private final LastfmApi api;
  private final Caller caller;
  private Session session;

  /** Creates a new authenticated client. */
  public LastfmClient(LastfmApi api, Caller caller, String userAgent, String sessionKey) {
    this(api, caller, userAgent);
    setSession(sessionKey);
  }

  /**
   * Creates a new unauthenticated client. The only allowed client method on this instance will be
   * {@link #getSession(String, Handler.Callback)}
   */
  public LastfmClient(LastfmApi api, Caller caller, String userAgent) {
    this.api = api;
    this.caller = caller;
    caller.setUserAgent(userAgent);
    caller.setCache(null);
  }

  /**
   * Returns {@code true} if this client is authenticated and can be used to make calls to the
   * Last.fm API. A client will be authenticated if it was created with {@link
   * LastfmClient(LastfmApi, Caller, String, String)}, or was authenticated after creation with
   * {@link #getSession(String, Handler.Callback)}.
   */
  public boolean isAuthenticated() {
    return session != null;
  }

  /** Returns the URL to redirect users to for browser-based authentication. */
  public Uri getAuthUrl() {
    return Uri.parse(
        "http://www.last.fm/api/auth/?api_key=" + API_KEY + "&cb=scroball://authenticate");
  }

  /**
   * Authenticates with the Last.fm API using the browser-based token authentication, setting up an
   * active session on this client.
   *
   * @param token token received from the Last.fm API through a redirect.
   * @param callback callback which will be called with an {@link AuthResult} as the message
   *     payload.
   */
  public void getSession(String token, Handler.Callback callback) {
    new GetSessionTask(
            api,
            caller,
            message -> {
              AuthResult result = (AuthResult) message.obj;
              if (result.sessionKey().isPresent()) {
                setSession(result.sessionKey().get());
              }
              callback.handleMessage(message);
              return true;
            })
        .execute(token);
  }

  /**
   * Updates the user's Now Playing status on the Last.fm API.
   *
   * @param track the track to take metadata from. Only track and artist will be used.
   * @param callback the callback which will be invoked with the request result, with a {@link
   *     Result} as the message payload.
   */
  public void updateNowPlaying(com.peterjosling.scroball.Track track, Handler.Callback callback) {
    int now = (int) System.currentTimeMillis() / 1000;
    new UpdateNowPlayingTask(api, session, callback).execute(getScrobbleData(track, now));
  }

  /**
   * Submits the specified scrobbles to the Last.fm API for the current user.
   *
   * @param scrobbles the list of scrobbles to submit. Must be 50 or fewer items.
   * @param callback the callback which will be invoked with the results of the submissions, with a
   *     list of {@link Result} as the message payload.
   */
  public void scrobbleTracks(List<Scrobble> scrobbles, Handler.Callback callback) {
    Preconditions.checkArgument(
        scrobbles.size() <= 50, "Cannot submit more than 50 scrobbles at once");
    final ScrobbleData[] scrobbleData = new ScrobbleData[scrobbles.size()];

    for (int i = 0; i < scrobbles.size(); i++) {
      Scrobble scrobble = scrobbles.get(i);
      scrobbleData[i] = getScrobbleData(scrobble.track(), scrobble.timestamp());
    }

    new ScrobbleTracksTask(api, session, callback).execute(scrobbleData);
  }

  public void getTrackInfo(com.peterjosling.scroball.Track track, Handler.Callback callback) {
    new GetTrackInfoTask(session, callback).execute(track);
  }

  /**
   * Loves the specified track on the Last.fm API.
   *
   * @param track the track to take metadata from. Only track and artist will be used.
   * @param callback the callback which will be invoked with the result of the request, with a
   *     {@link Result} as the message payload.
   */
  public void loveTrack(com.peterjosling.scroball.Track track, Handler.Callback callback) {
    new LoveTrackTask(api, session, callback).execute(track);
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
   * Returns {@code true} if the specified {@code errorCode} indicates an authentication error, and
   * the user must reauthenticate.
   */
  public static boolean isAuthenticationError(int errorCode) {
    return errorCode == ERROR_INVALID_SESSION || errorCode == ERROR_UNAUTHORIZED_TOKEN;
  }

  @NonNull
  private ScrobbleData getScrobbleData(com.peterjosling.scroball.Track track, int timestamp) {
    ScrobbleData data = new ScrobbleData(track.artist(), track.track(), timestamp);
    if (track.album().isPresent()) {
      data.setAlbum(track.album().get());
    }
    if (track.albumArtist().isPresent()) {
      data.setAlbumArtist(track.albumArtist().get());
    }
    if (track.duration().isPresent() && track.duration().get() > 0) {
      data.setDuration((int) (track.duration().get() / 1000));
    }
    return data;
  }

  private static class GetSessionTask extends AsyncTask<String, Void, AuthResult> {
    private final LastfmApi api;
    private final Caller caller;
    private final Handler.Callback callback;

    public GetSessionTask(LastfmApi api, Caller caller, Handler.Callback callback) {
      this.api = api;
      this.caller = caller;
      this.callback = callback;
    }

    @Override
    protected AuthResult doInBackground(String... params) {
      String token = params[0];

      Session session = api.getSession(token, API_KEY, API_SECRET);
      if (session != null) {
        return AuthResult.builder().sessionKey(session.getKey()).build();
      }

      de.umass.lastfm.Result result = caller.getLastResult();
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
      extends AsyncTask<ScrobbleData, Object, ScrobbleResult> {
    private final LastfmApi api;
    private final Session session;
    private final Handler.Callback callback;

    public UpdateNowPlayingTask(LastfmApi api, Session session, Handler.Callback callback) {
      this.api = api;
      this.session = session;
      this.callback = callback;
    }

    @Override
    protected ScrobbleResult doInBackground(ScrobbleData... params) {
      ScrobbleData scrobbleData = params[0];
      try {
        return api.updateNowPlaying(scrobbleData, session);
      } catch (CallException e) {
        Log.d(TAG, "Failed to update now playing status", e);
      }
      return null;
    }

    @Override
    protected void onPostExecute(ScrobbleResult scrobbleResult) {
      if (scrobbleResult != null && scrobbleResult.isSuccessful()) {
        Log.d(TAG, "Now playing status updated");
      } else {
        Log.d(TAG, String.format("Failed to update now playing status: %s", scrobbleResult));
      }

      Result result = Result.error(ERROR_UNKNOWN);
      if (scrobbleResult != null) {
        if (scrobbleResult.isSuccessful()) {
          result = Result.success();
        } else {
          int errorCode = scrobbleResult.getErrorCode();
          result = Result.error(errorCode >= 0 ? errorCode : ERROR_UNKNOWN);
        }
      }

      Message message = Message.obtain();
      message.obj = result;
      callback.handleMessage(message);
    }
  }

  private static class ScrobbleTracksTask extends AsyncTask<ScrobbleData, Object, List<Result>> {
    private final LastfmApi api;
    private final Session session;
    private final Handler.Callback callback;

    ScrobbleTracksTask(LastfmApi api, Session session, Handler.Callback callback) {
      this.api = api;
      this.callback = callback;
      this.session = session;
    }

    @Override
    protected List<Result> doInBackground(ScrobbleData... params) {
      try {
        List<ScrobbleResult> results = api.scrobble(ImmutableList.copyOf(params), session);
        ImmutableList.Builder<Result> builder = ImmutableList.builder();

        for (ScrobbleResult result : results) {
          if (result.isSuccessful()) {
            builder.add(Result.success());
          } else {
            int errorCode = result.getErrorCode();
            builder.add(Result.error(errorCode >= 0 ? errorCode : ERROR_UNKNOWN));
          }
        }
        return builder.build();
      } catch (CallException e) {
        Log.d(TAG, "Failed to submit scrobbles", e);
      }

      ImmutableList.Builder<Result> results = ImmutableList.builder();
      for (ScrobbleData p : params) {
        results.add(Result.error(ERROR_UNKNOWN));
      }
      return results.build();
    }

    @Override
    protected void onPostExecute(List<Result> results) {
      Message message = Message.obtain();
      message.obj = results;
      callback.handleMessage(message);
      Log.d(TAG, String.format("Scrobbles submitted: %s", Arrays.toString(results.toArray())));
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
        Log.d(TAG, "Failed to fetch track info", e);
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
        } else if (updatedTrack.getAlbum() != null) {
          builder.album(updatedTrack.getAlbum());
        }
        message.obj = builder.build();
      }

      callback.handleMessage(message);
    }
  }

  private static class LoveTrackTask
      extends AsyncTask<com.peterjosling.scroball.Track, Object, Result> {
    private final LastfmApi api;
    private final Session session;
    private final Handler.Callback callback;

    public LoveTrackTask(LastfmApi api, Session session, Handler.Callback callback) {
      this.api = api;
      this.session = session;
      this.callback = callback;
    }

    @Override
    protected Result doInBackground(com.peterjosling.scroball.Track... params) {
      com.peterjosling.scroball.Track track = params[0];
      try {
        de.umass.lastfm.Result result = api.love(track.artist(), track.track(), session);
        if (result.isSuccessful()) {
          return Result.success();
        }
        int errorCode = result.getErrorCode();
        return Result.error(errorCode >= 0 ? errorCode : ERROR_UNKNOWN);
      } catch (CallException e) {
        Log.d(TAG, "Failed to fetch track info", e);
      }
      return null;
    }

    @Override
    protected void onPostExecute(Result result) {
      Message message = Message.obtain();
      message.obj = result;
      callback.handleMessage(message);
      Log.d(TAG, String.format("Track loved: %s", result));
    }
  }

  /** Represents the result of an API call. */
  @AutoValue
  public abstract static class Result {

    public abstract int errorCode();

    public boolean isSuccessful() {
      return errorCode() < 0;
    }

    public static Result error(int errorCode) {
      Preconditions.checkArgument(errorCode >= 0, "Negative error codes are not possible");
      return new AutoValue_LastfmClient_Result(errorCode);
    }

    public static Result success() {
      return new AutoValue_LastfmClient_Result(-1);
    }
  }
}
