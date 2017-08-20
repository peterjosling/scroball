package com.peterjosling.scroball;

import com.google.auto.value.AutoValue;

/** Event which indicates an authentication error has occurred and the user should be logged out. */
@AutoValue
public abstract class AuthErrorEvent {

  public abstract int errorCode();

  public static AuthErrorEvent create(int errorCode) {
    return new AutoValue_AuthErrorEvent(errorCode);
  }
}
