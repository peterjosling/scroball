package com.peterjosling.scroball.transforms;

import com.peterjosling.scroball.ImmutableTrack;
import com.peterjosling.scroball.Track;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class VideoTitleCleanerTest {

  VideoTitleCleaner transform = new VideoTitleCleaner();

  @Test
  public void trimsLeadingTrailingWhitespace() {
    String title = "Some Track (with details)";
    String input = String.format("  %s   ", title);

    Track result = transform.transform(ImmutableTrack.builder().track(input).artist("").build());

    assertThat(result.track()).isEqualTo(title);
  }
}
