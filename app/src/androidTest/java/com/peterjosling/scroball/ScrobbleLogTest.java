package com.peterjosling.scroball;

import android.content.Context;
import android.support.test.runner.AndroidJUnit4;
import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4.class)
public class ScrobbleLogTest extends AndroidTestCase {

  Context context;
  ScrobbleLog scrobbleLog;
  Track track;
  ScrobbleStatus scrobbleStatus;
  Scrobble scrobble;

  @BeforeClass
  public void beforeClass() throws Exception {
    context = new RenamingDelegatingContext(getContext(), "test_");

    track = ImmutableTrack.builder()
        .duration(1)
        .artist("Artist")
        .track("Track")
        .album("Album")
        .albumArtist("Album artist")
        .build();

    scrobbleStatus = new ScrobbleStatus(0);

    scrobble = ImmutableScrobble.builder()
        .track(track)
        .timestamp((int) (System.currentTimeMillis() / 1000))
        .status(scrobbleStatus)
        .build();
  }

  @Before
  public void before() throws Exception {
    scrobbleLog = new ScrobbleLog(context);
    scrobbleLog.open();
  }

  @Test
  public void write_serialisesAllScrobbleData() {
    scrobbleLog.write(scrobble);
    List<Scrobble> scrobbles = scrobbleLog.read();

    assertThat(scrobbles).hasSize(1);
    Scrobble readScrobble = scrobbles.get(0);
    assertThat(readScrobble).isEqualTo(scrobble);
  }

  @Test
  public void write_setsDbIdOnNewEntries() {
    // TODO
  }

  @Test
  public void write_updatesExistingEntries() {
    // TODO
  }

  @Test
  public void readPending_readsAllPendingEntries() {
    Scrobble[] scrobbles = new Scrobble[]{scrobble, scrobble, scrobble};

    for (Scrobble scrobble : scrobbles) {
      scrobbleLog.write(scrobble);
    }

    List<Scrobble> pending = scrobbleLog.readPending();

    assertThat(pending).hasSize(3);

    for (int i = 0; i < scrobbles.length; i++) {
      assertThat(pending.get(i)).isEqualTo(scrobbles[i]);
    }
  }

  @Test
  public void readPending_treatsErroredScrobblesAsPending() {
    Scrobble erroredScrobble = ImmutableScrobble.builder().from(scrobble).build();
    erroredScrobble.status().setErrorCode(1);
  }

  @Test
  public void readPending_doesNotReadSubmittedScrobbles() {
    // TODO
  }
}
