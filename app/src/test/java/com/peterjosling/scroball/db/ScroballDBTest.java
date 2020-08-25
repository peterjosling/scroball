package com.peterjosling.scroball.db;

import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.common.collect.ImmutableList;
import com.peterjosling.scroball.Scrobble;
import com.peterjosling.scroball.ScrobbleStatus;
import com.peterjosling.scroball.Track;
import com.raizlabs.android.dbflow.config.FlowManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@Config(sdk = Build.VERSION_CODES.O_MR1)
@RunWith(AndroidJUnit4.class)
public class ScroballDBTest {

  private ScroballDB scroballDB = new ScroballDB();
  private Track track =
      Track.builder()
          .artist("Artist")
          .track("Track")
          .album("Album")
          .albumArtist("Album artist")
          .build();

  private ScrobbleStatus scrobbleStatus = new ScrobbleStatus(0);
  private Scrobble scrobble =
      Scrobble.builder()
          .track(track)
          .timestamp((int) (System.currentTimeMillis() / 1000))
          .status(scrobbleStatus)
          .build();

  @Before
  public void before() {
    FlowManager.init(RuntimeEnvironment.application);
  }

  @After
  public void after() {
    scroballDB.clear();
    FlowManager.destroy();
  }

  @Test
  public void write_serialisesAllScrobbleData() {
    scroballDB.writeScrobble(scrobble);
    List<Scrobble> scrobbles = scroballDB.readScrobbles();

    assertThat(scrobbles).hasSize(1);
    Scrobble readScrobble = scrobbles.get(0);
    assertThat(readScrobble).isEqualTo(scrobble);
  }

  @Test
  public void write_setsDbIdOnNewEntries() {
    assertThat(scrobble.status().getDbId()).isEqualTo(-1);
    scroballDB.writeScrobble(scrobble);
    assertThat(scrobble.status().getDbId()).isGreaterThan(0L);
  }

  @Test
  public void write_updatesExistingEntries() {
    scroballDB.writeScrobble(scrobble);
    scroballDB.writeScrobble(scrobble);
    assertThat(scroballDB.readScrobbles()).hasSize(1);
  }

  @Test
  public void readPending_readsAllPendingEntries() {
    Scrobble scrobble1 = copyScrobble(scrobble);
    Scrobble scrobble2 = copyScrobble(scrobble);
    Scrobble scrobble3 = copyScrobble(scrobble);
    List<Scrobble> scrobbles = ImmutableList.of(scrobble1, scrobble2, scrobble3);
    scroballDB.writeScrobbles(scrobbles);

    List<Scrobble> pending = scroballDB.readPendingScrobbles();

    assertThat(pending).containsAllIn(scrobbles);
  }

  @Test
  public void readPending_treatsErroredScrobblesAsPending() {
    Scrobble erroredScrobble = scrobble.toBuilder().build();
    erroredScrobble.status().setErrorCode(1);
    scroballDB.writeScrobble(erroredScrobble);

    List<Scrobble> pending = scroballDB.readPendingScrobbles();

    assertThat(pending.size()).isEqualTo(1);
    assertThat(pending.get(0)).isEqualTo(erroredScrobble);
  }

  @Test
  public void readPending_doesNotReadSubmittedScrobbles() {
    Scrobble pendingScrobble = copyScrobble(scrobble);
    Scrobble submittedScrobble = copyScrobble(scrobble);
    submittedScrobble.status().setScrobbled(true);
    List<Scrobble> scrobbles = ImmutableList.of(pendingScrobble, submittedScrobble);
    scroballDB.writeScrobbles(scrobbles);

    List<Scrobble> pending = scroballDB.readPendingScrobbles();

    assertThat(pending.size()).isEqualTo(1);
    assertThat(pending).contains(pendingScrobble);
  }

  private Scrobble copyScrobble(Scrobble input) {
    return input
        .toBuilder()
        .status(new ScrobbleStatus(input.status().getErrorCode(), input.status().getDbId()))
        .build();
  }
}
