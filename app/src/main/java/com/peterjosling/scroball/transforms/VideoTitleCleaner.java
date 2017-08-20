package com.peterjosling.scroball.transforms;

import com.peterjosling.scroball.Track;

public class VideoTitleCleaner implements MetadataTransform {

  private static final String[] REPLACEMENTS =
      new String[] {
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

  @Override
  public Track transform(Track track) {
    String title = track.track();

    for (String replacement : REPLACEMENTS) {
      title = title.replaceAll(replacement, "");
    }
    return track.toBuilder().track(title).build();
  }
}
