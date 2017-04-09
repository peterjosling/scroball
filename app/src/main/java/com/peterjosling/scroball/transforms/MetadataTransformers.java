package com.peterjosling.scroball.transforms;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.peterjosling.scroball.Track;

import java.util.Collection;

public class MetadataTransformers {

  private static final MetadataTransform VIDEO_TITLE_CLEANER = new VideoTitleCleaner();
  private static final MetadataTransform TITLE_EXTRACTOR = new TitleExtractor();

  private static final ImmutableMultimap<String, MetadataTransform> APP_TRANSFORMS =
      ImmutableMultimap.<String, MetadataTransform>builder()
          .putAll(
              "com.google.android.youtube", ImmutableList.of(TITLE_EXTRACTOR, VIDEO_TITLE_CLEANER))
          .build();

  public Track transformForPackageName(String packageName, Track track) {
    Collection<MetadataTransform> transforms = APP_TRANSFORMS.get(packageName);

    for (MetadataTransform transform : transforms) {
      track = transform.transform(track);
    }
    return track;
  }
}
