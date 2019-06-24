package com.peterjosling.scroball;

import android.net.ConnectivityManager;

import com.peterjosling.scroball.db.ScroballDB;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class ScrobblerTest {

  private static final Track TRACK =
      Track.builder().track("Track").artist("Artist").duration(3 * 60 * 1000).build();

  @Mock private LastfmClient client;
  @Mock private ScrobbleNotificationManager notificationManager;
  @Mock private ScroballDB db;
  @Mock private ConnectivityManager connectivityManager;
  @Mock private TrackLover trackLover;
  private Scrobbler scrobbler;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    scrobbler = new Scrobbler(client, notificationManager, db, connectivityManager, trackLover);
  }

  @Test
  public void submit_generatesNoScrobblesForShortDuration() {
    PlaybackItem item = new PlaybackItem(TRACK, 0, 1000);
  }
}
