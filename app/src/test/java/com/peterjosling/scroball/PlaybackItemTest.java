package com.peterjosling.scroball;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class PlaybackItemTest {

  Track track = Track.builder().track("Track").artist("Artist").build();

  @Test
  public void updateAmountPlayed_hasNoEffectWhenNotPlaying() {
    long timestamp = System.currentTimeMillis() - 10 * 1000;
    PlaybackItem playbackItem1 = new PlaybackItem(track, timestamp);
    PlaybackItem playbackItem2 = new PlaybackItem(track, timestamp);

    assertThat(playbackItem1.getAmountPlayed()).isEqualTo(0);
    assertThat(playbackItem2.getAmountPlayed()).isEqualTo(0);

    playbackItem1.updateAmountPlayed();
    playbackItem2.updateAmountPlayed();

    assertThat(playbackItem1.getAmountPlayed()).isEqualTo(0);
    assertThat(playbackItem2.getAmountPlayed()).isEqualTo(0);
  }

  @Test
  public void updateAmountPlayed_updatesWhenPlaying() {
    long delay = 10 * 1000;
    long alreadyPlayed = 2000;
    long startTime = System.currentTimeMillis() - delay;

    PlaybackItem playbackItem1 = new PlaybackItem(track, startTime);
    playbackItem1.startPlaying();

    PlaybackItem playbackItem2 = new PlaybackItem(track, startTime, alreadyPlayed);

    assertThat(playbackItem1.getAmountPlayed()).isEqualTo(0);
    assertThat(playbackItem2.getAmountPlayed()).isEqualTo(alreadyPlayed);

    playbackItem1.stopPlaying();
    playbackItem2.updateAmountPlayed();

    //    assertThat(playbackItem1.getAmountPlayed() / 1000).isEqualTo(delay / 1000);
    //    assertThat(playbackItem2.getAmountPlayed() / 1000).isEqualTo((delay + alreadyPlayed) / 1000);
    // TODO use fake clock to fix this test.
  }

  @Test
  public void updateAmountPlayed_updatesStartTimeToAvoidCountingTwice() {
    // TODO
  }
}
