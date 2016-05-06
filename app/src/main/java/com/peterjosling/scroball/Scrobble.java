package com.peterjosling.scroball;

import org.immutables.value.Value;

@Value.Immutable
public abstract class Scrobble {

  public abstract Track track();
  public abstract int timestamp();

  @Value.Default
  public ScrobbleStatus status() {
    return new ScrobbleStatus(0);
  }
}
