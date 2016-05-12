package com.peterjosling.scroball;

import com.google.common.base.Optional;

import org.immutables.value.Value;

@Value.Immutable
public abstract class AuthResult {

  public abstract Optional<String> sessionKey();
  public abstract Optional<Integer> httpErrorCode();
  public abstract Optional<Integer> errorCode();
  public abstract Optional<String> error();
}
