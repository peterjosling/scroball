package com.peterjosling.scroball;

import org.immutables.value.Value;

@Value.Immutable
public interface ScroballDBUpdateEvent {

  Scrobble scrobble();
}
