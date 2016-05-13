package com.peterjosling.scroball;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class PlaybackItemTest {

  Track track = ImmutableTrack.builder()
      .track("Track")
      .artist("Artist")
      .build();

  @Test
  public void updateAmountPlayed_hasNoEffectWhenNotPlaying() {
    PlaybackItem playbackItem1 = ImmutablePlaybackItem.builder()
        .track(track)
        .timestamp(System.currentTimeMillis() - 10 * 1000)
        .build();

    PlaybackItem playbackItem2 = ImmutablePlaybackItem.builder().from(playbackItem1)
        .playbackStartTime(playbackItem1.timestamp())
        .build();

    assertThat(playbackItem1.amountPlayed()).isEqualTo(0);
    assertThat(playbackItem2.amountPlayed()).isEqualTo(0);

    PlaybackItem modifiedPlaybackItem1 = playbackItem1.updateAmountPlayed();
    PlaybackItem modifiedPlaybackItem2 = playbackItem2.updateAmountPlayed();

    assertThat(modifiedPlaybackItem1.amountPlayed()).isEqualTo(0);
    assertThat(modifiedPlaybackItem2.amountPlayed()).isEqualTo(0);
  }

  @Test
  public void updateAmountPlayed_returnsNewItemWithUpdatedValueWhenPlaying() {
    long delay = 10 * 1000;
    long alreadyPlayed = 2000;
    long startTime = System.currentTimeMillis() - delay;

    PlaybackItem playbackItem1 = ImmutablePlaybackItem.builder()
        .track(track)
        .timestamp(startTime)
        .playbackStartTime(startTime)
        .isPlaying(true)
        .build();

    PlaybackItem playbackItem2 = ImmutablePlaybackItem.builder().from(playbackItem1)
        .amountPlayed(alreadyPlayed)
        .build();

    assertThat(playbackItem1.amountPlayed()).isEqualTo(0);
    assertThat(playbackItem2.amountPlayed()).isEqualTo(alreadyPlayed);

    PlaybackItem modifiedPlaybackItem1 = playbackItem1.updateAmountPlayed();
    PlaybackItem modifiedPlaybackItem2 = playbackItem2.updateAmountPlayed();

    assertThat(modifiedPlaybackItem1.amountPlayed() / 1000).isEqualTo(delay / 1000);
    assertThat(modifiedPlaybackItem2.amountPlayed() / 1000).isEqualTo((delay + alreadyPlayed) / 1000);
  }
}
