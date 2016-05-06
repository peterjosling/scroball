package com.peterjosling.scroball;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;

import java.util.Arrays;

public class MetadataExtractor {

  private static final String[] SEPARATORS =
      new String[]{" -- ", "--", " - ", " – ", " — ", "-", "–", "—", ":", "|", "///"};
  private static final String[] REPLACEMENTS = new String[]{
      // **NEW**
      "\\s*\\*+\\s?\\S+\\s?\\*+$",
      // [whatever]
      "\\s*\\[[^\\]]+\\]$",
      // (whatever version)
      "(?i)\\s*\\([^\\)]*version\\)$",
      // video extensions
      "(?i)\\s*\\.(avi|wmv|mpg|mpeg|flv|mp4|m4v)$",
      // (LYRIC VIDEO)
      "(?i)\\s*(LYRIC VIDEO\\s*)?(lyric video\\s*)",
      // (Official Track Stream)
      "(?i)\\s*(Official Track Stream*)",
      // (official)? (music)? video
      "(?i)\\s*(of+icial\\s*)?(music\\s*)?video",
      // (official)? (music)? audio
      "(?i)\\s*(of+icial\\s*)?(music\\s*)?audio",
      // (ALBUM TRACK)
      "(?i)\\s*(ALBUM TRACK\\s*)?(album track\\s*)",
      // (Cover Art)
      "(?i)\\s*(COVER ART\\s*)?(Cover Art\\s*)",
      // (official)
      "(?i)\\s*\\(\\s*of+icial\\s*\\)",
      // (1999)
      "(?i)\\s*\\(\\s*[0-9]{4}\\s*\\)",
      // 1080p HD (HQ)
      "\\s+\\(?\\s*(\\d{3,4}p\\s*)?(HD|HQ)\\s*\\)\\s*$",
      // video clip
      "(?i)\\s*video\\s*clip",
      // live
      "(?i)\\s+\\(?live\\)?$",
      // Leftovers after e.g. (official video)
      "\\(+\\s*\\)+",
      // Artist - The new "Track title" featuring someone
      "^(|.*\\s)\"(.*)\"(\\s.*|)$",
      // 'Track title'
      "^(|.*\\s)'(.*)'(\\s.*|)$",
      // Trim leading whitespace and dash
      "^[\\/\\s,:;~\\-\\s\"]+",
      // Trim trailing whitespace and dash. " and ! added because some track names end as
      // {"Some Track" Official Music Video!} and it becomes {"Some Track"!}
      "[\\/\\s,:;~\\-\\s\"\\s!]+$"
  };

  public Optional<Track> guessTrack(String text) {
    if (text == null) {
      return Optional.absent();
    }

    String title = null;
    String artist = null;

    for (String separator : SEPARATORS) {
      String[] components = text.split(separator);

      if (components.length > 1) {
        String[] titleComponents = Arrays.copyOfRange(components, 1, components.length);

        artist = components[0];
        title = Joiner.on(separator).join(titleComponents);
        break;
      }
    }

    if (title == null || artist == null) {
      return Optional.absent();
    }

    title = title.trim();
    artist = artist.trim();

    for (String replacement : REPLACEMENTS) {
      title = title.replaceAll(replacement, "");
    }

    Track track = ImmutableTrack.builder()
        .track(title)
        .artist(artist)
        .build();

    return Optional.of(track);
  }
}
