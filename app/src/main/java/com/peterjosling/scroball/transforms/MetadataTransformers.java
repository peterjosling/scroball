package com.peterjosling.scroball.transforms;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.peterjosling.scroball.Track;

import java.util.Collection;
import java.util.List;

public class MetadataTransformers {

  private static final MetadataTransform VIDEO_TITLE_CLEANER = new VideoTitleCleaner();
  private static final MetadataTransform TITLE_EXTRACTOR = new TitleExtractor();

  private static final List<MetadataTransform> VIDEO_TRANSFORMS =
      ImmutableList.of(TITLE_EXTRACTOR, VIDEO_TITLE_CLEANER);
  private static final ImmutableMultimap<String, MetadataTransform> APP_TRANSFORMS =
      ImmutableMultimap.<String, MetadataTransform>builder()
          .putAll("com.google.android.youtube", VIDEO_TRANSFORMS)
          .putAll("com.google.android.youtube.tv", VIDEO_TRANSFORMS)
          .put("com.pandora.android", new PandoraOfflineArtistCleaner())
          .put("com.sonos.acr", new SonosRoomCleaner())
          .build();

  public Track transformForPackageName(String packageName, Track track) {
    Collection<MetadataTransform> transforms = APP_TRANSFORMS.get(packageName);

    for (MetadataTransform transform : transforms) {
      track = transform.transform(track);
    }
    return track;
  }
}
