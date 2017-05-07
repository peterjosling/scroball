package com.peterjosling.scroball.transforms;

import com.peterjosling.scroball.Track;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class MetadataTransformersTest {

  private MetadataTransformers transformers = new MetadataTransformers();

  @Test
  public void transformForPackageName_youtube() {
    String packageName = "com.google.android.youtube";
    Track input1 = Track.builder()
        .track("     Some Artist  -  Track (with details) ")
        .artist("").build();
    Track output1 = Track.builder()
        .track("Track (with details)")
        .artist("Some Artist")
        .build();

    Track input2 = Track.builder()
        .track("   Another Artist  —  Something Music  (Lyric Video) (1080p HD)")
        .artist("")
        .build();
    Track output2 = Track.builder()
        .track("Something Music")
        .artist("Another Artist")
        .build();

    assertThat(transformers.transformForPackageName(packageName, input1)).isEqualTo(output1);
    assertThat(transformers.transformForPackageName(packageName, input2)).isEqualTo(output2);
  }
}
