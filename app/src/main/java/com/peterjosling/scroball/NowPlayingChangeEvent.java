package com.peterjosling.scroball;

import org.immutables.value.Value;

@Value.Immutable
public interface NowPlayingChangeEvent {

  Track track();
  String source();
}
