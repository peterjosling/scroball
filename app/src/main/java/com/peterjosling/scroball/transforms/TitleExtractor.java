package com.peterjosling.scroball.transforms;

import com.google.common.base.Joiner;
import com.peterjosling.scroball.ImmutableTrack;
import com.peterjosling.scroball.Track;

import java.util.Arrays;

public class TitleExtractor implements MetadataTransform {

  private static final String[] SEPARATORS =
      new String[]{" -- ", "--", " - ", " – ", " — ", "-", "–", "—", ":", "|", "///"};

  @Override
  public Track transform(Track track) {
    String title = null;
    String artist = null;

    for (String separator : SEPARATORS) {
      String[] components = track.track().split(separator);

      if (components.length > 1) {
        String[] titleComponents = Arrays.copyOfRange(components, 1, components.length);

        artist = components[0];
        title = Joiner.on(separator).join(titleComponents);
        break;
      }
    }

    if (title == null || artist == null) {
      return track;
    }

    title = title.trim();
    artist = artist.trim();

    return ImmutableTrack.builder().from(track).artist(artist).track(title).build();
  }
}
