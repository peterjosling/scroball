package com.peterjosling.scroball;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class MetadataExtractorTest {

  MetadataExtractor extractor = new MetadataExtractor();

  @Test
  public void trimsLeadingTrailingWhitespace() {
    String artist = "Some Artist";
    String title = "Some Track (with details)";
    String text = String.format("      %s   -  %s   ", artist, title);

    Track result = extractor.guessTrack(text).get();

    assertThat(result.artist()).isEqualTo(artist);
    assertThat(result.track()).isEqualTo(title);
  }

  @Test
  public void hyphenatedTextShouldBeSupportedWhenOtherSeparatorsPresent() {
    String artist = "Some-artist";
    String title = "Song-title";
    String text = String.format("%s - %s", artist, title);

    Track result = extractor.guessTrack(text).get();

    assertThat(result.artist()).isEqualTo(artist);
    assertThat(result.track()).isEqualTo(title);
  }
}
