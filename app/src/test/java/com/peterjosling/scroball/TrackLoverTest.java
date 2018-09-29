package com.peterjosling.scroball;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import com.google.common.collect.ImmutableList;
import com.peterjosling.scroball.db.LovedTracksEntry;
import com.peterjosling.scroball.db.ScroballDB;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class TrackLoverTest {

  @Mock private LastfmClient lastfmClient;
  @Mock private ScroballDB scroballDB;
  @Mock private ConnectivityManager connectivityManager;
  @Mock private NetworkInfo networkInfo;
  @Mock private LovedTracksEntry entry1;
  @Mock private LovedTracksEntry entry2;
  @Mock private Message message;
  private TrackLover trackLover;
  private Track track1 = Track.builder().artist("Artist").track("Track").build();
  private Track track2 = Track.builder().artist("Other Artist").track("Track").build();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    trackLover = new TrackLover(lastfmClient, scroballDB, connectivityManager);

    when(scroballDB.isLoved(any())).thenReturn(false);
    when(connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
    when(networkInfo.isConnectedOrConnecting()).thenReturn(true);
    when(scroballDB.writeLove(eq(track1), anyInt())).thenReturn(entry1);
    when(scroballDB.writeLove(eq(track2), anyInt())).thenReturn(entry2);

    entry1.artist = track1.artist();
    entry1.track = track1.track();
    entry1.status = 0;

    entry2.artist = track2.artist();
    entry2.track = track2.track();
    entry2.status = 0;
  }

  @Test
  public void loveTrack_performsNoActionIfAlreadyLoved() {
    when(scroballDB.isLoved(any())).thenReturn(true);

    trackLover.loveTrack(track1);

    verify(scroballDB, never()).writeLove(any(), anyInt());
    verifyZeroInteractions(lastfmClient);
  }

  @Test
  public void loveTrack_writesPendingLoveToDatabase() {
    trackLover.loveTrack(track1);

    verify(scroballDB).writeLove(track1, 0);
  }

  @Test
  public void loveTrack_callsTheApi() {
    trackLover.loveTrack(track1);

    verify(lastfmClient).loveTrack(eq(track1), any());
  }

  @Test
  public void loveTrack_queuesLikesWhenOffline() {
    when(networkInfo.isConnectedOrConnecting()).thenReturn(false);
    trackLover.loveTrack(track1);
    verify(lastfmClient, never()).loveTrack(any(), any());

    when(networkInfo.isConnectedOrConnecting()).thenReturn(true);
    trackLover.lovePending();
    verify(lastfmClient).loveTrack(eq(track1), any());
  }

  @Test
  public void loveTrack_storesUpdatedStatusOnCompletion() {
    LastfmClient.Result result = LastfmClient.Result.success();
    doAnswer(invocation -> {
      Handler.Callback callback = invocation.getArgument(1);
      message.obj = result;
      callback.handleMessage(message);
      return null;
    }).when(lastfmClient).loveTrack(any(), any());

    trackLover.loveTrack(track1);

    verify(entry1).save();
    assertThat(entry1.status).isEqualTo(result.errorCode());
  }

  @Test
  public void loveTrack_queuesParallelLikeRequests() {
    List<Handler.Callback> callbacks = new ArrayList<>();
    doAnswer(invocation -> {
      callbacks.add(invocation.getArgument(1));
      return null;
    }).when(lastfmClient).loveTrack(any(), any());

    trackLover.loveTrack(track1);
    trackLover.loveTrack(track2);

    assertThat(callbacks.size()).isEqualTo(1);
    verify(lastfmClient).loveTrack(eq(track1), any());
    verifyNoMoreInteractions(lastfmClient);

    message.obj = LastfmClient.Result.success();
    callbacks.get(0).handleMessage(message);

    verify(lastfmClient).loveTrack(eq(track2), any());
  }

  @Test
  public void loveTrack_retriesTransientFailures() {
    doAnswer(invocation -> {
      Handler.Callback callback = invocation.getArgument(1);
      message.obj = LastfmClient.Result.error(LastfmClient.ERROR_UNKNOWN);
      callback.handleMessage(message);
      return null;
    }).doAnswer(invocation -> {
      Handler.Callback callback = invocation.getArgument(1);
      message.obj = LastfmClient.Result.success();
      callback.handleMessage(message);
      return null;
    }).when(lastfmClient).loveTrack(any(), any());

    trackLover.loveTrack(track1);
    trackLover.lovePending();

    verify(lastfmClient, times(2)).loveTrack(eq(track1), any());
  }

  @Test
  public void loveTrack_removesItemFromQueueAfterSubmission() {
    doAnswer(invocation -> {
      Handler.Callback callback = invocation.getArgument(1);
      message.obj = LastfmClient.Result.success();
      callback.handleMessage(message);
      return null;
    }).when(lastfmClient).loveTrack(any(), any());

    trackLover.loveTrack(track1);
    trackLover.lovePending();
    trackLover.lovePending();
    trackLover.lovePending();

    verify(lastfmClient, times(1)).loveTrack(eq(track1), any());
  }

  @Test
  public void loveTrack_submitsPendingItemsFromDatabase() {
    when(scroballDB.readPendingLoves()).thenReturn(ImmutableList.of(entry1, entry2));
    doAnswer(invocation -> {
      Handler.Callback callback = invocation.getArgument(1);
      message.obj = LastfmClient.Result.success();
      callback.handleMessage(message);
      return null;
    }).when(lastfmClient).loveTrack(any(), any());

    // Have to run constructor again to get it to read from the database.
    trackLover = new TrackLover(lastfmClient, scroballDB, connectivityManager);

    trackLover.lovePending();

    verify(lastfmClient).loveTrack(eq(track1), any());
    verify(lastfmClient).loveTrack(eq(track2), any());
  }
}
