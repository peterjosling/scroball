package com.peterjosling.scroball;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.RenamingDelegatingContext;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4.class)
public class ScroballDBTest {

  Context context;
  ScroballDB scroballDB;
  Track track = ImmutableTrack.builder()
      .duration(1)
      .artist("Artist")
      .track("Track")
      .album("Album")
      .albumArtist("Album artist")
      .build();

  ScrobbleStatus scrobbleStatus = new ScrobbleStatus(0);
  Scrobble scrobble = ImmutableScrobble.builder()
      .track(track)
      .timestamp((int) (System.currentTimeMillis() / 1000))
      .status(scrobbleStatus)
      .build();

  @Before
  public void before() throws Exception {
    context = new RenamingDelegatingContext(InstrumentationRegistry.getContext(), "test_");

    scroballDB = new ScroballDB(ScroballDBHelper.getTestInstance(context));
    scroballDB.open();
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
    long id = 5;
    scrobble.status().setDbId(id);
    scroballDB.writeScrobble(scrobble);
    assertThat(scrobble.status().getDbId()).isEqualTo(id);
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
    Scrobble erroredScrobble = ImmutableScrobble.builder().from(scrobble).build();
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
    return ImmutableScrobble.builder().from(input)
        .status(new ScrobbleStatus(input.status().getErrorCode(), input.status().getDbId()))
        .build();
  }
}
