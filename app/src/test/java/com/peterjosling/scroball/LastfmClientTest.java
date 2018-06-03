package com.peterjosling.scroball;

import android.os.Handler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import de.umass.lastfm.Caller;
import de.umass.lastfm.Result;
import de.umass.lastfm.Session;
import de.umass.lastfm.scrobble.ScrobbleData;
import de.umass.lastfm.scrobble.ScrobbleResult;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class LastfmClientTest {

  /** Copied from {@link LastfmClient#API_KEY}. */
  private static final String API_KEY = "17f6f4f55152871370780cd9c0761509";
  /** Copied from {@link LastfmClient#API_SECRET}. */
  private static final String API_SECRET = "99eafa4c2412543f3141505121184b8a";

  @Mock private LastfmApi lastfmApi;
  @Mock private Caller caller;
  @Mock private Result result;
  @Mock private ScrobbleResult scrobbleResult;
  @Mock private Handler.Callback callback;
  private String sessionKey = "test_key";
  private Session session = Session.createSession(API_KEY, API_SECRET, sessionKey);
  private LastfmClient client;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    client = new LastfmClient(lastfmApi, caller, "test", sessionKey);
  }

  @Test
  public void isAuthenticated_falseWhenNoSession() {
    LastfmClient client = new LastfmClient(lastfmApi, caller, "test");
    assertThat(client.isAuthenticated()).isFalse();
  }

  @Test
  public void isAuthenticated_trueWhenCreatedWithSessionKey() {
    client = new LastfmClient(lastfmApi, caller, "test", "testkey");
    assertThat(client.isAuthenticated()).isTrue();
  }

  @Test
  public void getSession_setsSessionOnSuccess() {
    String token = "token";
    String sessionKey = "sessionKey";

    when(lastfmApi.getSession(any(), any(), any()))
        .thenReturn(Session.createSession(API_KEY, API_SECRET, sessionKey));
    AuthResult expectedAuthResult = AuthResult.builder().sessionKey(sessionKey).build();

    client = new LastfmClient(lastfmApi, caller, "test");
    client.getSession(token, callback);

    assertThat(client.isAuthenticated()).isTrue();
    verify(lastfmApi).getSession(eq(token), eq(API_KEY), eq(API_SECRET));
    verify(callback).handleMessage(argThat(message -> expectedAuthResult.equals(message.obj)));
  }

  @Test
  public void getSession_staysUnauthenticatedOnFailure() {
    when(lastfmApi.getSession(any(), any(), any())).thenReturn(null);
    when(caller.getLastResult()).thenReturn(result);
    when(result.isSuccessful()).thenReturn(false);

    client = new LastfmClient(lastfmApi, caller, "test");
    client.getSession("abc", callback);

    assertThat(client.isAuthenticated()).isFalse();
  }

  @Test
  public void getSession_returnsErrorResultData() {
    int errorCode = 123;
    int httpErrorCode = 456;
    String error = "error";
    AuthResult expectedResult =
        AuthResult.builder().errorCode(errorCode).httpErrorCode(httpErrorCode).error(error).build();

    when(lastfmApi.getSession(any(), any(), any())).thenReturn(null);
    when(caller.getLastResult()).thenReturn(result);
    when(result.isSuccessful()).thenReturn(false);
    when(result.getErrorCode()).thenReturn(errorCode);
    when(result.getHttpErrorCode()).thenReturn(httpErrorCode);
    when(result.getErrorMessage()).thenReturn(error);

    client = new LastfmClient(lastfmApi, caller, "test");
    client.getSession("abc", callback);

    verify(callback).handleMessage(argThat(message -> expectedResult.equals(message.obj)));
  }

  @Test
  public void updateNowPlaying_returnsResultOnSuccess() {
    String track = "My Track";
    String artist = "Some Artist";

    when(lastfmApi.updateNowPlaying(any(), any())).thenReturn(scrobbleResult);
    when(scrobbleResult.isSuccessful()).thenReturn(true);

    client.updateNowPlaying(Track.builder().track(track).artist(artist).build(), callback);

    ScrobbleData expectedScrobbleData = new ScrobbleData();
    expectedScrobbleData.setTrack(track);
    expectedScrobbleData.setArtist(artist);

    verify(lastfmApi).updateNowPlaying(refEq(expectedScrobbleData, "timestamp"), refEq(session));
    verify(callback)
        .handleMessage(argThat(message -> ((LastfmClient.Result) message.obj).isSuccessful()));
  }

  @Test
  public void updateNowPlaying_returnsResultOnNullResponse() {
    String track = "My Track";
    String artist = "Some Artist";

    when(lastfmApi.updateNowPlaying(any(), any())).thenReturn(null);

    client.updateNowPlaying(Track.builder().track(track).artist(artist).build(), callback);

    verify(callback)
        .handleMessage(argThat(message -> !((LastfmClient.Result) message.obj).isSuccessful()));
  }

  @Test
  public void updateNowPlaying_returnsResultOnError() {
    String track = "My Track";
    String artist = "Some Artist";

    when(lastfmApi.updateNowPlaying(any(), any())).thenReturn(scrobbleResult);
    when(scrobbleResult.isSuccessful()).thenReturn(false);
    when(scrobbleResult.getErrorCode()).thenReturn(LastfmClient.ERROR_OPERATION_FAILED);

    client.updateNowPlaying(Track.builder().track(track).artist(artist).build(), callback);

    verify(callback)
        .handleMessage(argThat(message -> !((LastfmClient.Result) message.obj).isSuccessful()));
  }
}
