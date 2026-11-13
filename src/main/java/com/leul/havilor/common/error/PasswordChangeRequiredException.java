package com.leul.havilor.common.error;

// The person still has a temporary password (Flow F30): until they set their own, only /users/me and
// /users/me/password work. The global handler turns this into 403 "Set a new password first".
public class PasswordChangeRequiredException extends RuntimeException {

    public PasswordChangeRequiredException() {
        super("Set a new password first");
    }
}
